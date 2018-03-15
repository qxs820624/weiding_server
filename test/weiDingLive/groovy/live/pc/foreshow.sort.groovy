import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 场景：回放列表排序
 访问地址：http://域名/live/foreshow.sort.groovy
 参数：
 {
 foreshowId: // 预告id
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
    Assert.isNotBlankParam(params, "foreshowId")     //预告id
    Assert.isNotBlankParam(params, "sortNum")   //排序值

    String appId=Strings.getAppId(params)
    LiveService liveService = getBean(LiveService)

    long foreshowId
    long sortNum
    try {
        sortNum = (params.sortNum ?:0)as long
        foreshowId = (params.foreshowId ?:0) as long
    } catch (NumberFormatException e) {
        throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "参数转换错误");
    }


    Map result = [:];
    int res = liveService.sortForeshow([foreshowId:foreshowId,sortNum:sortNum,appId:appId])
    result.status=res
    if (res ==1) {
        result.msg="成功"
    } else{
        result.msg="排序失败"
    }

    return result
}
