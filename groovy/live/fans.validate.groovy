import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 场景：添加或者取消直播关注
 访问地址：http://域名/live/fans.validate.groovy
 参数：
 {
 userId: // 代表操作发起者（当前登录用户）
 toUserId:// 代表操作目标
 }
 返回值：
 {
 "head": {
 "status": 200,
 "hasMore": false
 },
 "body": {
 "status": $int 1成功
 "msg": "校验失败原因"
 }
 }
 * */
ApiUtils.process {
    Assert.isNotBlankParam(params, "userId")     //发起者的userId
    Assert.isNotBlankParam(params, "toUserId")   //目标对象的userId
    Assert.isNotBlankParam(params, "liveId")   //直播id

    String appId=Strings.getAppId(params)
    LiveService liveService = getBean(LiveService)

    long userId
    long toUserId
    long liveId
    try {
        userId = params.userId as long
        toUserId = params.toUserId as long
        liveId = params.liveId as long
    } catch (NumberFormatException e) {
        throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "参数转换错误");
    }

    Map result = [:];
    int res = liveService.validateFans([userId:userId,toUserId:toUserId,liveId:liveId,appId:appId])
    result.status=res
    if (res ==2) {
        result.msg="打赏用户不存在"
    } else if (res ==3) {
        result.msg="被打赏用户不存在"
    } else if (res ==4) {
        result.msg="直播不存在"
    } else if (res ==5) {
        result.msg="主播不在该直播间"
    } else if (res ==6) {
        result.msg="打赏人不在该直播间"
    }

    return result
}
