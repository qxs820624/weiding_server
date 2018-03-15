package zs.live.dao.mysql.impl

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import zs.live.dao.mysql.DataBases
import zs.live.dao.mysql.LiveForeshowRes
import zs.live.entity.LiveForeshow
import zs.live.utils.DateUtil
import zs.live.utils.Strings
import zs.live.utils.VerUtils

/**
 * Created by Administrator on 2016/12/14.
 */

@Slf4j
@Repository
class LiveForeshowResImpl implements LiveForeshowRes {

    @Autowired
    DataBases dataBases;

    @Value('${live.test.user.list}')
    String testUser

    @Override
    public LiveForeshow get(long foreshowId){
        LiveForeshow entity = null
        def res = getLiveForeshowInfo(foreshowId)
        if (res){
            entity = new LiveForeshow()
            entity.foreshowId = res.foreshow_id
            entity.title = res.title
            entity.imgUrl = res.img_url
            entity.userId = res.user_id?:0
            entity.userName = res.user_name
            entity.nickname = res.nickname
            entity.userImage = res.user_image
            entity.beginTime = res.begin_time
            entity.urlTag = res.url_tag?:0
            entity.url = res.url
            entity.sortNum = res.sort_num?:0
            entity.isRecommend = res.is_recommend?:0
            entity.isTop = res.is_top?:0
            entity.isPush = res.is_push?:0
            entity.createTime = res.create_time
            entity.updateTime = res.update_time
            entity.endTime = res.end_time
            entity.appId = res.app_id
            entity.foreshowStatus = res.foreshow_status?:0
            entity.keyword = res.keyword
            entity.srpId = res.srp_id
            entity.liveRecordInfo = res.live_record_info
            entity.newsId = res.news_id?:0
            entity.foreshowType = res.foreshow_type
            entity.parentId = res.parent_id?:0
            entity.description = res.description
            entity.descriptionHtml = res.description_html
            entity.cateId = res.cate_id?:0
            entity.liveNum = res.live_num?:0
            entity.isCost = res.is_cost?:0
            entity.isSale = res.is_sale?:0
            entity.ruleJson = res.rule_json

        }
        return entity;
    }

    @Override
    public def getLiveForeshowInfo(long foreshowId){
        String sql = "SELECT *,(select count(DISTINCT log.foreshow_id) from live_foreshow a JOIN live_record_log log ON log.foreshow_id = a.foreshow_id where a.parent_id=b.foreshow_id and a.foreshow_status=2 and log.live_status=1) as live_num from live_foreshow b where foreshow_id=?";
        def queryParam = [foreshowId];
        return dataBases.msqlLiveSlave.firstRow(sql, queryParam);
    }

    @Override
    public int insert(LiveForeshow liveForeshow){
        String sql = """INSERT live_foreshow(title,img_url,user_id,user_name,nickname,user_image,begin_time,url_tag,url,
            sort_num,is_recommend,is_top,is_push,create_time,update_time,app_id,foreshow_status,
            keyword,srp_id,live_record_info,news_id,foreshow_type,parent_id,description,cate_id,pgc_title,
            show_title_in_list,not_show_in_client,description_html,is_cost,is_sale,rule_json)
        Values(?,?,?,?,?,?,?,?,?,?,?,?,?,now(),now(),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""";

        List res = dataBases.msqlLive.executeInsert(sql,
            [liveForeshow.title, liveForeshow.imgUrl, liveForeshow.userId,liveForeshow.userName,liveForeshow.nickname,
             liveForeshow.userImage,liveForeshow.beginTime,liveForeshow.urlTag, liveForeshow.url,liveForeshow.sortNum,
             liveForeshow.isRecommend,liveForeshow.isTop,liveForeshow.isPush, liveForeshow.appId,
             liveForeshow.foreshowStatus, liveForeshow.keyword,liveForeshow.srpId,liveForeshow.liveRecordInfo,liveForeshow.newsId,
             liveForeshow.foreshowType, liveForeshow.parentId,liveForeshow.description,liveForeshow.cateId,liveForeshow.pgcTitle,
             liveForeshow.showTitleInList,liveForeshow.notShowInClient,liveForeshow.descriptionHtml,liveForeshow.isCost,
             liveForeshow.isSale,liveForeshow.ruleJson]);
        return res?.size() ? (res.get(0).get(0) ?: 0)as int : 0
    }

