import zs.live.ApiUtils
import zs.live.ApiException
import zs.live.service.PubService
import zs.live.service.QcloudLiveService
import zs.live.utils.Assert
import zs.live.utils.Strings
import zs.live.utils.ClientType


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
 },
 "isPrivate": 0,
 "srpInfo":[
 {
 "srpId": "",
 "keyword":""
 }
 ]
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
    //String json = """{%20"liveBg":"直播背景图",%20"liveThumb":"直播列表缩略图",%20"title":%20"我发起了一个直播，快来看看吧！",%20"host":%20{%20"avatar":%20"http://user-pimg.b0.upaiyun.com/selfcreate/default/default_77.jpg!sy",%20"userId":%20"3425445112",%20"userName":%20"qb0005"%20},%20"isPrivate":%200,%20"srpInfo":[%20{%20"srpId":%20"1",%20"keyword":"1"%20},%20{%20"srpId":"2"%20,"keyword":"2"%20}%20]%20} """
   // String json1 = """{ "liveBg":"直播背景图", "liveThumb":"直播列表缩略图", "title": "我发起了一个直播，快来看看吧！", "host": { "avatar": "http://user-pimg.b0.upaiyun.com/selfcreate/default/default_77.jpg!sy", "userId": "3476812", "userName": "qb0005" }, "isPrivate": 0, "srpInfo":[ { "srpId": "1", "keyword":"1" }, { "srpId":"2" ,"keyword":"2" } ] }"""
   // String a = URLDecoder.decode(json as String, "utf-8")
   // System.out.println("a:::" + a)
    //json
    //{ "liveBg":"直播背景图", "liveThumb":"直播列表缩略图", "title": "我发起了一个直播，快来看看吧！", "host": { "avatar": "http://user-pimg.b0.upaiyun.com/selfcreate/default/default_77.jpg!sy", "userId": "3476812", "userName": "qb0005" }, "isPrivate": 0, "srpInfo":[ { "srpId": "", "keyword":"" } ] }
//    def liveparams = Strings.parseJson(a);
//    System.out.println("liveBg"+liveparams.liveBg)
//    System.out.println("liveThumb"+liveparams.liveThumb)
//    System.out.println("title"+liveparams.title)
//    System.out.println("host"+liveparams.host)
//    System.out.println("avatar"+liveparams.host.avatar)
//    System.out.println("srpInfo:"+liveparams.srpInfo)
//    System.out.println("srpId:"+liveparams.srpInfo.srpId)
//    System.out.println("srpIds:"+Strings.toListString(liveparams.srpInfo.srpId))
//    System.out.println("keywords:"+Strings.toListString(liveparams.srpInfo.keyword))
//
//    liveparams.appId = "souyue"
//    liveparams.appModel = "xiaomi"



    //------------升级判断
    //PubService pubService=bean(PubService)
//    ApiUtils.log.info("params:"+params)
//    PubService pubService = getBean(PubService)
//    def upgradeMap=pubService.upgradeVc(params.appId,params.channel,params.vc,ClientType.get(headers.get('User-Agent')))
//    def upgradeStatus=upgradeMap.status as int
//    if(upgradeStatus==ApiException.STATUS_UPGRADE_REMIND||upgradeStatus==ApiException.STATUS_UPGRADE_SKIP_HTML5){
//        throw new ApiException(upgradeStatus, Strings.toJson(upgradeMap))
//    }

    def liveparams1 = [:]
    liveparams1 = params;
    def host = Strings.parseJson(params.host)
    liveparams1.host = host
    def srpInfo = Strings.parseJson(params.srpInfo)
    liveparams1.srpInfo = srpInfo
    ApiUtils.log.info("createParams:"+params)


    QcloudLiveService qcloudLiveService = getBean(QcloudLiveService);
    def backMap = qcloudLiveService.createLive(liveparams1);
    return backMap
})
