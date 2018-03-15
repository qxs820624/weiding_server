import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 *  查看主播或用户信息：他/她的直播（直播中）
 * @author liyongguang
 * @date 2017/2/8.
 */
ApiUtils.process {
    Assert.isNotBlankParam(params, "userId");
    Assert.isNotBlankParam(params, "toUserId")
    String appId = Strings.getAppId(params)

    LiveService liveService = getBean(LiveService)
    long userId
    long toUserId
    try {
        userId = params.userId as long
        toUserId = params.toUserId as long
    } catch (NumberFormatException e) {
        throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "参数转换错误");
    }

    liveService.findUserLive(userId, toUserId, appId)
}
