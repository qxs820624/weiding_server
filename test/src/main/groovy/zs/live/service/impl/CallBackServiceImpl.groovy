package zs.live.service.impl

import com.alibaba.fastjson.JSON
import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.qcloud.Common.PullVd
import groovy.util.logging.Slf4j
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import zs.live.common.LiveCommon
import zs.live.common.QcloudCommon
import zs.live.dao.mysql.impl.QcloudLiveResImpl
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.entity.LiveRecord
import zs.live.service.CallBackService
import zs.live.service.LivePgcService
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.service.VestUserService
import zs.live.service.VideoMergeService
import zs.live.utils.Parallel
import zs.live.utils.Strings

/**
 * Created by Administrator on 2016/11/15.
 */
@Service("callBackService")
@Slf4j
class CallBackServiceImpl implements CallBackService{

    public static final Logger callBackLog = LoggerFactory.getLogger("callBack")
    @Autowired
    LiveQcloudRedis liveQcloudRedis;
    @Autowired
    LiveService liveService
    @Autowired
    QcloudLiveService qcloudLiveService
    @Autowired
    VideoMergeService videoMergeService
    @Autowired
    QcloudLiveResImpl qcloudLiveRes
    @Autowired
    LivePgcService livePgcService
    @Autowired
    VestUserService vestUserService

