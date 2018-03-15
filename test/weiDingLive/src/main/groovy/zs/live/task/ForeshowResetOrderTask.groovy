package zs.live.task

import groovy.util.logging.Slf4j
import org.springframework.context.ApplicationContext
import zs.live.dao.mysql.LiveForeshowRes

/**
 * 重置已结束预告排序（24小时外）
 */
@Slf4j
class ForeshowResetOrderTask implements Runnable {
    private final ApplicationContext context
    LiveForeshowRes liveForeshowRes

    public ForeshowResetOrderTask(ApplicationContext context){
        this.context = context
        liveForeshowRes = (LiveForeshowRes)context.getBean(LiveForeshowRes.class)
    }

    @Override
    void run() {
        try {
            def num = liveForeshowRes.resetForeshowOrder()
            log.info("预告排序重置数 >> ${num}")
        }catch (Exception e){
            log.info("ForeshowResetOrderTask Exception:",e);
        }

    }
}
