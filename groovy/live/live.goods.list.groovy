import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 场景：直播商品列表
 访问地址：http://域名/live/live.goods.list.groovy
 参数：
 {
     hostUserId: // 主播的用户id
     psize
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
    Assert.isNotBlankParam(params, "hostUserId")     //发起者的userId

    String appId=Strings.getAppId(params)
    LiveService liveService = getBean(LiveService)

    long hostUserId
    try {
        hostUserId = params.hostUserId as long
    } catch (NumberFormatException e) {
        throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "参数转换错误");
    }
    int psize = (params.psize ?: 10)as int
    long liveId  = (params.liveId ?: 0) as long
    long foreshowId  = (params.foreshowId ?: 0) as long

    Map map = [userId: hostUserId,liveId: liveId, forehsowId: foreshowId, appId: appId, psize: psize]
    List goodsList = []
    goodsList = liveService.getLiveGoodsList(map)
    return [goodList: goodsList]
}
