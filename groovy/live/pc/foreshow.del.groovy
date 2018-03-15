import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 删除直播预告
 地址：
 http://域名/live/pc/foreshow.del.groovy?foreshowId=
 参数：
 foreshowId:String 预告Id，多个用逗号隔开
 appId：
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
    Assert.isNotBlankParam(params, "foreshowId")
    String foreshowId = params.foreshowId ?: ""
    String appId=Strings.getAppId(params)

    LiveService liveService = bean(LiveService.class)

    List<Long> foreshowIdList = Strings.splitToLongList(foreshowId)
    foreshowIdList?.each{
        liveService.delForeshow([foreshowId:it,appId:appId])
    }

    return [status:1]
}
