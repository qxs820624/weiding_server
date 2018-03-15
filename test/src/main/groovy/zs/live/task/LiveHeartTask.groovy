package zs.live.task

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Repository
import zs.live.APP
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.entity.LiveRecord
import zs.live.service.QcloudLiveService
import zs.live.utils.Strings

/**
 * Created by kang on 2016/10/17.
 * 定时（5s）扫描redis, 所有正在直播的key, 判断时间时间差是否大于90s
 * 如果时间差小于90s则不处理
 * 大于90s则调用停止直播逻辑，并删除该redis
 * 定时器扫描该直播是否有m3u8地址
 * 没有的话删除 live_record 表对应记录 （该操作主要是为了规避客户端发起直播了但是没有发送推流请求）
 */
@Slf4j
class LiveHeartTask implements Runnable {

    LiveQcloudRedis liveQcloudRedis = (LiveQcloudRedis) context.getBean(LiveQcloudRedis.class);
    QcloudLiveService qcloudLiveService = (QcloudLiveService) context.getBean(QcloudLiveService.class);

    ApplicationContext context
    LiveHeartTask(ApplicationContext context){
        this.context = context
    }

    @Override
    void run() {
        while (true) {
            try {
                Map<String,String> liveMap = liveQcloudRedis.getLiveListInLive()
                if(liveMap){
                    Set<String> liveIdSet = liveMap.keySet();
                    liveIdSet.each {
                        LiveRecord l = Strings.parseJson(liveMap.get(it),LiveRecord.class)
                        if(l.liveMode == 1){//只有互动直播才有心跳
                            long heartTime = liveQcloudRedis.getHeartTime(it as int)
                            long current = System.currentTimeMillis();
                            long sub = (current - heartTime)/1000
                            //如果直播超时则调用停止直播逻辑
                            if(sub > qcloudLiveService.liveHeartTaskTime){
                                log.info("心跳扫描超时停止直播，liveId:"+it+",时间差："+sub)
                                qcloudLiveService.stopLiveComm(l.liveId,l.roomId,l.userId,l.appId,"心跳扫描超时定时任务")
                            }
                            //推流失败时结束直播
                            if(!l.channelId){
                                log.info("心跳扫描没有channelId，liveId:"+it)
                            }
                        }
                    }
                }
            }catch (Exception e){
                log.info("LiveHeartTask Exception:",e.getMessage())
                e.printStackTrace()
            }finally{
                Thread.sleep(5 * 1000)
            }
        }
    }
}
