import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Strings
import zs.live.utils.VerUtils


/**
 * 应用场景：显示某用户的直播
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
        onlyUgc: "onlyUgc"
    ]
    List liveList = []

    def livingInfo = liveService.findUserLive(userId, toUserId, appId)
    if (livingInfo){
        livingInfo.viewType = 83    //83 正在直播,87 直播回放
        livingInfo.invokeType = 82  //82 正在直播,84 直播回放
        liveList << livingInfo
    }

    List tempList = []
    if(VerUtils.toIntVer(params.vc as String) < VerUtils.toIntVer("5.6")){
        tempList = liveService.findMyLiveRecordList(map)
    }else {
        tempList = liveService.findMyLiveRecordListNew(map)
    }
    tempList.each {
        it.viewType = 87    //83 正在直播,87 直播回放
        it.invokeType = 84  //82 正在直播,84 直播回放
    }
    def hasMore = false
    if(tempList.size() > psize){
        hasMore = true
        tempList = tempList.subList(0, psize)
    }
    liveList.addAll(tempList)
    binding.setVariable('head', [hasMore: hasMore])
    return [liveList:liveList]
}

