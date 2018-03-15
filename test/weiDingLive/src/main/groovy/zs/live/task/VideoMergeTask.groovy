package zs.live.task

import com.alibaba.fastjson.JSON
import groovy.util.logging.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import zs.live.common.LiveCommon
import zs.live.dao.mysql.LiveRes
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.dao.redis.LiveRedis
import zs.live.service.LiveService
import zs.live.service.VideoMergeService
import zs.live.utils.Strings

/**
 * Created by wangtaofang on 2016/12/5 0005.
 * 视频合并定时任务的流程：
 *      1、从live_foreshow表里查询出需要合并的数据列表；
 *      2、判断食品是否合并过的依据：字段live_record_info的值里是否含有video_address字段，若有则已经合并过，不需要合并；
 *      3、若需要合并，根据foreshowId从live_record_log表中查询出所生成的所有回放的地址；
 *      4、判断每个直播所生成的回放的时长和表中time_span（我们自己统计的时长）进行对比，
 *          如果时长相差超过10分钟，则认为该直播没有完全生成回放，不进行合并；
 *      5、当所有直播对应的回放都全部生成时，才进行合并，并且当预告对应的直播回放的地址只有一段时，不进行合并；
 *      6、定时任务每分钟进行一次，当天合并过得预告额foreshowId会放入一个map中，合并之前会进行校验，当foreshowId存在于map中时，不进行合并，
 *          此map会在第二天进行清空，即当天合并过得预告不会进行第二次合并。
 */
@Slf4j
class VideoMergeTask implements Runnable{
    public static final Logger timerLog = LoggerFactory.getLogger("timerLog")

    LiveRes liveRes = (LiveRes) context.getBean(LiveRes.class);
    LiveService liveService = (LiveService) context.getBean(LiveService.class);
    VideoMergeService vms = (VideoMergeService)context.getBean(VideoMergeService.class)
    LiveCommon liveCommon = (LiveCommon) context.getBean(LiveCommon.class)
    LiveRedis liveRedis = (LiveRedis) context.getBean(LiveRedis.class)

    ApplicationContext context
    VideoMergeTask(ApplicationContext context){
        this.context = context
    }
    Map map = [:]

    @Override
    void run() {
        while (true){
            try{
                List liveList = liveRes.findForeshowIdListForMerge()
                for (def liveForeshow : liveList){
                    String appId = liveForeshow.app_id
                    long foreshowId = (liveForeshow.foreshow_id ?: 0) as long
//                    if(liveCommon.liveEnv.equals("online")){
//                        if(!(liveForeshow.title?liveForeshow.title as String:"").startsWith("zstest")){
//                            timerLog.info("wangtf video merge 预上线只合并zstest的视频:foreshowId:{}",foreshowId)
//                            continue
//                        }
//                    }
                    if(!liveRedis.get(foreshowId as String)){   //当天合并过的视频不再进行合并
                        def liveRecordInfo = liveForeshow.live_record_info
                        def videoAddress = liveRecordInfo ? Strings.parseJson(liveRecordInfo as String).video_address : ""
                        if(!videoAddress){  //当视频已经合并过不再进行合并
                            //根据预告id查询出预告对应的所有直播，判断每个直播的回看地址是否都已经生成
                            def liveRecordList = liveRes.findLiveRecordListByForeId([foreshowId: foreshowId,all: true,appId:appId])
                            boolean flag = true
                            List urlList = []
                            List fileIds = []
                            int timeSpanAll = 0
                            String fileId = ""
                            liveRecordList?.each{
                                int timeSpan = it.time_span as int
                                if(it.video_address){
                                    Map videoAddressMap = liveService.getVideoAddressUrlList(it.video_address,[roomId: it.room_id, liveId: it.live_id,foreshowId: foreshowId,appId: appId])
                                    timeSpanAll = (videoAddressMap.timeSpan ?:0) as int
                                    List liveRecordUrl = videoAddressMap.url
                                    fileId = videoAddressMap.fileId?.get(0)
                                    fileIds.addAll(videoAddressMap.fileId)
                                    liveRecordUrl?.each{
                                        urlList.add(it.url)
                                    }
                                }
                                timerLog.info("wangtf video merge 直播时长，foreshowId:{},统计的直播回放时长:{},腾讯云返回的时长：{}",foreshowId,timeSpan,timeSpanAll)
                                if(timeSpan-timeSpanAll >= 10*60){
                                    timerLog.info("wangtf video merge 对应的直播没有完全生成回看 foreshowId:{},liveId：{}",foreshowId,it.live_id)
                                    flag = false
                                }
                            }
                            //当所有直播的回看地址都已经生成，再进行合并
                            if(flag && urlList){
                                if(urlList.size() == 1){
                                    //当只有一段视频地址时，不进行合并，直接更新预告表
                                    def urlListAll = []
                                    urlList.each{
                                        urlListAll.add(["url": it as String])
                                    }
                                    def videoAddressMap = [duration:timeSpanAll,fileId:fileId,playSet:urlListAll]
                                    String videoAddressInfo = JSON.toJSON([videoAddressMap])
                                    liveService.updateForeshowMergeVideoAddressInfo(foreshowId,videoAddressInfo,timeSpanAll,appId)
                                    timerLog.info("wangtf video merge 只有一段视频不合并并只更新 foreshowId:{},fileId：{},urlList:{}",foreshowId,fileId,urlList)
                                }else {
                                    String res = vms.concatVideo(foreshowId as long,fileIds,liveForeshow.title as String,["mp4","m3u8"],liveForeshow.app_id)
                                    timerLog.info("wangtf video merge foreshowId:{},fileIds:{},res:{}",foreshowId,fileIds,res)
                                    if(res){     //如果调用合并视频失败，则下次循环继续合并
                                        int code = Strings.parseJson(res).code as int
                                        if(code == 0){
                                            liveRedis.set(foreshowId as String,foreshowId as String, 60*60)
                                        }
                                    }
                                }
                            }else {
                                timerLog.info("wangtf video merge 回放地址没有全部生成，foreshowId:{}",foreshowId)
                            }
                        }
                    }
                }
            }catch (Exception e){
                e.printStackTrace()
            }finally{
                Thread.sleep(60*1000)
            }
        }
    }

//    public static void main(String[] args) {
//        new VideoMergeTask().run()
//    }
}
