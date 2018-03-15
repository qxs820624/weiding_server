import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.common.LiveCommon
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.service.LivePgcService
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.service.VestUserService
import zs.live.utils.Assert
import zs.live.utils.RandomUtil
import zs.live.utils.Strings

/**
 * 场景：马甲发评论
 * */
ApiUtils.processNoEncry ({
    Assert.isNotBlankParam(params, "liveId")
    Assert.isNotBlankParam(params, "roomId")     //房间id
    Assert.isNotBlankParam(params, "message")
    Assert.isNotBlankParam(params, "type")

    int type = params.type as int
    long liveId = params.liveId as long
    int roomId = params.roomId as int
    String message = params.message as String
    String appId=Strings.getAppId(params)
    long userId = (params.userId ?: 0)as long

    long time = System.currentTimeMillis()
    LiveService liveService = getBean(LiveService)
    QcloudLiveService qcloudLiveService = getBean(QcloudLiveService)
    LiveQcloudRedis liveQcloudRedis = getBean(LiveQcloudRedis)
    LivePgcService livePgcService = getBean(LivePgcService)
    VestUserService vestUserService=getBean(VestUserService)

    def live = liveService.findLiveByLiveId(liveId as long)
    if(!live){
        live = liveService.findLiveRecordByLiveId(liveId)
        if(!live){
            return [status: 0,msg:"直播不存在！"]
        }
    }
    def userInfo = null
    //判断该用户是否被禁言
    if(liveQcloudRedis.hgetForbidCommentUserInfo(liveId,userId)){
        return [status:0,msg:"该用户已被禁言！"]
    }
    long liveTime = System.currentTimeMillis()
    String msg = ""
    int role = 0
    long roleTime = System.currentTimeMillis()
    if(type == 1){//随机马甲
        List userList = vestUserService.getVestUserListByLiveId(liveId,1)
        if(userList&&userList.size()>0){
            userInfo = userList? userList.get(0) : null
        }else{
            return [status: 0,msg:"没有马甲！"]
        }
    }else if(type == 2){//指定马甲
        if(userId){
//            def vestUser = liveQcloudRedis.getVestUserInfoByLiveIdAndUserId(liveId,userId)
            def vestUser = vestUserService.getVestUserFromRedis(liveId,userId)
            if(vestUser){
                userInfo = vestUser
            }else {
                msg = "该马甲不在直播中！"
            }
        }
    }else if(type == 5){//系统

    }else if(type == 6){//微信网页版发送消息
        role = livePgcService.getUserRole(liveId, userId)
        userInfo = liveService.getUserInfo(userId)
    }else{
        role = livePgcService.getUserRole(liveId, userId)
        System.out.println("===type:3,role:2场控==type:4,role:1主持人===liveId:"+liveId+",userId:"+userId+",type:"+type+",role:"+role)
        if((type == 3 && role == LiveCommon.LIVE_ROLE_2) || (type == 4 && role == LiveCommon.LIVE_ROLE_1)){//场控 或者主持人
            userInfo = liveService.getUserInfo(userId)
        }else {
            msg = "该用户不是场控或者主持人！"
        }
    }
    ApiUtils.log.info("wangtf PHP后台发送评论,liveId:{},roomId:{},获取直播信息用时：{},获取用户角色用时：{},获取用户信息时长：{}",liveId,roomId,liveTime-time,roleTime-liveTime,System.currentTimeMillis()-roleTime)
    if(userInfo){
        //发送评论
        long startTime = System.currentTimeMillis()
        ApiUtils.log.info("vest.send.comment,liveId=>{},userInfo=>{}",liveId,userInfo)
        def res = qcloudLiveService.sendVestMsg([roomId: roomId,fromAccount: live.userId as long,appId:live.appId,
                                                 msgInfo: [userId: userInfo.userId, nickname: userInfo.nickname,userImage: userInfo.userImage, message: message,liveId:liveId,role:role?:LiveCommon.LIVE_ROLE_3]])
        ApiUtils.log.info("wangtf  PHP后台发送评论时长，liveId:{},res:{},time:{}",liveId,res,System.currentTimeMillis()-startTime)
        String actionStatus = Strings.parseJson(res)?.ActionStatus
        if("OK".equals(actionStatus)){
            return [status: 1,msg:"发送成功！"]
        }else {
            return [status: 0,msg:"发送失败！"]
        }
    }else {
        return [status: 0,msg:msg]
    }

})
