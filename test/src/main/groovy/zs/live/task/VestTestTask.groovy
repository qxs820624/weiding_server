package zs.live.task

import groovy.util.logging.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import zs.live.dao.mysql.LiveRes
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.service.VestUserService
import zs.live.utils.RandomUtil
import zs.live.utils.Strings

/**
 * Created by Administrator on 2016/11/28.
 */
@Slf4j
class VestTestTask implements Runnable {
    public static final Logger imMsgLog = LoggerFactory.getLogger("imMsg")
    LiveQcloudRedis liveQcloudRedis = (LiveQcloudRedis) context.getBean(LiveQcloudRedis.class);
    QcloudLiveService qcloudLiveService = (QcloudLiveService) context.getBean(QcloudLiveService.class);
    LiveRes liveRes = (LiveRes) context.getBean(LiveRes.class);
    LiveService liveService = (LiveService) context.getBean(LiveService.class);
    VestUserService vestUserService = (VestUserService) context.getBean(VestUserService.class)

    ApplicationContext context

    VestTestTask(ApplicationContext context){
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
                    imMsgLog.info("vest test liveIds for in live =====================>{}", keys)
                    keys.each{
                        def live = Strings.parseJson(liveMap.get(it))
                        int isPrivate = (live.isPrivate ?: 0) as int
                        long liveId = live.liveId as long
                        if(isPrivate == 0){
                            int time = 60
                            int userCount = 5
                            //从马甲中随机获取进入直播的马甲信息列表
                            if(time > 0 && userCount > 0){
//                                int start = RandomUtil.getRandomNumber(1, 20000)
//                                List userList = liveQcloudRedis.getAllVestUserList(start, start+userCount)
                                List userList = vestUserService.getVestUserList(userCount)
                                if(!userList){
                                    List vestList = liveRes.findAllVestUserList()
                                    vestUserService.initAllVestUserInRedis(vestList)
                                    userList = vestUserService.getVestUserList(userCount)
                                }
                                sendGiftMsg(userList,live,time)
                            }
                        }
                    }
                }
                Thread.sleep(60*1000)
            }catch (Exception e){
                e.printStackTrace()
            }
        }
    }

    def sendGiftMsg(List userList, def live, int time){
        long hostUid = (live.userId ?: 0)as long
        new Thread(){
            public void run(){
                int count  = userList.size()
                List timeList  = RandomUtil.splitRedPackets(time, count)
                for (int i=0;i<count;i++){
                    long sleepTime = timeList.get(i) * 1000
                    def user = Strings.parseJson(userList.get(i))
                    Map map = [msgInfo:[message: "主播好美！", color: "#dee055",type:1],
                               roomId:live.roomId as int,
                               fromAccount:hostUid,
                               appId: live.appId]
                    String res = qcloudLiveService.sendVestImMsg(map)
                    Thread.sleep(sleepTime)
                }
            }
        }.start()
    }
}
