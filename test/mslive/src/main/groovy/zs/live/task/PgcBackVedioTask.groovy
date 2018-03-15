package zs.live.task

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import zs.live.common.LiveCommon
import zs.live.dao.mysql.QcloudLiveRes
import zs.live.entity.LiveRecord
import zs.live.service.LivePgcService
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.service.impl.QcloudLiveCommon
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
@Slf4j
class PgcBackVedioTask implements Runnable{

    LivePgcService livePgcService = (LivePgcService) context.getBean(LivePgcService.class)
    QcloudLiveRes qcloudLiveRes = (QcloudLiveRes) context.getBean(QcloudLiveRes.class);
    LiveService liveService = (LiveService) context.getBean(LiveService.class)

    ApplicationContext context

    PgcBackVedioTask(ApplicationContext context){
        this.context = context
    }

    @Override
    void run() {
        while (true) {
            try{
                //查询当天的直播，
                List<LiveRecord> liveRecordList = qcloudLiveRes.getPgcNoBackVedioLiveRecordList()
                if(liveRecordList){
                    log.info("查询当天的会议直播：{}",liveRecordList.size());
                    liveRecordList.each {
                        //如果有回看则取回放时间
                        long oldTimeSub = 0L
                        long newTime = 0L
                        if(it.videoAddress){
                            def videoInfo = liveService.getVideoAddressUrlList(it.videoAddress,[:])
                            oldTimeSub = videoInfo.timeSpan as long
                        }
                    //    log.info("定时任务调用会议直播回看，liveId==>{},roomId==>{},foreshowId=>{}",it.liveId,it.roomId,it.foreshowId)
                        String videoAddressNew=  livePgcService.updateBackVideoAddress(it);
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
                        log.info("定时任务调用会议直播回看，liveId==>{},roomId==>{},foreshowId=>{},subTime={},videoAddress={}",it.liveId,it.roomId,it.foreshowId,subTime,videoAddressNew)
                       if(subTime>10L ){ //新生成的比旧的回放地址的时间多至少10s
                            qcloudLiveRes.updateLiveRecordVideoAddress(videoAddressNew, it.liveId, null)
                            if (it.foreshowId) {
                                log.info("更新预告状态为回看状态，foreshowId==>{}", it.foreshowId);
                                liveService.updatePgcForeshowMergeInfo(it.foreshowId,it.appId)
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
