import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

ApiUtils.processNoEncry({
    Assert.isNotBlankParam(params as Map, "userId")
    Assert.isNotBlankParam(params as Map, "kickUserId")
    Assert.isNotBlankParam(params as Map, "roomId")
    Assert.isNotBlankParam(params as Map, "liveId")

    ApiUtils.log.info("wangtf kick.member.groovy params:{}", params)
    long userId = params.userId as long
    long kickUserId = params.kickUserId as long
    int roomId = params.roomId as int
    long liveId = params.liveId as long
    String appId = Strings.getAppId(params)

    LiveService liveService = getBean(LiveService)

    return liveService.kickGroupMember(userId, kickUserId, roomId, liveId, appId)
})
