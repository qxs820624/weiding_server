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
class VestCommentTask implements Runnable {
    public static final Logger imMsgLog = LoggerFactory.getLogger("imMsg")
    LiveQcloudRedis liveQcloudRedis = (LiveQcloudRedis) context.getBean(LiveQcloudRedis.class);
    QcloudLiveService qcloudLiveService = (QcloudLiveService) context.getBean(QcloudLiveService.class);
    LiveRes liveRes = (LiveRes) context.getBean(LiveRes.class);
    LiveService liveService = (LiveService) context.getBean(LiveService.class);
    GiftService giftService = (GiftService) context.getBean(GiftService.class);
    LiveCommon liveCommon = (LiveCommon) context.getBean(LiveCommon.class)
    VestUserService vestUserService=(VestUserService) context.getBean(VestUserService.class)

    ApplicationContext context

    VestCommentTask(ApplicationContext context){
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
//                    imMsgLog.info("vest comment liveIds for in live =====================>{}", keys)
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
                            int pgcStatus = live.pgcStatus ? live.pgcStatus as int : LiveCommon.FORESHOW_STATUS_0
                            int liveMode = live.liveMode ? live.liveMode as int : 1
                            long beginTime = live.beginTime ? DateUtil.getTimestamp(live.beginTime  as String) : 0
                            long currentTime =System.currentTimeMillis()
                            if(liveMode == 2 && (pgcStatus == LiveCommon.FORESHOW_STATUS_6 || currentTime < beginTime)){
                                //当会议直播未开始或者暂停状态时，读取无源的配置
                                continue
                            }
                            def config = liveRes.findVestCountConfigInfo(liveId, "VestCommentSet", live.appId ?: "")
                            if(!config){//默认统一走搜悦的配置
                                config = liveRes.findVestCountGlobalConfigInfo("VestCommentSetDefault", live.appId ?: "")
                            }
                            if(!config){
                                imMsgLog.info("VestCommentTask, config is null：liveId=>{},appId=>{}",liveId,live.appId)
                                continue
                            }
                            def configValue = Strings.parseJson(config.value)
                            int time = (configValue.time?:0) as int
                            int commentCount = RandomUtil.getRandomNumber((configValue.comment_min?:0) as int,(configValue.comment_max?:0) as int)
                            imMsgLog.info("VestCommentTask, 马甲评论配置信息，liveId:{},commentCount:{}",liveId,commentCount)
                            //从马甲中随机获取进入直播的马甲信息列表
                            if(time > 0 && commentCount > 0){
                                //获取直播绑定的所有马甲
                                List userList = []
                                userList=vestUserService.getVestUserListByLiveId(liveId,commentCount)
                                if(userList&&userList.size()>0){
                                    imMsgLog.info("VestCommentTask, liveId：{}，获取到的马甲数量:{}",liveId,userList.size())
                                    sendCommentMsg(userList,live,time)
                                }
                            }
                        }
                    }
                }
            }catch (Exception e){
                e.printStackTrace()
            }finally{
                Thread.sleep(7*60*1000)
            }
        }
    }

    def sendCommentMsg(List userList, def live, int time){
        long hostUid = (live.userId ?: 0)as long
        List commentList = liveQcloudRedis.getVestCommentList()
        if(!commentList){
            commentList = liveRes.findVestCommentList(live.appId)
            liveQcloudRedis.setVestCommentListRedis(Strings.toJson(commentList))
        }
        imMsgLog.info("VestCommentTask, get commentList liveId:{},commentList:{}",live.liveId,commentList)
        if(commentList){
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
                        int num = RandomUtil.getRandomNumber(0, commentList.size()-1)
                        def message = commentList.get(num)
                            Map map = [
                                msgInfo: [userId: user.userId, nickname: user.nickname,userImage: user.userImage, message: message,liveId:live.liveId],
                                fromAccount: live.userId as long,
                                roomId: live.roomId as int,
                                appId:live.appId
                            ]
                        String res = qcloudLiveService.sendVestMsg(map)
                        Thread.sleep(sleepTime)
                    }
                }
            }.start()
        }

    }
}
