import zs.live.ApiUtils
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 直播禁言接口
 */
ApiUtils.processNoEncry({
    Assert.isNotBlankParam(params as Map, "userId")
    Assert.isNotBlankParam(params as Map, "forbidUserId")
    Assert.isNotBlankParam(params as Map, "roomId")
    Assert.isNotBlankParam(params as Map, "liveId")

    ApiUtils.log.info("wangtf forbid.comment.groovy params:{}", params)
    long userId = params.userId as long
    long forbidUserId = params.forbidUserId as long
    int roomId = params.roomId as int
    long liveId = params.liveId as long
    String appId = Strings.getAppId(params)

    LiveService liveService = getBean(LiveService)

    return liveService.forbidCommentToGroupMember(userId,forbidUserId,roomId,liveId,appId)

})
