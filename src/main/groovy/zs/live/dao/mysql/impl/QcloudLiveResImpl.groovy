package zs.live.dao.mysql.impl

import com.alibaba.fastjson.JSON
import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import groovy.sql.GroovyRowResult
import groovy.util.logging.Slf4j
import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import zs.live.common.QcloudCommon
import zs.live.dao.mongo.LiveMongodb
import zs.live.dao.mysql.DataBases
import zs.live.dao.mysql.QcloudLiveRes
import zs.live.dao.redis.LiveRedis
import zs.live.entity.LiveRecord
import zs.live.utils.DateUtil
import zs.live.utils.Strings

/**
 * Created by Administrator on 2016/10/13.
 */
@Service
@Slf4j
class QcloudLiveResImpl implements QcloudLiveRes{

    @Autowired
    DataBases dataBases
    @Autowired
    LiveRedis liveRedis
    @Autowired
    LiveMongodb liveMongodb

    @Override
    boolean createLiveRecord(LiveRecord live) {
        try{
            def backList = dataBases.msqlLive.executeInsert(
                """
                INSERT live_record(live_id,title,srp_id,keyword,app_id,app_model,live_thump,live_bg,user_id,user_image,nickname,room_id,
                chat_id,create_time,is_private,foreshow_id,live_type,vc,m3u8_url,play_url,channel_id,
                rtmp_url,begin_time,pgc_type,pgc_status,brief,field_control_user_id,compere_user_id,live_mode,xiaoyu_live_id,xiaoyu_id,xiaoyu_conf_no,
                view_json,brief_html,live_from_sdk)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """,
                [
                    live.liveId,live.title,live.srpId,live.keyword,live.appId,live.appModel ,live.liveThump ,live.liveBg,live.userId ,live.userImage ,
                    live.nickname ,live.roomId ,live.chatId ,live.createTime ,live.isPrivate,live.foreshowId,live.liveType,live.vc,live.m3u8Url,live.playUrl,live.channelId,
                    live.rtmpUrl,live.beginTime,live.pgcType,live.pgcStatus,live.brief,live.fieldControlId,live.compereUserId,live.liveMode,live.xiaoYuLiveId,live.xiaoYuId?:"",
                    live.xiaoYuConfNo,JSON.toJSONString(live.viewJson),live.briefHtml,live.liveFromSdk
                ]
            )
            log.info("create live record return=>{}",backList)
        }catch (Exception e){
            e.printStackTrace()
        }
        return true
    }

    @Override
    boolean updateLiveRecord(Map updateParam) {
        boolean res = false
        try{
            int i = dataBases.msqlLive.executeUpdate(
                "UPDATE live_record set channel_id = ? , m3u8_url = ?, push_time = ?,update_time = ? where live_id = ?",
                [updateParam.channelId,updateParam.m3u8Url,updateParam.pushTime,updateParam.pushTime,updateParam.liveId]
            )
            res = i>0
            log.info("update channelId return value=>{},updateParam=>{}",i,updateParam);
        }catch (Exception e){
          log.error("updateLiveRecord",e);
        }
        return res
    }

    @Override
    boolean updateLiveRecordForPgc(Map updateParam) {
        boolean res = false
        try{
            int i = dataBases.msqlLive.executeUpdate(
                "update live_record set title=?,xiaoyu_id=?, begin_time =?,live_bg = ? ," +
                    "compere_user_id=?,field_control_user_id=?,brief=? ,brief_html=? where live_id = ?",
                [updateParam.title,updateParam.xiaoyuId,updateParam.beginTime,
                 updateParam.liveBg,updateParam.compereUserId,updateParam.fieldControlId,updateParam.brief,updateParam.briefHtml,updateParam.liveId]
            )
            res = i>0
        }catch (Exception e){
            log.error("updateLiveRecordForPgc",e);
        }
        return res
    }

    @Override
    boolean updateLiveRecordLogForPgc(Map updateParam) {
        boolean res = false
        try{
            int i = dataBases.msqlLive.executeUpdate(
                "update live_record_log set title=?,xiaoyu_id=?, begin_time =?,live_bg = ? ," +
                    "compere_user_id=?,field_control_user_id=?,brief=? ,brief_html=? where live_id = ?",
                [updateParam.title,updateParam.xiaoyuId,updateParam.beginTime,
                 updateParam.liveBg,updateParam.compereUserId,updateParam.fieldControlId,updateParam.brief,updateParam.briefHtml,updateParam.liveId]
            )
            res = i>0
        }catch (Exception e){
            log.error("updateLiveRecordLogForPgc",e);
        }
        return res
    }

