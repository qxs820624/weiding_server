import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.service.LivePgcService
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings


/*设置场控、主持人
  访问地址：http:/*//***//*live/pgc/live.pgc.role.set.groovy
  传参：params{
    userId: // 代表操作发起者（当前登录用户）
    toUserId:// 代表操作目标（String类型，多个以逗号隔开）
    liveId：//直播id
    operType: // “add” 或 “del”
    op_role：$int 100：超级管理员（不需要权限验证）
}
返回值：result:
{
    "head": {
    "status": 200(成功),
    "hasMore": false
},
    "body":{
    "status": $int 1成功
    "msg": "失败原因"
}
}*/


ApiUtils.processNoEncry {
    Assert.isNotBlankParam(params,"liveId")
    Assert.isNotBlankParam(params,"userId")
    Assert.isNotBlankParam(params,"toUserId")
    Assert.isNotBlankParam(params,"operType")

    Long liveId = (params.liveId?:0) as long
    String userId = params.userId
    String toUserId = params.toUserId ? params.toUserId.replace("，",",") : ""
    String operType = params.operType ?: ""
    Long op_role = (params.op_role?:0) as long
    String appId=Strings.getAppId(params)

    LivePgcService livePgcService = getBean(LivePgcService)
    def resMap = livePgcService.updateFieldControl([liveId:liveId,userId:userId,toUserId:toUserId,operType:operType,op_role:op_role,appId:appId])
    return [status:(resMap?.status?:0) as int,msg:resMap?.msg?:""]
}
