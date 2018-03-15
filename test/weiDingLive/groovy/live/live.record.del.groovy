import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 用户删除直播回放(客户端操作)
 地址：
 http://域名/live/live.record.del.groovy?userId=&liveId=
 参数：
 userId:long
 liveId:String
 appId:
 返回：
 {

 "head": {
 "status": 200,
 "hasMore": false
 },
 "body": {
 "status":1 成功
 }

 }
 */

ApiUtils.process {
    Assert.isNotBlankParam(params, "userId")
    Assert.eitherOrJudgeParam(params,"liveId","foreshowId")

    Long userId=params.userId? params.userId as long :0
    Long liveId = (params.liveId ?:0 ) as long
    Long foreshowId = (params.foreshowId ?:0 ) as long
    String appId=Strings.getAppId(params)

    LiveService liveService = bean(LiveService.class)
    def res = liveService.delLiveRecord([userId:userId,liveId:liveId,foreshowId:foreshowId,appId:appId])
    return [status:res]
}
