import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 场景：取的用户关注的列表
 http://域名/live/pgc/live.pgc.list.user.foreshow.groovy
 参数：
 {
 userId: // 用户id

 }
 返回值：
 {
 "head": {
 "status": 200,
 "hasMore": false
 },
 "body": {
 "foreshowList": [
 {
 "foreshowId": "21212321321aaddd",
 "title": "新的预告",
 "imgUrl": "20161028174446_750_300.png"
 }
 ]
 }
 }
 * */
ApiUtils.processNoEncry {
    Assert.isNotBlankParam(params, "userId")     //发起者的userId

    LiveService liveService = getBean(LiveService)
    String appId=Strings.getAppId(params)

    long userId
    long foreshowId
    try {
        userId = params.userId as long
    } catch (NumberFormatException e) {
        throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "参数转换错误");
    }
    String operType = params.operType ?: ""
    String appId = Strings.getAppId(params)

    int result;
    if (operType == "add") {
        result = liveService.addAppointment([userId:userId, foreshowId:foreshowId,appId:appId]);
    } else if (operType == "del") {
        result = liveService.cancelAppointment([userId:userId, foreshowId:foreshowId,appId:appId]);
    }else{
        throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "operateType参数错误");
    }

    return [status:result]
}