    @Override
    public int update(LiveForeshow liveForeshow){
        String sql = """UPDATE live_foreshow  SET title =?,  img_url =?,  user_id =?,  user_name =?,  nickname =?,  user_image =?,
            begin_time =?,  url_tag =?,  url =?,  sort_num =?,  is_recommend =?,  is_top =?,  is_push =?, update_time =now(),
            foreshow_status =?,  keyword =?,  srp_id =?,  live_record_info =?,  news_id =?,  foreshow_type =?,  parent_id =?,
            description =?,  cate_id =? , pgc_title = ?,show_title_in_list=?,not_show_in_client=?,
            description_html =?,is_cost =?,is_sale =?,rule_json=? WHERE foreshow_id =?""";

        return dataBases.msqlLive.executeUpdate(sql,
            [liveForeshow.title, liveForeshow.imgUrl, liveForeshow.userId,liveForeshow.userName,liveForeshow.nickname,
             liveForeshow.userImage,liveForeshow.beginTime,liveForeshow.urlTag, liveForeshow.url,liveForeshow.sortNum,
             liveForeshow.isRecommend,liveForeshow.isTop,liveForeshow.isPush,liveForeshow.foreshowStatus, liveForeshow.keyword,liveForeshow.srpId,
             liveForeshow.liveRecordInfo,liveForeshow.newsId,liveForeshow.foreshowType, liveForeshow.parentId,liveForeshow.description,
             liveForeshow.cateId,liveForeshow.pgcTitle,liveForeshow.showTitleInList,liveForeshow.notShowInClient,
             liveForeshow.descriptionHtml,liveForeshow.isCost,liveForeshow.isSale,liveForeshow.ruleJson,liveForeshow.foreshowId]);
    }

    /**
     * 根据parentId取出直播预告
     * @param foreshowId
     * @return
     */
    @Override
    List listForeShowByParentId(Map paramMap){
        long foreshowId = paramMap.parentId
        int psize = paramMap.psize?:0
        String sortInfo = paramMap.sortInfo
        long userId = paramMap.userId
        String appId = paramMap.appId
        def status = paramMap.status
        String vc = paramMap.vc
        def from = paramMap.from

        def testUserList = testUser ?  Arrays.asList(testUser.split(",")): null;

        StringBuilder sql =new StringBuilder(" select t1.* ,log.room_id,log.live_id,log.video_address,log.total_watch_count from (select t2.* from live_foreshow t2 where  t2.parent_id=? and t2.foreshow_type<5 ");
        List params = [foreshowId]
        long createTime = 0
        int orderId = 0
        if (sortInfo){
            createTime = (Strings.parseJson(sortInfo)?.createTime ?: 0) as long
            orderId = (Strings.parseJson(sortInfo)?.orderId ?:0) as int
        }
        if(orderId){
            sql.append(" AND t2.child_sort < ?")
            params << orderId
        }else if(createTime){
            String ctimeString = DateUtil.getFormatDteByLong(createTime)
            sql.append(" AND t2.child_sort = 0 AND t2.begin_time < ? ")
            params << ctimeString
        }

        if(!testUserList || !testUserList.contains(userId as String)){
            sql.append(" and left(t2.title,6)!='zstest' ");
        }
        //5.6以前的版本不显示手机采集的会议直播
        if(VerUtils.toIntVer(vc) < VerUtils.toIntVer("5.6")) {
            sql.append(" and t2.foreshow_type!=3 ");
        }
        //需要显示的设备 1 h5(h5) 2 客户端(app)
        if (from == 'h5'){
            sql.append(" and t2.not_show_in_client != 2")
        }else{
            sql.append(" and t2.not_show_in_client != 1")
        }

        if (status){
            sql.append(" and t2.foreshow_status=? ");
            params << status
        }else {
            sql.append(" and (t2.foreshow_status=0 or t2.foreshow_status=1 or t2.foreshow_status=2 or t2.foreshow_status=6) ");
        }
        sql.append(" ORDER BY t2.child_sort DESC,t2.begin_time ");

        if (psize>0){
            sql.append(" limit ?");
            params << psize
        }
        sql.append(" ) as t1 ");
        sql.append(" left join live_record_log log on log.foreshow_id=t1.foreshow_id ")
//        sql.append(" where 1=1 and log.live_status=1 and log.video_address is not null and log.is_private=0 ")
        sql.append(" ORDER BY t1.child_sort DESC,t1.begin_time DESC  ")

        log.info("LiveForeshowResImpl.listForeShowByParentId====>{},params====>{}",sql.toString(),params)
        def rows = dataBases.msqlLiveSlave.rows(sql.toString(),params)
        List resultList = []
        rows.each{
            resultList << [
                    chargeType: it.foreshow_type==3 ? 1 : 2,//1:收费，2:不收费
                    foreshowId:it.foreshow_id,
                    description:it.description,
                    title:it.title,
                    showTitle: it.show_title_in_list,
                    beginTime:it.begin_time,
                    liveRecordInfo:it.live_record_info,
                    liveThumb:it.img_url,
                    srpId:it.srp_id,
                    keyword:it.keyword,
                    shortUrl:it.url,
                    foreshowType:it.foreshow_type,
                    anchorInfo:[
                            userId:it.user_id,
                            nickname:it.nickname,
                            userImage:it.user_image
                    ],
                    sortInfo:[
                            orderId:it.child_sort,
                            createTime:DateUtil.getTimestamp(it.begin_time as String)
                    ],
                    roomId:it.room_id,
                    liveId:it.live_id,
                    videoAddress: it.video_address,
                    totalWatchCount:it.total_watch_count
            ]
        }
        return resultList
    }

