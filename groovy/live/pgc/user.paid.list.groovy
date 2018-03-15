import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 我购买的付费直播列表
 * @param userId
 */
ApiUtils.processNoEncry({
    Assert.isNotBlankParam(params as Map, "userId")

    LiveService liveService = bean(LiveService.class)

    String appId = Strings.getAppId(params)
    long userId = params.userId as long;
    int pageSize = (params.pageSize ?: 10) as int
    String sortInfo = params.sortInfo
    List liveList = liveService.getUserPaidList(userId,appId,pageSize+1,sortInfo)

    def hasMore = false
    if (liveList.size()>pageSize){
        hasMore = true
        liveList.remove(pageSize)//删除多查的那条
    }
    binding.setVariable('head', [hasMore: hasMore])

    [liveList:liveList]
})
