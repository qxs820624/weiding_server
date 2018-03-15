import zs.live.ApiUtils
import zs.live.service.LiveSdkSevice
import zs.live.service.QcloudLiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 请求参数：
 {
 "liveBg":"直播背景图",
 "liveThumb":"直播列表缩略图",
 "title": "我发起了一个直播，快来看看吧！",
 "host": {
 "avatar": "http://user-pimg.b0.upaiyun.com/selfcreate/default/default_77.jpg!sy",
 "userId": "3476812",
 "userName": "qb0005"
 }

 返回参数
 {
 "head": {
 "status": 200,
 "hasMore": false
 },
 "body": {
 "roomId":""
 "chatId":""
 "liveId": "7099",
 "shortUrl": "http://tt.zhongsou.com/u/wlB."
 }
 }
 *
 *
 *
 *
 */

ApiUtils.process({
    Assert.isNotBlankParam(params,"userId")
    Assert.isNotBlankParam(params,"appId")


    def liveparams = [:]
    liveparams = params;
    def host = Strings.parseJson(params.host)
    liveparams.host = host
    ApiUtils.log.info("createParams:"+liveparams)


    LiveSdkSevice liveSdkSevice = getBean(LiveSdkSevice);
    def backMap = liveSdkSevice.create(liveparams);
    return backMap
})
