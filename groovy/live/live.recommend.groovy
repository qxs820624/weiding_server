import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 场景：直播推荐和取消推荐接口
 访问地址：http://域名/live/live.recommend.groovy
 参数：
 {
    type: // 1:展示，2：取消展示
    foreshowId:// 预告id
    liveId: // 直播id
    operType: //1：推荐 2：直播首页 3 同时到推荐、直播首页
 }
 返回值：
 {
     "head": {
        "status": 200,
        "hasMore": false
    },
    "body": {
        "status": 1
    }
 }
 * */
ApiUtils.processNoEncry {
    Assert.isNotBlankParam(params,"type")
    Assert.isNotBlankParam(params,"operType")
    Assert.eitherOrJudgeParam(params, "foreshowId","liveId")

    String appId=Strings.getAppId(params)
    LiveService liveService = getBean(LiveService)

    long foreshowId = (params.foreshowId ?: 0) as long
    long liveId = (params.liveId ?: 0) as long
    int type = params.type ? params.type as int : 0
    int operType = params.operType ? params.operType as int : 0

    int result = liveService.updateLiveRecommend(type, foreshowId, liveId,operType)
    return [status:result]
}
