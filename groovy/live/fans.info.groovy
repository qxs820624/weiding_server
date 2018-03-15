import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.service.GiftService
import zs.live.service.LivePgcService
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 获得主播/用户信息接口
 访问地址：http://域名/live/fans.info.groovy
 参数：
 {
    userId: // 代表操作发起者（当前登录用户）
    toUserId:// 代表操作目标
 }
 返回值：
 isFollow ,0是未关注，1是关注，2 是互相关注
 {
     "head": {
     "status": 200,
     "hasMore": false
     },
     "body": {
     "followCount": 15,//关注数
     "fansCount": 13,//粉丝数
     "isFollow": 2
    }
 }
 * */
ApiUtils.process {
    Assert.isNotBlankParam(params, "userId");
    Assert.isNotBlankParam(params, "toUserId")

    String appId = Strings.getAppId(params)

    LiveService liveService = getBean(LiveService)
    GiftService giftService = getBean(GiftService)
    LivePgcService livePgcService = getBean(LivePgcService)
    long userId
    long toUserId
    long liveId
    try {
        userId = params.userId as long
        toUserId = params.toUserId as long
        liveId = (params.liveId ?: 0) as long
    } catch (NumberFormatException e) {
        throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "参数转换错误");
    }

    def fansCount = liveService.getMyFansCount(toUserId, 1, 0, 0L, appId); //1代表直播的关注关系，0代表所有的关注情况，即单方面和相互关注
    def followCount = liveService.getMyFollowingsCount(toUserId, 1, 0, 0L, appId);
    def isFollow = liveService.isFollow(userId, toUserId, 1, appId);
    def userInfo = liveService.getUserInfo(toUserId)
    def charmMapHost = giftService.getUserCharmCount(userInfo,appId)
    int role = livePgcService.getUserRole(liveId, toUserId)
    int myRole = livePgcService.getUserRole(liveId, userId)
    return [
            followCount: followCount,
            fansCount: fansCount,
            isFollow:isFollow,
            signature: userInfo?.signature ?: "该用户很懒，还没有留下签名～",
            charmCount: (charmMapHost?.charmCount ?:0) as int,
            role: role,
            myRole: myRole
    ];
}