    @Override
    int fillLiveForeshow(long foreshowId, long cateId, List<Long> subForeshowIdList, String appId) {
        StringBuilder sql = new StringBuilder("UPDATE live_foreshow SET parent_id =?,cate_id = ? WHERE app_id = '"+ appId +"' AND foreshow_type IN (1,2,3) AND foreshow_id IN (")
        subForeshowIdList.size().times{
            sql.append(" ?,")
        }
        sql.deleteCharAt(sql.length()-1)
        sql.append(")")
        def params = [foreshowId, cateId]
        params.addAll(subForeshowIdList)
        log.info("LiveForeshowResImpl.fillLiveForeshow sql====>{}, params====>{}", sql.toString(), params)
        return dataBases.msqlLive.executeUpdate(sql.toString(), params);
    }

    @Override
    def findForeshowListByParentId(long foreshowId, String appId) {
        String sql = "SELECT * FROM live_foreshow WHERE app_id = ? AND parent_id=? AND foreshow_status = 2 ORDER BY begin_time DESC"
        return dataBases.msqlLiveSlave.rows(sql.toString(),[appId,foreshowId])
    }

    @Override
    List<Long> listLiveIdByParentId(long foreshowId, int status) {
        StringBuilder sql =new StringBuilder("select  t2.live_id from live_foreshow t1 right join live_record_log t2 on t1.foreshow_id=t2.foreshow_id  where t1.parent_id=? and t1.foreshow_type<5 ");
        List params = [foreshowId]

        sql.append(" and t1.foreshow_status=? ");
        params << status
        def rows = dataBases.msqlLiveSlave.rows(sql.toString(),params)
        def resultList = []
        rows.each{
            resultList << it.live_id
        }
        resultList
    }
    /**
     * 根据预告id查询
     * @param foreshowId
     * @param status
     * @return
     */
    List<Long> listLiveIdById(long foreshowId, int status) {
        StringBuilder sql =new StringBuilder("select  t2.live_id from live_foreshow t1 left join live_record_log t2 on t1.foreshow_id=t2.foreshow_id  where t1.foreshow_id=? and t1.foreshow_type<5 ");
        List params = [foreshowId]


        sql.append(" and t1.foreshow_status=? ");
        params << status


        def rows = dataBases.msqlLiveSlave.rows(sql.toString(),params)
        def resultList = []
        rows.each{
            resultList << it.live_id
        }
        resultList
    }

    def updateParentId(long foreshowId,String appId){
        String sql ="update live_foreshow set parent_id = 0,cate_id=0 where parent_id=? and app_id = ?";
        log.info("LiveForeshowResImpl.updateParentId=====>{},foreshowId=====>{}",sql,foreshowId)
        return dataBases.msqlLive.executeUpdate(sql,[foreshowId, appId]);
    }
    def updateParentIdByForeshowId(long foreshowId,String appId){
        String sql ="update live_foreshow set parent_id = 0,cate_id=0 where foreshow_id=? and app_id = ?";
        log.info("LiveForeshowResImpl.updateParentId=====>{},foreshowId=====>{}",sql,foreshowId)
        return dataBases.msqlLive.executeUpdate(sql,[foreshowId, appId]);
    }

    @Override
    def setForeshowCateId( long foreshowId, long cateId ){
        String sql ="update live_foreshow set cate_id = ? where foreshow_id=?";
        log.info("LiveForeshowResImpl.setForeshowCateId=====>{},foreshowId=====>{}",sql,foreshowId)
        return dataBases.msqlLive.executeUpdate(sql,[cateId,foreshowId]);
    }

