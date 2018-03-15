import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 应用场景：显示正在直播的列表
 *
 */
ApiUtils.process {
    Assert.isNotBlankParam(params,"userId")
    int psize = (params.psize ?: 10) as int
    LiveService liveService = bean(LiveService.class)
    String appId = Strings.getAppId(params)
    long userId = params.userId as long
    Map map = [
        psize: psize+1,
        sortInfo: params.sortInfo,
        appId: appId,
        liveStyle: 0,
        userId: userId
    ]
    def foreshowList
    if(!params.sortInfo){   //当前页为第一页的时候获取预告列表
        foreshowList = liveService.findLiveForeshowList(map)
    }
    def liveList= liveService.findNewsLiveList(map)
    def hasMore = false
    if(liveList.size() > psize){
        hasMore = true
        liveList = liveList.subList(0, psize)
    }
    binding.setVariable('head', [hasMore: hasMore])
    return [liveList: liveList, foreshowList: foreshowList]
}

