package zs.live.dao.mysql.impl

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import groovy.sql.GroovyRowResult
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import zs.live.common.LiveCommon
import zs.live.dao.mongo.LiveMongodb
import zs.live.dao.mysql.DataBases
import zs.live.dao.mysql.LiveRes
import zs.live.entity.LiveForeshow
import zs.live.entity.LiveRecord
import zs.live.service.QcloudLiveService
import zs.live.utils.DateUtil
import zs.live.utils.Http
import zs.live.utils.ImageUtils
import zs.live.utils.Strings
import zs.live.utils.VerUtils

import java.sql.SQLException
import java.text.SimpleDateFormat

/**
 * Created by Administrator on 2016/7/25.
 */
@Slf4j
@Repository
class LiveResImpl implements LiveRes {

    @Autowired
    DataBases dataBases;
    @Autowired
    LiveMongodb liveMongodb
    @Autowired
    QcloudLiveService qcloudLiveService

    @Value('${live.test.user.list}')
    String testUser

    @Override
    public List getMyFans(long userId, int fansType, int followType, long lastId, int pageSize,String appId) {
        List list = [];
        String sql = "select fans_id, fans_user_id,follow_type,create_time from live_user_fans where user_id=? and fans_type=? ";
        def queryParam = [userId, fansType];
        if (followType != 0) {
            sql = sql + " and follow_type=? ";
            queryParam << followType;
        }
        if (lastId != 0) {
            sql = sql + " and fans_id < ? ";
            queryParam << lastId;
        }
        sql = sql + " order by fans_id desc limit ?";
        queryParam << pageSize;
        dataBases.msqlLiveSlave.eachRow(sql, queryParam) {
            list << [id: it.fans_id, userId: it.fans_user_id, followType: it.follow_type];
        }
        return list;

    }

    @Override
    public int getMyFansCount(long userId, int fansType, int followType, long lastId,String appId) {

        String sql = "select count(1) as count from live_user_fans where user_id=? and fans_type=? ";
        def queryParam = [userId, fansType];
        //等于0是查询所有
        if (followType != 0) {
            sql += "  and follow_type=? ";
            queryParam << followType;
        }
        if (lastId != 0) {
            sql = sql + " and fans_id < ? ";
            queryParam << lastId;
        }
        def ret = dataBases.msqlLiveSlave.firstRow(sql, queryParam);
        if (!ret) {
            return 0;
        } else {
            return ret.count
        }
    }

    @Override
    public List getMyFollowings(long fansUserId, int fansType, int followType, long lastId, int pageSize,String appId) {

        List list = [];
        String sql = "select fans_id, user_id,follow_type,create_time  from live_user_fans where fans_user_id=? and fans_type=? ";
        def queryParam = [fansUserId, fansType];
        if (followType != 0) {
            sql = sql + " and follow_type=? ";
            queryParam << followType;
        }
        if (lastId != 0) {
            sql = sql + " and fans_id < ? ";
            queryParam << lastId;
        }
        sql = sql + " order by fans_id desc limit ?";
        queryParam << pageSize;
        dataBases.msqlLiveSlave.eachRow(sql, queryParam) {
            list << [id: it.fans_id, userId: it.user_id, followType: it.follow_type];
        }
        return list;

    }

    @Override
    public int getMyFollowingsCount(long fansUserId, int fansType, int followType, long lastId,String appId) {
        String sql = "select count(1) as count from live_user_fans where fans_user_id=? and fans_type=? ";
        def queryParam = [fansUserId, fansType];
        //等于0是查询所有
        if (followType != 0) {
            sql += "  and follow_type=? ";
            queryParam << followType;
        }
        if (lastId != 0) {
            sql = sql + " and fans_id < ? ";
            queryParam << lastId;
        }
        def ret = dataBases.msqlLiveSlave.firstRow(sql, queryParam);
        if (!ret) {
            return 0;
        } else {
            return ret.count
        }
    }

    @Override
    public int addFollow(long fromUserId, long toUserId, int fansType,String appId) {
        //return 0: 已经关注，冗余的接口调用
        // return 1: 新增关注成功
        //return 2: 新增关注成功，并且变成互相关注的关系

        //先反向查询
        String sql = "select follow_type from live_user_fans where user_id =? and fans_user_id =? and fans_type=? ";
        def queryParam = [fromUserId, toUserId, fansType];
        log.info("addFollow sql queryParam=>{}",queryParam)
        def result = dataBases.msqlLiveSlave.firstRow(sql, queryParam);

        if (!result) {
            //客户端重复调用会重复插入，会发生异常，因为数据库有惟一索引
            try {
                dataBases.msqlLive.executeInsert("insert into live_user_fans(user_id,fans_user_id,fans_type,follow_type,app_id)  values(?,?,?,1,?)",
                        [toUserId, fromUserId, fansType,appId]);
                //防止两方几乎同时调用产生的数据不一致问题
                dataBases.msqlLive.executeUpdate("delete from live_user_fans where user_id =? and fans_user_id =? and fans_type=? ",
                        [fromUserId, toUserId, fansType]);
            } catch (SQLException e) {
                //发生异常是重复调用本接口
                return 0;
            }
            return 1;
            //已经成功
        } else if (result.follow_type == 2) {

            //已经关注
            return 0;
        } else if (result.follow_type == 1) {

            try {
                dataBases.msqlLive.executeInsert("insert into live_user_fans(user_id,fans_user_id,fans_type,follow_type,app_id)  values(?,?,?,2,?)",
                        [toUserId, fromUserId, fansType,appId]);
                dataBases.msqlLive.executeUpdate("update live_user_fans set follow_type=2 where user_id =? and fans_user_id =? and fans_type=? ",
                    [fromUserId, toUserId, fansType])
            } catch (SQLException e) {

            }

            return 2;
            //关注成功
        }
    }

    @Override
    def getFollowInfoByUserId(long fromUserId, long toUserId, int fansType, String appId) {
        def result = null
        try {
            result = dataBases.msqlLiveSlave.firstRow("SELECT * from live_user_fans where user_id =? and fans_user_id =? and fans_type=? ",[toUserId, fromUserId,1])
        }catch (Exception e){
            e.printStackTrace()
        }
        return result
    }

    @Override
    public int cancelFollow(long fromUserId, long toUserId, int fansType,String appId) {
        //return 0: 本来就没有关注，取消操作无意义是冗余的接口调用
        //return 1: 取消关注成功
        //return 2: 取消关注成功，由互相关注变成被关注
        String sql = "select follow_type from live_user_fans where user_id =? and fans_user_id =? and fans_type=?";
        def queryParam = [toUserId, fromUserId, fansType];
        def result = dataBases.msqlLiveSlave.firstRow(sql, queryParam);

        if (!result) {
            //本来就没有关注，取消操作无意义
            return 0;
        } else if (result.follow_type == 2) {
            //互相关注的情况，先删除一行，再将另一个改成1
            dataBases.msqlLive.executeUpdate("delete from live_user_fans where user_id =? and fans_user_id =? and fans_type=? ",
                    [toUserId, fromUserId, fansType]);
            dataBases.msqlLive.executeUpdate("update live_user_fans set follow_type=1 where user_id =? and fans_user_id =? and fans_type=? ",
                [fromUserId, toUserId, fansType])
            return 2;
        } else if (result.follow_type == 1) {
            dataBases.msqlLive.executeUpdate("delete from live_user_fans where user_id =? and fans_user_id =? and fans_type=? ",
                    [toUserId, fromUserId, fansType]);
            //防止多线程下出错
            dataBases.msqlLive.executeUpdate("delete from live_user_fans where user_id =? and fans_user_id =? and fans_type=? ",
                    [fromUserId, toUserId, fansType]);
            return 1;
        }
    }

    @Override
    public def isFollow(long fromUserId, long toUserId, int fansType,String appId) {
        String sql = "select follow_type from live_user_fans where user_id =? and fans_user_id =? and fans_type=? ";
        def queryParam = [toUserId, fromUserId,fansType];
        def result = dataBases.msqlLiveSlave.firstRow(sql, queryParam);
        if (!result) {
            //行不存在，一定是没有关注
            return 0;
        } else if (result.follow_type == 2) {
            return 2;
        } else if(result.follow_type == 1){
            return 1;
        }
        return 0;
    }

    @Override
    GroovyRowResult findLiveByLiveId(long liveId){
        def rowResult
        try {
            String sql = "select * from live_record where live_id=? "
            rowResult= dataBases.msqlLiveSlave.firstRow(sql, [liveId])
            log.info("rowResul====>"+rowResult)
        }catch (Exception e){
            e.printStackTrace()
        }
        return rowResult
    }

    @Override
    def findLiveByForeshowId(long foreshowId, String appId) {
        def live
        try {
            String sql = "SELECT * FROM live_record WHERE foreshow_id=? "
            live= dataBases.msqlLiveSlave.firstRow(sql, [foreshowId])
        }catch (Exception e){
            e.printStackTrace()
        }
        return live
    }

    @Override
    def fingLiveConfig(String appId, int configId) {
        def result
        String sql = "SELECT value FROM live_config WHERE  config_id=?"
        try {
            dataBases.msqlLiveSlave.eachRow(sql,[configId]){ row ->
                result = Strings.parseJson(row.value)
            }
        }catch (Exception e){
            e.printStackTrace()
        }
        return result
    }

    @Override
    def addLiveConfig(Map map) {
        String sql = "INSERT INTO live_config (name,value,description,pid,app_id) VALUES (?,?,?,?,?)"
        try {
            dataBases.msqlLiveSlave.executeInsert(sql,[map.name,Strings.toJson(map.value),map.description,map.pid as int,map.appId])
        }catch (Exception e){
            e.printStackTrace()
        }
    }

    @Override
    def findLiveBackVedioConfig(String appId){
        def result
        String sql = "SELECT value FROM live_config WHERE name = ? and app_id=? "
        try {
            def config= dataBases.msqlLiveSlave.firstRow(sql, ["playbackVedioConfig",appId])
            if(config){
                result = Strings.parseJson(config.value)
            }
        }catch (Exception e){
            e.printStackTrace()
        }
        return result
    }

