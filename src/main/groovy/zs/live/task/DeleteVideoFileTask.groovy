package zs.live.task

import groovy.util.logging.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import zs.live.common.LiveCommon
import zs.live.dao.mysql.LiveRes
import zs.live.service.LiveService
import zs.live.service.VideoMergeService

/**
 * Created by Administrator on 2016/12/5 0005.
 */
@Slf4j
class DeleteVideoFileTask implements Runnable{
    public static final Logger timerLog = LoggerFactory.getLogger("timerLog")

    LiveRes liveRes = (LiveRes) context.getBean(LiveRes.class);
    LiveService liveService = (LiveService) context.getBean(LiveService.class);
    VideoMergeService vms = (VideoMergeService)context.getBean(VideoMergeService.class)
    LiveCommon liveCommon = (LiveCommon) context.getBean(LiveCommon.class)

    ApplicationContext context

    DeleteVideoFileTask(ApplicationContext context){
        this.context = context
    }
    Map map = [:]

    @Override
    void run() {
        while (true){
            try{
                System.out.println("========DeleteVideoFileTask start!=========")
                List liveRecordList=[]
                String env = liveCommon.liveEnv
                if(env.equals("test")){
                    // 查询出所有未删除的直播数据，根据liveId进行删除
                    liveRecordList = liveRes.findTestLiveRecordList()
                    timerLog.info("wangtf 需要删除的视频直播文件列表，size：{}",liveRecordList?.size())
                }else {
                    // 查询出所有zstest的直播数据和状态为不符合回看的直播数据，然后进行删除
                    liveRecordList = liveRes.findZstestLiveRecordList()
                    timerLog.info("wangtf 需要删除的视频直播文件列表，size：{}",liveRecordList?.size())
                }
                liveRecordList?.each{
                    long liveId = it.live_id as long
                    long foreshowId = it.foreshow_id as long
                    int liveMode = (it.live_mode ?:1)as int
                    int res = 0
                    try{
                        if(liveMode == 1){//互动直播
                            res = liveService.delLiveRecord([liveId:liveId,appId:it.app_id,op_role:100])
                            timerLog.info("wangtf 删除互动直播数据，删除直播数据，liveId:{},foreshowId:{},res:{}", liveId,foreshowId,res)
                        }else {//会议直播
                            if(foreshowId){
                                res = liveService.delForeshow([foreshowId:foreshowId,appId: it.app_id])
                                timerLog.info("wangtf 删除会议直播数据，删除预告：liveId:{},foreshowId:{},res:{}", liveId,foreshowId,res)
                                res = liveService.delLiveRecord([liveId: liveId,appId: it.app_id,op_role: 100])
                                timerLog.info("wangtf 删除会议直播数据，删除直播数据，liveId:{},foreshowId:{},res:{}", liveId,foreshowId,res)
                            }else {
                                res = liveService.delLiveRecord([liveId: liveId,appId: it.app_id,op_role: 100])
                                timerLog.info("wangtf 删除会议直播数据，没有绑定预告，删除直播数据，liveId:{},foreshowId:{},res:{}", liveId,foreshowId,res)
                            }
                        }
                    }catch (Exception e1){
                        e1.printStackTrace()
                    }
                    Thread.sleep(10*1000)
                }
                Thread.sleep(1*24*60*60*1000)
            }catch (Exception e){
                e.printStackTrace()
                Thread.sleep(60*60*1000)
            }
        }
    }

}
