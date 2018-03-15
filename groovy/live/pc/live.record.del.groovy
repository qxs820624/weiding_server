import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 管理员删除直播回放（删除回放表，删除腾讯云记录）
 地址：
 http://域名/live/pc/live.record.del.groovy?userId=&liveId=
 参数：
 userId:long
 liveId:String 直播id，多个用逗号隔开
 foreshowId:String 预告id，多个用逗号隔开
 delType:1 根据liveId删除，2 根据预告Id删除
 appId:
 op_role:100
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

ApiUtils.processNoEncry {
    Assert.eitherOrJudgeParam(params, "liveId","foreshowId")

    Long userId = (params.userId?:0) as long
    String liveId = params.liveId ?:""
    String foreshowId = params.foreshowId ?:""
    int delType = (params.delType?:0) as int
    int op_role = (params.op_role ?:0 ) as long
    String appId=Strings.getAppId(params)

    LiveService liveService = bean(LiveService.class)
    if(delType == 1){//根据liveId删除
        List<Long> liveIdList = Strings.splitToLongList(liveId)
        liveIdList?.each{
            res = liveService.delLiveRecord([userId:userId,liveId:it,appId:appId,op_role:op_role])
        }
    }else if(delType == 2){//根据预告Id删除
        List<Long> foreshowIdList = Strings.splitToLongList(foreshowId)
        foreshowIdList?.each{
            res = liveService.delLiveRecord([userId:userId,foreshowId:it,appId:appId,op_role:op_role])
        }
    }else{
        throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "操作类型错误");
    }

    return [status:1]
}
