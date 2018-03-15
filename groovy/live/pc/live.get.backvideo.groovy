import zs.live.ApiUtils
import zs.live.entity.LiveRecord
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.utils.Assert

ApiUtils.processNoEncry {
    Assert.isNotBlankParam(params,"liveId")
    long liveId = params.liveId as long
    LiveService liveService = getBean(LiveService)
    LiveRecord live = liveService.findLiveRecordByLiveId(liveId)
    QcloudLiveService qcloudLiveService = getBean(QcloudLiveService)
    String backString  = qcloudLiveService.getBackInfo(live.roomId,liveId,live.foreshowId,live.appId,"手工调用重置回看地址")
    return backString
}
