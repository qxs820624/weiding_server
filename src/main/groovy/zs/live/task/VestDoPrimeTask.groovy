package zs.live.task

import groovy.util.logging.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import zs.live.common.LiveCommon
import zs.live.dao.mysql.LiveRes
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.service.QcloudLiveService
import zs.live.service.VestUserService
import zs.live.utils.DateUtil
import zs.live.utils.RandomUtil
import zs.live.utils.Strings

/**
 * Created by Administrator on 2016/11/28.
 */
@Slf4j
class VestDoPrimeTask implements Runnable {
    public static final Logger imMsgLog = LoggerFactory.getLogger("imMsg")
    LiveQcloudRedis liveQcloudRedis = (LiveQcloudRedis) context.getBean(LiveQcloudRedis.class);
    QcloudLiveService qcloudLiveService = (QcloudLiveService) context.getBean(QcloudLiveService.class);
    LiveRes liveRes = (LiveRes) context.getBean(LiveRes.class);
    LiveCommon liveCommon = (LiveCommon) context.getBean(LiveCommon.class)
    VestUserService vestUserService=(VestUserService) context.getBean(VestUserService.class)

    ApplicationContext context
    VestDoPrimeTask(ApplicationContext context){
        this.context = context
    }
    @Override
    void run() {
        while (true){
            try{
                //获取正在直播中的所有直播列表
                Map liveMap = liveQcloudRedis.getLiveListInLive()
                if(liveMap) {
                    Set keys = liveMap.keySet()
//                    imMsgLog.info("vest do prime liveIds for in live =====================>{}", keys)
                    for (String key : keys) {
                        def live = Strings.parseJson(liveMap.get(key))
                        int isPrivate = (live.isPrivate ?: 0) as int
                        long liveId = live.liveId as long
                        if(isPrivate == 0){
                            String title = live.title ? live.title as String: ""
                            if(liveCommon.liveEnv.equals("pre")){
                                if(!title.startsWith("zstest")){
                                    continue;
                                }
                            }
                            if(liveCommon.liveEnv.equals("online")){
                                if(title.startsWith("zstest")){
                                    continue;
                                }
                            }
                            int pgcStatus = live.pgcStatus ? live.pgcStatus as int : LiveCommon.FORESHOW_STATUS_0
                            int liveMode = live.liveMode ? live.liveMode as int : 1
                            long beginTime = live.beginTime ? DateUtil.getTimestamp(live.beginTime  as String) : 0
                            long currentTime =System.currentTimeMillis()
                            if(liveMode == 2 && (pgcStatus == LiveCommon.FORESHOW_STATUS_6 || currentTime < beginTime)){
                                //当会议直播未开始或者暂停状态时，读取无源的配置
                                continue
                            }
                            def config = liveRes.findVestCountConfigInfo(liveId, "VestDoPrimeSet", live.appId ?: "")
                            if(!config){//默认统一走搜悦的配置
                                config = liveRes.findVestCountGlobalConfigInfo("VestDoPrimeSetDefault",live.appId ?: "")
                            }
                            if(!config){
                                imMsgLog.info("VestDoPrimeTask, config is null：liveId=>{},appId=>{}",liveId,live.appId)
                                continue
                            }
                            def configValue = Strings.parseJson(config.value as String)
                            int time = (configValue.time?:60) as int
                            int userCount = RandomUtil.getRandomNumber((configValue.user_min ?: 0) as int, (configValue.user_max ?: 0) as int)
                            int primeMin = (configValue.prime_min ?: 0) as int
                            int primeMax = (configValue.prime_max ?: 0) as int
                            //从马甲中随机获取进入直播的马甲信息列表
                            if(time > 0 && userCount > 0){
                                imMsgLog.info("VestDoPrimeTask，马甲点赞的配置信息 liveId:{},time:{},usercount:{}",liveId,time,userCount)
                                List userList = []
                                userList = vestUserService.getVestUserListByLiveId(liveId,userCount)
                                if(userList&&userList.size()>0){
                                    sendDoPrimeMsg(userList,live,time,primeMin,primeMax)
                                }
                            }
                        }
                    }
                }
            }catch (Exception e){
                e.printStackTrace()
            }finally{
                Thread.sleep(60 * 1000)
            }
        }
    }

    def sendDoPrimeMsg(List userList, def live, int time, int primeMin, int primeMax){
        new Thread(){
            public void run(){
                int count  = userList.size()
                List timeList  = RandomUtil.splitRedPackets(time, count)
                for (int i=0;i<count;i++){
                    long sleepTime = timeList.get(i) * 1000
                    int primeCount = RandomUtil.getRandomNumber(primeMin,primeMax)
                    def userInfo = userList.get(i)
                    sendDoPrimeMsg(primeCount,live,userInfo)
                    Thread.sleep(sleepTime)
                }
            }
        }.start()
    }
    def sendDoPrimeMsg(int primeCount,def live,def userInfo){
        new Thread(){
            public void run(){
                for (int j=0;j<primeCount;j++){
                    int type = j == 0 ? 2 : 3//点赞
                    Map msgInfo = [userId:userInfo.userId as long,nickname:userInfo.nickname,type:type,liveId:live.liveId as long]
                    Map map = [msgInfo: msgInfo, fromAccount: live.userId as long, roomId: live.roomId as int,appId:live.appId]
                    String res = qcloudLiveService.sendVestDoPrimeMsg(map)
                    Thread.sleep(200)
                }
            }
        }.start()
    }

}
