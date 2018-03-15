import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Strings
import zs.live.utils.VerUtils


/**
 * 应用场景：显示我的回放列表,分享落地页也需要使用
 *
 */
ApiUtils.processNoEncry {
    int psize = (params.psize ?: 10) as int
    LiveService liveService = bean(LiveService.class)
    String appId = Strings.getAppId(params)
    long toUserId = (params.toUserId ?: 0)as long
    long userId = (params.userId ?: 0)as long
    boolean displayPrivate = false
    if(toUserId != userId){
        displayPrivate = true
    }
    Map map = [
        psize: psize+1,
        sortInfo: params.sortInfo ?:[],
        userId: toUserId,
        displayPrivate: displayPrivate,
        appId: appId,
        vc:params.vc
    ]
    List liveList = []
    if(VerUtils.toIntVer(params.vc as String) < VerUtils.toIntVer("5.6")){
        liveList = liveService.findMyLiveRecordList(map)
    }else {
        liveList = liveService.findMyLiveRecordListNew(map)
    }
    def hasMore = false
    if(liveList.size() > psize){
        hasMore = true
        liveList = liveList.subList(0, psize)
    }

    binding.setVariable('head', [hasMore: hasMore])
    return [liveList:liveList]
}