    @Override
    boolean updateLiveRecordForPgcRelateForeshow(Map updateParam) {
        boolean res = false
        try{
            int i = dataBases.msqlLive.executeUpdate(
                "update live_foreshow set  begin_time =? ,pgc_title=? where foreshow_id = ?",
                [updateParam.beginTime, updateParam.pgcTitle,updateParam.foreshowId]
            )
            res = i>0
        }catch (Exception e){
            log.error("updateLiveRecordForPgcRelateForeshow",e);
        }
        return res
    }

    @Override
    int getRoomIdByUserIdAndAppId(long userId,String appId){
        def result = dataBases.msqlLiveSlave.firstRow("SELECT room_id as roomId from live_user_av_room where user_id=? and app_id=?",
            [userId,appId]
        )
        return result?.roomId?:0
    }

    @Override
    boolean insertRoomId(long userId,String appId, int roomId){
        def backList = dataBases.msqlLive.executeInsert(
            "INSERT live_user_av_room (room_id,user_id,app_id) VALUES (?,?,?)",
            [roomId,userId,appId]
        )
        log.info("insert live_user_av_room backList: "+ backList)
        return true
    }

    boolean insertLiveRecordLog(LiveRecord l,Map liveRecordMap){
        boolean res = false
        try{
            def backList = dataBases.msqlLive.executeInsert(
                """
               INSERT live_record_log(live_id,title,srp_id,keyword,app_id,app_model,live_thump,live_bg,user_id,
                                        user_image,nickname,room_id,
                               chat_id,create_time,is_private,time_span,live_status,m3u8_url,channel_id,task_id,
                               push_time,update_time,total_watch_count,vest_count,watch_count,foreshow_id,live_type,admire_count,vc,play_url,
                               field_control_user_id,compere_user_id,brief,brief_html,live_mode,pgc_status,begin_time,pgc_type,rtmp_url,xiaoyu_id,xiaoyu_conf_no,xiaoyu_live_id,view_json,live_from_sdk,live_recommend,recommend_time,is_show_live_homepage)
                    Values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE update_time = ?
                """,
                [   l.liveId,l.title,l.srpId,l.keyword,l.appId,l.appModel,l.liveThump,l.liveBg,l.userId,
                    l.userImage,l.nickname,l.roomId,
                    l.chatId,l.createTime,l.isPrivate,l.timeSpan,l.liveStatus,l.m3u8Url,l.channelId,l.taskId,
                    l.pushTime? DateUtil.getDate(l.pushTime,DateUtil.FULL_DATE_PATTERN) : null,
                    new Date(),//updateTime　存入更新时间
                    l.totalWatchCount,
                    l.vestCount,
                    l.watchCount,
                    l.foreshowId,
                    l.liveType,
                    l.admireCount,
                    l.vc,
                    l.playUrl,
                    l.fieldControlId,
                    l.compereUserId,
                    l.brief,
                    l.briefHtml,
                    l.liveMode,
                    l.pgcStatus,
                    l.beginTime ? DateUtil.getDate(l.beginTime,DateUtil.FULL_DATE_PATTERN) : null,
                    l.pgcType,
                    l.rtmpUrl,
                    l.xiaoYuId,
                    l.xiaoYuConfNo,
                    l.xiaoYuLiveId,
                    JSON.toJSONString(l.viewJson),
                    l.liveFromSdk,
                    l.liveRecommend,
                    l.recommendTime ? DateUtil.getDate(l.recommendTime,DateUtil.FULL_DATE_PATTERN) : null,
                    l.isShowLiveHomepage,
                    new Date() //updateTime
                 ]
            )
            res = true
            log.info("insert live_record_log backList: "+ backList)
        }catch (Exception e){
            e.printStackTrace()
        }
        return res
    }

    /**
     * 删除live_record记录
     * @param liveId
     * @return
     */
    boolean deleteLiveRecord(long liveId){
        return  dataBases.msqlLive.execute("delete from live_record where live_id="+liveId)
    }

    int getLiveIdByRoomId(int roomId){
        GroovyRowResult result;
        try{
            result = dataBases.msqlLiveSlave.firstRow("SELECT live_id as liveId from live_record where room_id=?", [roomId])
        }catch (Exception e){
            e.printStackTrace()
        }
        return result?result.liveId:0
    }

