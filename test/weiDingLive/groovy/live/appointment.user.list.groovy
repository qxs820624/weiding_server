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
    Assert.isNotBlankParam(params, "foreshowId")   //预告id

    String appId=Strings.getAppId(params)
    int lastId = (params.lastId ?: 0) as int
    int psize = (params.psize ?: 0) as int
    LiveService liveService = getBean(LiveService)
    long foreshowId
    try {
        foreshowId = params.foreshowId as long
    } catch (NumberFormatException e) {
        throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "参数转换错误");
    }

    List result = liveService.findaAppointmentUserList(foreshowId,lastId,appId, psize)

    return [userList:result]
}
