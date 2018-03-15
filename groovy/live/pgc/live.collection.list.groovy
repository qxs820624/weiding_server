import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 直播收藏列表接口
 * 参数：{
        userId
 * }
 *
 */
ApiUtils.process({
    Assert.isNotBlankParam(params,"userId")

    long userId
    try{
        userId = params.userId as long
    }catch (NumberFormatException e) {
        throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "参数转换错误");
    }
    params.userId = userId
    int psize = (params.psize ?: 20) as int
    params.psize = psize+1
    String appId = Strings.getAppId(params)
    LiveService liveService = bean(LiveService)

    def result = liveService.getLiveCollectionListByUserId(params as Map, appId)
    boolean hasMore = false
    if(result.size() > psize){
        result = result.subList(0,psize)
        hasMore = true
    }
    binding.setVariable('head', [hasMore: hasMore])
    return [liveList: result]
})