    @Override
    def getLiveForeshowListForLive(Map map) {
        def result = []
        String sql = "SELECT * FROM live_foreshow WHERE app_id = ? AND (foreshow_status =0 or foreshow_status =1) AND foreshow_type = 1 ORDER BY sort_num DESC,begin_time ASC"
        try {
            result = dataBases.msqlLiveSlave.rows(sql,[map.appId])
        }catch (Exception e){
            e.printStackTrace()
        }
        return result
    }


    @Override
    def getNotStartedLiveForeshowList(Map map) {
        //按波总要求加入
        Map categoryMap=[appId:map.appId,isHide:1,isDel:1,userId:map.userId]
        List categoryList = []
        findLiveCategroyList(categoryMap).each {
            if ((it.type as int) == 1){
                categoryList << [cat_id:it.cat_id]
            }else {
                categoryList << [cat_id:it.share_cat_id]
            }
        }
        //查询共享的预告id
        def shareForeshowId = this.findShareLiveByAppId(map.appId,1)

        def testUserList = testUser ?  Arrays.asList(testUser.split(",")): [];
        def queryParam = [map.appId,map.userId,map.appId];
        def result = []
        StringBuilder sql=new StringBuilder("select a.* ,b.live_id,b.live_mode,b.pgc_type,c.appoint_id from live_foreshow  a left join live_record b on " +
            " b.foreshow_id=a.foreshow_id left join live_appointment c  on a.foreshow_id=c.foreshow_id and  " +
            " c.app_id=? and c.user_id=?  where a.not_show_in_client!= 1 and  a.foreshow_type !=5 and ( a.foreshow_status=0 or a.foreshow_status=6 and a.begin_time > now() ) ")
           if(VerUtils.toIntVer(map.vc)<VerUtils.toIntVer("5.6"))
               sql.append(" and a.foreshow_type!=3 ")

        if (categoryList){
            if(shareForeshowId){
                sql.append(" AND ( a.cate_id in (${Strings.toListString(categoryList*.cat_id)}) or (a.cate_id=0 and a.app_id=? ) " +
                    "or a.foreshow_id in(${Strings.toListString(shareForeshowId*.extend_id)}) )")
            }else{
                sql.append(" AND ( a.cate_id in (${Strings.toListString(categoryList*.cat_id)}) or (a.cate_id=0 and a.app_id=? ) )")
            }
        }else {
            if(shareForeshowId){
                sql.append(" AND (a.app_id =? or a.foreshow_id in(${Strings.toListString(shareForeshowId*.extend_id)})) ")
            }else{
                sql.append(" AND a.app_id =? ")
            }
        }


        if(map.srpId){  //康总加入的逻辑
            sql=new StringBuilder("select a.* ,b.live_id,b.live_mode,b.pgc_type,c.appoint_id from live_foreshow  a inner join live_foreshow_srp_relation s on a.foreshow_id = s.foreshow_id left join live_record b on " +
                " b.foreshow_id=a.foreshow_id left join live_appointment c  on a.foreshow_id=c.foreshow_id and  " +
                " c.app_id=? and c.user_id=?  where a.not_show_in_client!= 1 and s.srp_id= ? and a.foreshow_type !=5  and ( a.foreshow_status=0 or a.foreshow_status=6 and a.begin_time > now() ) ")
            if(VerUtils.toIntVer(map.vc)<VerUtils.toIntVer("5.6"))
                sql.append(" and a.foreshow_type!=3 ")

            queryParam = [map.appId,map.userId,map.srpId,map.appId];
            if (categoryList){
                if(shareForeshowId){
                    sql.append(" AND ( a.cate_id in (${Strings.toListString(categoryList*.cat_id)}) or (a.cate_id=0 and a.app_id=? ) " +
                        "or a.foreshow_id in(${Strings.toListString(shareForeshowId*.extend_id)}) )")
                }else{
                    sql.append(" AND ( a.cate_id in (${Strings.toListString(categoryList*.cat_id)}) or (a.cate_id=0 and a.app_id=? ) )")
                }
            }else {
                if(shareForeshowId){
                    sql.append(" AND (a.app_id =? or a.foreshow_id in(${Strings.toListString(shareForeshowId*.extend_id)})) ")
                }else{
                    sql.append(" AND a.app_id =? ")
                }
            }

        }
     //   StringBuilder sql =  new StringBuilder("select * from live_foreshow where app_id = ?  and foreshow_type !=5 and foreshow_status=0  ");
     //   queryParam <<System.currentTimeMillis()/1000;
        if(!testUserList || !testUserList.contains(map.userId as String)){
            sql.append(" and left(a.title,6)!='zstest' ");
        }
        //5.5.2以前的版本不显示当前用户手机采集的会议直播
        if(map.userId && VerUtils.toIntVer(map.vc) < VerUtils.toIntVer("5.5.2")){
            sql.append(" and a.foreshow_id NOT IN (SELECT f.foreshow_id from live_foreshow f, live_record r where f.foreshow_id = r.foreshow_id AND (f.foreshow_status = 0 or f.foreshow_status = 1 or f.foreshow_status = 6) and f.foreshow_type = 2 and r.pgc_type =1 AND r.user_id = ?)")
            queryParam << map.userIdz
        }
        //在非搜悦app中不显示非H5类型的未开始的互动预告
        if(!Strings.APP_NAME_SOUYUE.equals(map.appId)){
            sql.append(" and ((a.url_tag = 85 and a.foreshow_type = 1) or a.foreshow_type in(2,3)) ");
        }
        sql.append(" ORDER BY sort_num desc, begin_time ");
        try {
            println "getNotStartedLiveForeshowList SQL >> ${sql.toString()}"
            println "getNotStartedLiveForeshowList PARAMS >> ${queryParam}"
            result = dataBases.msqlLiveSlave.rows(sql.toString(),queryParam)
        }catch (Exception e){
           log.error("getNotStartedLiveForeshowList ",e);
        }
        return result
    }

    @Override
    def findNewstLiveList(Map map) {
        int psize = map.psize as int
        long createTime = map.sortInfo? (Strings.parseJson(map.sortInfo)?.createTime ?: 0) as long : 0
        String dateTime = DateUtil.getFormatDteByLong(createTime)
        int orderId = map.sortInfo? (Strings.parseJson(map.sortInfo)?.orderId ?:0) as int : 0
        int pno = map.sortInfo ? 100 : 1
        StringBuffer sql = new StringBuffer("SELECT * FROM live_record WHERE live_mode=1 and app_id='"+map.appId+"' ")
        sql.append(" AND is_private = 0 AND live_type = "+map.liveStyle)
        if(orderId){
            sql.append(" AND newlive_sort_num < "+orderId)
        }else if(createTime){
            sql.append(" AND newlive_sort_num = 0 AND create_time < '"+dateTime+"' ")
        }
        sql.append(" ORDER BY newlive_sort_num DESC,create_time DESC LIMIT "+psize)
        List liveList = []
        try {
            log.info("live list sql =====>{}", sql.toString())
            liveList = dataBases.msqlLiveSlave.rows(sql.toString())
        }catch (Exception e){
            e.printStackTrace()
        }
        return liveList
    }



    @Override
    def findFaceLiveList(Map map) {
        List liveList;
        String appId = map.appId
        def config =qcloudLiveService.getQcloudInfo(appId)
        int faceListLimit = config?.faceListLimit?:0
        def testUserList = testUser ?  Arrays.asList(testUser.split(",")): [];
        try {
            def queryParam = [];
            //live_type=0 是非官方直播，live_mode=0是互动直播
            StringBuilder sql = new StringBuilder("select * from live_record where is_private = 0 and live_type =0 and live_mode=1 ");
            //5.6.1以前的版本不显示由直播sdk发起的直播（由于invokType不一致，跳转有问题）
            if(appId.equals(Strings.APP_NAME_SOUYUE) && VerUtils.toIntVer(map.vc) < VerUtils.toIntVer("5.6.1")) {
                sql.append(" and live_from_sdk !=1 ");
            }
            if(map.sortInfo){
               long liveId=( Strings.parseJson(map.sortInfo)?.orderId ?: 0) as long;
                if(liveId){
                sql.append(" and live_id < ? ");
                queryParam << liveId;
                }
            }
            if(!testUserList || !testUserList.contains(map.userId as String) || map.currentUserId? (!testUserList.contains(map.currentUserId as String)) : false){
                sql.append(" and left(title,6)!='zstest' ");
            }
            if(faceListLimit == 1 || map.isRecommend == 1){
                sql.append(" and app_id=? ");
                queryParam << appId
            }
            sql.append(" order by  live_id desc limit ? ")
            queryParam << map.psize;
            log.info("findFaceLiveList sql:{},map:{}",sql.toString(),queryParam);
            liveList = dataBases.msqlLiveSlave.rows(sql.toString(),queryParam);
        }catch (Exception e){
           log.info("findFaceLiveList map:{}",map,e);
        }
        return liveList
    }

    @Override
    def findLiveForeshowListForRecord(Map map) {
        int psize = (map.psize ?: 0 )as int
        long createTime = map.sortInfo? (Strings.parseJson(map.sortInfo)?.createTime ?: 0) as long : 0
        String dateTime = DateUtil.getFormatDteByLong(createTime)
        int orderId = map.sortInfo? (Strings.parseJson(map.sortInfo)?.orderId ?:0) as int : 0
        StringBuffer sql = new StringBuffer("SELECT * FROM live_foreshow WHERE foreshow_type = 1 and foreshow_status = ").append(LiveCommon.FORESHOW_STATUS_2)
        if(map.appId){
            sql.append(" And app_id='"+map.appId+"' ")
        }
        if(orderId){
            sql.append(" AND sort_num < "+orderId)
        }else if(createTime){
            sql.append(" AND sort_num = 0  AND begin_time < '"+dateTime+"' ")
        }
        sql.append(" ORDER BY sort_num DESC,begin_time DESC ")
        if(psize > 0 ){
            sql.append(" LIMIT "+psize)
        }
        List liveList = []
        try {
            log.info("live  foreshow list sql=======>{}", sql.toString())
            liveList = dataBases.msqlLiveSlave.rows(sql.toString())
        }catch (Exception e){
            e.printStackTrace()
        }
        return liveList
    }

