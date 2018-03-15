package zs.live.task

import com.alibaba.fastjson.JSON
import groovy.util.logging.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import zs.live.common.LiveCommon
import zs.live.dao.mysql.LiveRes
import zs.live.dao.redis.LiveRedis
import zs.live.service.LiveService
import zs.live.service.VideoMergeService
import zs.live.utils.Http
import zs.live.utils.Strings

/**
 * Created by liaojing on 2017/05/05.
 * 检测是否有合并失败的直播，并发送短信消息：
 *      1、从live_foreshow表里查询出需要合并的数据列表；
 *      2、判断是视频是否合并过的依据：字段live_record_info的值里是否含有video_address字段，若有则已经合并过，不需要合并；
 *      3、定时任务每10分钟进行一次，扫描20分钟之前的数据。
 */
@Slf4j
class VideoMergeValidateTask implements Runnable{
    public static final Logger timerLog = LoggerFactory.getLogger("timerLog")

    LiveRes liveRes = (LiveRes) context.getBean(LiveRes.class);
    LiveService liveService = (LiveService) context.getBean(LiveService.class);
    VideoMergeService vms = (VideoMergeService)context.getBean(VideoMergeService.class)
    LiveCommon liveCommon = (LiveCommon) context.getBean(LiveCommon.class)
    LiveRedis liveRedis = (LiveRedis) context.getBean(LiveRedis.class)

    ApplicationContext context

    VideoMergeValidateTask(ApplicationContext context){
        this.context = context
    }
    Map map = [:]

    @Override
    void run() {
        String messageUrl = liveService.messageUrl
        while (true){
            try{
                List liveList = liveRes.findForeshowIdListForMergeValidate()
                List errorList = []
                for (def liveForeshow : liveList){
                    if(!liveForeshow){
                        continue
                    }
                    def liveRecordInfo = liveForeshow.live_record_info
                    String title = liveForeshow.title?:""
                    def videoAddress = liveRecordInfo ? Strings.parseJson(liveRecordInfo as String).video_address : ""
                    if(!title.startsWith("zstest") && !videoAddress){  //视频合并有问题，需要通知研发(不校验测试数据)
                        errorList.add(liveForeshow)
                    }
                }
                if(liveCommon.liveEnv.equals("online") && errorList){//测试环境不发短信
                    timerLog.info("VideoMergeValidateTask errorList=>{}",errorList*.foreshow_id.toString())
                    try {
                        String cont = errorList*.foreshow_id.toString()
                        timerLog.info("VideoMergeValidateTask errorList:{}",cont)
                        if(liveCommon.liveEnv.equals("test")){
                            cont = "不用处理:"+cont
                        }
                       def res = Http.post(messageUrl,[phones:"13021027185",cont:cont])
                        timerLog.info("VideoMergeValidateTask errorList=>{},messageResult=>{}",cont,res)
                    }catch (Exception e1){
                        e1.printStackTrace()
                    }
                }else{
                    timerLog.info("VideoMergeValidateTask is perfect")
                }
            }catch (Exception e){
                e.printStackTrace()
            }finally{
                Thread.sleep(10*60*1000)
            }
        }
    }

}
