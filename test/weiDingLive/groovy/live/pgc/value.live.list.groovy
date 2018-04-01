import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.utils.Assert
import zs.live.utils.Strings
import zs.live.utils.VerUtils

/**
 * 应用场景：直播首页数据
 * 示例请求：http://61.135.210.239:8811/live/pgc/value.live.list.groovy?userId=2900329&appId=souyue&categoryId=1&psize=1
 */
ApiUtils.processNoEncry() {
    Assert.isNotBlankParam(params,"userId")
    Assert.isNotBlankParam(params,"categoryId")

    LiveService liveService = bean(LiveService.class)
    QcloudLiveService qcloudLiveService = bean(QcloudLiveService.class)

    boolean isAndroidOs = Strings.isAndroidOs(request,params)
    int psize = (params.psize ?: 10) as int
    String sortInfo = params.sortInfo
    int categoryId = params.categoryId as int
    String appId = Strings.getAppId(params)
    long userId = params.userId as long
    def targetUserId = params.targetUserId

    long start_time = System.currentTimeMillis()
    //直播提醒：查询当前用户一小时内未开始直播；表：live_foreshow，字段：begin_time;
    def liveNotice = liveService.getMyLiveNotice(userId,appId,params.vc)
    long end1 = System.currentTimeMillis()
    //直播分类
    List liveCategroyList = []
    def rollImgList = []
    List liveList = []
    liveCategroyList = liveService.getLiveCategroyList(userId,appId)
    long end2 = System.currentTimeMillis()
    Map map = [
        userId:userId,
        targetUserId:targetUserId,
        appId:appId,
        categoryId:categoryId!=0?categoryId:null,
        vc:params.vc,
        psize: psize*2,
        cateList:liveCategroyList.subList(1,liveCategroyList.size()),
        sortInfo:sortInfo,
        isAndroidOs:isAndroidOs
    ]
    def hasMore = true
//    Map configSetInfo = qcloudLiveService.getAppConfigSetInfo(appId)
//    int liveClassify = (configSetInfo.liveClassify ?:0) as int
//    int classifyChannel = (configSetInfo.classifyChannel ?:0) as int
//    int styleCategory = (configSetInfo.styleCategory ?:0) as int
//    int slideshowStatus = (configSetInfo.slideshowStatus ?:0) as int
//    if(configSetInfo.size()>0 && VerUtils.toIntVer(params.vc ) > VerUtils.toIntVer("5.7.0") ){
//        if(!liveClassify){  //分类是否关闭
//            liveCategroyList = []
//        }else {
//            liveCategroyList.get(0).category =  URLDecoder.decode(configSetInfo.recommendTitle, "utf-8")
//        }
//        if(classifyChannel && styleCategory && !categoryId){
//            //查询推荐的数据
//            Map liveMap = [:]
//            liveMap = liveService.getRecommendLiveList(map)
//            if(liveMap.faceList?.size()*2<psize){
//                hasMore = false
//            }
//            liveList.addAll(liveMap?.resultList)
//            liveList.addAll(liveMap?.faceList)
//        }else {
//            if(classifyChannel && !categoryId){
//                map.isRecommend = 1
//            }
//            liveList = liveService.getValueLiveList(map)
//            if (liveList.size()<psize){
//                hasMore = false
//            }
//        }
//        if(slideshowStatus){  //轮播图是否关闭
//            rollImgList = liveService.getRollImgList(map)
//        }
//    }else {
        liveList = liveService.getValueLiveList(map)
        if (liveList.size()<psize){
            hasMore = false
        }
        rollImgList = liveService.getRollImgList(map)
//    }
    long end3 = System.currentTimeMillis()
    binding.setVariable('head', [hasMore: hasMore])
    def foreshowList = liveService.findNotStartedLiveForeshowList(map);
    long end4 = System.currentTimeMillis()
    ApiUtils.log.info("value.live.list liveNotice:${end1-start_time},liveCategroyList:${end2-end1},liveList:${end3-end2},foreshowList:${end4-end3}")
    return [
        liveNotice:liveNotice,                  //提醒
        rollImgList:rollImgList,                //轮播图
        foreshowViewList:foreshowList,
        foreshowCount:foreshowList.size,            //预告数
        liveCategroyList:liveCategroyList,      //分类
        liveList:liveList                       //直播、暂停、系列、回放
    ]
}
