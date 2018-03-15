import zs.live.ApiUtils
import zs.live.service.LiveSdkSevice
import zs.live.service.QcloudLiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

ApiUtils.processNoEncry({
    Assert.isNotBlankParam(params,"userId")
    Assert.isNotBlankParam(params,"roomId")
    Assert.isNotBlankParam(params,"liveId")
    long liveId = params.liveId as long
    int roomId = params.roomId as int
    long userId = params.userId as long
    String appId = Strings.getAppId(params)
    LiveSdkSevice liveSdkSevice = getBean(LiveSdkSevice)
    return liveSdkSevice.stop(liveId,roomId,userId,appId,"接口调用")
})
