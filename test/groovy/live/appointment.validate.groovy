import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 场景：验证用户是否添加直播预约
 访问地址：http://域名/live/appointment.validate.groovy
 参数：
 {
     userId: // 用户id
     foreshowId:// 预告id
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
    int result = liveService.validateIsOpenRemind(userId, foreshowId,appId)
    return [status:result]
}
