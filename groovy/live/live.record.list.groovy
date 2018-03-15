import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Strings


/**
 * 应用场景：显示正在直播的列表
 *
 */
ApiUtils.processNoEncry {
    int psize = (params.psize ?: 10) as int
    String appId = Strings.getAppId(params)
    String vc = params.vc ?: "5.4"
    LiveService liveService = bean(LiveService.class)
    Map map = [
        psize: psize+1,
        sortInfo: params.sortInfo ?:[],
        appId: appId,
        liveStyle: 1,
        userId: (params.userId ?: 0)as long,
        vc: vc
    ]
    List liveList=liveService.findLiveRecordList(map)
    def hasMore = false
    if(liveList.size() > psize){
        hasMore = true
        liveList = liveList.subList(0, psize)
    }

    binding.setVariable('head', [hasMore: hasMore])
    return [liveList:liveList]
}

