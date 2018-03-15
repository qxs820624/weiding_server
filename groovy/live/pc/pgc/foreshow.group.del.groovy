import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.service.LiveForeshowService
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 19. php后台删除直播系列———梁占峰

 http://域名/live/pc/pgc/foreshow.group.del.groovy
 请求参数：

 {
 catId: 123 //必传
 }

 返回值:

 {
 "head": {
 "status": 200,
 "hasMore": false
 },
 "body": {
 "msg": "成功"
 }
 }
 */

ApiUtils.processNoEncry {
    Assert.isNotBlankParam(params, "foreshowId")

    LiveForeshowService liveForeshowService = getBean(LiveForeshowService)

    long foreshowId

    try {
        foreshowId = (params.foreshowId ?:0)as long
    } catch (NumberFormatException e) {
        throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "参数转换错误");
    }
    String appId = Strings.getAppId(params)
    Map result = [:];
    int res = liveForeshowService.remove(foreshowId, appId)
    if (res) {
        result.status=1
        result.msg="成功"
    } else{
        result.status=0
        result.msg="失败"
    }
    return result
}
