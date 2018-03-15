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
class VestMegTask implements Runnable{
    public static final Logger imMsgLog = LoggerFactory.getLogger("imMsg")

    LiveQcloudRedis liveQcloudRedis = (LiveQcloudRedis) context.getBean(LiveQcloudRedis.class);
    QcloudLiveService qcloudLiveService = (QcloudLiveService) context.getBean(QcloudLiveService.class);
    LiveRes liveRes = (LiveRes) context.getBean(LiveRes.class);
    LiveCommon liveCommon = (LiveCommon) context.getBean(LiveCommon.class)
    VestUserService vestUserService = (VestUserService) context.getBean(VestUserService.class)

    ApplicationContext context
    VestMegTask(ApplicationContext context){
        this.context = context
    }

    @Override
    void run() {
        while (true) {
            try{
                long startTime = System.currentTimeMillis()
                //获取正在直播中的所有直播列表
                Map liveMap = liveQcloudRedis.getLiveListInLive()
                if(liveMap){
                    Set keys = liveMap.keySet()
//                    imMsgLog.info("vest into chatRoom liveIds for in live =====================>{}",keys)
                    for (String key : keys) {
                        long keyTime = System.currentTimeMillis()
                        def live = Strings.parseJson(liveMap.get(key))
                        int isPrivate = (live.isPrivate ?: 0) as int
                        long liveId = live.liveId as long
                        int roomId = live.roomId as int
                        int liveMode = live.liveMode ? live.liveMode as int : 1
                        Parallel.run([1], {
                            //给客户端同步当前观众人数
                            sendVestCountMsg(0,liveId,liveMode,live.roomId as int,live.userId as long,false,live.appId)
                        }, 1)
                        if(isPrivate == 0){
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
                            int pgcStatus = live.pgcStatus ? live.pgcStatus as int : LiveCommon.FORESHOW_STATUS_0
                            long beginTime = live.beginTime ? DateUtil.getTimestamp(live.beginTime  as String) : 0
                            long currentTime =System.currentTimeMillis()
                            imMsgLog.info("VestMegTask,直播状态判断，liveId:{},roomId={},pgcStatus:{},beginTime:{},currentTime:{}", liveId,roomId,pgcStatus,beginTime,currentTime)
//                            if(liveMode == 2 && (pgcStatus == LiveCommon.FORESHOW_STATUS_6 || currentTime < beginTime)){
                            if(liveMode != 1 && currentTime < beginTime){
                                //当会议直播未开始或者，读取无源的配置
                                if(!((live.foreshowId ?: 0) as long)){
                                    continue;
                                }
                                imMsgLog.info("VestMegTask,当会议直播未开始或者暂停状态时，读取无源的配置:{},roomId={}", liveId,roomId)
                                pgcNotSourceSendMsg(live)
                            }else {
                                long confTime = System.currentTimeMillis()
                                //根据liveId获取直播绑定的马甲数量
                                long vestCount = vestUserService.getVestUserCountByLiveId(liveId)
                                long vestAllCount = vestUserService.getVestUserVirtualCountByLiveId(liveId)+vestCount
                                //获取马甲每分钟入群人数的配置信息
                                def config = liveRes.findVestCountConfigInfo(liveId, "VestSet", live.appId ?: "")
                                int count = 0 //每分钟进入直播的马甲数量
                                int onlyCount = 0 //只增长观看人数
                                int onlyCountMin = 0 //只增长观看人数,最小数
                                int onlyCountMax = 0 //只增长观看人数,最大数
                                int sendNum = 0 //增长人数发送次数
                                int onlySendMin = 0 //增长人数最小发送次数
                                int onlySendMax = 0 //增长人数最大发送次数
                                int stop = 0 //增长人数最大发送次数
                                if(!config){//默认统一走搜悦的配置
                                    config = liveRes.findVestCountGlobalConfigInfo("VestSetDefault",live.appId)
                                }
                                if(!config){
                                    imMsgLog.info("VestMegTask, config is null：liveId=>{},appId=>{}",liveId,live.appId)
                                    continue
                                }
                                def configValue = Strings.parseJson(config.value)
                                imMsgLog.info("VestMegTask, 马甲入群配置信息：liveId:{},roomId={},config:{}",liveId,roomId, configValue)
                                count = RandomUtil.getRandomNumber((configValue.min?:0) as int, (configValue.max?:0) as int)
                                onlyCountMin = (configValue.only_count_min?:0) as int
                                onlyCountMax = (configValue.only_count_max?:0) as int
                                onlySendMin = (configValue.only_send_min?:10) as int
                                onlySendMax = (configValue.only_send_max?:30) as int
                                stop = (configValue.stop ?: 10000) as int
                                if(onlyCountMin > 0 && onlyCountMax > 0){
                                    onlyCount = RandomUtil.getRandomNumber(onlyCountMin, onlyCountMax)
                                    sendNum = RandomUtil.getRandomNumber(onlySendMin, onlySendMax)
                                }
                                if(count > 0){  //count=0:后台设置没有马甲入群
                                    imMsgLog.info("VestMegTask,马甲进入观众列表liveId=>{},roomId={},vestCount=>{},马甲读取配置时间：{},解析加读取time:{}",liveId,roomId,vestCount,System.currentTimeMillis()-confTime,System.currentTimeMillis()-keyTime)
                                    long redisTime = System.currentTimeMillis()
                                    if(vestCount <= liveQcloudRedis.maxVestCount ){
                                        //从马甲中随机获取进入直播的马甲信息列表
//                                        int start = RandomUtil.getRandomNumber(0, 25000)
//                                        List userList = liveQcloudRedis.getAllVestUserList(start, start+count)
                                        List userList = vestUserService.getVestUserList(count)
                                        if(!userList){
                                            imMsgLog.info("VestMegTask, 马甲缓存中没有马甲，读取马甲数据库：liveId{},count:{}",liveId,count)
                                            List vestList = liveRes.findAllVestUserList()
                                            vestUserService.initAllVestUserInRedis(vestList)
//                                            userList = liveQcloudRedis.getAllVestUserList(start, start+count-1)
                                            userList = vestUserService.getVestUserList(count)
                                        }
                                        //发送马甲入群消息
                                        imMsgLog.info("VestMegTask,发送马甲入群消息,liveId=>{},roomId={},count:{},vestList=>{},从马甲缓存中获取马甲列表时间：{}",liveId,roomId,count,userList.size(),System.currentTimeMillis()-redisTime)
                                        sendVestInRoomMsg(userList, live)
                                    }else {//当马甲人数大于1000时，只需要更新用户数量即可，不需要发马甲入群消息
                                        imMsgLog.info("VestMegTask, 马甲人数大于1000时,liveId=>{},roomId={},马甲进观众列表的人数:{},stop:{},count=>{}",liveId,roomId, vestCount,stop,count)
                                        Parallel.run([1], {
                                            //给客户端同步当前观众人数
                                            sendVestCountMsg(count,liveId,liveMode,live.roomId as int,live.userId as long,true,live.appId)
                                        }, 1)
                                    }
                                }else{
                                    imMsgLog.info("VestMegTask 马甲设置进入马甲人数为0，liveId=>{},roomId={}",liveId,roomId)
                                }
                                if(onlyCount > 0 && vestAllCount < stop){//配置文件中设置的只增长马甲的人数
                                    imMsgLog.info("VestMegTask 增加马甲数量,liveId=>{},,roomId={},vestCount:{},stop:{},onlycount=>{}",liveId,roomId, vestAllCount,stop,onlyCount)
                                    sendVestCountInRoomMsg(onlyCount,sendNum,live)
                                }
                            }
                        }
                        imMsgLog.info("VestMegTask, 每次循环所需时间，liveId:{} time：{}",liveId,System.currentTimeMillis()-keyTime)
                    }
                }
                long endTime = System.currentTimeMillis()
                imMsgLog.info("VestMegTask, 直播中循环一次列表所需时间,time:{}", endTime-startTime)
//                //获取已结束的会议直播
//                new Thread() {
//                    public void run() {
//                        def pgcList = liveRes.findStopedPgcList()
//                        for(def live:pgcList){
//                            if(live.foreshowId){
//                                String title = live.title ? live.title as String: ""
//                                if(liveCommon.liveEnv.equals("pre")){
//                                    if(!(title.startsWith("zstest"))){
//                                        continue;
//                                    }
//                                }
//                                if(liveCommon.liveEnv.equals("online")){
//                                    if(title.startsWith("zstest")){
//                                        continue;
//                                    }
//                                }
////                        pgcNotSourceSendMsg(live)
//                                sendLiveRecordTotalWatchCount(live)
//                            }
//                        }
//                        imMsgLog.info("VestMegTask, 循环一次已结束的会议直播列表所需时间,time:{}", System.currentTimeMillis()-endTime)
//                    }
//                }.start()
//                imMsgLog.info("VestMegTask, 循环一次所需的时间，time:{}", System.currentTimeMillis()-startTime)
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
    //当会议直播处于无源状态(暂停或结束)时，读取无源的马甲配置
    def pgcNotSourceSendMsg(def live){
        long startTime = System.currentTimeMillis()
        long  liveId = (live.liveId ?: 0) as long
//        int vestCount = liveQcloudRedis.getLiveWatherCount(liveId)
        int vestCount = vestUserService.getLiveRoomWatchCount(liveId)
        def config = liveRes.findVestCountConfigInfo(liveId, "VestMeetingAddSet", live.appId ?: "")
        if(!config){//默认统一走搜悦的配置
            config = liveRes.findVestCountGlobalConfigInfo("VestMeetingAddSetDefault", live.appId ?: "")
        }
        if(!config){
            imMsgLog.info("pgcNotSourceSendMsg,config is null：liveId=>{},appId=>{}",liveId,live.appId)
            return
        }
        def addConfValue = Strings.parseJson(config.value as String)
        if(addConfValue){
            int userMin = (addConfValue.user_min ?: 0) as int
            int userMax = (addConfValue.user_max ?: 0) as int
            int time = (addConfValue.time ?: 60) as int
            int bodyMin = (addConfValue.body_min ?: 0) as int
            int bodyMax = (addConfValue.body_max ?: 0) as int
            int bodyCount = RandomUtil.getRandomNumber(bodyMin, bodyMax)
            imMsgLog.info("VestMegTask 会议直播无源配置信息, liveId:{},roomId={},userMax:{}, bodyCount:{}",liveId,live.roomId, userMax, bodyCount)
            if(vestCount < userMax && bodyCount > 0){
                if((vestCount+bodyCount) > userMax)
                    bodyCount = userMax-vestCount
                //从马甲中随机获取进入直播的马甲信息列表
//                int start = RandomUtil.getRandomNumber(0, 25000)
//                List userList = liveQcloudRedis.getAllVestUserList(start, start+bodyCount-1)
                List userList = vestUserService.getVestUserList(bodyCount)
                if(!userList){
                    List vestList = liveRes.findAllVestUserList()
                    vestUserService.initAllVestUserInRedis(vestList)
                    userList = vestUserService.getVestUserList(bodyCount)
                }
                //发送马甲入群消息
                imMsgLog.info("VestMegTask 会议直播无源发送马甲入群, liveId:{},roomId={},vestCount:{},userMax:{},bodyCount:{},userList:{},马甲读取配置时间:{}",liveId,live.roomId, vestCount, userMax, bodyCount,userList.size(),System.currentTimeMillis()-startTime)
                sendVestInRoomMsg(userList, live)
            }
        }
    }

    /**
     * 马甲入群的消息在一分钟内发完，每个消息之间有时间间隔
     * @param userList
     * @param live
     * @return
     */
    def sendVestInRoomMsg(List userList, def live) {
        new Thread(){
            public void run(){
                long t = System.currentTimeMillis()
                if(!userList){
                    return
                }
                int size = userList.size()
                //一分钟之内随机发马甲
                List<Integer> SleepNum = RandomUtil.splitRedPackets(60,size)
                if(!SleepNum){
                    imMsgLog.info("VestMegTask sendVestInRoomMsg SleepNum is null,liveId={},SleepNum={},size={}",live.liveId,SleepNum,size)
                    return
                }
                List userOkList = []
                for(int i=0;i<size;i++){
                    long startTime = System.currentTimeMillis()
                    def userInfo = userList.get(i)
                    userInfo.liveId = live.liveId
                    String res = qcloudLiveService.sendVestIntoRoomMsg([userInfo: [userInfo],fromAccount: live.userId as long, roomId: live.roomId as int,appId:live.appId])
                    long qTime = System.currentTimeMillis()
                    //将马甲用户放入观众列表中
                    String actionStatus = Strings.parseJson(res)?.ActionStatus
                    if("OK".equals(actionStatus)){
                        userOkList.add(userInfo)
                        liveQcloudRedis.setLiveWather(live.liveId as long,Strings.toJson(userInfo),userInfo.userId as long)
                        if(live.liveMode as int == LiveCommon.LIVE_MODE_1){
                            liveQcloudRedis.addLiveWatchCount(live.liveId as long,1,24*60*60)
                        }else {
                            liveQcloudRedis.addLiveWatchCount(live.liveId as long, 1)
                        }

                    }
                    long redisTime = System.currentTimeMillis()
                    if(i< size){
                        Thread.sleep(SleepNum.get(i)*1000)
                    }
                    imMsgLog.info("VestMegTask，一个马甲进房间所需时间，liveId:{},time:{},请求腾讯云时间：{}，放入缓存时间：{}", live.liveId,System.currentTimeMillis()-startTime,qTime-startTime,redisTime-qTime)
                }
                //将新绑定的马甲用户更新到缓存中,set马甲发普通消息的列表,另外用于判断马甲数是否大于1000
//                liveQcloudRedis.setVestUserListByLiveId(live.liveId as long, userOkList)
                vestUserService.setVestUserListByLiveId(live.liveId as long, userOkList)
            }
        }.start()
    }
    /**
     * 发送每分钟增加马甲数量消息
     * @param count
     * @param liveId
     * @param roomId
     * @param hostUid
     * @return
     */
    def sendVestCountMsg(int count,long liveId,int liveMode,int roomId,long hostUid,boolean setRedis,String appId){
        //将更新的马甲数量放入缓存中
        imMsgLog.info("VestMegTask, 给客户端同步当前观众人数,liveId=>{},roomId={},watchCount=>{},count=>{},",liveId,roomId,vestUserService.getLiveRoomWatchCount(liveId as long),count)
        Map msgInfo = [watchCount: vestUserService.getLiveRoomWatchCount(liveId as long), liveId:liveId]
        Map map = [msgInfo: msgInfo,fromAccount: hostUid,roomId: roomId,appId:appId]
        String res = qcloudLiveService.sendVestCountMsg(map)
        String actionStatus = Strings.parseJson(res)?.ActionStatus
        if("OK".equals(actionStatus) && setRedis){  //增加马甲数量时，增加马甲uv的缓存
            if(liveMode == 1){
                liveQcloudRedis.setVestCountPerMinute(liveId, count, 24*60*60)
//                liveQcloudRedis.setVestOnlyCountRedis(liveId, count, 24*60*60)
            }else {
                liveQcloudRedis.setVestCountPerMinute(liveId, count)
//                liveQcloudRedis.setVestOnlyCountRedis(liveId, count)
            }
            liveQcloudRedis.addWatherTotalCount(liveId, count)
        }
    }

    /**
     * 马甲入群人数的增长的消息在一分钟内发完，每个消息之间有时间间隔
     * @param count
     * @param sendNum
     * @param live
     * @return
     */
    def sendVestCountInRoomMsg(int count,int sendNum, def live) {
        new Thread(){
            public void run(){
                if(!count || !sendNum){
                    return
                }
                List<Integer> countList = RandomUtil.splitRedPackets(count,sendNum)
                if(!countList){
                    imMsgLog.info("VestMegTask sendVestCountInRoomMsg countList is null,liveId={},count={},sendNum={}",live.liveId,count,sendNum)
                    return
                }
                int size = countList.size()
                int liveMode = (live.liveMode ?: 1)as int
                //一分钟之内随机发马甲
                List<Integer> SleepNum = RandomUtil.splitRedPackets(60,size)
                if(!SleepNum){
                    imMsgLog.info("VestMegTask sendVestCountInRoomMsg SleepNum is null,liveId={},SleepNum={},size={}",live.liveId,SleepNum,size)
                    return
                }
                for(int i=0;i<size;i++){
                    if(liveMode == 1 && !liveQcloudRedis.getLiveRecord(live.liveId as long)){
                        break;
                    }
                    sendVestCountMsg(countList.get(i),live.liveId,liveMode,live.roomId as int,live.userId as long,true,live.appId)
                    if(i< size){
                        Thread.sleep(SleepNum.get(i)*1000)
                    }
                }
            }
        }.start()
    }
}
