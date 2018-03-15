import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 场景：添加或者取消直播预约
 访问地址：http://域名/live/appointment.edit.groovy
 参数：
 {
     userId: // 预约人id
    foreshowId:// 预告id
     operType: // “add” 或 “del”
     appId:
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
    Assert.isNotBlankParam(params, "userId")     //发起者的userId
    Assert.isNotBlankParam(params, "foreshowId")   //目标对象的userId
    Assert.isNotBlankParam(params, "operType")

    String appId=Strings.getAppId(params)
    LiveService liveService = getBean(LiveService)

    long userId
    long foreshowId
    try {
        userId = params.userId as long
        foreshowId = params.foreshowId as long
    } catch (NumberFormatException e) {
        throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "参数转换错误");
    }
    String operType = params.operType ?: ""

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
