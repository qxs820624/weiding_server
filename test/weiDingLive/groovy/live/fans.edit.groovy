import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 场景：添加或者取消直播关注
 访问地址：http://域名/live/fans.edit.groovy
 参数：
 {
     userId: // 代表操作发起者
     toUserId:// 代表操作目标
     operType: // “add” 或 “del”
 }
 返回值：
 说明：
 msg 的值的含义
 0 : 已经添加或已经取消，本次调用属于冗余重复调用，即调用未产生实际影响
 1 : 添加关注或取消关注成功
 2 : 添加后是互相关注的关系或取消前是互相关注的关系
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
ApiUtils.process {
    Assert.isNotBlankParam(params, "userId")     //发起者的userId
    Assert.isNotBlankParam(params, "toUserId")   //目标对象的userId
    Assert.isNotBlankParam(params, "operType")

    String appId=Strings.getAppId(params)
    LiveService liveService = getBean(LiveService)

    long userId
    long toUserId
    try {
        userId = params.userId as long
        toUserId = params.toUserId as long
    } catch (NumberFormatException e) {
        throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "参数转换错误");
    }
    String operType = params.operType ?: ""

    int result;
    if (operType == "add") {
        result = liveService.addFollow(userId, toUserId, 1,appId); // 1代表直播的关注关系
    } else if (operType == "del") {
        result = liveService.cancelFollow(userId, toUserId, 1,appId); // 1代表直播的关注关系
    }else{
        throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "operType参数错误");
    }
    //给第三方同步数据
    ApiUtils.log.info("fans.edit params=>{},result=>{}",params,result)
    if(result != 0){
        liveService.fansSyncData(params)
    }
    String msg = "成功"
    if(result == 0){
        msg = "已经添加或已经取消"
    }
    return [status:1,msg: msg]
}
