import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 直播收藏接口
 * 参数：{
        userId
        foreshowId
        type		//1：收藏，2：取消收藏
 * }
 *
 */
ApiUtils.process({
    Assert.isNotBlankParam(params,"userId")
    Assert.isNotBlankParam(params,"foreshowId")
    Assert.isNotBlankParam(params,"type")

    long userId
    long foreshowId
    int type
    try{
        userId = params.userId as long
        foreshowId = params.foreshowId as long
        type = params.type as int
    }catch (NumberFormatException e) {
        throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "参数转换错误");
    }
    if(!userId || !foreshowId || !type){
        return [status: 0]
    }
    String appId = Strings.getAppId(params)
    LiveService liveService = bean(LiveService)

    def status = liveService.collectLive(userId,foreshowId,type,appId)
    return [status: status]
})