    int getLiveIdByRoomIdFromLog(int roomId){
        GroovyRowResult result;
        try{
            result = dataBases.msqlLiveSlave.firstRow("SELECT live_id as liveId from live_record_log where room_id=? order by id desc", [roomId])
        }catch (Exception e){
            e.printStackTrace()
        }
        return result?result.liveId:0
    }

    @Override
    int updateLiveRecordVideoAddress(String videoAddress, long liveId,String fileId) {
        List vo = Strings.parseJson(videoAddress,List)
        if(!vo || vo.size() == 0){
            videoAddress = null
        }
        int res = 0
        if(StringUtils.isNotBlank(fileId)){
            //fileId不为空时 只更新fileId
            try{
                res = dataBases.msqlLive.executeUpdate(
                    "UPDATE live_record_log set update_time = ?,file_id = ? where live_id = ?",
                    [new Date(),fileId,liveId]
                )
                log.info("updateLiveRecordFileId=>{},liveId=>{},fileId=>{}",res,liveId,fileId);
            }catch (Exception e){
                e.printStackTrace()
            }
        }else{//否则更新videoAddress
            try{
                res = dataBases.msqlLive.executeUpdate(
                    "UPDATE live_record_log set video_address = ?,update_time = ? where live_id = ?",
                    [videoAddress,new Date(),liveId]
                )
                log.info("updateLiveRecordVideoAddress=>{},liveId=>{}，videoAddress=>{}",res,liveId,videoAddress);
            }catch (Exception e){
                e.printStackTrace()
            }
        }

        return res
    }

    @Override
    List<LiveRecord> getNoBackVedioLiveRecordListToday(){
        List<LiveRecord> list = new ArrayList<LiveRecord>();
        try {
            dataBases.msqlLiveSlave.eachRow("SELECT * from live_record_log where live_status != 4 and to_days(update_time) = TO_DAYS(NOW()) and channel_id is not null and live_mode = 1",{
            //kpc 2016.12.06 改成扫描当天的直播 有地址也扫。。  当天不测试的话 不会发太多直播的 如果以后太多的话 可以控制一下小时，或者有回看了 就不处理了
            //这么修改主要是处理fileId是多段的情况，可能其中一段生成了，另一端没生成 这样就会出现视频不完整的情况
                LiveRecord l = new LiveRecord()
                l.liveId = (it.live_id?:0) as long
                l.roomId = (it.room_id?:0) as int
                l.foreshowId = (it.foreshow_id?:0) as long
                l.appId = it.app_id
                l.channelId = it.channel_id
                l.liveStatus = (it.live_status?:0) as int
                l.fileId = it.file_id
                l.videoAddress = it.video_address
                l.timeSpan = (it.time_span?:0) as long
                l.title = it.title
                list.add(l)
            })
        }catch (Exception e){
            e.printStackTrace()
        }
        return list
    }

    List<LiveRecord> getPgcNoBackVedioLiveRecordList(){
        List<LiveRecord> list = new ArrayList<LiveRecord>();
        try {
            Date endDate = DateUtil.getRollDay(new Date(),-1)
            dataBases.msqlLiveSlave.eachRow("SELECT * from live_record_log where live_status != 4  and update_time > ${endDate} and live_mode in(2,3) and (statistics_info ='' or statistics_info is null) ",{
                LiveRecord l = new LiveRecord()
                l.liveId = it.live_id as long
                l.roomId = it.room_id as int
                l.foreshowId = it.foreshow_id as long
                l.appId = it.app_id
                l.channelId = it.channel_id
                l.liveStatus = it.live_status
                l.fileId = it.file_id
                l.pgcType = it.pgc_type
                l.xiaoYuConfNo = it.xiaoyu_conf_no
                l.xiaoYuId = it.xiaoyu_id
                l.xiaoYuLiveId = it.xiaoyu_live_id
                l.videoAddress=it.video_address;
                l.m3u8Url=it.m3u8_url;
                l.statisticsInfo=it.statistics_info
                l.title=it.title
                l.updateTime = it.update_time
                list.add(l)
            })
        }catch (Exception e){
            e.printStackTrace()
        }
        return list
    }

