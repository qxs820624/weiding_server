package zs.live.task

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
  定时任务总控制管理，直接将任务加进来执行，增加时注意增加一下线程数数据量
  该定时任务在224和120服务器上执行
 */
@Slf4j
@Component
class ControlCenterTaskManager120 {

    //***************重要，当任务增加一个时，要增加一个线程数量，线程数要大于等于任务数，否则不能执行*******
    private static final ExecutorService es = Executors.newFixedThreadPool(20)
    private static final ScheduledExecutorService singleScheduledES= Executors.newScheduledThreadPool(2);

    @Value('${spring.cloud.client.ipAddress}')
    String webIp
    @Autowired
    ApplicationContext context

    @PostConstruct
    public void start() {
       def execList= ["103.29.134.224","103.29.134.187"] //要执行的服务器IP
        if(execList.contains(webIp)){
            log.info("******启动定时任务总控制224,120管理******")
            /**
             * 更新直播预告表中的foreshow_status为开始，并且给关注用户发消息
             * 整分钟过1秒开始，每60秒运行一次//4分钟后开始
             */
            Calendar c = Calendar.getInstance();
            c.add(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 10);
            long initialDelay=(c.getTimeInMillis()-System.currentTimeMillis())/1000;
            log.info("启动更新预告开始状态任务");
            singleScheduledES.scheduleAtFixedRate(new ForeshowBeginTask(context), initialDelay, 60L, TimeUnit.SECONDS)
            log.info("启动预告排序重置任务")
            singleScheduledES.scheduleWithFixedDelay(new ForeshowResetOrderTask(context),0,10,TimeUnit.MINUTES)

            log.info("启动心跳任务")
            es.execute(new LiveHeartTask(context))
            log.info("启动结束推流任务")
            es.execute(new LiveStopTask(context))
            log.info("启动删除视频文件任务")
            es.execute(new DeleteVideoFileTask(context))
        }
        def execVestList= ["103.29.134.224","103.29.134.225","103.29.134.187"] //马甲要执行的服务器IP
        if(execVestList.contains(webIp)){
            log.info("启动马甲发评论任务")
            es.execute(new VestCommentTask(context))
            log.info("启动马甲点赞任务")
            es.execute(new VestDoPrimeTask(context))
            log.info("启动马甲关注任务")
            es.execute(new VestFansTask(context))
            log.info("启动马甲打赏礼物任务")
            es.execute(new VestGiftTask(context))
            log.info("启动马甲任务")
            es.execute(new VestMegTask(context))
            log.info("启动马甲退出房间任务")
            es.execute(new VestQuitRoomTask(context))
            log.info("启动直播结束列表定时任务")
            es.execute(new VestLiveLogMegTask(context))

            log.info("启动直播码定时取回看任务")
            es.execute(new BizidBackVedioTask(context))
            log.info("启动直播流量统计任务")
            es.execute(new VideoFluxTask(context))
        }
    }

}
