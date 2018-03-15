import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 查询用户是否被关注
 */
ApiUtils.processNoEncry({
    Assert.isNotBlankParam(params, "userId")
    Assert.isNotBlankParam(params,"toUserId")

    long userId = (params.userId ?: 0) as long  //用户id
    long toUserId = (params.toUserId ?: 0) as long  //被关注用户id
    String appId = Strings.getAppId(params)

    LiveService liveService = bean(LiveService)

    def isFollow = liveService.isFollow(userId, toUserId, 1, appId);

    return [isFollow: isFollow]     //0:没有关注，1：关注，2：互相关注
})