    @Value('${live.secretId}')
    String secretId
    @Value('${live.secretKey}')
    String secretKey
    @Value('${live.video.uploadMode}')
    String uploadMode //push：推送至云 pull：云端拉取
    /**
     *  //json
     /**
     * {
     "CallbackCommand": "Group.CallbackAfterNewMemberJoin", // 回调命令
     "GroupId" : "@TGS#2J4SZEAEL",
     "Type": "Public", // 群组类型
     "JoinType": "Apply", // 入群方式：Apply（申请入群）；Invited（邀请入群）。
     "Operator_Account": "leckie", // 操作者成员
     "NewMemberList": [ // 新入群成员列表
     {
     "Member_Account": "jared"
     },
     {
     "Member_Account": "tommy"
     }
     ]
     }
     * @param reqBodyStr
     * @return
     */
    @Override
    String callBack(String reqBodyStr) {
        def reqBody = Strings.parseJson(reqBodyStr)
        String callbackCommand = reqBody.CallbackCommand
        int roomId = reqBody.GroupId? reqBody.GroupId as int : 0
        long liveId = liveQcloudRedis.getLiveIdByRoomId(roomId)
        long time = System.currentTimeMillis()
        callBackLog.info("roomId={},liveId={}, 对应消息reqBody:===>{}",roomId,liveId,reqBodyStr)
        Map mapData = [:]  //放入redis的数据（业务使用，分享落地页、回放展示评论等）
        Map params = [
                        liveId:liveId,
                        roomId:roomId,
                        msg:reqBodyStr,
                        createTime: time
                    ]  //放入mongo、kafka的数据（统计使用）
        if(callbackCommand.equals(QcloudCommon.CALLBACK_AFTERNEWMEMBER_JOIN)){  //群成员进群
            def newMemberList = reqBody.NewMemberList
            newMemberList.each{
                Map userMap = liveService.getUserInfo(it.Member_Account as long)
                liveQcloudRedis.setLiveWather(liveId,Strings.toJson(userMap),it.Member_Account as long)
                liveQcloudRedis.addLiveWatchCount(liveId, 1)
            }
            //最大观众总数增1
            liveQcloudRedis.addWatherTotalCount(liveId, 1L)
            //真实观众的pv数增1
            liveQcloudRedis.addLiveRealWatchCount(liveId,1)
            //appId人数数增1(云平台需求)
            liveService.addAppLimitInfo(liveId,1)
            //同步数据到mongo
            params.userId = (reqBody.Operator_Account?: "0") as String
            params.userAction = QcloudCommon.AVIMCMD_EnterLive
            params.msgType = QcloudCommon.CALLBACK_AFTERNEWMEMBER_JOIN
        }else if(callbackCommand.equals(QcloudCommon.CALLBACK_AFTERMEMBER_EXIT)){ //群成员退出
            def exitMemberList = reqBody.ExitMemberList
            exitMemberList.each{
                liveQcloudRedis.delLiveWather(liveId,it.Member_Account as long)
                liveQcloudRedis.addLiveWatchCount(liveId,-1)
            }
            //appId人数减1(云平台需求)
            liveService.addAppLimitInfo(liveId,-1)
            //同步数据到mongo
            params.userId = (reqBody.Operator_Account?: "0") as String
            params.userAction = QcloudCommon.AVIMCMD_ExitLive
            params.msgType = QcloudCommon.CALLBACK_AFTERMEMBER_EXIT
        }else if(callbackCommand.equals(QcloudCommon.CALLBACK_BEFORE_SENDMSG)){ //发消息
            //{"CallbackCommand":"Group.CallbackAfterSendMsg","From_Account":"2900261","GroupId":"34","MsgBody":[{"MsgContent":{"Data":"{\n  \"userAction\" : 4\n}","Desc":"","Ext":""},"MsgType":"TIMCustomElem"}],"Type":"AVChatRoom"}
            def userData = reqBody?.MsgBody?.MsgContent?.Data?:[]
            if(!userData || !userData.get(0)){//Data 是搜悦直播自己定义的消息类型，Text是腾讯云的消息格式(指的是互动直播真实观众发普通消息)
                if(reqBody?.MsgBody?.MsgContent?.Text){ //客户端真实观众发送普通聊天消息
                    long userId = (reqBody?.From_Account ?: 0) as long
                    def userInfo = liveService.getUserInfo(userId)
                    int role = livePgcService.getUserRole(liveId, userId)
                    def data = [userAction: QcloudCommon.AVIMCMD_Comment,actionParam:[userImage: userInfo.userImage ?: "",nickname:userInfo.nickname ?: "",userId: userId,liveId: liveId,role: role]]
                    def text = reqBody.MsgBody.MsgContent.Text
                    def msgContent = [Text:text,Data:JSON.toJSONString(data)]
                    reqBody.MsgBody = [[MsgContent:msgContent]]
                }
                //将数据放入redis队列-----田帅使用
                mapData = [liveId: liveId,time: time,data: reqBody]
                //同步数据到mongo
                params.msg=Strings.toJson(reqBody)
                params.userId = (reqBody.From_Account?:"0") as String
                params.userAction = QcloudCommon.AVIMCMD_Comment
                params.msgType = QcloudCommon.CALLBACK_BEFORE_SENDMSG
            }else{
                String userActionStr = Strings.parseJson(userData.get(0)).userAction
                int userAction = userActionStr?userActionStr as int : 0
                if(QcloudCommon.AVIMCMD_EnterLive == userAction || QcloudCommon.AVIMCMD_FOLLOW == userAction
                    || QcloudCommon.AVIMCMD_Praise_first == userAction ||  QcloudCommon.AVIMCMD_WITH_USER_ROLE == userAction){
                    //真实观众进入房间之后发消息 普通观众关注，普通观众点赞，会议直播普通用户评论
                    long userId = (reqBody?.From_Account ?: 0) as long
                    def userInfo = liveService.getUserInfo(userId)
                    def data
                    if(QcloudCommon.AVIMCMD_WITH_USER_ROLE == userAction){
                        def actionParam = Strings.parseJson(userData.get(0)).actionParam ?: ""
                        data = [userAction: userAction,actionParam:[userImage: userInfo.userImage ?: "",nickname:userInfo.nickname ?: "",userId: userId,message:Strings.parseJson(actionParam).message,role:Strings.parseJson(actionParam).role]]
                    }else
                        data = [userAction: userAction,actionParam:[userImage: userInfo.userImage ?: "",nickname:userInfo.nickname ?: "",userId: userId,liveId: liveId]]
                    def msgContent = [Data:JSON.toJSONString(data)]
                    reqBody.MsgBody = [[MsgContent:msgContent]]
                    callBackLog.info("wangtf real user msg,liveId:{},roomId:{},data:{}",liveId,roomId, reqBody)
                }else if(QcloudCommon.AVIMCMD_Praise == userAction){
                    //更新点赞数
                    liveQcloudRedis.setLivePraise(liveId)
                }else if(QcloudCommon.AVIMCMD_ENTERLIVE_FILL_DATA == userAction) {
                    liveQcloudRedis.addWatherTotalCount(liveId, 1L)
//                    liveQcloudRedis.addVestCount(liveId)
                }else if(QcloudCommon.AVIMCMD_Host_Leave == userAction){
                    log.info("主播离开，liveId：{}",liveId)
                    liveQcloudRedis.setHeartTimeHostLeave(liveId)
                }
                //将数据放入redis队列-----田帅使用
                mapData = [liveId: liveId,time: time,data: reqBody]
                //同步数据到mongo
                params.msg=Strings.toJson(reqBody)
                try{
                    params.userId = Strings.parseJson(Strings.toJson(Strings.parseJson(userData.get(0))?.actionParam)) ?.userId?:Strings.parseJson(reqBodyStr)?.From_Account ?:""
                }catch (Exception e){
                    params.userId = Strings.parseJson(reqBodyStr)?.From_Account ?:""
                }
                params.userAction = userAction
                params.msgType = QcloudCommon.CALLBACK_BEFORE_SENDMSG
            }

            Parallel.run([1],{
                //将数据放入redis队列-----田帅使用
                def back = liveQcloudRedis.pushCommentDataToRedis(Strings.toJson(mapData))
                callBackLog.info("wangtf push comment data to redis liveId:{},roomId:{},back:{}",liveId,roomId, back)
            },1)
        }else if (callbackCommand.equals(QcloudCommon.CALLBACK_AFTERGROUP_DESTROYED)){  //解散房间
            //解散房间在结束直播之后，所以此时查询redis是没有记录的，固需要从数据库查询
            if(!liveId){
                liveId = qcloudLiveRes.getLiveIdByRoomIdFromLog(roomId)
            }
            //appId人数减去真实观众数(云平台需求)
            liveService.addAppLimitInfo(liveId,0)
            //获取正在直播的 直播数据
            LiveRecord live = liveService.findLiveByLiveId(liveId)
            if(live){
                qcloudLiveService.stopLiveComm(liveId,roomId,live.userId,live.appId,"客户端解散房间回调调用(没有结束过直播)==》")
            }else{
                //如果没有正在直播的 就查询直播记录表里有没有 有的话 也结束一遍直播
                LiveRecord liveRecord = liveService.findLiveRecordByLiveId(liveId)
                if(liveRecord){
                    qcloudLiveService.stopByCallBy(liveId,roomId,liveRecord.userId,liveRecord.appId,"客户端解散房间回调调用（结束过一次直播）==》")
                }
            }
            //同步数据到mongo
            params.liveId = liveId
            params.userId = (reqBody.Operator_Account?:"0") as String
            params.userAction = QcloudCommon.AVIMCMD_DESTORY
            params.msgType = QcloudCommon.CALLBACK_AFTERGROUP_DESTROYED
        }
        Parallel.run([1],{
            if(params && params.msgType){
                try{
                    //同步数据到mongo
                    qcloudLiveRes.addQcloudMsg(params)
                }catch(Exception e){}
                try{
                    //通知王龙直播统计 TODO 暂时只是与王龙联调，后期会删掉，统一由分析nginx日志的项目处理
                    params.jsonType = LiveCommon.STATISTIC_JSON_TYPE_2
                    qcloudLiveService.sendLiveDataToStatistic(params)
                }catch(Exception e){}
            }
        },1)
    }

