package zs.live.task

import com.qcloud.Utilities.MD5
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import zs.live.common.LiveCommon
import zs.live.dao.mysql.QcloudLiveRes
import zs.live.entity.LiveRecord
import zs.live.service.LivePgcService
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.service.QcloudPgcLiveService
import zs.live.service.impl.QcloudLiveCommon
import zs.live.utils.Parallel
import zs.live.utils.Strings

/**
 * Created by kpc on 2016/10/25.
 * 定时任务（30秒）
 * 定时扫描直播记录表中当天的没有回看记录的直播
 * 条件
 * 1.当天 （频率不高所以直接查询库）
 * 2.有channelId（发起成功）
 * 3.直播时长控制
 */

class BizidBackVedioTask implements Runnable{
    private static final Logger log = LoggerFactory.getLogger(BizidBackVedioTask.class);

    LivePgcService livePgcService = (LivePgcService) context.getBean(LivePgcService.class)
    QcloudLiveRes qcloudLiveRes = (QcloudLiveRes) context.getBean(QcloudLiveRes.class);
    LiveService liveService = (LiveService) context.getBean(LiveService.class)
    QcloudLiveService qcloudLiveService = (QcloudLiveService) context.getBean(QcloudLiveService.class)
    QcloudPgcLiveService qcloudPgcLiveService = (QcloudPgcLiveService) context.getBean(QcloudPgcLiveService.class)
    LiveCommon liveCommon = (LiveCommon) context.getBean(LiveCommon.class)
    ApplicationContext context

    BizidBackVedioTask(ApplicationContext context){
        this.context = context
    }

    @Override
    void run() {
        while (true) {
            try{
                //查询当天的直播，
                List<LiveRecord> liveRecordList = qcloudLiveRes.getQcloudNoBackVedioLiveRecordList()
                if(liveRecordList){
                    log.info("查询当天的无回看直播：{}",liveRecordList.size());
                    for (LiveRecord live : liveRecordList) {
                        String title = live.title ? live.title as String: ""
                        if(liveCommon.liveEnv.equals("pre")){
                            if(!(title.startsWith("zstest"))){
                                continue;
                            }
                        }
                        if(liveCommon.liveEnv.equals("online")){
                            if(title.startsWith("zstest")){
                                continue;
                            }
                        }
                        //如果有回看则取回放时间
                        long oldTimeSub = 0L
                        long newTime = 0L
                        if(live.videoAddress){
                            def videoInfo = liveService.getVideoAddressUrlList(live.videoAddress,[:])
                            oldTimeSub = videoInfo.timeSpan as long
                        }
                    //    log.info("定时任务调用会议直播回看，liveId==>{},roomId==>{},foreshowId=>{}",live.liveId,live.roomId,live.foreshowId)

                        def config = qcloudLiveService.getQcloudInfo(live.appId)
                        log.info("qcloudInfo====>"+Strings.toJson(config))
                        int bizid = config.bizid as int
                        String qcloudAppid = config.qcloudAppid
                        String channelId;
                        if(live.liveMode == 1){
                            channelId = bizid+ "_" + MD5.stringToMD5(live.roomId+"_"+live.userId+"_"+"main");
                        }else{
                            channelId = qcloudPgcLiveService.getStreamId(bizid,live.liveId,live.roomId)
                        }

                        long txTime = System.currentTimeMillis() + 1000*60*60*24 //开始时间的 一天以后
                        String apiKey = config.apiKey
                        log.info("liveId ===>{},channelId=={},txTime==>{},apiKey==>{}",live.liveId,channelId,txTime,apiKey)
                        String videoAddressNew =  QcloudLiveCommon.liveTapeGetFilelist(qcloudAppid,channelId,apiKey,txTime)
//                        String videoAddressNew
//                        if(live.liveMode == 1){
//                            videoAddressNew = QcloudLiveCommon.liveTapeGetFilelistByTime(qcloudAppid,channelId,apiKey,txTime,live.createTime,live.updateTime)
//                        }else{
//                            videoAddressNew =  QcloudLiveCommon.liveTapeGetFilelist(qcloudAppid,channelId,apiKey,txTime)
//                        }
                        log.info("liveId===>{},videoAddressNew=={}",live.liveId,videoAddressNew)
                        if(videoAddressNew){
                            List obj = Strings.parseJson(videoAddressNew,List)
                            def bb = []
                            obj.each {
                                int duration = (it?.duration?:0) as int
                                if(duration !=0){
                                    bb.add(it)
                                }
                            }
                            videoAddressNew = Strings.toJson(bb)
                            def videoInfo = liveService.getVideoAddressUrlList(videoAddressNew,[:])
                            newTime = videoInfo.timeSpan as long
                        }
                        long subTime = newTime - oldTimeSub;
                        log.info('定时任务调用会议直播回看2，liveId==>{},roomId==>{},foreshowId=>{},subTime={},videoAddress={}',live.liveId,live.roomId,live.foreshowId,subTime,videoAddressNew)
                       if(subTime>10L ){ //新生成的比旧的回放地址的时间多至少10s
                            qcloudLiveRes.updateLiveRecordVideoAddress(videoAddressNew, live.liveId, null)
                            if (live.foreshowId) {
                                log.info("更新预告状态为回看状态，foreshowId==>{}", live.foreshowId);
                                if(live.liveMode == LiveCommon.LIVE_MODE_1){
                                    liveService.updateForeshowMergeInfo(live.foreshowId,live.appId)
                                }else{
                                    liveService.updatePgcForeshowMergeInfo(live.foreshowId,live.appId)
                                }
                            }else{
                                //颜值直播获取截图
                                Parallel.run([1],{
                                    Map videoAddressMap = liveService.getVideoAddressUrlList(videoAddressNew,[:])
                                    List fileId = videoAddressMap?.fileId?:[]
                                    liveService.getSnapshotUrl([fileId:fileId.get(0),appId:live.appId,liveId:live.liveId])
                                },1)
                            }

                       }
                        //休眠1秒，防止腾讯云报4400（访问超过限制的错误）
                        Thread.sleep(1 * 1000)
                    }
                }
            }catch (Exception e){
                log.info("QcloudBackVedioTask Exception:",e.getMessage())
                e.printStackTrace()
            }finally{
                Thread.sleep(30 * 1000)
            }
        }
    }

}
