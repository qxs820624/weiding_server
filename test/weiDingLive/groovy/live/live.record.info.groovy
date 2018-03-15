import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.ClientType
import zs.live.utils.Strings

/**
 * 直播历史信息接口（返回直播回看的播放地址和视频状态）,分享落地页也需要使用
 地址：
 http://localhost:8080/live/live.record.info.groovy?liveId=
 参数：
 token:String
 liveId:String
 vc:
 appName:
 返回：
 {

 "head": {
 "status": 200,
 "hasMore": false
 },
 "body": {
 "liveStatus":1,
 "url":[]
 }

 }
 */

ApiUtils.processNoEncry {
    Assert.eitherOrJudgeParam(params,"foreshowId","liveId") //当从回看列表跳转时需要foreshowId，当从我的直播列表跳转的时候需要liveId
    int clarity	= (params.clarity ?: 2) as int		//1: 普通，2：高清
    String fileType = params.fileType ?: ""   // "flv","mp4","m3u8"
    def isAppleOs =  ClientType.isAppledOs(request)
    LiveService liveService = bean(LiveService.class)
    long foreshowId = (params.foreshowId ?:0 ) as long
    long userId = (params.userId ?:0) as long
    long liveId = (params.liveId ?:0 ) as long
    String appId = Strings.getAppId(params)
    String vc = params.vc ?: "5.4"
    Map map = [
        foreshowId: foreshowId,
        liveId: liveId,
        fileType: fileType,
        clarity : clarity,
        isAppleOs :isAppleOs,
        appId: appId,
        userId:userId,
        dropIsPrivate:1,
        vc: vc
    ]
    def res = liveService.getLiveRecordInfo(map)
    return res
}
