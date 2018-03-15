import zs.live.ApiUtils
import zs.live.service.LivePgcService
import zs.live.service.XiaoYuService
import zs.live.utils.Assert

/**
 * 删除会议直播，在没有创建预告前可以删除，创建后不可以删除
 */

ApiUtils.processNoEncry({
    Assert.isNotBlankParam(params, "liveId");        //long

    LivePgcService livePgcService = getBean(LivePgcService)
    return livePgcService.delete(params);

})