    @Override
    def findLiveForeshowByForeshowId(long foreshowId, String appId) {
        StringBuffer sql = new StringBuffer("SELECT * FROM live_foreshow WHERE 1=1")
        sql.append(" AND foreshow_id = "+foreshowId)
        try {
            log.info("live foreshow by foreshowId sql=======>{}", sql.toString())
            return dataBases.msqlLiveSlave.firstRow(sql.toString())
        }catch (Exception e){
            e.printStackTrace()
        }
        return null
    }

    @Override
    def findLiveRecordListByForeId(Map map) {
        StringBuffer sql = new StringBuffer("SELECT * FROM live_record_log WHERE 1=1 ")
        sql.append(" AND live_status = 1 ")
        if(map.foreshowId){
            sql.append(" AND foreshow_id = "+map.foreshowId)
        }else{
            sql.append(" AND app_id = "+map.appId)
        }
        if(!map.all){
            sql.append(" AND video_address is not null ")
        }
        if(!map.dropIsPrivate){//个人中心中可查看私密直播，合并等其他操作不能查询出私密直播
            sql.append(" AND is_private = 0 ")
        }
        sql.append(" ORDER BY create_time ASC ")
        List liveList = []
        try {
            log.info("live record list by foreshowId sql=======>{}".toString(), sql.toString())
            liveList = dataBases.msqlLiveSlave.rows(sql.toString())
        }catch (Exception e){
            e.printStackTrace()
        }
        return liveList
    }

    @Override
    def findMyLiveRecordList(Map map){
        int psize = map.psize as int
        long createTime = map.sortInfo? (Strings.parseJson(map.sortInfo).createTime ?: 0) as long : 0
        String dateTime = DateUtil.getFormatDteByLong(createTime)
        int orderId = map.sortInfo? (Strings.parseJson(map.sortInfo).orderId ?:0) as int : 0
        StringBuffer sql = new StringBuffer("SELECT *,create_time as lastId FROM live_record_log WHERE live_mode = 1 ")
        boolean displayPrivate = map.displayPrivate as boolean
        if(displayPrivate){
            sql.append(" AND is_private = 0")
        }
        sql.append(" AND live_status = 1 AND video_address is not null and LENGTH(trim(video_address))>1")
        if(map.userId){
            sql.append(" AND user_id = "+map.userId)
        }
        if(orderId){
            sql.append(" AND backlive_sort_num < "+orderId)
        }else if(createTime){
            sql.append(" AND backlive_sort_num = 0 AND create_time < '"+dateTime+"' ")
        }
        sql.append(" ORDER BY backlive_sort_num DESC,create_time desc LIMIT "+psize)
        List liveList = []
        try {
            log.info("live record my sql=======>{}", sql.toString())
            liveList = dataBases.msqlLiveSlave.rows(sql.toString())
        }catch (Exception e){
            e.printStackTrace()
        }
        return liveList
    }
    @Override
    def findMyLiveRecordListForUgc(Map map){
        int psize = map.psize as int
        long createTime = map.sortInfo? (Strings.parseJson(map.sortInfo).createTime ?: 0) as long : 0
        String dateTime = DateUtil.getFormatDteByLong(createTime)
        int orderId = map.sortInfo? (Strings.parseJson(map.sortInfo).orderId ?:0) as int : 0
        StringBuffer sql = new StringBuffer("SELECT *,create_time as lastId FROM live_record_log WHERE live_type != 1 ")
        def testUserList = testUser ?  Arrays.asList(testUser.split(",")): [];
        boolean displayPrivate = map.displayPrivate as boolean
        if(displayPrivate){
            sql.append(" AND is_private = 0")
        }
        sql.append(" AND live_status = 1 AND video_address is not null and LENGTH(trim(video_address))>1")
        if(map.userId){
            sql.append(" AND user_id = "+map.userId)
        }
        if(orderId){
            sql.append(" AND backlive_sort_num < "+orderId)
        }else if(createTime){
            sql.append(" AND backlive_sort_num = 0 AND create_time < '"+dateTime+"' ")
        }
        if(map.isRecommend){
            sql.append(" AND live_recommend = 1 AND app_id= '"+map.appId+"' ")
            if(!testUserList || map.currentUserId? (!testUserList.contains(map.currentUserId as String)) : false){
                sql.append(" and left(title,6)!='zstest' ");
            }
        }
        sql.append(" ORDER BY backlive_sort_num DESC,create_time desc LIMIT "+psize)
        List liveList = []
        try {
            log.info("live record my sql=======>{}", sql.toString())
            liveList = dataBases.msqlLiveSlave.rows(sql.toString())
        }catch (Exception e){
            e.printStackTrace()
        }
        return liveList
    }
    @Override
    def findMyLiveRecordListNew(Map map) {
        int psize = map.psize as int
        long createTime = map.sortInfo? (Strings.parseJson(map.sortInfo).createTime ?: 0) as long : 0
        String dateTime = DateUtil.getFormatDteByLong(createTime)
        int orderId = map.sortInfo? (Strings.parseJson(map.sortInfo).orderId ?:0) as int : 0
        StringBuffer sql = new StringBuffer("SELECT *,MIN(create_time) AS lastId,COUNT(DISTINCT foreshow_id) FROM live_record_log WHERE foreshow_id > 0 ")
        boolean displayPrivate = map.displayPrivate as boolean
        if(displayPrivate){
            sql.append(" AND is_private = 0")
        }
        sql.append(" AND live_status = 1 AND video_address is not null and LENGTH(trim(video_address))>1")
        if(map.userId){
            sql.append(" AND user_id = "+map.userId)
        }
        if(orderId){
            sql.append(" AND backlive_sort_num < "+orderId)
        }else if(createTime){
            sql.append(" AND backlive_sort_num = 0 AND create_time < '"+dateTime+"' ")
        }
        sql.append(" GROUP BY foreshow_id ORDER BY backlive_sort_num DESC,create_time desc LIMIT "+psize)
        List liveList = []
        try {
            log.info("live record my sql=======>{}", sql.toString())
            liveList = dataBases.msqlLiveSlave.rows(sql.toString())
        }catch (Exception e){
            e.printStackTrace()
        }
        return liveList
    }

    @Override
    def updateLiveRecordSortNum(long liveId,int sortNum,String appId){
        int res = 0
        try {
            dataBases.msqlLive.executeUpdate("update live_record_log set backlive_sort_num=?,update_time=? where live_id=? and app_id=?",
                [sortNum, new Date(), liveId,appId])
            res = 1
        }catch (Exception e){
            e.printStackTrace()
        }
        return res
    }

    @Override
    def updateForeshowChildSortNum(long foreshowId,int sortNum,String appId){
        int res = 0
        try {
            dataBases.msqlLive.executeUpdate("update live_foreshow set child_sort=?,update_time=? where foreshow_id=? and app_id=?",
                [sortNum, new Date(), foreshowId,appId])
            res = 1
        }catch (Exception e){
            e.printStackTrace()
        }
        return res
    }

    @Override
    def updateForeshowSortNum(long foreshowId,int sortNum,String appId){
        int res = 0
        try {
            dataBases.msqlLive.executeUpdate("update live_foreshow set sort_num=?,update_time=? where foreshow_id=? and app_id=?",
                [sortNum, new Date(), foreshowId,appId])
            res = 1
        }catch (Exception e){
            e.printStackTrace()
        }
        return res
    }

    @Override
    def addAppointment(long foreshowId,long userId,String appId){
        int res = 0
        try {
            //先查询是否存在
            String sql = "select appoint_id from live_appointment where foreshow_id =? and  user_id=? ";
            def queryParam =  [foreshowId, userId];
            def result = dataBases.msqlLiveSlave.firstRow(sql, queryParam);
            //插入预约记录
            if(!result){
                dataBases.msqlLive.executeInsert("insert into live_appointment(foreshow_id,user_id,create_time,app_id)  values(?,?,?,?)",
                    [foreshowId, userId, new Date(),appId]);
            }
            res = 1
        }catch (Exception e){
            e.printStackTrace()
        }
        return res
    }

    @Override
    def delAppointment(long foreshowId,long userId,String appId){
        int res = 0
        try {
            dataBases.msqlLive.executeUpdate("delete from live_appointment where foreshow_id =? and  user_id=? ",
                [foreshowId, userId]);
            res = 1
        }catch (Exception e){
            e.printStackTrace()
        }
        return res
    }

    @Override
    GroovyRowResult findLiveRecordByLiveId(long liveId){
        def rowResult
        try {
            String sql = "select * from live_record_log where live_id=? "
            rowResult= dataBases.msqlLiveSlave.firstRow(sql, [liveId])
        }catch (Exception e){
            e.printStackTrace()
        }
        return rowResult
    }

    @Override
    GroovyRowResult findLiveRecordByForeshowId(long foreshowId){
        def rowResult
        try {
            String sql = "select * from live_record_log where foreshow_id=? "
            rowResult= dataBases.msqlLiveSlave.firstRow(sql, [foreshowId])

        }catch (Exception e){
            e.printStackTrace()
        }
        return rowResult
    }

    @Override
    def delLiveRecordByLiveId(long liveId,int liveStatus,String appId){
        int res = 0
        try {
            res = dataBases.msqlLive.executeUpdate("update live_record_log set live_status=?,update_time=? where live_id=? and live_status!=?",
                [liveStatus, new Date(),liveId,liveStatus]);
            if(res > 0){
                res =1
            }
        }catch (Exception e){
            e.printStackTrace()
        }
        return res
    }

    def delLiveRecordByForeshowId(long foreshowId,int liveStatus,String appId){
        int res = 0
        try {
            res = dataBases.msqlLive.executeUpdate("update live_record_log set live_status=?,update_time=? where foreshow_id=? and live_status!=?",
                [liveStatus, new Date(),foreshowId,liveStatus]);
            if(res > 0){
                res =1
            }
        }catch (Exception e){
            e.printStackTrace()
        }
        return res
    }

