import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 应用场景：直播首页数据
 * 示例请求：http://61.135.210.239:8811/live/pgc/value.live.list.groovy?userId=2900329&appId=souyue&categoryId=1&psize=1
 */
ApiUtils.process {

    LiveService liveService = bean(LiveService.class)

    boolean isAndroidOs = Strings.isAndroidOs(request,params)
    int psize = (params.psize ?: 10) as int
    String sortInfo = params.sortInfo
    String appId = Strings.getAppId(params)
    String srpId = params.srpId
    long userId = params.userId as long
    //直播分类
    List liveCategroyList = liveService.getLiveCategroyList(userId,appId)
    Map map = [
        userId:userId,
        appId:appId,
        psize:psize,
        sortInfo:sortInfo,
        vc:params.vc,
        srpId:srpId,
        cateList:liveCategroyList.subList(1,liveCategroyList.size()),
        isAndroidOs:isAndroidOs
    ]
    //直播列表
    List liveList = liveService.getValueLiveListBySrp(map)

    def hasMore = true
    if (liveList.size()<psize){
        hasMore = false
    }
    binding.setVariable('head', [hasMore: hasMore])

    def foreshowViewList = liveService.findNotStartedLiveForeshowList(map);
    return [
        foreshowCount:foreshowViewList.size,            //预告数
        foreshowViewList: foreshowViewList,      //预告列表
        liveList:liveList                       //直播、暂停、系列、回放
    ]
}
