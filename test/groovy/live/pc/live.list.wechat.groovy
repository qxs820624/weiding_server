import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Strings

/**
 * 为小程序提供直播数据列表
 * 返回免费的直播回放的所有数据
 */
ApiUtils.processNoEncry({

    String appId = Strings.getAppId(params)
    int psize = (params.psize ?: 10)as int
    long userId = (params.userId ?: 0) as long
    LiveService liveService = bean(LiveService)
    //回放，带分页
    Map overMap = [
        appId:appId,
        psize: psize,
        sortInfo:params.sortInfo,
        vc: params.vc ?: "5.6",
        userId: userId,
    ]
    //直播列表
    List liveList = liveService.getLiveListForSmallProgram(overMap)
    def hasMore = false
    if(liveList.size() > psize){
        hasMore = true
        liveList = liveList.subList(0, psize)
    }
    binding.setVariable('head', [hasMore: hasMore])
    return [liveList: liveList]
})
