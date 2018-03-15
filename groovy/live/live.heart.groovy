import zs.live.ApiUtils
import zs.live.service.QcloudLiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

ApiUtils.process({
    Assert.isNotBlankParam(params,"liveId")
    Assert.isNotBlankParam(params,"timeSpan")
    long liveId = params.liveId as long
    long timeSpan = params.timeSpan as long
    QcloudLiveService qcloudLiveService = getBean(QcloudLiveService)
    int status= qcloudLiveService.sendHeadRate(liveId,timeSpan)
    return ["status":status]
})
