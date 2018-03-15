import zs.live.ApiUtils
import zs.live.service.LiveSdkSevice
import zs.live.service.QcloudLiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

ApiUtils.process ({
    Assert.isNotBlankParam(params,"userId")
    Assert.isNotBlankParam(params,"roomId")
    Assert.isNotBlankParam(params,"liveId")
    LiveSdkSevice liveSdkSevice = getBean(LiveSdkSevice)
    long liveId = params.liveId as long
    int roomId = params.roomId as int
    long userId = params.userId as long
    String appId=Strings.getAppId(params)
    def backMap = liveSdkSevice.start(liveId,roomId,userId,appId)
    return backMap
})
