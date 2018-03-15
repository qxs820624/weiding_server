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
import zs.live.service.VestUserService
import zs.live.utils.DateUtil
import zs.live.utils.RandomUtil
import zs.live.utils.Strings

/**
 * Created by Administrator on 2016/11/28.
 */
@Slf4j
class VestQuitRoomTask implements Runnable {
    public static final Logger imMsgLog = LoggerFactory.getLogger("imMsg")
    LiveQcloudRedis liveQcloudRedis = (LiveQcloudRedis) context.getBean(LiveQcloudRedis.class);
    QcloudLiveService qcloudLiveService = (QcloudLiveService) context.getBean(QcloudLiveService.class);
    LiveRes liveRes = (LiveRes) context.getBean(LiveRes.class);
    LiveService liveService = (LiveService) context.getBean(LiveService.class);
    GiftService giftService = (GiftService) context.getBean(GiftService.class);
    LiveCommon liveCommon = (LiveCommon) context.getBean(LiveCommon.class)
    VestUserService vestUserService=(VestUserService) context.getBean(VestUserService.class)
    Map liveMap = new HashMap()
    ApplicationContext context

    VestQuitRoomTask(ApplicationContext context){
        this.context = context
    }
    @Override
    void run() {
        while (true){
            try{
                long startTime = System.currentTimeMillis()
                //获取正在直播中的所有直播列表
                Map liveMap = liveQcloudRedis.getLiveListInLive()
                if(liveMap) {
                    Set keys = liveMap.keySet()
//                    imMsgLog.info("vest quit room liveIds for in live =====================>{}", keys)
                    for (String key : keys) {
                        def live = Strings.parseJson(liveMap.get(key))
                        int isPrivate = (live.isPrivate ?: 0) as int
                        long liveId = live.liveId as long
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
                            int pgcStatus = live.pgcStatus ? live.pgcStatus as int : 0
                            int liveMode = live.liveMode ? live.liveMode as int : 1
                            long beginTime = live.beginTime ? DateUtil.getTimestamp(live.beginTime as String) : 0
                            long currentTime =System.currentTimeMillis()
                            imMsgLog.info("VestQuitRoomTask,直播状态判断，liveId:{},pgcStatus:{},beginTime:{},currentTime:{}", liveId,pgcStatus,beginTime,currentTime)
//                            if(liveMode == 2 && (pgcStatus == LiveCommon.FORESHOW_STATUS_6 || currentTime < beginTime)){   //当会议直播未开始或者暂停状态时，读取无源的配置
                            if(liveMode != 1 && currentTime < beginTime){
                                if(!((live.foreshowId ?: 0) as long)){
                                    continue;
                                }
                                imMsgLog.info("VestQuitRoomTask, 会议直播未开始或者暂停状态时，读取无源的配置,liveId:{}",liveId)
                                pgcNotSourceSendMsg(live)
                            }else {
                                //根据liveId获取直播绑定的马甲数量
//                                long vestAllCount =vestUserService.getVestUserVirtualAndRealCountByLiveId(liveId)
                                long watchCount = vestUserService.getLiveRoomWatchCount(liveId)
                                def config = liveRes.findVestCountConfigInfo(liveId, "VestQuitRoomSet", live.appId ?: "")
                                if(!config){//默认统一走搜悦的配置
                                    config = liveRes.findVestCountGlobalConfigInfo("VestQuitRoomSetDefault",live.appId ?: "")
                                }
                                if(!config){
                                    imMsgLog.info("VestQuitRoomTask,config is null：liveId=>{},appId=>{}",liveId,live.appId)
                                    continue
                                }
                                def configValue = Strings.parseJson(config.value as String)
                                int time = (configValue.time?:0) as int
                                int userCount = RandomUtil.getRandomNumber((configValue.user_min ?: 0) as int, (configValue.user_max ?: 0) as int)
                                int sendCount = RandomUtil.getRandomNumber(10,20)
                                int stop = (configValue.stop ?: 0) as int
                                imMsgLog.info("VestQuitRoomTask,直播中的马甲减少的配置信息 liveId:{},time:{},vestCount:{},userCount:{},stop:{}",liveId,time,watchCount,userCount,stop)
                                //首先减少马甲数量的缓存，当马甲数量的缓存为0时，减少观众列表中的马甲列表
                                if(time > 0 && userCount > 0 && watchCount > stop){
                                    imMsgLog.info("VestQuitRoomTask,直播中的马甲减少，先减数量，后减少头像, liveId:{},vestAllCount:{},stop:{}",liveId,watchCount, stop)
                                    quitRoom(live, time, userCount, sendCount)
                                }
                            }
                        }
                    }
                }
                long endTime = System.currentTimeMillis()
                imMsgLog.info("VestQuitRoomTask, 直播中循环一次列表所需时间,time:{}", endTime-startTime)
                //获取已结束的会议直播
//                def pgcList = liveRes.findStopedPgcList()
//                pgcList?.each{live ->
//                    if(live.foreshowId){
//                        pgcNotSourceSendMsg(live)
//                    }
//                }
//                imMsgLog.info("VestQuitRoomTask, 循环一次已结束的会议直播列表所需时间,time:{}", System.currentTimeMillis()-endTime)
                imMsgLog.info("VestQuitRoomTask, 循环一次所需的时间，time:{}", System.currentTimeMillis()-startTime)
            }catch (Exception e){
                e.printStackTrace()
            }finally{
                Thread.sleep(60*1000)
            }
        }
    }
    //当会议直播处于无源状态(未开始，暂停或结束)时，读取无源的马甲配置
    def pgcNotSourceSendMsg(def live){
        long liveId = (live.liveId ?: 0) as long
        if(liveId && liveMap.get(liveId)){
            long time = (liveMap.get(liveId) ?: 0) as long
            if(System.currentTimeMillis()-time > 4*60*1000){
                liveMap.remove(liveId)
            }else {
                imMsgLog.info("VestQuitRoomTask,马甲人数大于区间人数，正在减少，liveId:{}",liveId)
                return
            }
        }
        def config = liveRes.findVestCountConfigInfo(liveId, "VestMeetingQuitSet", live.appId ?: "")
        if(!config){//默认统一走搜悦的配置
            config = liveRes.findVestCountGlobalConfigInfo( "VestMeetingQuitSetDefault", live.appId ?: "")
        }
        if(!config){
            imMsgLog.info("VestQuitRoomTask,config is null：liveId=>{},appId=>{}",liveId,live.appId)
            return
        }
        imMsgLog.info("VestQuitRoomTask, 会议直播读取无源的配置,liveId:{},config:{}",liveId, config)
        def quitConfValue = Strings.parseJson(config.value as String)
        if(quitConfValue){
            int userMin = (quitConfValue.user_min ?: 0) as int
            int userMax = (quitConfValue.user_max ?: 0) as int
            int time = (quitConfValue.time ?: 60) as int
            int bodyMin = (quitConfValue.body_min ?: 0) as int
            int bodyMax = (quitConfValue.body_max ?: 0) as int
            int bodyCount = RandomUtil.getRandomNumber(bodyMin, bodyMax)
            int sendCount = RandomUtil.getRandomNumber(5,20)
            long vestAllCount = vestUserService.getVestUserVirtualAndRealCountByLiveId(liveId)//所有的马甲的uv
            imMsgLog.info("VestQuitRoomTask, 无源的会议直播配置信息, liveId:{}, vestAllCount:{}, userMax:{}, bodyCount:{}",liveId,vestAllCount, userMax,bodyCount)
            if(vestAllCount > 0 && vestAllCount > userMin && time > 0){
                if(vestAllCount > userMax){//当马甲总数大于区间内的最大值，则在三分钟内将人数减少到区间的范围
                    liveMap.put(liveId, System.currentTimeMillis())
                    int userCount = RandomUtil.getRandomNumber(userMin, userMax)
                    userCount = vestAllCount- userCount
                    imMsgLog.info("VestQuitRoomTask, 马甲总数大于区间内的最大值,在三分钟内将人数减少到区间的范围, liveId:{}, vestAllCount:{}, userMax:{}, userCount:{}",liveId,vestAllCount, userMax, userCount)
                    if(userCount > 0)
                        quitRoomInThreeMinute(live,userCount,sendCount)
                }else {//当马甲人数在区间内时，每分钟减少n个人
                    imMsgLog.info("VestQuitRoomTask, 马甲总数在区间内,每分钟减少n个人 liveId:{}, vestAllCount:{}, userMax:{}, bodyCount:{}",liveId,vestAllCount, userMax, bodyCount)
                    if((vestAllCount-bodyCount) >= userMin && bodyCount >0){
                        quitRoom(live, 60, bodyCount, sendCount)
                    }
                }
            }else {
                imMsgLog.info("VestQuitRoomTask, 马甲总数小于区间内的最小值，马甲不减少 liveId:{}, vestAllCount:{}, userMin:{}",liveId,vestAllCount, userMin)
            }
        }
    }
    //每分钟减少n个人
    def quitRoom(def live,int time, int userCount, int sendCount){
        new Thread(){
            public void run(){
                long liveId = live.liveId as long
                long vestCount = vestUserService.getVestUserVirtualCountByLiveId(liveId as long)
                long vestUserCount = vestUserService.getVestUserCountByLiveId(liveId)
                if(vestCount <= 0){//当马甲数量缓存为0时，减少马甲的头像缓存
                    delVestCountOrDelWatchList(live, vestUserCount, userCount, sendCount, time, false)
                }else {//当马甲数量的缓存不为0时，减少马甲数量缓存，不减少马甲用户列表的缓存
                    delVestCountOrDelWatchList(live, vestCount , userCount, sendCount, time, true)
                }
            }
        }.start()
    }
    //当马甲数不为0时，减少马甲数，当马甲数为0时，减少观众列表中的马甲
    def delVestCountOrDelWatchList( def live,long vestCount, int userCount, int sendCount, int time,boolean flag){
        long liveId = (live.liveId ?: 0) as long
        int roomId = (live.roomId ?: 0) as int
        int liveMode = (live.liveMode ?: 0) as int
        long hostUid = (live.userId ?: 0) as long
        if(vestCount < userCount){
            userCount = vestCount
        }
        if(sendCount > userCount){
            sendCount = userCount
        }
        List timeList = RandomUtil.splitRedPackets(time, sendCount)
        List countList = RandomUtil.splitRedPackets(userCount, sendCount)
        for (int i=0; i<sendCount; i++){
            if(liveMode == 1 && !liveQcloudRedis.getLiveRecord(live.liveId as long)){
                break;
            }
            long sleepTime = timeList.get(i)*1000
            int count = countList.get(i)
            if(flag){
                if(vestUserService.getVestUserVirtualCountByLiveId(liveId) < count){
                    count = vestUserService.getVestUserVirtualCountByLiveId(liveId)
                }
                sendVestCountMsg(count, liveId, liveMode, roomId, hostUid,live.appId)
            }else {
                sendVestUserQuitRoomMsg(count,vestCount, liveId,liveMode, roomId, hostUid,live.appId)
            }
            Thread.sleep(sleepTime)
        }
    }
    //在三分钟内将马甲数减少到区间内的值
    def quitRoomInThreeMinute(def live, int userCount, int sendCount){
        new Thread(){
            public void run() {
                long vestCount = vestUserService.getVestUserVirtualCountByLiveId(live.liveId as long)
                if(vestCount >= userCount){//只减少马甲数量缓存
                    imMsgLog.info("VestQuitRoomTask,只减少马甲数量缓存, liveId:{},roomId={},vestCount:{},userCount:{} ",live.liveId,live.roomId,vestCount,userCount)
                    delVestCountOrDelWatchList(live, vestCount, userCount, sendCount, 3*60, true)
                }else if(vestCount == 0 ){
                    //当只有马甲头像缓存时，只删除马甲头像缓存
                    long vestUserCount = vestUserService.getVestUserCountByLiveId(live.liveId as long)
                    if(vestUserCount < userCount){
                        userCount = vestUserCount
                    }
                    imMsgLog.info("VestQuitRoomTask,只删除马甲头像缓存, liveId:{},roomId={},vestCount:{},userCount:{} ",live.liveId,live.roomId,vestUserCount,userCount)
                    delVestCountOrDelWatchList(live,vestUserCount, userCount, sendCount, 3*60, false)
                } else{
                    //当马甲数量的缓存小于需要减少的马甲人数时，前两分钟减少马甲数量缓存，后一分钟减少马甲头像的缓存
                    long vestUserCount = vestUserService.getVestUserCountByLiveId(live.liveId as long)
                    imMsgLog.info("VestQuitRoomTask,，前两分钟减少马甲数量缓存，后一分钟减少马甲头像的缓存, liveId:{},roomId={},vestCount:{},vestUsderCount:{}userCount:{} ",live.liveId,live.roomId,vestCount,vestUserCount,userCount)
                    delVestCountOrDelWatchList(live, vestCount, vestCount  as int,sendCount, 2*60, true)
                    delVestCountOrDelWatchList(live, vestUserCount, (userCount-vestCount) as int,sendCount, 1*60, false)
                }
                imMsgLog.info("VestQuitRoomTask, 马甲三分钟内减少,liveId:{},减少了{}人,当前马甲数：{}，", live.liveId, userCount,vestUserService.getLiveRoomWatchCount(live.liveId as long) )
                liveMap.remove(live.liveId as long)
            }
        }.start()
    }
    /**
     * 发送每分钟减少马甲数量消息
     * @param count
     * @param liveId
     * @param roomId
     * @param hostUid
     * @return
     */
    def sendVestCountMsg(int count,long liveId,int liveMode, int roomId,long hostUid,String appId){
        //将更新的马甲数量放入缓存中
        imMsgLog.info("VestQuitRoomTask,只减少马甲的数量缓存 liveId=>{},watchCount=>{},count=>{},",liveId,vestUserService.getLiveRoomWatchCount(liveId as long),count)
        Map msgInfo = [watchCount: vestUserService.getLiveRoomWatchCount(liveId as long)-count, liveId:liveId]
        Map map = [msgInfo: msgInfo,fromAccount: hostUid,roomId: roomId,appId:appId]
        String res = qcloudLiveService.sendVestCountMsg(map)
        String actionStatus = Strings.parseJson(res)?.ActionStatus
        if("OK".equals(actionStatus)){
            if(liveMode == 1){
                liveQcloudRedis.setVestCountPerMinute(liveId, -count, 24*60*60)
            }else {
                liveQcloudRedis.setVestCountPerMinute(liveId, -count)
            }
        }
    }
    /**
     * 马甲退房间，将用户信息从观众列表缓存和马甲用户列表缓存中删除
     * @param count
     * @param vestUserCount
     * @param liveId
     * @param roomId
     * @param hostUid
     */
    def sendVestUserQuitRoomMsg(int count,long vestUserCount, long liveId,int liveMode,int roomId,long hostUid,String appId){
        imMsgLog.info("VestQuitRoomTask,将马甲头像从观众列表中删掉 liveId=>{},watchCount=>{},count=>{},",liveId,vestUserService.getLiveRoomWatchCount(liveId as long),count)
        //获取要退出房间的马甲列表
        List userList = vestUserService.getVestUserListByLiveId(liveId,count)
        //调用腾讯云接口发送消息
        if(!userList){
            return
        }
        Map msgInfo = [watchCount: vestUserService.getLiveRoomWatchCount(liveId as long)-userList.size(), liveId:liveId]
        Map map = [msgInfo: msgInfo,fromAccount: hostUid,roomId: roomId,appId:appId]
        String res = qcloudLiveService.sendVestCountMsg(map)
        String actionStatus = Strings.parseJson(res)?.ActionStatus
        if("OK".equals(actionStatus)){
            //将用户信息从观众列表缓存和马甲用户列表缓存中删除
            for (def user : userList){
                long vestUserId = user.userId as long
                liveQcloudRedis.delVestWather(liveId, vestUserId)
                vestUserService.delVestUserByLiveIdAndUserId(liveId,user)
            }
            if(liveMode==1){
                liveQcloudRedis.addLiveWatchCount(liveId, -userList.size(), 24*60*60)    //更新头像uv的缓存
            }else {
                liveQcloudRedis.addLiveWatchCount(liveId, -userList.size())    //更新头像uv的缓存
            }
        }
    }

}
