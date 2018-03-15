import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.service.LiveForeshowService
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 http://域名/live/pc/pgc/foreshow.group.fill.groovy
 请求参数：
 ```groovy
 {
 foreshowId: 123 //必传
 subForeshowIds:456,789 //英文逗号分割
 }
 ```
 返回值:
 ```groovy
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
    Assert.isNotBlankParam(params, "subForeshowIds")

    LiveForeshowService liveForeshowService = getBean(LiveForeshowService)

    long foreshowId
    def subForeshowIdList = []
    String[] subForeshowIds = params.subForeshowIds?.split(",")
    try {
        foreshowId = (params.foreshowId ?:0)as long

        subForeshowIds?.each{
            subForeshowIdList << (it as long)
        }
    } catch (NumberFormatException e) {
        throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "参数转换错误");
    }
    String appId = Strings.getAppId(params)
    int res = liveForeshowService.fillLiveForeshow(foreshowId, subForeshowIdList,appId)
    Map result = [:];
    if (subForeshowIdList && res == subForeshowIdList.size() ) {
        result.status=1
        result.msg="成功"
    } else{
        result.status=0
        result.msg="失败"
    }

    return result
}