    @Override
    def delForeshow(long foreshowId,String appId){
        int res = 0
        try {
            res = dataBases.msqlLive.executeUpdate("delete from live_foreshow  where foreshow_id=? and app_id=?",
                [foreshowId,appId]);
            if(res > 0){
                res =1
            }
        }catch (Exception e){
            e.printStackTrace()
        }
        return res
    }

    @Override
    def updateLiveForeshowId(long foreshowId,String appId){
        int res = 0
        try {
            dataBases.msqlLive.executeUpdate("update live_record set foreshow_id=0,live_type=0,update_time=? where foreshow_id=? and app_id=?",
                [new Date(), foreshowId,appId])
            dataBases.msqlLive.executeUpdate("update live_record_log set foreshow_id=0,live_type=0,update_time=? where foreshow_id=? and app_id=?",
                [new Date(), foreshowId,appId])
            res = 1
        }catch (Exception e){
            e.printStackTrace()
        }
        return res
    }

    @Override
    int isOpenRemind(String appId, long userId, long foreshowId) {
        def rowResult
        try {
            String sql = "SELECT * FROM live_appointment WHERE user_id=? AND foreshow_id=? ORDER BY create_time DESC"
            rowResult= dataBases.msqlLiveSlave.firstRow(sql, [userId,foreshowId])
            if(rowResult)
                return 1
        }catch (Exception e){
            e.printStackTrace()
        }
        return 0
    }

    @Override
    List<Long> listOpenRemind(String appId, long userId) {
        def resultList = []
        try {
            String sql = "SELECT foreshow_id FROM live_appointment WHERE user_id=? "
            def params = [userId]

            def rows= dataBases.msqlLiveSlave.rows(sql, params)
            resultList.addAll(rows.foreshow_id)
        }catch (Exception e){
            e.printStackTrace()
        }
        return resultList
    }

    @Override
    def findAppointmentUserlist(long foreshowId, int lastId, String appId, int psize) {
        List list = [];
        StringBuffer sql= new StringBuffer("SELECT * FROM live_appointment WHERE  foreshow_id=? ")
        if(lastId > 0){
            sql.append(" AND appoint_id < "+lastId)
        }
        sql.append(" ORDER BY appoint_id DESC LIMIT "+psize)
        try {
            dataBases.msqlLiveSlave.eachRow(sql.toString(), [foreshowId], {row ->
                list << [id: row.appoint_id, userId: row.user_id]
            })
        }catch (Exception e){
            e.printStackTrace()
        }
        return list;
    }

    @Override
    def findLiveRecordByForeId(long foreshowId,String appId) {
        String sql = "select * from live_record where foreshow_id = ?"
        try {
            return dataBases.msqlLiveSlave.firstRow(sql,[foreshowId])
        }catch (Exception e){
            e.printStackTrace()
        }
        return null
    }

    @Override
    def updateLiveForeshowStatus(long foreshowId,int status, String appId) {
        int res = 0
        try {
         return   dataBases.msqlLive.executeUpdate("update live_foreshow set sort_num=0,foreshow_status=? where foreshow_id =? and app_id=? ",
                [status,foreshowId,appId])

        }catch (Exception e){
            e.printStackTrace()
        }
        return res
    }

    @Override
    def updateLiveForeshowLiveRecordInfo(def liveRecordInfo,int foreshow_status, long foreshowId, String appId) {
        int res = 0
        try {
            dataBases.msqlLive.executeUpdate("update live_foreshow set live_record_info=?,foreshow_status=?,update_time=? where foreshow_id =?",
                [liveRecordInfo,foreshow_status,new Date(),foreshowId])
            res = 1
        }catch (Exception e){
            e.printStackTrace()
        }
        return res
    }

    @Override
    def findAllVestUserList() {
        def resultList = []
        try {
            //为了方便计算，将马甲按顺序放到10个redis的key中，目前取28200.库中有17条记录没有用
            dataBases.msqlLiveSlave.eachRow("SELECT user_id,nickname,user_image FROM live_vest_users order by pwd limit 28200",{row ->
                //由于数据库里的用户头像太大，客户端展示有问题，所以需要对头像做以下处理
                String userImage = ImageUtils.getSmallImg(row.user_image ? row.user_image as String : "")
                resultList << [
                    userId: row.user_id as long,
                    nickname:row.nickname,
                    userImage: userImage
                ]
            })
        }catch (Exception e){
            e.printStackTrace()
        }
        return resultList
    }

    @Override
    def findVestCountConfigInfo(long liveId,String name,String appId) {
        try {
            return dataBases.msqlLiveSlave.firstRow("SELECT value FROM live_config WHERE app_id=? AND name = ? AND live_id=? ", [appId,name,liveId])
        }catch (Exception e){
            e.printStackTrace()
        }
        return null
    }

    @Override
    def findVestCountGlobalConfigInfo(String name,String appId) {
        try {
            return dataBases.msqlLiveSlave.firstRow("SELECT value FROM live_config WHERE app_id=? AND name = ? ", [appId,name])
        }catch (Exception e){
            e.printStackTrace()
        }
        return null
    }

    @Override
    def findBackVedioAddress() {
        def resultList = []
        try {
            dataBases.msqlLiveSlave.eachRow("SELECT video_address  FROM live_record_log where video_address is not null and create_time > '2016-12-00' and create_time < '2016-12-25'",{row ->
           //线上导出
           ///dataBases.msqlLiveSlave.eachRow("SELECT * from live_record_log where (title like '%test%' or keyword like '%test%') and video_address is not null and create_time < '2016-12-04 11:44:26'",{row ->
                resultList << [
                    video_address: row.video_address
                ]
            })
        }catch (Exception e){
            e.printStackTrace()
        }
        return resultList
    }

    def getMyLiveNotice(Map map){
        def res = []
        try {
            StringBuilder sb = new StringBuilder("SELECT * from live_foreshow where (foreshow_status = 0 or foreshow_status = 1) and foreshow_type = 1")
            List params = new ArrayList()
            /*if (map.startDate){
                sb.append(" and begin_time > ?")
                params << map.startDate
            }*/
            if (map.endDate){
                sb.append(" and begin_time < ?")
                params << map.endDate
            }
            if (map.userId){
                sb.append(" and user_id = ?")
                params << map.userId
            }
            if (map.appId && !map.appId.equals("com.zhongsou.souyueprime")){//精华区的主播提醒不区分appId
                sb.append(" and app_id = ?")
                params << map.appId
            }

            //5.5.2增加查询手机端采集的会议直播
            if(map.userId && VerUtils.toIntVer(map.vc) >= VerUtils.toIntVer("5.5.2")){
                sb.append(" UNION ALL SELECT f.* from live_foreshow f, live_record r where f.foreshow_id = r.foreshow_id AND (f.foreshow_status = 0 or f.foreshow_status = 1 or f.foreshow_status = 6) and f.foreshow_type = 2 and r.pgc_type =1")
                if (map.endDate){
                    sb.append(" and f.begin_time < ?")
                    params << map.endDate
                }
                if (map.userId){
                    sb.append(" and f.user_id = ?")
                    params << map.userId
                }
                if (map.appId && !map.appId.equals("com.zhongsou.souyueprime")){//精华区的主播提醒不区分appId
                    sb.append(" and f.app_id = ?")
                    params << map.appId
                }
            }
            sb.append(" ORDER BY sort_num DESC,begin_time ASC limit 1")
            log.info("getMyLiveNotice,sql:{},params:{}",sb.toString(),params)
            res = dataBases.msqlLiveSlave.firstRow(sb.toString(), params)
        }catch (Exception e){
            e.printStackTrace()
        }
        return res
    }

    private StringBuilder createSql(Map map){
        StringBuilder sb = new StringBuilder("SELECT b.foreshow_id,b.title,b.show_title_in_list,b.img_url,b.user_id,b.user_name,b.nickname,b.user_image,b.begin_time,b.url_tag,b.url,b.sort_num,b.child_sort,b.create_time,b.update_time,b.foreshow_status,b.foreshow_type,b.description,b.live_record_info,b.keyword,b.srp_id")

        if (map.foreshowStatus != 0) {//未开始的没有record记录，无法关联
            sb.append(",r.live_id,r.live_mode,r.room_id,r.vc,r.app_id recode_app_id")
        }

        if (map.foreshowStatus == LiveCommon.FORESHOW_STATUS_2){//减少不必要的查询,回放才查询系列期数
            //系列期数 ---- start ----
            sb.append(getLiveGroupLiveNumSql(map))
            //系列期数 ---- end ----
        }else {
            sb.append(",0 as live_num")
        }
        sb.append(" from live_foreshow b")

        //直播中关联直播中数据
        if (map.foreshowStatus == LiveCommon.FORESHOW_STATUS_1 || map.foreshowStatus == LiveCommon.FORESHOW_STATUS_6) {
            sb.append(" LEFT JOIN live_record r ON b.foreshow_id=r.foreshow_id")
        }else if (map.foreshowStatus == LiveCommon.FORESHOW_STATUS_2) {//此处只能用left，因为互动回放与预告是多对一关系（可以省掉非互动直播拼接观看人数的查询）
            sb.append(" LEFT JOIN live_record_log r ON b.foreshow_id=r.foreshow_id")
        }
        //在srp词或者圈子下
        if(map.srpId){
            sb.append(" INNER JOIN live_foreshow_srp_relation s ON b.foreshow_id = s.foreshow_id")
        }
        sb.append(" where 1=1")

        if (map.parentId!=0 && map.foreshowStatus == LiveCommon.FORESHOW_STATUS_2) {//回放只查询未删除的（用户删除的还是要显示）
            sb.append(" AND (r.live_status=1 or r.live_status=2 or r.live_status=3) ")
        }
        return sb
    }

