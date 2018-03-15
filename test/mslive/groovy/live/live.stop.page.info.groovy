import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

ApiUtils.process({
    Assert.isNotBlankParam(params,"liveId")
    Assert.isNotBlankParam(params,"userId")
    Assert.isNotBlankParam(params,"appId")
    Assert.isNotBlankParam(params,"toUserId")
    long start = System.currentTimeMillis()
    ApiUtils.log.info("live.stop.page.info start,params=>{}",params)
    long liveId = params.liveId  as long
    long userId = params.userId as long
    String appId = Strings.getAppId(params)
    long toUserId = params.toUserId as long
    LiveService liveService = getBean(LiveService)
    def res = liveService.getStopPageInfo(liveId,userId,toUserId,appId)
    ApiUtils.log.info("live.stop.page.info end,time=>{},params=>{}",(System.currentTimeMillis()-start),params)
    return res
})
