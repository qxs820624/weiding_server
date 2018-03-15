import zs.live.ApiUtils
import zs.live.service.LivePgcService
import zs.live.service.XiaoYuService
import zs.live.utils.Assert

/**
 * 修改会议直播 php 端调用
 */
ApiUtils.processNoEncry({
    Assert.isNotBlankParam(params, "title");        //String
    Assert.isNotBlankParam(params, "beginTime");  //long
    Assert.isNotBlankParam(params, "liveId");  //long


    LivePgcService livePgcService = getBean(LivePgcService)
    return livePgcService.modify(params);

})

