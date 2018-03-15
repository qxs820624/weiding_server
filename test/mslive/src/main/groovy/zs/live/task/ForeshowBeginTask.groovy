package zs.live.task

import groovy.util.logging.Slf4j
import org.springframework.context.ApplicationContext
import zs.live.dao.mysql.LiveRes
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService

/**
 * 根据当前时间判断，将到时间的直播预告（互动和会议）的状态(foreshow_status=1)改为开始模式
 * Created by Administrator on 2016/10/29.
 */
@Slf4j
class ForeshowBeginTask implements Runnable{
    private final ApplicationContext context
    private final LiveRes liveRes
    private final LiveService liveService

    public ForeshowBeginTask(ApplicationContext context){
        this.context = context
        liveRes=  (LiveRes) context.getBean(LiveRes.class);
        liveService = (LiveService) context.getBean(LiveService.class);
    }
    @Override
    void run() {
            try {
                //更新直播预告表中的foreshow_status为开始，并且给关注用户发消息
                liveService.updateForeshowStatusBegin()
                //更新live_record表中的pgc_status为开始
                liveService.updateLivePgcStatusBegin()
//                Thread.sleep(1*60*1000)
            }catch (Exception e){
                log.info("ForeshowBeginTask Exception:",e);
            }

    }

}