    @Override
    List<LiveRecord> getQcloudNoBackVedioLiveRecordList() {
        List<LiveRecord> list = new ArrayList<LiveRecord>();
        try {
            Date endDate = DateUtil.getRollDay(new Date(),-1)
            dataBases.msqlLiveSlave.eachRow("SELECT * from live_record_log where live_status != 4  and update_time > ${endDate} and pgc_type !=2 order by live_id desc",{

                //and video_address is null 暂时去掉这个条件
                //kpc 2016.12.06 改成扫描当天的直播 有地址也扫。。  当天不测试的话 不会发太多直播的 如果以后太多的话 可以控制一下小时，或者有回看了 就不处理了
                //这么修改主要是处理fileId是多段的情况，可能其中一段生成了，另一端没生成 这样就会出现视频不完整的情况
                //暂时注释掉
                // dataBases.msqlLiveSlave.eachRow("SELECT * from live_record_log where live_status != 4 and to_days(update_time) = TO_DAYS(NOW())  ",{
                LiveRecord l = new LiveRecord()
                l.liveId = it.live_id as long
                l.roomId = it.room_id as int
                l.foreshowId = it.foreshow_id as long
                l.appId = it.app_id
                l.channelId = it.channel_id
                l.liveStatus = it.live_status
                l.fileId = it.file_id
                l.pgcType = it.pgc_type
                l.xiaoYuConfNo = it.xiaoyu_conf_no
                l.xiaoYuId = it.xiaoyu_id
                l.xiaoYuLiveId = it.xiaoyu_live_id
                l.videoAddress=it.video_address;
                l.liveMode = it.live_mode
                l.userId = it.user_id
                l.createTime = it.create_time
                l.beginTime = it.create_time
                l.updateTime = it.update_time
                l.title = it.title
                list.add(l)
            })
        }catch (Exception e){
            e.printStackTrace()
        }
        return list
    }

    @Override
    def addQcloudMsg(Map params){
        try{
            long liveId = (params.liveId?:0) as long
            String user_id = (params.userId?:"") as String
            int user_action = params.userAction as int
            if(!liveId || !user_id
                || user_action==QcloudCommon.AVIMCMD_ENTERLIVE_FILL_DATA
                || user_action==QcloudCommon.AVIMCMD_SYNCHRONIZING_INFORMATION){
                return
            }
            DBObject dbObject = new BasicDBObject()
            dbObject.put("live_id",params.liveId as long)
            dbObject.put("room_id",params.roomId as int)
            dbObject.put("msg",params.msg)
            dbObject.put("user_id",params.userId as String) //String类型,取的是腾讯的Account数据，有可能出现String，如：adminjava
            dbObject.put("user_action",user_action)
            dbObject.put("msg_type",params.msgType)
            dbObject.put("create_time",params.createTime as long)
            liveMongodb.insertLiveMsg(dbObject)
        }catch (Exception e){
            e.printStackTrace()
        }
    }
    @Override
    def findQcloudMsgList(Map params){
        int psize = params.psize as int
        long liveId = params.liveId as long
        long lastId = params.lastId as long
        DBObject query = new BasicDBObject()
        query.append("live_id", liveId)
        query.append("user_action", new BasicDBObject('$in',[QcloudCommon.AVIMCMD_WITH_USER_ROLE,QcloudCommon.AVIMCMD_SEND_MESSAGE]))
        query.append("msg_type",QcloudCommon.CALLBACK_BEFORE_SENDMSG)
        if(lastId > 0){
            query.append("create_time", new BasicDBObject('$lt', lastId))
        }
        DBObject sort = new BasicDBObject()
        sort.append("create_time",-1)
        return liveMongodb.findLiveMsgList(query,sort,psize);
    }
    @Override
    long findQcloudJoinRoomUserList(Map params){
        long liveId = params.liveId as long
        DBObject query = new BasicDBObject()
        query.append("live_id", liveId)
        query.append("user_action", QcloudCommon.AVIMCMD_EnterLive)
        query.append("msg_type",QcloudCommon.CALLBACK_AFTERNEWMEMBER_JOIN)
        return liveMongodb.findLiveUserCount(query);
    }