    @Override
    int videoMergeCallBack(Map params) {
        def qcloudInfo = qcloudLiveService.getQcloudInfo(params.appId)
        if(!qcloudInfo){
            log.info("videoMergeCallBack qcloudInfo is null,params=>{}",params)
            return 0
        }
        String secretId = qcloudInfo.secretId
        String secretKey = qcloudInfo.secretKey

        long foreshowId = (params.foreshowId  ?: 0)as long
        if(!foreshowId){
            return 0
        }
        int status = 0
        List fileIds = []
        int timeSpan = 0
        List urlList = []
        params.fileInfo.each{
            fileIds.add(it.fileId)
            if(timeSpan == 0){
                String videoInfoStr = QcloudLiveCommon.getVideoInfo(secretId,secretKey,it.fileId)
                log.info("wangtf 从腾讯云获取视频的详细信息, foreshowId:{},fileId:{},data:{}",foreshowId,it.fileId,videoInfoStr)
                def videoInfo = Strings.parseJson(videoInfoStr)
                def fileSet = videoInfo?.fileSet
                fileSet.each{
                    timeSpan = it.duration as int
                }
            }
            urlList.add([url:it.fileUrl as String])
        }
        if(urlList && fileIds){
            String fileId = StringUtils.join(fileIds.toArray(), ",")
            def videoAddress = [duration:timeSpan,fileId:fileId,playSet:urlList]
            String videoAddressInfo = JSON.toJSON([videoAddress])
            log.info("wangtf video merge, foreshowId:{},videoAddress :{}",foreshowId,videoAddress)
            if(timeSpan){
                status = liveService.updateForeshowMergeVideoAddressInfo(foreshowId,videoAddressInfo,timeSpan,Strings.APP_NAME_SOUYUE)
            }
        }

//        //同一个视频回调次数计数
////        int timeCount = 0
////        timeCount = (liveQcloudRedis.hgetVideoMergeRedis(foreshowId,"timeCount") ?: 0)as int
////        liveQcloudRedis.hsetVideoMergeRedis(foreshowId, "timeCount", (++timeCount) as String)
////        log.info("wangtf video merge ,timeCount:{},foreshowId:{}", timeCount,foreshowId)
//        String fileId = params.file_id ?: ""
//        List urlList = []
//        int timeSpan = 0
//        Set keySet = params.keySet()
//        if (uploadMode == LiveCommon.LIVE_UPLOADMODE_PUSH){ //当视频合并上传方式为推送至云，则只有一次回调，直接进行更新
//            status = updateVideoAddressByFileId(fileId, foreshowId)
//            if(status == 1)
//                liveQcloudRedis.delVideoMergeRedis(foreshowId)
//        }else{
//            if(keySet.contains("file_id")){
//                status = updateVideoAddressByFileId(fileId, foreshowId)
//                if(status == 1)
//                    liveQcloudRedis.delVideoMergeRedis(foreshowId)
//            }else {
//                //上传的回调
//                keySet?.each{
//                    if(!(it.equals("foreshowId") || it.equals("vc"))){
//                        def data = Strings.parseJson(JSON.toJSON(it))
//                        if((data.status as int) != 0){
//                            //视频拉取失败时，需要重新拉取视频
////                        liveQcloudRedis.delVideoMergeRedis(foreshowId)
//                            String sourceUrl = data.data.source_url
//                            if(sourceUrl){
//                                int index = sourceUrl.indexOf("time=")
//                                int num = 1
//                                if(index < 0){
//                                    sourceUrl+="?time=1"
//                                }else {
//                                    num =  (sourceUrl.substring(index+5) ?: 1) as int
//                                    sourceUrl = sourceUrl.replace("time="+num,"time="+(++num))
//                                }
//                                if(num <= 3){
//                                    videoMergeService.reTryUploadVideo(sourceUrl as String,foreshowId as long)
//                                    status = 10001
//                                }
//                                log.info("wangtf video merge fail ,status:{},back sourceUrl:{}",data.status, sourceUrl)
//                            }
//                        }else {
//                            status = updateVideoAddressByFileId(fileId, foreshowId)
//                        }
//                    }
//                }
//            }
//        }
        return status
    }


    @Override
    String pullEvent(String appId) {
        def qcloudInfo = qcloudLiveService.getQcloudInfo(appId)
        if(!qcloudInfo){
            log.info("pullEvent qcloudInfo is null,appId=>{}",appId)
            return null
        }
        String secretId = qcloudInfo.secretId
        String secretKey = qcloudInfo.secretKey

        return QcloudLiveCommon.pullEvent(secretId,secretKey)
    }

    @Override
    String confirmEvent(List msgHandle,String appId) {
        def qcloudInfo = qcloudLiveService.getQcloudInfo(appId)
        if(!qcloudInfo){
            log.info("confirmEvent qcloudInfo is null,appId=>{}",appId)
            return null
        }
        String secretId = qcloudInfo.secretId
        String secretKey = qcloudInfo.secretKey

        return QcloudLiveCommon.confirmEvent(secretId,secretKey,msgHandle)
    }
}