    private StringBuilder getLiveGroupLiveNumSql(Map map){
        StringBuilder sb = new StringBuilder();
        if(VerUtils.toIntVer(map.vc) < VerUtils.toIntVer("5.6.7")) {
            sb.append(",(select count(DISTINCT log.foreshow_id) from live_foreshow a JOIN live_record_log log ON log.foreshow_id = a.foreshow_id where a.parent_id=b.foreshow_id and a.foreshow_status=2 and log.live_status=1")
            //5.6以前的版本不显示手机采集的会议直播
            if(VerUtils.toIntVer(map.vc) < VerUtils.toIntVer("5.6")) {
                sb.append(" and a.foreshow_type!=3 ");
            }

            //需要显示的设备 1 h5(h5) 2 客户端(app)
            def from = map.from
            if (from == 'h5'){
                sb.append(" AND a.not_show_in_client!= 2")
            }else{
                sb.append(" AND a.not_show_in_client!= 1")
            }
        }else{
            sb.append(",((select count(*) from live_foreshow f where f.parent_id=b.foreshow_id " )
            //5.6以前的版本不显示手机采集的会议直播
            if(VerUtils.toIntVer(map.vc) < VerUtils.toIntVer("5.6")) {
                sb.append(" and f.foreshow_type!=3 ");
            }
            if(!Strings.APP_NAME_SOUYUE.equals(map.appId)){
                sb.append(" and (f.foreshow_status in (1,6) or (f.foreshow_status = 0 and ((f.url_tag = 85 and f.foreshow_type = 1) or f.foreshow_type in(2,3)))) ");
            }else{
                sb.append(" and f.foreshow_status in (0,1,6) ")
            }
            //需要显示的设备 1 h5(h5) 2 客户端(app)
            def from = map.from
            if (from == 'h5'){
                sb.append(" AND f.not_show_in_client!= 2")
            }else{
                sb.append(" AND f.not_show_in_client!= 1")
            }
            sb.append(") + (select count(DISTINCT log.foreshow_id) from live_foreshow a JOIN live_record_log log ON log.foreshow_id = a.foreshow_id where a.parent_id=b.foreshow_id and a.foreshow_status=2 and log.live_status=1")
              //5.6以前的版本不显示手机采集的会议直播
            if(VerUtils.toIntVer(map.vc) < VerUtils.toIntVer("5.6")) {
                sb.append(" and a.foreshow_type!=3 ");
            }
            //需要显示的设备 1 h5(h5) 2 客户端(app)
            if (from == 'h5'){
                sb.append(" AND a.not_show_in_client!= 2")
            }else{
                sb.append(" AND a.not_show_in_client!= 1")
            }
            sb.append(")")
        }

        sb.append(" ) as live_num")
    }

    def findForeshowListSql(Map map){
        def testUserList = testUser ?  Arrays.asList(testUser.split(",")): null;
        StringBuilder sb = createSql(map)
        List params = new ArrayList()
        if(!testUserList || !testUserList.contains(map.userId as String)){
            sb.append(" and left(b.title,6)!='zstest' ");
        }

        //在非搜悦app中不显示非H5类型的未开始的互动预告
        if(map.foreshowStatus == 0 && !Strings.APP_NAME_SOUYUE.equals(map.appId)){
            sb.append(" and ((b.url_tag = 85 and b.foreshow_type = 1) or b.foreshow_type in(2,3)) ");
        }
        //在srp词或者圈子下
        if(map.srpId){
            sb.append(" AND s.srp_id =?")
            params << map.srpId
        }
        if(map.isRecommend){
            sb.append(" and b.live_recommend = 1 and b.app_id='${map.appId}' ")
        }
        if(map.cateIds){//因为添加了共享分类，所有限定本appid下分类包含的预告和为分类但属于此appid的预告
            sb.append(" AND (b.cate_id in (${Strings.toListString(map.cateIds)}) or (b.cate_id=0 and b.app_id='"+map.appId+"') OR (b.foreshow_id in (select s.extend_id from  live_share_relation s where s.type = 1 and share_app_id = '"+map.appId+"'  ) and b.is_show_live_homepage = 1))")
        }else if(!map.parentId){//查询系列时不加app_id的条件
            sb.append(" AND (b.app_id = ? OR (b.foreshow_id in (select s.extend_id from  live_share_relation s where s.type = 1 and share_app_id = '"+map.appId+"'  ) and b.is_show_live_homepage = 1))")
            params << map.appId
        }
        if (map.categoryId){//如果按分类查询，排除此类别下系列中属于此类的子预告
            sb.append(" AND b.cate_id = ?")
            params << map.categoryId

            sb.append(" AND b.foreshow_id not in (select m.foreshow_id from live_foreshow m,live_foreshow n where m.parent_id=n.foreshow_id and m.cate_id=n.cate_id and n.cate_id = ?)")
            params << map.categoryId
        }else if(map.srpId && map.foreshowStatus == 2){
            sb.append(" and s.foreshow_id not in (select m.foreshow_id from live_foreshow m ,live_foreshow n, live_foreshow_srp_relation o,live_foreshow_srp_relation p   where m.foreshow_id=o.foreshow_id and n.foreshow_id=p.foreshow_id and m.parent_id=n.foreshow_id and o.srp_id=? and p.srp_id =?)")
            params << map.srpId
            params <<map.srpId
        }else{//如果查询全部，不需要查询系列里的子预告
            if (map.parentId as String){
                sb.append(" AND b.parent_id = ?")
                params << map.parentId
            }
        }
        if (map.foreshowStatus as String){
            sb.append(" AND b.foreshow_status = ?")
            params << map.foreshowStatus
        }

        if (map.beginTime){
            sb.append(" AND b.begin_time > ? AND b.begin_time < ?")
            params << new Date()
            params << map.beginTime
        }

        if (map.targetUserId as String){//按主播查看时不包括系列
            sb.append(" AND b.user_id = ? AND b.foreshow_type != 5")
            params << (map.targetUserId as long)
        }

        //5.6以前的版本不显示手机采集的会议直播
        if(VerUtils.toIntVer(map.vc) < VerUtils.toIntVer("5.6")) {
            sb.append(" and b.foreshow_type!=3 ");
        }

        //需要显示的设备 1 h5(h5) 2 客户端(app)
        def from = map.from
        if (from == 'h5'){
            sb.append(" AND b.not_show_in_client!= 2")
        }else{
            sb.append(" AND b.not_show_in_client!= 1")
        }
        if(map.isFree){ //是否要显示付费直播
            sb.append(" AND b.foreshow_type != 3 ")
        }
        long updateTime = map.sortInfo? (Strings.parseJson(map.sortInfo)?.createTime ?: 0) as long : 0
        String dateTime = DateUtil.getFormatDteByLong(updateTime)
        int orderId = map.sortInfo? (Strings.parseJson(map.sortInfo)?.orderId ?:0) as int : 0

        if(orderId){
            if ((map.parentId as String) && ((map.parentId as int) > 0)){
                sb.append(" AND b.child_sort < ?")
            }else {
                sb.append(" AND b.sort_num < ?")
            }
            params << orderId
        }else if(dateTime){
            sb.append(" AND b.sort_num = 0 AND b.begin_time < ? ")
            params << dateTime
        }

        //5.5.2以前的版本不显示手机采集的会议直播
        if(map.userId && VerUtils.toIntVer(map.vc) < VerUtils.toIntVer("5.5.2")){
            sb.append(" AND b.foreshow_id NOT IN (SELECT f.foreshow_id from live_foreshow f, live_record r where f.foreshow_id = r.foreshow_id AND (f.foreshow_status = 0 or f.foreshow_status = 1 or f.foreshow_status = 6) and f.foreshow_type = 2 and r.pgc_type =1 " +
                " AND r.user_id = ?)")
            params << map.userId
        }
        if ((map.parentId as String) && ((map.parentId as int) > 0)){
            if ((map.foreshowStatus as String) && map.foreshowStatus == LiveCommon.FORESHOW_STATUS_0){
                sb.append(" ORDER BY b.child_sort DESC,b.begin_time ASC")
            }else {
                sb.append(" ORDER BY b.child_sort DESC,b.begin_time DESC")
            }
        }else {
            if ((map.foreshowStatus as String) && map.foreshowStatus == LiveCommon.FORESHOW_STATUS_0){
                sb.append(" ORDER BY b.sort_num DESC,b.begin_time ASC")
            }else {
                sb.append(" ORDER BY b.sort_num DESC,b.begin_time DESC")
            }
        }
        if(map.isRecommend){
            sb.append(",b.recommend_time DESC ")
        }
        if (map.psize){
            sb.append(" limit ?")
            params << map.psize
        }
        return [sql:sb,params:params]
    }
    def getResultSql(Map map){

        def sqlMap = findForeshowListSql(map)
        StringBuilder sql = new StringBuilder()
        String sql1 = sqlMap.sql.toString()
        List params = sqlMap.params
        if(!map.categoryId && !map.parentId && [1,2].contains(map.foreshowStatus) && (!map.isAndroidOs ||(map.isAndroidOs && VerUtils.toIntVer(map.vc)>VerUtils.toIntVer("5.6.9")))){
            sql.append("SELECT * FROM ("+sql1+") as value_table ")
            sql.append(" UNION (SELECT * FROM (SELECT r.foreshow_id,r.title,1 AS show_title_in_list,r.live_bg AS img_url,r.user_id,NULL AS user_name,r.nickname,r.user_image,r.create_time AS begin_time,0 AS url_tag,NULL AS url," +
                "")
            if(map.foreshowStatus == 1){//直播中
                sql.append("r.newlive_sort_num as sort_num, 0 AS child_sort,r.create_time,r.update_time, 1 AS foreshow_status,0 AS foreshow_type,NULL AS description,NULL AS live_record_info,r.keyword,r.srp_id,r.live_id,r.live_mode,r.room_id,r.vc,r.app_id recode_app_id,0 AS live_num " +
                    " FROM live_share_relation s left join live_record r on s.extend_id = r.live_id ")
            }else  if(map.foreshowStatus == 2){//回放
                sql.append("r.backlive_sort_num as sort_num,0 AS child_sort,r.create_time,r.update_time, 2 AS foreshow_status,0 AS foreshow_type,NULL AS description,NULL AS live_record_info,r.keyword,r.srp_id,r.live_id,r.live_mode,r.room_id,r.vc,r.app_id recode_app_id,0 AS live_num " +
                    " FROM live_share_relation s left join live_record_log r on s.extend_id = r.live_id ")
            }
            if(map.srpId){
                sql.append(" INNER JOIN live_foreshow_srp_relation sr ON s.extend_id = sr.live_id ")
            }
            sql.append(" where s.type = 2 and r.is_show_live_homepage = 1 and s.share_app_id=? ")
            params << map.appId
            def testUserList = testUser ?  Arrays.asList(testUser.split(",")): null;
            if(!testUserList || !testUserList.contains(map.userId as String)){
                sql.append(" and left(r.title,6)!='zstest' ");
            }
            if(map.srpId){
                sql.append(" AND sr.srp_id =?")
                params << map.srpId
            }
            long updateTime = map.sortInfo? (Strings.parseJson(map.sortInfo)?.createTime ?: 0) as long : 0
            String dateTime = DateUtil.getFormatDteByLong(updateTime)
            int orderId = map.sortInfo? (Strings.parseJson(map.sortInfo)?.orderId ?:0) as int : 0
            if (map.foreshowStatus == 1){
                if(orderId){
                    sql.append(" AND r.newlive_sort_num < ?")
                    params << orderId
                }else if(dateTime){
                        sql.append(" AND r.newlive_sort_num = 0 AND r.create_time < ? ")
                        params << dateTime
                }
            }else if(map.foreshowStatus == 2){
                if(orderId) {
                    sql.append(" AND r.backlive_sort_num < ?")
                    params << orderId
                }else if(dateTime){
                    sql.append(" AND r.backlive_sort_num = 0 AND r.create_time < ? ")
                    params << dateTime
                }

            }
            sql.append(") as face_table) order by sort_num desc, begin_time desc ")
            if (map.psize){
                sql.append(" limit "+map.psize)
            }
            sqlMap.put("sql",sql)

        }
        return sqlMap
    }
    def findForeshowList(Map map){
        def resultList = []
        try {
            def sqlMap = getResultSql(map)
            log.info("findForeshowList SQL >> "+sqlMap.sql)
            log.info("findForeshowList PARAMS >> "+sqlMap.params)
            resultList = dataBases.msqlLiveSlave.rows(sqlMap.sql.toString(),sqlMap.params)
        }catch (Exception e){
            e.printStackTrace()
        }
        return resultList
    }

