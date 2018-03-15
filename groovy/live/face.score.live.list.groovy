import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 应用场景：显示正在直播列表（颜值）的列表
 *
 */
ApiUtils.process {
    Assert.isNotBlankParam(params,"userId")

    int psize = (params.psize ?: 10) as int
    String appId = Strings.getAppId(params)
    long userId = params.userId as long

    Map map = [
        psize: psize+1,
    //    lastId: (params.lastId ?: 0) as int ,
        sortInfo:(params.sortInfo ?: "") as String,
        appId: appId,
        userId: userId,
        vc:params.vc
    ]

    LiveService liveService = bean(LiveService.class)

    def liveList= liveService.findFaceLiveList(map)
    def hasMore = false
    if(liveList.size() > psize){
        hasMore = true
        liveList = liveList.subList(0, psize)
    }
    binding.setVariable('head', [hasMore: hasMore])
    return [liveList: liveList]
}

