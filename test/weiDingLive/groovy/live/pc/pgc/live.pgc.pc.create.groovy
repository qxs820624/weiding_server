import zs.live.ApiUtils
import zs.live.service.LivePgcService
import zs.live.utils.Assert

/**
 * 创建会议直播 php调用
 */
ApiUtils.processNoEncry({
    Assert.isNotBlankParam(params, "title");        //String
    Assert.isNotBlankParam(params, "beginTime");  //long
    Assert.isNotBlankParam(params, "pgcType");  //long

    LivePgcService livePgcService = getBean(LivePgcService)
    Map map=[:];
    map.putAll(params)
    return livePgcService.create(map);

})
