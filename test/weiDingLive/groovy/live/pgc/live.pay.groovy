import zs.live.ApiUtils
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 直播禁言接口
 */
ApiUtils.processNoEncry({
    Assert.isNotBlankParam(params as Map, "userId")
    Assert.isNotBlankParam(params as Map, "liveId")

    ApiUtils.log.info("wangtf live.pay.groovy  params:{}", params)
    long userId = params.userId as long
    long liveId = params.liveId as long
    String appId = Strings.getAppId(params)

    Map map =[
            liveId: liveId,
            userId: userId,
            payType: params.payType ?: 0,
            orderType: params.orderType?: 3,
            amount: params.amount,
            orderId: Strings.getOrderId(liveId, userId)
    ]
    LiveService liveService = getBean(LiveService)

    return liveService.payOrder(map)

})
