import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 场景：回放列表排序
 访问地址：http://域名/live/live.record.sort.groovy
 参数：
 {
 liveId: // 直播id
 sortNum:// 排序值
 appId:
 }
 返回值：
 {
 "head": {
 "status": 200,
 "hasMore": false
 },
 "body": {
 "status": $int 1成功
 "msg": "排序失败原因"
 }
 }
 * */
ApiUtils.processNoEncry {
    Assert.isNotBlankParam(params, "liveId")     //直播id
    Assert.isNotBlankParam(params, "sortNum")   //排序值

    String appId=Strings.getAppId(params)
    LiveService liveService = getBean(LiveService)

    long sortNum
    long liveId
    try {
        sortNum = (params.sortNum ?:0)as long
        liveId = (params.liveId ?:0) as long
    } catch (NumberFormatException e) {
        throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "参数转换错误");
    }


    Map result = [:];
    int res = liveService.sortLiveRecord([liveId:liveId,sortNum:sortNum,appId:appId])
    result.status=res
    if (res ==1) {
        result.msg="成功"
    } else{
        result.msg="排序失败"
    }

    return result
}