    def findLiveCategroyList(Map map){
        def resultList = []
        def testUserList = testUser ?  Arrays.asList(testUser.split(",")): null;
        StringBuilder sb = new StringBuilder("SELECT * from live_category_info where 1=1 ")
        List params = new ArrayList()
        if(!testUserList || !testUserList.contains(map.userId as String)){
            sb.append(" and left(category_name,6)!='zstest' ");
        }
        if(map.appId){
            sb.append(" and app_id = ?")
            params.add(map.appId)
        }
        if (map.isHide){//1 显示   2 隐藏
            sb.append(" and is_hide = ?")
            params.add(map.isHide)
        }
        if (map.isDel){//1 未删除，2 已删除
            sb.append(" and is_del = ?")
            params.add(map.isDel)
        }

        sb.append(" ORDER BY sort_num ASC,create_time DESC")


        resultList= dataBases.msqlLiveSlave.rows(sb.toString(),params)
        return resultList
    }

    def addUserForeshow(long userId, long foreshowId,String appId){
        String querySql = "SELECT id FROM live_user_foreshow WHERE user_id =? and  foreshow_id=? AND end_time IS NULL";
        def queryParam = [userId, foreshowId];
        int res = 0
        try {
            //先查询是否存在
            def result = dataBases.msqlLiveSlave.firstRow(querySql, queryParam);
            //插入记录
            if(!result){
                String insertSql = 'INSERT INTO live_user_foreshow (`user_id`, `foreshow_id`, `create_time`, `app_id`) VALUES (?,?,?,?)';
                def insertParam = [userId, foreshowId,new Date(),appId];
                dataBases.msqlLive.executeInsert(insertSql, insertParam);
            }
            res = 1
        }catch (Exception e){
            e.printStackTrace()
        }
        return res
    }

    def removeUserForeshow(long userId, long foreshowId,String appId){
        String updateSql = "UPDATE live_user_foreshow SET `end_time`=? WHERE user_id = ? AND foreshow_id = ? and end_time is NULL";
        def updateParam = [new Date(), userId, foreshowId]
        dataBases.msqlLive.executeUpdate(updateSql, updateParam)

    }

    @Override
    def listUserForeshow(long userId,int foreshowType, int psize, String sortInfo,String appId) {
        def resultList = []
        //按波总要求加入
        Map categoryMap=[appId:appId,isHide:1,isDel:1,userId:userId]
        List categoryList = []
        findLiveCategroyList(categoryMap).each {
            if ((it.type as int) == 1){
                categoryList << [cat_id:it.cat_id]
            }else {
                categoryList << [cat_id:it.share_cat_id]
            }
        }
        StringBuilder sql = new StringBuilder()
        sql.append("SELECT f.url AS url,f.img_url AS img_url,f.foreshow_id AS foreshow_id,f.foreshow_type AS foreshow_type, f.title AS title, f.sort_num,f.create_time FROM live_user_foreshow uf ")
         .append(" JOIN live_foreshow f ON uf.foreshow_id = f.foreshow_id ")
         .append(" WHERE 1=1 AND uf.user_id = ? AND uf.end_time IS NULL ")
        if (categoryList){
            sql.append(" AND ( f.cate_id in (${Strings.toListString(categoryList*.cat_id)}) or (f.cate_id=0 and f.app_id='"+appId+"') )")
        }else {
            sql.append(" AND uf.app_id = '"+appId+"'")
        }
        List params = [userId]
        if (foreshowType){
            sql.append(" AND f.foreshow_type = ? ")
            params << foreshowType
        }

        long createTime = 0
        int orderId = 0
        if (sortInfo){
            createTime = (Strings.parseJson(sortInfo)?.createTime ?: 0) as long
            orderId = (Strings.parseJson(sortInfo)?.orderId ?:0) as int
        }
        if(orderId){
            sql.append(" AND f.sort_num < ?")
            params << orderId
        }else if(createTime){
            String ctimeString = DateUtil.getFormatDteByLong(createTime)
            sql.append(" AND f.sort_num = 0 AND f.create_time < ? ")
            params << ctimeString
        }
        sql.append(" ORDER BY f.sort_num DESC, f.create_time DESC LIMIT ?")
        params << psize

        dataBases.msqlLiveSlave.eachRow(sql.toString(),params,{
            resultList << [
                liveThumb:it.img_url,
                foreshowId:it.foreshow_id,
                foreshowType:it.foreshow_type,
                title: it.title,
                sortInfo:[
                    orderId:it.sort_num,
                    createTime:DateUtil.getTimestamp(it.create_time as String)
                ]
            ]
        })
        return resultList
    }
    @Override
    def getUserForeshow(long userId,long foreshowId,String appId){
        String sql = "SELECT * FROM live_user_foreshow uf WHERE 1=1 AND uf.user_id = ? AND uf.foreshow_id=? and uf.app_id=? AND uf.end_time IS NULL "
        List params = [userId,foreshowId,appId]

        def result =dataBases.msqlLiveSlave.firstRow(sql,params)
        return result
    }

    @Override
    def updateFieldControlByAdd(Map params){
        int res = 0
        try {
            dataBases.msqlLive.executeUpdate("update live_record set field_control_user_id=CONCAT_WS(',',field_control_user_id,?),update_time=? where live_id=? and app_id=?",
                [params.toUserId, new Date(), params.liveId,params.appId])
            dataBases.msqlLive.executeUpdate("update live_record_log set field_control_user_id=?,update_time=? where live_id=? and app_id=?",
                [params.toUserId, new Date(), params.liveId,params.appId])
            res = 1
        }catch (Exception e){
            e.printStackTrace()
        }
        return res
    }

    @Override
    def updateFieldControlByAll(Map params){
        int res = 0
        try {
            dataBases.msqlLive.executeUpdate("update live_record set field_control_user_id=?,update_time=? where live_id=? and app_id=?",
                [params.toUserId, new Date(), params.liveId,params.appId])
            dataBases.msqlLive.executeUpdate("update live_record_log set field_control_user_id=?,update_time=? where live_id=? and app_id=?",
                [params.toUserId, new Date(), params.liveId,params.appId])
            res = 1
        }catch (Exception e){
            e.printStackTrace()
        }
        return res
    }

    def findLastForeShow(long parentId,int foreshowStatus,String vc){
        String sql = "select * from live_foreshow where parent_id = ${parentId} and foreshow_status = ${foreshowStatus} and not_show_in_client!= 1 order by child_sort DESC,begin_time desc"
        if(VerUtils.toIntVer(vc) < VerUtils.toIntVer("5.6")) {//小于5.6版本不显示付费直播
            sql = "select * from live_foreshow where parent_id = ${parentId} and foreshow_status = ${foreshowStatus} and foreshow_type!=3 and not_show_in_client!= 1 order by child_sort DESC,begin_time desc"
        }
        dataBases.msqlLiveSlave.firstRow(sql)
    }

    @Override
    def updateForeshowIdByLiveId(long liveId,long foreshowId, String appId){
        String sql = """UPDATE live_record  SET  foreshow_id =? WHERE app_id=? AND live_id =?""";

        return dataBases.msqlLive.executeUpdate(sql,
            [foreshowId, appId, liveId]);

    }
    @Override
    def findVestCommentList(String appId) {
        def resultList = []
        try {
            dataBases.msqlLiveSlave.eachRow("SELECT * from live_marked_words where app_id = ? AND type = 1 AND is_del = 1",[appId]){row ->
                resultList << (row.content ?: "")as String
            }
        }catch (Exception e){
            e.printStackTrace()
        }
        return resultList
    }

    @Override
    def updateForeshowStatus(long foreshowId,int foreshowStatus){
        return dataBases.msqlLive.executeUpdate("update live_foreshow set foreshow_status=? where foreshow_id=?",
            [foreshowStatus, foreshowId])
    }
    @Override
    def updateForeshowStatusBegin(){
        dataBases.msqlLive.executeUpdate("update live_foreshow set foreshow_status=? where foreshow_status=0 and begin_time<?",
            [LiveCommon.FORESHOW_STATUS_1,new Date()])
    }

