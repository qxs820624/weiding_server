import com.qcloud.Utilities.MD5
import zs.live.ApiUtils
import zs.live.common.LiveCommon
import zs.live.dao.mysql.QcloudLiveRes
import zs.live.entity.LiveRecord
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.service.QcloudPgcLiveService
import zs.live.service.impl.QcloudLiveCommon
import zs.live.utils.Assert
import zs.live.utils.Strings

ApiUtils.processNoEncry {
    Assert.isNotBlankParam(params,"liveId")
    long liveId = params.liveId as long
    String env = params.env
    LiveService liveService = getBean(LiveService)
    QcloudLiveRes qcloudLiveRes = getBean(QcloudLiveRes)
    LiveRecord live = liveService.findLiveRecordByLiveId(liveId)

    QcloudLiveService qcloudLiveService = getBean(QcloudLiveService)
    QcloudPgcLiveService qcloudPgcLiveService = getBean(QcloudPgcLiveService)
    def config = qcloudLiveService.getQcloudInfo(live.appId)
    ApiUtils.log.info("qcloudInfo====>"+Strings.toJson(config))
    int bizid = config.bizid as int
    String qcloudAppid = config.qcloudAppid
    String channelId;
    if(live.liveMode == LiveCommon.LIVE_MODE_1){
        channelId = bizid+ "_" + MD5.stringToMD5(live.roomId+"_"+live.userId+"_"+"main");
    }else{
       // channelId = qcloudPgcLiveService.getStreamId(bizid,live.liveId,live.roomId)
        channelId = bizid+"_" +env+"_"+live.liveId+"_"+live.roomId
    }

    long txTime = System.currentTimeMillis() + 1000*60*60*24 //开始时间的 一天以后
    String apiKey = config.apiKey
    ApiUtils.log.info("liveId ===>{},channelId=={},txTime==>{},apiKey==>{}",live.liveId,channelId,txTime,apiKey)
    String videoAddressNew =  QcloudLiveCommon.liveTapeGetFilelist(qcloudAppid,channelId,apiKey,txTime)
    ApiUtils.log.info("liveId===>{},videoAddressNew=={}",live.liveId,videoAddressNew)
    ApiUtils.log.info('手动调用会议直播回看2，liveId==>{},roomId==>{},foreshowId=>{},videoAddress={}',live.liveId,live.roomId,live.foreshowId,videoAddressNew)
    if(videoAddressNew){
        qcloudLiveRes.updateLiveRecordVideoAddress(videoAddressNew, live.liveId, null)
        if (live.foreshowId) {
            ApiUtils.log.info("更新预告状态为回看状态，foreshowId==>{}", live.foreshowId);
            if(live.liveMode == LiveCommon.LIVE_MODE_1){
                liveService.updateForeshowMergeInfo(live.foreshowId,live.appId)
            }else{
                liveService.updatePgcForeshowMergeInfo(live.foreshowId,live.appId)
            }
        }
    }














//    if(liveMode != LiveCommon.LIVE_MODE_1){
//        //如果有回看则取回放时间
//        long oldTimeSub = 0L
//        long newTime = 0L
//        if(live.videoAddress){
//            def videoInfo = liveService.getVideoAddressUrlList(live.videoAddress,[:])
//            oldTimeSub = videoInfo.timeSpan as long
//        }
//        //    log.info("定时任务调用会议直播回看，liveId==>{},roomId==>{},foreshowId=>{}",it.liveId,it.roomId,it.foreshowId)
//        LivePgcService livePgcService = getBean(LivePgcService)
//        String videoAddressNew=  livePgcService.updateBackVideoAddress(live);
//        if(videoAddressNew){
//            List obj = Strings.parseJson(videoAddressNew,List)
//            def bb = []
//            obj.each {
//                int duration = (it?.duration?:0) as int
//                if(duration !=0){
//                    bb.add(it)
//                }
//            }
//            videoAddressNew = Strings.toJson(bb)
//            def videoInfo = liveService.getVideoAddressUrlList(videoAddressNew,[:])
//            newTime = videoInfo.timeSpan as long
//        }
//        long subTime = newTime - oldTimeSub;
//        ApiUtils.log.info("subTime=={},newTime=={},oldTimeSub=={}",subTime,newTime,oldTimeSub)
//        //   log.info("定时任务调用会议直播回看，liveId==>{},roomId==>{},foreshowId=>{},videoAddress={}",it.liveId,it.roomId,it.foreshowId,videoAddress)
//        if(subTime>10L ){ //新生成的比旧的回放地址的时间多至少10s
//            QcloudLiveRes qcloudLiveRes = getBean(QcloudLiveRes)
//            qcloudLiveRes.updateLiveRecordVideoAddress(videoAddressNew, live.liveId, null)
//            if (live.foreshowId) {
//                log.info("更新预告状态为回看状态，foreshowId==>{}", live.foreshowId);
//                liveService.updatePgcForeshowMergeInfo(live.foreshowId,live.appId)
//            }
//        }
//        backString = videoAddressNew
//    }else{
//        QcloudLiveService qcloudLiveService = getBean(QcloudLiveService)
//        backString  = qcloudLiveService.getBackInfo(live.roomId,liveId,live.foreshowId,live.appId,"手工调用重置回看地址")
//    }
















    return videoAddressNew
}
