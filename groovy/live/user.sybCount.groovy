import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.service.GiftService
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 获得主播/用户信息接口
 访问地址：http://域名/live/user.sybCount.groovy
 参数：
 {
    userId: // 代表操作发起者（当前登录用户）
    opId：//用户中心加密使用
    openId：//用户中心加密使用
 }
 {
     "head": {
     "status": 200,
     "hasMore": false
     },
     "body": {
     "sybCount": 15//当前搜悦币
    }
 }
 * */
ApiUtils.process {
    Assert.isNotBlankParam(params, "userId")
    Assert.isNotBlankParam(params, "opId")
    Assert.isNotBlankParam(params, "openId")

    long userId = (params.userId ?:0) as long
    String openId = params.openId ?: ""
    String opId = params.opId ?: ""
    String appId = Strings.getAppId(params)

    GiftService giftService = getBean(GiftService)

    def charmMap = giftService.getUserSybCount(userId,opId,openId,appId)
    Map res = [
        sybCount:(charmMap?.sybCount ?:0) as int,//当前中搜币
        charmCount:(charmMap?.charmCount ?:0) as int,//当前用户魅力值
        msg:charmMap.msg?.""
    ]
    return res
}