    @Override
    def findStopedPgcList() {
        def resultList = []
        try {
            dataBases.msqlLiveSlave.eachRow("SELECT * from live_record_log where live_mode != 1 AND live_status != 4"){row ->
                resultList << [
                        liveId: row.live_id as long,
                        foreshowId: row.foreshow_id as long,
                        roomId: row.room_id as int,
                        userId: row.user_id as long,
                        liveMode: row.live_mode as int,
                        appId: row.app_id as String
                ]
            }
        }catch (Exception e){
            e.printStackTrace()
        }
        return resultList
    }
    @Override
    def updateLivePgcStatusByLiveId(long liveId,int pgcStatus){
        dataBases.msqlLive.executeUpdate("update live_record set pgc_status=? where live_id=?",
            [pgcStatus,liveId])
    }
    @Override
    def getNotBeginLiveRecordsByTime(){
        def resultList=[]
        dataBases.msqlLiveSlave.eachRow("SELECT * from live_record where pgc_status=0 and begin_time<?",[new Date()]){
            resultList<<[liveId:it.live_id,foreshowId:it.foreshow_id,appId:it.app_id]
        }
        return resultList
    }
    @Override
    def findNotBeginForeshowListByTime(){
        def resultList=[]
        dataBases.msqlLiveSlave.eachRow("select * from live_foreshow where foreshow_status=? and begin_time<?",
            [LiveCommon.FORESHOW_STATUS_0,new Date()])
            {
            def foreshowBean=toForeshowBean(it)
            if(foreshowBean){
                resultList<<foreshowBean
            }
        }
        return  resultList
    }
    @Override
    def toForeshowBean(def obj){
        LiveForeshow liveForeshow
        try{
            liveForeshow = obj?new LiveForeshow(
                foreshowId: (obj.foreshow_id?: 0) as long,
                title: obj.title ?: "",
                imgUrl: obj.img_url ?: "",
               userId: (obj.user_id ?: 0) as long,
               userName: obj.user_name ?: "",
                userImage: obj.user_image ?: "",
                nickname: obj.nickname ?: "",
                beginTime: obj.begin_time ?: null,
                urlTag:  obj.url_tag?: "",
                url: obj.url?obj.url: "",
                sortNum:  (obj.sort_num ?: 0) as int,
                isRecommend:(obj.is_recommend ?: 0) as int,
                isTop: ( obj.is_top ?: 0) as int,
                isPush: ( obj.is_push ?: 0) as int,
                createTime:  obj.create_time ?: null,
                updateTime:  obj.update_time ?: null,
                appId:  obj.app_id?: "",
                foreshowStatus:( obj.foreshow_status ?: 0) as int,
                keyword: obj.keyword ?: "",
                srpId: obj.srp_id ?: "",
                liveRecordInfo:obj.live_record_info ?: "",
                newsId: ( obj.news_id ?: 0) as int,
                foreshowType: ( obj.foreshow_type ?: 0) as int,
                parentId:( obj.parent_id ?: 0) as long,
                description: obj.description ?: "",
                cateId: ( obj.cate_id ?: 0) as long,
                pgcTitle: obj.pgc_title ?: "",
                endTime: obj.end_time ?:null,
                childSort:( obj.child_sort ?: 0) as int,
                liveRecommend:( obj.live_recommend ?: 0) as int,
                recommendTime: obj.recommend_time ?: null
            ):null
        } catch (Exception e) {
            log.error("toForeshowBean,error:{}", e.getMessage())
        }
        return liveForeshow
    }

    @Override
    def findLiveRecordLogByFileId(String fileId) {
        return dataBases.msqlLiveSlave.firstRow("SELECT * FROM live_record_log WHERE video_address LIKE ?", ["%"+fileId+"%"])
    }

    @Override
    List findForeshowIdListForMerge() {
        def resultList = []
        try{
            Date updateTime = DateUtil.getRollDay(new Date(),-1)
            String sql ="SELECT * FROM live_foreshow WHERE foreshow_status = 2 and update_time >? ORDER BY begin_time desc"
            resultList = dataBases.msqlLiveSlave.rows(sql,[updateTime])
        }catch (Exception e){
            e.printStackTrace()
        }
        return resultList
    }

    @Override
    List findForeshowIdListForMergeValidate() {
        def resultList = []
        try{
            StringBuilder sb = new StringBuilder("SELECT * FROM live_foreshow WHERE foreshow_status = 2 and foreshow_type != 5 and update_time> ? and update_time < ? ORDER BY foreshow_id asc")
            Date gtDate = DateUtil.addDateByDay(new Date(),-1)
            Date ltDate = DateUtil.addDateByMinute(new Date(),-20)
            resultList = dataBases.msqlLiveSlave.rows(sb.toString(),[gtDate,ltDate])
        }catch (Exception e){
            e.printStackTrace()
        }
        return resultList
    }

    /**
     * 获取轮播数据
     * @return
     */
    def getRollImgList(String appId){
        def resultList = []
        try{
            StringBuilder sb = new StringBuilder("SELECT * FROM live_banner WHERE app_id = ? and begin_time <= ? and end_time > ? ORDER BY sort_num DESC,begin_time DESC limit 10")
            Date date = new Date()
            resultList = dataBases.msqlLiveSlave.rows(sb.toString(),[appId,date,date])
        }catch (Exception e){
            e.printStackTrace()
        }
        return resultList
    }

    /**
     * 根据id获取预告
     * @param foreshowId
     * @return
     */
    def getForeshowById(long foreshowId){
        dataBases.msqlLiveSlave.firstRow("SELECT *,(select count(1) from live_foreshow a where a.parent_id=b.foreshow_id and a.foreshow_status=2) as live_num FROM live_foreshow b WHERE foreshow_id = ?",[foreshowId])
    }

    @Override
    def findTestLiveRecordList() {
        Date date = DateUtil.getRollDay(new Date(), -3)
        return dataBases.msqlLiveSlave.rows("SELECT * from live_record_log where  live_status !=4 AND create_time <? ORDER  BY create_time desc",[date])
    }

    @Override
    def findZstestLiveRecordList() {
        Date date = DateUtil.getRollDay(new Date(), -3)
        return dataBases.msqlLiveSlave.rows("SELECT * from live_record_log where  live_status=2 or (live_status !=4 AND title like '%zstest%') AND live_mode=1 AND create_time <? ORDER  BY create_time desc",[date])
//        return dataBases.msqlLiveSlave.rows("SELECT * from live_record_log where live_status in (2,4) ")
    }

    @Override
    def findLiveRecordListForUpdateCountRedis() {
        def liveList = dataBases.msqlLiveSlave.rows("SELECT live_id FROM live_record ")
        def liveRecordList = dataBases.msqlLiveSlave.rows("SELECT live_id FROM live_record_log WHERE live_mode !=1 AND live_status != 4 ORDER BY create_time DESC ")
        if(liveList && liveRecordList){
            liveList.addAll(liveRecordList)
            return liveList
        }else if(!liveList){
            return liveRecordList
        }else
            return liveList
    }

    @Override
    def findDataListBySql(String sql) {
        return dataBases.msqlLiveSlave.rows(sql)
    }

    @Override
    def getUserLiveRecord(String appId, long userId, boolean isFollow) {
        StringBuilder sql = new StringBuilder("SELECT r.* FROM live_record r WHERE r.app_id = ? AND r.live_mode = 1 AND r.user_id = ?")
        if ( !isFollow){
            sql.append(" AND r.is_private = 0")
        }
        sql.append(" ORDER BY r.begin_time")
        return dataBases.msqlLiveSlave.firstRow(sql.toString(),[appId , userId])
    }

    @Override
    def saveForeshowSrpRelation(long foreshowId, long liveId, long parentId, List interestList, List srpList) {
        if (foreshowId == 0){
            return false
        }

        dataBases.msqlLive.execute("DELETE FROM live_foreshow_srp_relation WHERE foreshow_id =?", [foreshowId])
        StringBuilder sql = new StringBuilder("INSERT INTO `live_foreshow_srp_relation` (`srp_id`, `keyword`, `foreshow_id`, `live_id`, `category`, `parent_id`) VALUES")
        def sqlParams = []

        interestList?.each{
            sql.append("( ?, ?, ?, ?, 2, ?),")
            sqlParams +=[it.srpId,it.keyword,foreshowId,liveId, parentId]
        }

        srpList?.each{
            sql.append("( ?, ?, ?, ?, 1, ?),")
            sqlParams +=[it.srpId,it.keyword,foreshowId,liveId, parentId]
        }
        if (sqlParams){
            def res = dataBases.msqlLive.executeInsert(sql.substring(0,sql.length()-1),sqlParams)
            return res?res.size():0 == sqlParams.size();
        }
        return true
    }

    @Override
    def getListByIds(List ids){
        def resultList = []
        def id = Strings.toListString(ids)
        StringBuilder sb = new StringBuilder("SELECT * FROM live_foreshow WHERE foreshow_id in (${id})")
        dataBases.msqlLiveSlave.rows(sb.toString()).each {
            resultList << [
                foreshow_id:it.foreshow_id,
                title:it.title,
                show_title_in_list:it.show_title_in_list,
                img_url: it.img_url,
                user_id:it.user_id,
                user_name:it.user_name,
                nickname: it.nickname,
                user_image: it.user_image,
                begin_time: it.begin_time,
                url_tag: it.url_tag,
                url: it.url,
                sort_num: it.sort_num,
                create_time: it.create_time,
                update_time: it.update_time,
                foreshow_status: it.foreshow_status,
                foreshow_type: it.foreshow_type,
                description: it.description,
                live_num:0,
                live_record_info:it.live_record_info,

                "keyword": it.keyword,
                "srp_id": it.srp_id
            ]
        }
        return resultList
    }

