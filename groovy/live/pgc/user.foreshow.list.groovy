import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 手机端用户关注系列列表
 * @param userId
 */
ApiUtils.processNoEncry({
    Assert.isNotBlankParam(params as Map, "userId")
    long userId = params.userId as long;
    int psize = (params.psize ?: 10) as int
    String sortInfo = params.sortInfo
    String vc = params.vc as String
    String appId=Strings.getAppId(params)

    LiveService liveService = getBean(LiveService)
    def foreshowList = liveService.listUserForeshow(userId, psize, sortInfo,appId,vc);

    def hasMore = true
    if (!foreshowList || foreshowList.size()<psize){
        hasMore = false
    }
    binding.setVariable('head', [hasMore: hasMore])

    [foreshowList:foreshowList]
})
