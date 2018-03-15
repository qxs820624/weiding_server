import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.service.LiveForeshowService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * php后台删除 互动/会议直播预告
 * @param foreshowId
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
    int res = liveForeshowService.removeGroupDataList(foreshowId, appId)
    if (res) {
        result.status=1
        result.msg="成功"
    } else{
        result.status=0
        result.msg="失败"
    }

    return result
}
