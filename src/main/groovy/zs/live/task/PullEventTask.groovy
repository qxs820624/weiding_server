package zs.live.task

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import zs.live.dao.mysql.QcloudLiveRes
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.service.CallBackService
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.service.VideoGatherService
import zs.live.utils.DateUtil
import zs.live.utils.Parallel

/**
 * Created by Administrator on 2017/3/29 0029.
 */
class PullEventTask implements Runnable {

    VideoGatherService videoGatherService = (VideoGatherService)context.getBean(VideoGatherService.class)
    CallBackService callBackService = (CallBackService)context.getBean(CallBackService.class)
    LiveQcloudRedis liveQcloudRedis = (LiveQcloudRedis)context.getBean(LiveQcloudRedis.class)
    QcloudLiveRes qcloudLiveRes = (QcloudLiveRes)context.getBean(QcloudLiveRes.class)
    QcloudLiveService qcloudLiveService = (QcloudLiveService) context.getBean(QcloudLiveService.class)
    LiveService liveService = (LiveService) context.getBean(LiveService.class)
    public static final Logger timerLog = LoggerFactory.getLogger("timerLog")
    ApplicationContext context
    PullEventTask(ApplicationContext context){
        this.context = context
    }

    @Override
    void run() {
        while (true) {
            try {
                List appIds = qcloudLiveService.getAllQcloudInfo()
                appIds.each {appId ->
                    String events = callBackService.pullEvent(appId)
                    timerLog.info("PullEvent ${appId} >>> " + events)
                    JSONObject jsonObject = JSON.parse(events)
                    if (jsonObject.code == 0){
                        def msgHandles = []
                        jsonObject.eventList.each {
                            def msgHandle = it.msgHandle
                            def eventContent = it.eventContent
                            def eventType = eventContent.eventType
                            def data = eventContent.data ?: [:]
                            def localEvent = false
                            switch (eventType){
                                case "NewFileUpload"://视频上传完成
                                    msgHandles << msgHandle
                                    Parallel.run([1], {
                                        def params = [
                                            status:data.status,
                                            message:data.message,
                                            task:"file_upload",//file_upload 上传回调 transcode转码回调
                                            data:data,
                                        ]
                                        videoGatherService.uploadCallBack(params)
                                    },10)
                                    break
                                case "PullComplete"://URL转拉完成
                                    break
                                case "TranscodeComplete"://视频转码完成
                                    def vodTaskId = data.vodTaskId ?: ""
                                    localEvent = checkEvent(vodTaskId)
                                    if (localEvent) {
                                        msgHandles << msgHandle

                                        Parallel.run([1], {
                                            def params = [
                                                status : data.status,
                                                message: data.message,
                                                task   : "transcode",//file_upload 上传回调 transcode转码回调
                                                data   : data,
                                            ]
                                            videoGatherService.uploadCallBack(params)
                                        }, 10)
                                    }
                                    break
                                case "ConcatComplete"://视频拼接完成
                                    Parallel.run([1], {
                                        def vodTaskId = data.vodTaskId ?: ""
                                        //检查是否是本环境创建的任务
                                        localEvent = checkEvent(vodTaskId)
                                        if (localEvent){
                                            msgHandles << msgHandle

                                            long foreshowId = (liveQcloudRedis.hgetVideoMergeRedis(vodTaskId,"foreshowId") ?: 0) as long
                                            List fileInfo = data.fileInfo ?: []
                                            def params = [:]
                                            params.foreshowId = foreshowId
                                            params.fileInfo = fileInfo
                                            params.appId = appId
                                            timerLog.info("wangtf video merge call back, foreshowId:{},vodTaskId:{},fileInfo:{}", foreshowId, vodTaskId,fileInfo)
                                            timerLog.info("ConcatComplete 合并完成 任务标识 >> ${vodTaskId} 预告ID >> ${foreshowId} 完成时间 >> ${DateUtil.getTodayDate(DateUtil.FULL_DATE_PATTERN)}")
                                            callBackService.videoMergeCallBack(params)
                                        }
                                    },10)
                                    break
                                case "CreateImageSpriteComplete"://视频截取雪碧图完成
                                    break
                                case "CreateSnapshotByTimeOffsetComplete"://视频按时间点截图完成
                                    timerLog.info("wangtf 视频按时间点截图完成,data:{}",data)
                                    def vodTaskId = data.vodTaskId ?: ""
                                    localEvent = checkEvent(vodTaskId)
                                    if (localEvent) {
                                        msgHandles << msgHandle
                                        Parallel.run([1], {
                                            long liveId = (liveQcloudRedis.hgetSnapshotRedis(vodTaskId,"liveId")?: 0) as long
                                            //根据liveId更新数据库
                                            liveService.updateLiveThumpByLiveId(liveId,data)
                                        }, 1)
                                    }
                                    break
                                default:
                                    break;
                            }

                            if (msgHandles.size()>8){//由于腾讯SDK入参使用了TreeMap，会导致参数排序错误，进而引起接口错误，所有限制在10个以内
                                //确认事件通知
                                def confirm = callBackService.confirmEvent(msgHandles,appId)
                                timerLog.info("PullEvent ${appId} 事件句柄 >>> {} 事件确认结果 >>> {}", msgHandles, confirm)
                                msgHandles = []
                            }
                        }

                        //确认事件通知
                        def confirm = callBackService.confirmEvent(msgHandles,appId)
                        timerLog.info("PullEvent  ${appId} 事件句柄 >>> {} 不足8条的事件确认结果 >>> {}", msgHandles, confirm)
                    }else {
                        timerLog.info("PullEvent  ${appId} 暂无事件通知")
                    }
                }
            }catch (Exception e){
                e.printStackTrace()
            }finally{
                //最高调用频率 1000次/分钟，相当于每60毫秒/次
                Thread.sleep(1000)
            }
        }
    }

    /**
     * 检查是否本地产生的任务
     * @param taskId
     * @return
     */
    private def checkEvent(String taskId){
        def result = false
        def event = qcloudLiveRes.getLiveEventByTaskId(taskId)
        if (event){
            result = true
        }
        return result
    }
}
