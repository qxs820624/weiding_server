package zs.live.task

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
  定时任务总控制管理，直接将任务加进来执行，增加时注意增加一下线程数数据量
 该定时任务在224和187服务器上执行
 */
@Slf4j
@Component
class ControlCenterTaskManager187 {

    //***************重要，当任务增加一个时，要增加一个线程数量，线程数要大于等于任务数，否则不能执行*******
    private static ExecutorService es = Executors.newFixedThreadPool(5)
    @Value('${spring.cloud.client.ipAddress}')
    String webIp
    @Autowired
    ApplicationContext context

    @PostConstruct
    public void start() {
        def execList= ["103.29.134.224","103.29.134.187"] //要执行的服务器IP
        if(execList.contains(webIp)){
            log.info("******启动定时任务总控制224,187管理******")
            log.info("启动视频合并任务")
            es.execute(new VideoMergeTask(context))
            log.info("启动拉取事件通知任务")
            es.execute(new PullEventTask(context))
            log.info("启动视频合并校验任务")
            es.execute(new VideoMergeValidateTask(context))
            log.info("启动给运营发短信任务")
            es.execute(new LiveNoticeTask(context))
        }
    }

}
