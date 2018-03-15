import zs.live.ApiUtils
import zs.live.service.QcloudLiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 删除腾讯云缓存的接口
 地址：
 http://域名/live/pc/qcloud.info.del.groovy?appId=
 参数：
 pfAppName:String app的唯一Id

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
    Assert.isNotBlankParam(params, "appId")
    String appId = Strings.getAppId(params)
    QcloudLiveService qcloudLiveService = bean(QcloudLiveService.class)
    qcloudLiveService.delQcloudInfo(appId)
    return [status:1,msg:"删除redis缓存成功"]
}
