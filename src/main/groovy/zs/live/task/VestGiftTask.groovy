package zs.live.task

import groovy.util.logging.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import zs.live.common.LiveCommon
import zs.live.dao.mysql.LiveRes
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.service.GiftService
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.utils.Parallel
import zs.live.service.VestUserService
import zs.live.utils.RandomUtil
import zs.live.utils.Strings

/**
 * Created by Administrator on 2016/11/28.
 */
@Slf4j
class VestGiftTask implements Runnable {
    public static final Logger imMsgLog = LoggerFactory.getLogger("imMsg")
    LiveQcloudRedis liveQcloudRedis = (LiveQcloudRedis) context.getBean(LiveQcloudRedis.class);
    QcloudLiveService qcloudLiveService = (QcloudLiveService) context.getBean(QcloudLiveService.class);
    LiveRes liveRes = (LiveRes) context.getBean(LiveRes.class);
    LiveService liveService = (LiveService) context.getBean(LiveService.class);
    GiftService giftService = (GiftService) context.getBean(GiftService.class);
    LiveCommon liveCommon = (LiveCommon) context.getBean(LiveCommon.class)
    VestUserService vestUserService=(VestUserService) context.getBean(VestUserService.class)
    ApplicationContext context

    VestGiftTask(ApplicationContext context){
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
                    for (String key : keys) {
                        def live = Strings.parseJson(liveMap.get(key))
                        int isPrivate = (live.isPrivate ?: 0) as int
                        long liveId = live.liveId as long
                        if(isPrivate == 0){
//                            imMsgLog.info("VestGiftTask， live env:{},title:{}",liveCommon.liveEnv,live.title)
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
                            int liveMode = (live.liveMode ?: 1) as int
                            if(liveMode == 2){
                                continue
                            }
                            def config = liveRes.findVestCountConfigInfo(liveId, "VestGiftSet", live.appId ?: "")
                            if(!config){//默认统一走搜悦的配置
                                config = liveRes.findVestCountGlobalConfigInfo("VestGiftSetDefault",live.appId ?: "")
                            }
                            if(!config){
                                imMsgLog.info("VestGiftTask, config is null：liveId=>{},appId=>{}",liveId,live.appId)
                                continue
                            }
                            def configValue = Strings.parseJson(config.value)
                            int time = (configValue.time?:0) as int
                            int userCount = RandomUtil.getRandomNumber((configValue.user_min ?: 0) as int, (configValue.user_max ?: 0) as int)
                            int giftMin = (configValue.gift_min ?: 0) as int
                            int giftMax = (configValue.gift_max ?: 0) as int
                            imMsgLog.info("VestGiftTask，马甲打赏礼物配置信息 , liveId:{},time:{},userCount:{},giftmin:{},giftMax:{}",liveId,time,userCount,giftMin,giftMax)
                            //从马甲中随机获取进入直播的马甲信息列表
                            if(time > 0 && userCount > 0){
                                //获取直播绑定的所有马甲
                                List userList = []
                                userList = vestUserService.getVestUserListByLiveId(liveId,userCount)
                                if(userList&&userList.size()>0){
                                    sendGiftMsg(userList,live,time, giftMin, giftMax)
                                }
                            }
                        }
                    }
                }
            }catch (Exception e){
                e.printStackTrace()
            }finally{
                Thread.sleep(60*1000)
            }
        }
    }

    def sendGiftMsg(List userList, def live, int time, int giftMin, int giftMax){
        long hostUid = (live.userId ?: 0)as long
        new Thread(){
            public void run(){
                int count  = userList.size()
                List timeList  = RandomUtil.splitRedPackets(time, count)
                for (int i=0;i<count;i++){
                    if(!liveQcloudRedis.getLiveRecord(live.liveId as long)){
                        break;
                    }
                    long sleepTime = timeList.get(i) * 1000
                    def user = userList.get(i)
                    int giftCount = RandomUtil.getRandomNumber(giftMin, giftMax)
                    def giftInfo = getGiftInfo(live.appId ?: "")
                    giftInfo.giftCount = giftCount
                    def userInfo = liveService.getUserInfo(hostUid)
                    def charmMapHost = giftService.getUserCharmCount(userInfo,live.appId)
                    //打赏礼物
                    Map map = [
                        userInfo: user,
                        anchorInfo: [charmCount: (charmMapHost?.charmCount ?:0) as int,liveId: live.liveId as long],
                        giftInfo: giftInfo,
                        giftCount: giftCount,
                        fromAccount: live.userId as long,
                        roomId: live.roomId as int,
                        appId:live.appId
                    ]
                    String res = qcloudLiveService.sendGiftImMsg(map)
                    String actionStatus = Strings.parseJson(res)?.ActionStatus
                    if("OK".equals(actionStatus)){
                        def hostUserInfo = liveService.getUserInfo(live.userId as long)
                        Parallel.run([1], {
                            //将打赏礼物的记录入库，供客户端展示使用
                            Map giftMap = [
                                giftName: giftInfo.giftName,
                                userAdd: 0,
                                userSub: 0,
                                toUserName: hostUserInfo.userName,
                                creatTime: System.currentTimeMillis(),
                                giftNum: giftCount
                            ]
                            giftService.addVestPayGiftOrder(giftMap)
                        }, 1)
                    }
                    Thread.sleep(sleepTime)
                }
            }
        }.start()
    }
    def getGiftInfo(String appId){
        List freeGiftList = liveQcloudRedis.getFreeGiftListRedis()
        if(!freeGiftList){
            freeGiftList = []
            List giftList = giftService.getGiftList( 0, 0, appId )
            giftList?.each{
                if((it.giftPrice ?: 0) as int == 0){
                    freeGiftList.add(it)
                }
            }
            liveQcloudRedis.setFreeGiftListRedis(Strings.toJson(freeGiftList))
        }
        int count = RandomUtil.getRandomNumber(0,freeGiftList.size()-1)
        def value = freeGiftList.get(count)
        return  value
    }
}
