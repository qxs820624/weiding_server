package zs.live.task

import groovy.util.logging.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Repository
import zs.live.APP
import zs.live.ApiUtils
import zs.live.common.LiveCommon
import zs.live.common.QcloudCommon
import zs.live.dao.mysql.LiveRes
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.service.QcloudLiveService
import zs.live.service.VestUserService
import zs.live.service.impl.VestUserServiceImpl
import zs.live.utils.DateUtil
import zs.live.utils.Parallel
import zs.live.utils.RandomUtil
import zs.live.utils.Strings



/**
 * 马甲发送消息，定时任务1分钟
 */
@Slf4j
class VestLiveLogMegTask implements Runnable{
    public static final Logger imMsgLog = LoggerFactory.getLogger("imMsg")

    LiveQcloudRedis liveQcloudRedis = (LiveQcloudRedis) context.getBean(LiveQcloudRedis.class);
    QcloudLiveService qcloudLiveService = (QcloudLiveService) context.getBean(QcloudLiveService.class);
    LiveRes liveRes = (LiveRes) context.getBean(LiveRes.class);
    LiveCommon liveCommon = (LiveCommon) context.getBean(LiveCommon.class)
    VestUserService vestUserService = (VestUserService) context.getBean(VestUserService.class)

    ApplicationContext context
    VestLiveLogMegTask(ApplicationContext context){
        this.context = context
    }

    @Override
    void run() {
        while (true) {
            try{
                //获取已结束的会议直播
                long startTime = System.currentTimeMillis()
                def pgcList = liveRes.findStopedPgcList()
                for(def live:pgcList){
                    if(live.foreshowId){
                        String title = live.title ? live.title as String: ""
                        if(liveCommon.liveEnv.equals("pre")){
                            if(!(title.startsWith("zstest"))){
                                continue;
                            }
                        }
                        if(liveCommon.liveEnv.equals("online")){
                            if(title.startsWith("zstest")){
                                continue;
                            }
                        }
//                        pgcNotSourceSendMsg(live)
                        sendLiveRecordTotalWatchCount(live)
                    }
                }
                imMsgLog.info("VestMegTask, 循环一次已结束的会议直播列表所需时间,time:{}", System.currentTimeMillis()-startTime)
            }catch (Exception e){
                imMsgLog.info("VestMegTask Exception:",e.getMessage())
                e.printStackTrace()
            }finally{
                Thread.sleep(60*1000)
            }
        }
    }
    //直播结束后同步当前房间人数
    def sendLiveRecordTotalWatchCount(live){
        //将更新的马甲数量放入缓存中
        imMsgLog.info("VestMegTask,直播结束时同步观看人数，当前直播的累计观看人数， liveId=>{},watchCount=>{}",live.liveId,liveQcloudRedis.getLiveWatherTotalCount(live.liveId as long))
        Map msgInfo = [watchCount: liveQcloudRedis.getLiveWatherTotalCount(live.liveId as long), liveId:live.liveId]
        Map map = [msgInfo: msgInfo,fromAccount: live.userId,roomId: live.roomId,appId:live.appId]
        String res = qcloudLiveService.sendVestCountMsg(map)
    }

}
