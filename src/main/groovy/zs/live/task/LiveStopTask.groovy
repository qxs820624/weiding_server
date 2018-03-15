package zs.live.task

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Repository
import zs.live.APP
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.dao.redis.LiveRedis
import zs.live.service.QcloudLiveService

/**
 * 定时任务（1分钟）
 * Created by Administrator on 2016/10/29.
 */
@Slf4j
class LiveStopTask implements Runnable{
    LiveQcloudRedis liveQcloudRedis = (LiveQcloudRedis) context.getBean(LiveQcloudRedis.class);
    QcloudLiveService qcloudLiveService = (QcloudLiveService) context.getBean(QcloudLiveService.class);

    ApplicationContext context
    LiveStopTask(ApplicationContext context){
        this.context = context
    }

    @Override
    void run() {
        while (true) {
            try {
                Map errorMap = liveQcloudRedis.getAllLiveStopErrorCode()
                if(errorMap){
                    log.info("取出 liveStopErrorMap:=="+errorMap)
                    Set keys = errorMap.keySet()
                    keys?.each {
                        String[] field = it.split(liveQcloudRedis.LIVE_STOP_ERROR_CODE_FIELD)
                        long userId = field[0] as long
                        String appId = field[1]
                        Map errMap = qcloudLiveService.dealLastErrorStop(userId,appId,"强制结束直播定时任务")
                        if(errMap){
                            //TODO 落地日志
                        }
                    }
                }
            }catch (Exception e){
                log.info("LiveStopTask Exception:",e.getMessage())
                e.printStackTrace()
            }finally{
                Thread.sleep(60 * 1000)
            }
        }
    }

}