    @Override
    def getAppIdsByForeshow(Map params){
        //生产预告的app
        StringBuilder sb = new StringBuilder("SELECT f.app_id FROM live_foreshow f WHERE f.foreshow_id = ${params.foreshowId}")
        sb.append(" UNION")
        //使用共享的app
        sb.append(" SELECT c.app_id FROM live_foreshow f INNER JOIN live_category_info c ON f.cate_id = c.share_cat_id AND f.foreshow_id = ${params.foreshowId}")
        def result = dataBases.msqlLiveSlave.rows(sb.toString())
        log.info("getAppIdsByForeshow params=>{},result=>{}",params,result)
        return result
    }

    @Override
    def getAppIdsFromConfig(){
        StringBuilder sb = new StringBuilder("SELECT app_id FROM live_app_config ")
        def result = dataBases.msqlLiveSlave.rows(sb.toString())
        return result
    }

    @Override
    def searchLiveListByKeyword(Map map) {
        List paramsList = []
        StringBuilder sql = new StringBuilder("SELECT * ")
        if(map.isGroup){
            sql.append(getLiveGroupLiveNumSql(map))
        }
        sql.append(" FROM live_foreshow b WHERE (b.cate_id in (${Strings.toListString(map.cateIds)}) or (b.cate_id=0 and b.app_id=?))")
        paramsList.add(map.appId)
        sql.append(" AND foreshow_status NOT IN (3,4)")
        def testUserList = testUser ?  Arrays.asList(testUser.split(",")): null;
        if(!testUserList || !testUserList.contains(map.userId as String)){
            sql.append(" and left(title,6)!='zstest' ");
        }
        if(map.keyword){
            sql.append(" AND title LIKE ?")
            paramsList.add("%"+map.keyword+"%")
        }
        if(map.isGroup){
            sql .append(" AND foreshow_type = 5 ")
        }else {
            //在非搜悦app中不显示非H5类型的未开始的互动预告
            if(!Strings.APP_NAME_SOUYUE.equals(map.appId)){
                sql.append(" and ((b.foreshow_status!=0 and b.foreshow_type != 5) or (b.foreshow_status=0 and b.url_tag = 85 and b.foreshow_type = 1) or b.foreshow_type in(2,3)) ");
            }
//            if(!Strings.APP_NAME_SOUYUE.equals(map.appId)){
//                sql.append(" and ((url_tag not in(24,10) and foreshow_type = 1) or foreshow_type in(2,3)) ");
//            }
        }
        sql.append(" AND not_show_in_client!= 1")
        if(map.lastId){
            sql.append(" AND begin_time < ?")
            paramsList.add(map.lastId)
        }
        sql.append(" ORDER BY begin_time DESC LIMIT ?")
        paramsList.add(map.psize as int)
        log.info("searchLiveListByKeyword,sql:{},params:{}",sql.toString(),paramsList)
        return dataBases.msqlLiveSlave.rows(sql.toString(),paramsList)
    }

    @Override
    def updateLiveThumpByLiveId(long liveId, String liveThump) {
        return dataBases.msqlLive.executeUpdate("UPDATE live_record_log SET live_thump=? WHERE live_id=?",[liveThump,liveId])
    }

    @Override
    def updateStatisticsInfoByLiveId(long liveId, String statisticsInfo) {
        return dataBases.msqlLive.executeUpdate("UPDATE live_record_log SET statistics_info=? WHERE live_id=?",[statisticsInfo,liveId])
    }

    @Override
    def getAllForeshowList() {
        return dataBases.msqlLiveSlave.rows("SELECT foreshow_id,create_time,update_time,live_record_info FROM live_foreshow WHERE live_record_info LIKE ? ORDER BY create_time DESC",["%video_address%"])
    }

    @Override
    def findCollectLiveByuserIdAndForeshowId(long userId, long foreshowId,String appId) {
        return dataBases.msqlLiveSlave.firstRow("SELECT * FROM live_collection WHERE foreshow_id=? AND user_id=? AND app_id = ?",[foreshowId, userId,appId])
    }

    @Override
    def collectLive(long userId, long foreshowId, String appId) {
        return dataBases.msqlLive.executeInsert("INSERT INTO live_collection(user_id,foreshow_id,create_time,app_id) VALUES (?,?,?,?)",[userId,foreshowId,new Date(),appId])
    }

    @Override
    def updateCollectLive(long userId, long foreshowId, int status, String appId) {
        return dataBases.msqlLive.executeUpdate("UPDATE live_collection SET status = ?,create_time=? WHERE user_id=? AND foreshow_id=? AND app_id=?",[status,new Date(),userId,foreshowId,appId])
    }

    @Override
    def findLiveCollectionListByUserId(Map map, String appId) {
        long createTime = map.sortInfo? (Strings.parseJson(map.sortInfo)?.createTime ?: 0) as long : 0
        String dateTime = DateUtil.getFormatDteByLong(createTime)
        StringBuilder sql = new StringBuilder("SELECT * FROM live_collection WHERE status = 0 AND user_id = ? AND app_id = ? ")
        def paramsMap = []
        paramsMap.add(map.userId as long)
        paramsMap.add(appId)
        if(dateTime){
            sql.append(" AND create_time < ? ")
            paramsMap.add(dateTime)
        }
        sql.append("ORDER BY create_time DESC LIMIT ?")
        paramsMap.add(map.psize as int)
        return dataBases.msqlLiveSlave.rows(sql.toString(), paramsMap)
    }

    @Override
    def delCollectionByForeshowId(long foreshowId) {
        return dataBases.msqlLive.executeUpdate("UPDATE live_collection SET status = 1,create_time=? WHERE foreshow_id=?",[new Date(),foreshowId])
    }

    @Override
    def updateLiveRecommendStatus(String tableName,long foreshowId,long liveId, int status,int operType) {
        String sql = ""
        List paramsList = []
        if (tableName.equals("live_record")){
            if(operType == 1){
                sql = "UPDATE live_record set live_recommend = ?,recommend_time= ? WHERE live_id=?"
                paramsList = [status,new Date(), liveId]
            }else if(operType == 2){
                sql = "UPDATE live_record set is_show_live_homepage = ? WHERE live_id=?"
                paramsList = [status,liveId]
            }else if(operType == 3){
                sql = "UPDATE live_record set live_recommend = ?,recommend_time= ?,is_show_live_homepage=? WHERE live_id=?"
                paramsList = [status,new Date(),status,liveId]
            }
        }else if (tableName.equals("live_record_log")){
            if(operType == 1){
                sql = "UPDATE live_record_log set live_recommend = ?,recommend_time= ? WHERE live_id=?"
                paramsList = [status,new Date(), liveId]
            }else if(operType == 2){
                sql = "UPDATE live_record_log set is_show_live_homepage = ? WHERE live_id=?"
                paramsList = [status,liveId]
            }else if(operType == 3){
                sql = "UPDATE live_record_log set live_recommend = ?,recommend_time= ?,is_show_live_homepage=? WHERE live_id=?"
                paramsList = [status,new Date(),status,liveId]
            }
        }else if (tableName.equals("live_foreshow")){
            if(operType == 1){
                sql = "UPDATE live_foreshow set live_recommend = ?,recommend_time= ? WHERE foreshow_id=?"
                paramsList = [status,new Date(), foreshowId]
            }else if(operType == 2){
                sql = "UPDATE live_foreshow set is_show_live_homepage = ? WHERE foreshow_id=?"
                paramsList = [status,foreshowId]
            }else if(operType == 3){
                sql = "UPDATE live_foreshow set live_recommend = ?,recommend_time= ?,is_show_live_homepage=? WHERE foreshow_id=?"
                paramsList = [status,new Date(),status,foreshowId]
            }
        }
        return dataBases.msqlLive.executeUpdate(sql,paramsList)
    }
    /**
     * 查询直播中预告列表
     * @param map
     * @return
     */
    def findLivingForeshowList(Map map){
        //TODO 查询直播中预告列表
    }

    /**
     * 查询暂停中预告列表
     * @param map
     * @return
     */
    def findPausedForeshowList(Map map){
        //TODO 查询暂停中预告列表
    }

    /**
     * 查询直播预告列表
     * @param map
     * @return
     */
    /*def findForeshowList(Map map){
        //TODO 查询直播预告列表
    }*/

    /**
     * 查询回放中预告列表
     * @param map
     * @return
     */
    def findOverForeshowList(Map map){
        //TODO 查询回放中预告列表
    }

    @Override
    def getForeshowNumberForSrpId(String srpId){
        StringBuilder sql = new StringBuilder("select count(*) as count from live_foreshow r where r.srp_id = ? and r.foreshow_status != 3 and r.foreshow_status != 4 ")
        def row= dataBases.msqlLiveSlave.firstRow(sql.toString(),[srpId])
        return row && row.count ? row.count as int : 0
    }

    @Override
    def findShareLiveByAppId(String appId,Integer type) {
        StringBuilder sql = new StringBuilder("SELECT * FROM live_share_relation WHERE share_app_id = ?")
        def paramsMap = [appId]
        if(type != null){
            sql.append(" and type=?")
            paramsMap << type
        }
        def result = dataBases.msqlLiveSlave.rows(sql.toString(), paramsMap)
        def listId = []
        result.each {
            listId << it.extend_id
        }
        return result
    }

    @Override
    List findNoticeLive() {
        def resultList = []
        try{
            StringBuilder sql = new StringBuilder("select * from live_record where (live_type=1 and  pgc_status=1 and begin_time> ? and begin_time < ? )")
            sql.append("or (live_type=0 and create_time >? and create_time < ?) limit 50")
            Date gtDate = DateUtil.addDateByMinute(new Date(),-2)
            Date ltDate = new Date()
            resultList = dataBases.msqlLiveSlave.rows(sql.toString(),[gtDate,ltDate,gtDate,ltDate])
        }catch (Exception e){
            e.printStackTrace()
        }
        return resultList
    }

    @Override
    List findNoticeMobie() {
        def resultList = []
        try{
            StringBuilder sql = new StringBuilder("select * from live_union_account where status=1 limit 50")
            resultList = dataBases.msqlLiveSlave.rows(sql.toString())
        }catch (Exception e){
            e.printStackTrace()
        }
        return resultList
    }


}
