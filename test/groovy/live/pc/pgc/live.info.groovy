import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.common.LiveCommon
import zs.live.entity.LiveForeshow
import zs.live.entity.LiveRecord
import zs.live.service.GiftService
import zs.live.service.LiveForeshowService
import zs.live.service.LivePgcService
import zs.live.service.LiveService
import zs.live.service.ShortURLService
import zs.live.utils.Assert
import zs.live.utils.DateUtil
import zs.live.utils.Strings

/**
 * 直播信息_主播及直播相关信息（观看直播时调用）
 *
 * */
ApiUtils.processNoEncry {
    Assert.isNotBlankParam(params, "foreshowId")
    Assert.isNotBlankParam(params, "userId")
    String vc = params.vc ?: "5.4"


    long foreshowId = (params.foreshowId ?:0) as long
    long userId = (params.userId ?:0) as long



    String appId=Strings.getAppId(params)

    LiveService liveService = bean(LiveService)
    GiftService giftService = bean(GiftService)
    ShortURLService shortURLService = bean(ShortURLService)
    LiveForeshowService liveForeshowService = bean(LiveForeshowService)
    LivePgcService livePgcService = bean(LivePgcService)


    Map result = [:]
    LiveForeshow liveForeshow = liveForeshowService.get(foreshowId);
    if(!liveForeshow){
        result.record = [pgcStatus: LiveCommon.FORESHOW_STATUS_3]
        return result;
    }
    LiveRecord liveRecord = liveService.getLiveByForeshow(foreshowId,appId) //正在直播中
    LiveRecord liveRecordLog =liveService.findLiveRecordByForeshowId(foreshowId) //有回放的

    boolean  isOpenRemind = liveService.isOpenRemind(userId,foreshowId,appId)
    def isOpenRemindStr="0";
    if(isOpenRemind) isOpenRemindStr=1;
    int isMerge = 0
    int isHost = 0
    if(userId == liveForeshow.userId){
        isHost=1;
    }
    if(liveRecord){
        def userInfo = liveService.getUserInfo(liveRecord.userId)
        def charmMapHost = giftService.getUserCharmCount(userInfo,appId)
        def shortUrl = ""
        if(liveRecord.liveMode == LiveCommon.LIVE_MODE_3){
            shortUrl = shortURLService.getShortUrlLivePay([foreshowId:foreshowId, liveId: liveRecord.liveId,userId: userId, vc: vc,appId: appId])
        }else if(liveRecord.liveMode == LiveCommon.getLIVE_MODE_2()){
            shortUrl = shortURLService.getShortUrlPgcForeshow([foreshowId:foreshowId, liveId: liveRecord.liveId,userId: userId, vc: vc,appId: appId])
        }else{
            shortUrl = shortURLService.getShortUrlForeshow([foreshowId:foreshowId,liveMode: liveRecord.liveMode,userId: liveRecord.userId,roomId: liveRecord.roomId, vc: vc,appId: appId])
        }
        //def admireCount = liveService.getLivePraise(liveId) ?: 0
        def watchCount= liveService.getLiveWatherCount(liveRecord.liveId) ?: 0
        if(isHost == 1 && liveRecord.pgcType == 1){
            isHost = 1
        }else {
            isHost = 0
        }

        result.host=[
            "userId": liveRecord.userId,
            "userImage": liveRecord.userImage,
            "nickname": liveRecord.nickname,
            "charmCount": (charmMapHost?.charmCount ?:0) as int, //魅力值
        ]
        long time = System.currentTimeMillis()
        long beginTimeLong =DateUtil.getTimestamp(liveRecord.beginTime)
        int  pgcStatus = liveForeshow.foreshowStatus;
        if(beginTimeLong<=System.currentTimeMillis()&&pgcStatus==0){
            pgcStatus=1
        }

        def pauseLog = liveForeshowService.findPauseLog(foreshowId,liveRecord.liveId)
        long timeSpan=new BigDecimal((time-beginTimeLong)/1000).toLong()
        if(timeSpan<=0){
            timeSpan=0
        }
        result.record=[
            "liveId":liveRecord.liveId,
            "title": liveRecord.title,
            "liveBg": liveRecord.liveBg,
            "roomId": liveRecord.roomId,
            "chatId": liveRecord.chatId,
            "admireCount": -1, //热度
            "timeSpan": timeSpan,//单位：秒
            "watchCount": watchCount, //观看人数
            "shortUrl":shortUrl,//直播分享短链
            "beginTime":DateUtil.getTimestamp(liveRecord.beginTime ?: String.valueOf(liveForeshow.beginTime)),
            "msg" : pauseLog?.pauseMessage,//相关状态的提示信息"playUrl":  //flv 或者mp4, 直播流或者回放录像
            "playUrl":liveService.formatPlayUrl(liveRecord,vc,params),
            "role":livePgcService.getUserRole(liveRecord.liveId,userId)?:3,
            "pgcStatus":pgcStatus,//新接口使用
            "isOpenRemind":isOpenRemindStr,
            "brief":liveRecord.brief,
            "briefHtml":liveRecord.briefHtml,
            "m3u8Url":liveService.formatM3u8Url(liveRecord,vc),
            "liveMode": liveRecord.liveMode,
            "url": liveForeshow.url,
            "urlTag": liveForeshow.urlTag,
            "newsId": liveForeshow.newsId,
            "isHost": isHost,
            "isMerge": isMerge
        ]
    }else if(liveRecordLog){
        if(isHost == 1 && liveRecordLog.pgcType == 1){
            isHost = 1
        }else {
            isHost = 0
        }
        Map dataMap = [
            liveId: liveRecordLog.liveId,
            roomId: liveRecordLog.roomId,
            foreshowId: liveRecordLog.foreshowId,
            appId: liveRecordLog.appId,
            fileType: params.fileType ?:"",
            vc:vc
        ]
        def urlList = []
        def liveRecordInfo =  Strings.parseJson(liveForeshow?.liveRecordInfo)
        def videoAddress = liveRecordInfo ? liveRecordInfo.video_address : ""
        def videoAddressMap =[:];
        def timeSpan = liveRecordInfo?.time_span
        if(videoAddress){
            isMerge = 1
            videoAddressMap = liveService.getVideoAddressUrlList(videoAddress, dataMap)
            urlList.addAll(videoAddressMap.url)
        }else{
            videoAddressMap = liveService.getVideoAddressUrlList(liveRecordLog.videoAddress, dataMap)
            timeSpan=videoAddressMap.timeSpan as int
            urlList.addAll(videoAddressMap.url)
        }

        def userInfo = liveService.getUserInfo(liveRecordLog.userId)
        def charmMapHost = giftService.getUserCharmCount(userInfo,appId)
        def shortUrl = ""
        if(liveRecordLog.liveMode == LiveCommon.LIVE_MODE_3){
            shortUrl = shortURLService.getShortUrlLivePay([foreshowId:foreshowId, liveId: liveRecordLog.liveId,userId: userId, vc: vc,appId: appId])
        }else if(liveRecordLog.liveMode == LiveCommon.getLIVE_MODE_2()){
            shortUrl = shortURLService.getShortUrlPgcForeshow([foreshowId:foreshowId, liveId: liveRecordLog.liveId,userId: userId, vc: vc,appId: appId])
        }else {
            shortUrl = shortURLService.getShortUrlForeshow([foreshowId: foreshowId,liveMode:liveRecordLog.liveMode, userId: liveRecordLog.userId, vc: vc,appId: appId])
        }
        def watchCount= liveService.getLiveWatherTotalCount(liveRecordLog.liveId) ?: 0
      //  def pauseLog = liveForeshowService.findPauseLog(foreshowId,liveRecordLog.liveId)

        result.host=[
            "userId": liveRecordLog.userId,
            "userImage": liveRecordLog.userImage,
            "nickname": liveRecordLog.nickname,
            "charmCount": (charmMapHost?.charmCount ?:0) as int, //魅力值
        ]
        result.record=[
            "liveId":liveRecordLog.liveId,
            "title": liveRecordLog.title,
            "liveBg": liveRecordLog.liveBg,
            "roomId": liveRecordLog.roomId,
            "chatId": liveRecordLog.chatId,
            "admireCount": -1, //热度
            "timeSpan": timeSpan,
            "watchCount": watchCount, //观看人数
            "shortUrl":shortUrl,//直播分享短链
            "beginTime":DateUtil.getTimestamp(String.valueOf(liveForeshow.beginTime ?: "")),
            "msg" : "",//相关状态的提示信息"playUrl":  //flv 或者mp4, 直播流或者回放录像
            "playUrl":liveService.formatPlayUrl(liveRecordLog,vc,params)?:"",
            "role":livePgcService.getUserRole(liveRecordLog.liveId,userId)?:3,
            "pgcStatus":liveForeshow.foreshowStatus,//新接口使用
            "isOpenRemind":isOpenRemindStr,
            "brief":liveRecordLog.brief,
            "briefHtml":liveRecordLog.briefHtml,
            "liveRecordUrl":urlList,
            "m3u8Url":liveService.formatM3u8Url(liveRecordLog,vc)?:"",
            "liveMode": liveRecordLog.liveMode,
            "url": liveForeshow.url,
            "urlTag": liveForeshow.urlTag,
            "newsId": liveForeshow.newsId,
            "isHost": isHost,
            "isMerge": isMerge
        ]
    }else if(liveForeshow.foreshowType == LiveCommon.FORESHOW_TYPE_1){  //互动直播的预告
        //直播未开始时，返回预告的跳转信息
        def userInfo = liveService.getUserInfo(liveForeshow.userId)
        def charmMapHost = giftService.getUserCharmCount(userInfo,appId)
        result.host=[
            "userId": liveForeshow.userId,
            "userImage": liveForeshow.userImage,
            "nickname": liveForeshow.nickname,
            "charmCount": (charmMapHost?.charmCount ?:0) as int, //魅力值
        ]
        result.record=[
            "title": liveForeshow.title,
            "liveBg": liveForeshow.imgUrl,
            "beginTime":DateUtil.getTimestamp(String.valueOf(liveForeshow.beginTime ?: "")),
            "pgcStatus":liveForeshow.foreshowStatus,//新接口使用
            "isOpenRemind":isOpenRemindStr,
            "liveMode": LiveCommon.FORESHOW_TYPE_1,
            "url": liveForeshow.url,
            "urlTag": liveForeshow.urlTag,
            "newsId": liveForeshow.newsId,
            "isHost": isHost,
            "isMerge": isMerge
        ]
    }else {
        result.record = [pgcStatus: LiveCommon.FORESHOW_STATUS_3]
    }
    return result
}