    def updateStatus(long foreshowId,int status){
        String sql ="update live_foreshow set foreshow_status =? where foreshow_id=?";
        List params = [status,foreshowId]
        log.info("------------LiveForeshowResImpl.pause--------->>>>>>{}",sql)
        log.info("------------LiveForeshowResImpl.pause--------->>>>>>{}",params)
        dataBases.msqlLive.executeUpdate(sql,params);
    }

    @Override
    def listInfoByParentIds(List<Long> parentIdList, int status,String appId,String vc) {
        def resultList = []
        if (!parentIdList){
            return resultList
        }
        StringBuilder sql = new StringBuilder("SELECT t1.parent_id,t1.title,t1.foreshow_id,t1.begin_time " )
        if(VerUtils.toIntVer(vc) < VerUtils.toIntVer("5.6.7")) {
            sql.append(", count(t1.parent_id) as count ")
        }else {
            sql.append(",((select count(*) from live_foreshow f where f.parent_id=t1.parent_id " )
            //5.6以前的版本不显示手机采集的会议直播
            if(VerUtils.toIntVer(vc) < VerUtils.toIntVer("5.6")) {
                sql.append(" and f.foreshow_type!=3 ");
            }
            if(!Strings.APP_NAME_SOUYUE.equals(appId)){
                sql.append(" and (f.foreshow_status in (1,6) or(f.foreshow_status = 0 and ((f.url_tag = 85 and f.foreshow_type = 1) or f.foreshow_type in(2,3)))) ");
            }else{
                sql.append(" and f.foreshow_status in (0,1,6) ")
            }
            sql.append(") + (select count(DISTINCT log.foreshow_id) from live_foreshow a JOIN live_record_log log ON log.foreshow_id = a.foreshow_id where a.parent_id=t1.parent_id and a.foreshow_status=2 and log.live_status=1")
            //5.6以前的版本不显示手机采集的会议直播
            if(VerUtils.toIntVer(vc) < VerUtils.toIntVer("5.6")) {
                sql.append(" and a.foreshow_type!=3 ");
            }
            sql.append("))  as count ")
        }

        sql.append("FROM live_foreshow t1 WHERE t1.parent_id in(")
        List params = []
        parentIdList.size().times{
            sql.append(" ?,")
        }
        sql.deleteCharAt(sql.length()-1)
        sql.append(")")
        params.addAll(parentIdList)

        if(VerUtils.toIntVer(vc) < VerUtils.toIntVer("5.6.7")){
            sql.append(" AND t1.foreshow_status=? ");
            params << status
        }else {
            sql.append(" AND t1.foreshow_status not in (3,4,5) ");
        }


        sql.append(" GROUP BY t1.parent_id HAVING t1.begin_time = (SELECT MAX(t2.begin_time) FROM live_foreshow t2  WHERE t2.foreshow_id = t1.foreshow_id)")
        log.info("------------LiveForeshowResImpl.listInfoByParentIds--------->>>>>>{}",sql)
        log.info("------------LiveForeshowResImpl.listInfoByParentIds--------->>>>>>{}",params)
        def rows = dataBases.msqlLiveSlave.rows(sql.toString(),params)

        rows.each{
            resultList << [
                    title: "回放："+it.title, //最新预告的标题
                    parentId: it.parent_id, //预告的标题
                    count: it.count //同一系列共几场
            ]
        }
        resultList
    }


    @Override
    def updateUpdateTime(long foreshowId,Date updateTime){
        String sql ="update live_foreshow set begin_time =? where foreshow_id=?";
        List params = [updateTime,foreshowId]
      return   dataBases.msqlLive.executeUpdate(sql,params);
    }

    @Override
    def insertPauseLog(Map map) {
        String sql = "insert live_foreshow_pause_log(foreshow_id,live_id,pause_message,create_time) values(?,?,?,now())"
        List params = [map.foreshowId,map.liveId,map.pauseMessage]
        List res = dataBases.msqlLive.executeInsert(sql,params)
        return res?.size() ? (res.get(0).get(0) ?: 0)as int : 0
    }

    @Override
    def findPauseLog(long foreshowId, long liveId) {
        String sql = "select * from live_foreshow_pause_log where foreshow_id = ? and live_id = ? order by id desc"
        List params = [foreshowId,liveId]
        return dataBases.msqlLiveSlave.firstRow(sql,params)
    }

    @Override
    def resetForeshowOrder(){
        String sql ="update live_foreshow set sort_num = 0 where update_time < ? and sort_num >0 and foreshow_status=2";
        List params = [DateUtil.addDateByDay(new Date(),-1)]
//        List params = [DateUtil.addDateByMinute(new Date(),-30)]//测试用
        println "resetForeshowOrder >> " + params
        return dataBases.msqlLive.executeUpdate(sql,params);
    }
}
