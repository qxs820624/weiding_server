import zs.live.ApiUtils
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
    QcloudLiveService qcloudLiveService = getBean(QcloudLiveService)
    return qcloudLiveService.stopLive(liveId,roomId,userId,appId)
})
