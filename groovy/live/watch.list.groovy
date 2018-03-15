import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 直播信息_观众列表（观看直播时调用）
 *
 * */
ApiUtils.process {
    Assert.isNotBlankParam(params, "liveId")

    int psize = (params.psize ?: 10) as int
    long liveId = (params.liveId ?: 1) as long
    String lastUserId= params.lastUserId ?: ""
    String appId=Strings.getAppId(params)

    LiveService liveService = getBean(LiveService)

    Map result = [:]
    def userList = liveService.findWatchList(liveId,lastUserId,psize+1)
    boolean hasMore = false
    if(userList && userList.size() > psize){
        hasMore = true
        userList = userList.subList(0, psize)
    }
    result.user = userList
    binding.setVariable('head', [hasMore: hasMore])
    return result;
}
