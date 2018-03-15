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
 视频采集任务管理
 该任务在224和187服务器上执行
 */
@Slf4j
@Component
class VideoGatherCenterTaskManager187 {

    int minThreads = 5

    @Value('${spring.cloud.client.ipAddress}')
    String webIp
    @Autowired
    ApplicationContext context

    @PostConstruct
    public void start() {
        ExecutorService es = Executors.newFixedThreadPool(minThreads)
        def execList= ["103.29.134.224","103.29.134.187"] //要执行的服务器IP
        if(execList.contains(webIp)){
            log.info("启动视频采集任务，线程数 >> ${minThreads}")
            //自动抓取视频
            es.execute(new VideoGatherTask(context))
            //手动添加视频
            es.execute(new VideoGatherCMSTask(context))
        }
    }
}
