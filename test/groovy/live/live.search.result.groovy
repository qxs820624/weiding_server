import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Strings

/**
 * 根据关键词匹配标题，查询直播列表
 */
ApiUtils.process({
    long userId = (params.userId ?: 0) as long
    String keyword = params.keyword ?: ""
    String appId = Strings.getAppId(params)
    def sortInfo = params.sortInfo ? Strings.parseJson(params.sortInfo) : null
    int psize = params.psize ? params.psize as int : 10
    String vc = params.vc
    ApiUtils.log.info("live.search.result.groovy,params:{},sortInfo:{}",params,sortInfo)
    LiveService liveService = bean(LiveService)
    def result = liveService.searchLiveListByKeyWord(userId,keyword,psize,sortInfo,appId,vc)
    def hasMore = result.hasMore
    binding.setVariable('head', [hasMore: hasMore])
    return [liveList:result.liveList,groupList:result.groupList]
})
