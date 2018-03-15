import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

ApiUtils.process({
    Assert.isNotBlankParam(params, "userId")

    long userId = (params.userId ?: 0) as long
    String appId = Strings.getAppId(params)

    LiveService liveService = bean(LiveService)

    int code = liveService.checkLiveAccessByUserId(userId, appId)
    if(code == 1){  //有直播权限
        return [state: 1]
    }else {
        throw new ApiException(700,"您没有直播权限   欢迎联系客服开通呦！")
    }
})