    @Override
    def findQLiveRecordMsgList(Map params) {
        int psize = params.psize as int
        List liveIdList = params.liveIdList
        long lastId = params.lastId as long
        DBObject query = new BasicDBObject()
        query.append("live_id", new BasicDBObject('$in',liveIdList))
        query.append("user_action", new BasicDBObject('$in',[QcloudCommon.AVIMCMD_Comment,QcloudCommon.AVIMCMD_GIFT,QcloudCommon.AVIMCMD_SEND_MESSAGE]))
        if(lastId > 0){
            query.append("create_time", new BasicDBObject('$gt', lastId))
        }
        DBObject sort = new BasicDBObject()
        sort.append("create_time",1)
        return liveMongodb.findLiveMsgList(query,sort,psize);
    }

    @Override
    boolean deleteLiveComment(long time, long liveId) {
        DBObject query = new BasicDBObject("live_id", liveId).append("create_time", time)
        boolean result = liveMongodb.deleteLiveComment(query)
        return result
    }

    @Override
    boolean savePartner(long liveId, String partnerId) {
        if (!liveId || !partnerId){
            return false
        }
        String sql = 'INSERT INTO live_partner_record (`live_id`, `partner_id`) VALUES (?, ?)'
        def res = dataBases.msqlLive.executeInsert(sql, [liveId, partnerId])
        return res
    }

    @Override
    def addLiveRecordPayExtend(long liveId, Map payMap) {
        int type = (payMap.type?:0) as int
        if(!liveId){
            return false
        }
        payMap = payMap?.findAll{key,value -> value}
        if (!payMap){
            return true
        }
        StringBuilder sql = new StringBuilder('INSERT INTO `live_record_pay` (`col_name`, `col_value`, `live_id`, `create_time`,`type`) VALUES')
        def params=[]
        payMap.each{key,value ->
            sql.append("(?, ?, ?, NOW(),?),")
            params.addAll([key, value, liveId,type])
        }
        def res = dataBases.msqlLive.executeInsert(sql.substring(0,sql.length() - 1).toString(),params)
        return res?res.size():0 == payMap.size();
    }

    @Override
    def delLiveRecordPayExtend(long liveId,int type){
        dataBases.msqlLive.execute("DELETE FROM live_record_pay WHERE live_id=? and type=?",[liveId,type])
    }

    @Override
    def updateLiveRecordPayExtend(long liveId, Map payMap) {
        int type = (payMap.type?:0) as int
        if(!liveId){
            return false
        }
        this.delLiveRecordPayExtend(liveId,type)
        return addLiveRecordPayExtend(liveId, payMap)
    }

    @Override
    def findLiveRecordPayExtendByLiveId(long liveId,int type) {
        Map resultMap = [:]
        dataBases.msqlLiveSlave.eachRow("SELECT * FROM live_record_pay WHERE live_id=? and type= ? and state=?",[liveId,type,'1']){row ->
            String key = row.col_name
            String value = row.col_value
            resultMap.put(key,value)
        }
        return resultMap
    }

    @Override
    def getQcloudInfo(String appId){
        def cloudResult
        try {
            if(appId) {
                StringBuffer buffer = new StringBuffer()
                buffer.append("SELECT app_id,config_info,liveshare_value,mall_url,face_list_limit,config_set_info ")
                buffer.append(" from live_app_config where app_id=? and status = 1")
                cloudResult = dataBases.msqlLive.firstRow(buffer.toString(), [appId])
            }
            return cloudResult
        }catch (Exception e){
            e.printStackTrace()
        }
    }

    @Override
    def getAllQcloudInfo(){
        def cloudResult
        try {
            StringBuffer buffer = new StringBuffer()
            buffer.append("SELECT app_id as appId,config_info as configInfo,liveshare_value liveshareValue")
            buffer.append(" from live_app_config where status = 1")
            cloudResult = dataBases.msqlLive.rows(buffer.toString())
        }catch (Exception e){
            e.printStackTrace()
        }
        return cloudResult
    }

    @Override
    def addLiveEvent(Map liveEvent){
        def result
        try{
            def params = [
                liveEvent?.taskId,
                liveEvent?.appId
            ]
            result = dataBases.msqlLive.executeInsert("INSERT INTO live_event (task_id, app_id, create_time) VALUES (?, ?, NOW())",params)
        }catch (Exception e){
            e.printStackTrace()
        }
        return result
    }

    @Override
    def getLiveEventByTaskId(String taskId){
        def result
        try{
            result = dataBases.msqlLiveSlave.firstRow("SELECT * from live_event where task_id = ?", [taskId])
        }catch (Exception e){
            e.printStackTrace()
        }
        return result
    }

}

