import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.common.LiveCommon
import zs.live.entity.LiveForeshow
import zs.live.entity.LiveRecord
import zs.live.service.GiftService
import zs.live.service.LiveForeshowService
import zs.live.service.LivePgcService
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.service.ShortURLService
import zs.live.utils.Assert
import zs.live.utils.DateUtil
import zs.live.utils.Strings
import zs.live.service.PubService
import zs.live.utils.ClientType
import zs.live.utils.VerUtils

/**
 * 直播信息_主播及直播相关信息（观看直播时调用）
 *
 * */
ApiUtils.process {
    Assert.isNotBlankParam(params, "foreshowId")
    Assert.isNotBlankParam(params, "userId")


    long foreshowId = (params.foreshowId ?:0) as long
    long userId = (params.userId ?:0) as long
    String vc = params.vc ?: "5.4"
    long inviter = (params.inviter ?:0) as long


    String appId=Strings.getAppId(params)



//    //------------升级判断
//    if(VerUtils.toIntVer(vc)<VerUtils.toIntVer("5.6")){
//        throw new ApiException(700,"请升级到5.6以上版本观看")
//    }
    PubService pubService=bean(PubService)
    ApiUtils.log.info("params:"+params)
//    def upgradeMap=pubService.upgradeVc(appId,params.channel,params.vc,Strings.isAndroidOs(request))
//   // def upgradeMap=pubService.upgradeVc(appId,params.channel,params.vc,"android")
//    def upgradeStatus=upgradeMap.status as int
//    if(upgradeStatus==ApiException.STATUS_UPGRADE_REMIND||upgradeStatus==ApiException.STATUS_UPGRADE_SKIP_HTML5){
//        throw new ApiException(upgradeStatus, Strings.toJson(upgradeMap))
//    }


    LiveService liveService = bean(LiveService)
    GiftService giftService = bean(GiftService)
    ShortURLService shortURLService = bean(ShortURLService)
    LiveForeshowService liveForeshowService = bean(LiveForeshowService)
    LivePgcService livePgcService = bean(LivePgcService)
    QcloudLiveService qcloudLiveService = bean(QcloudLiveService)

    Map result = [:]
    long start = System.currentTimeMillis()
    LiveForeshow liveForeshow = liveForeshowService.get(foreshowId);
    if(!liveForeshow){
        result.record = [pgcStatus: LiveCommon.FORESHOW_STATUS_3]
        return result;
    }
    long end1 = System.currentTimeMillis()
    LiveRecord liveRecord = liveService.getLiveByForeshow(foreshowId,appId) //正在直播中
    LiveRecord liveRecordLog = null
    if(!liveRecord){
        liveRecordLog =liveService.findLiveRecordByForeshowId(foreshowId) //有回放的
    }
    long end2 = System.currentTimeMillis()
    boolean  isOpenRemind = liveService.isOpenRemind(userId,foreshowId,appId)
    boolean isCollection = liveService.isCollectLive(userId,foreshowId,appId)   //直播是否被收藏
//    boolean isCollection = false //由于要先上线搜索功能，固暂时屏蔽收藏功能
    long end3 = System.currentTimeMillis()
    def isOpenRemindStr="0";
    if(isOpenRemind) isOpenRemindStr=1;
    int isMerge = 0
    int hasGoods = 0    //是否有商品
    ApiUtils.log.info("/pgc/live.info.groovy isOpenRemind_time:${end3-end2},liveRecord_time:${end2-end1},liveForeshow_time:${end1-start}")
    if(liveRecord){
        def hostUserInfo = liveService.getUserInfo(liveRecord.userId)
        def charmMapHost = giftService.getUserCharmCount(hostUserInfo,appId)
        long end4 = System.currentTimeMillis()
        def shortUrl = ""
        if(liveRecord.liveMode == LiveCommon.LIVE_MODE_3){
//            shortUrl = shortURLService.getShortUrlLivePay([foreshowId:foreshowId, liveId: liveRecord.liveId,userId: userId, vc: vc,appId: appId])
        }else if(liveRecord.liveMode == LiveCommon.getLIVE_MODE_2()){
//            shortUrl = shortURLService.getShortUrlPgcForeshow([foreshowId:foreshowId, liveId: liveRecord.liveId,userId: userId, vc: vc,appId: appId])
        }else {
//            shortUrl = shortURLService.getShortUrlForeshow([foreshowId:foreshowId,liveMode: liveRecord.liveMode,userId: liveRecord.userId,roomId: liveRecord.roomId, vc: vc,appId: appId])
        }
        //def admireCount = liveService.getLivePraise(liveId) ?: 0
        def watchCount= liveService.getLiveWatherCount(liveRecord.liveId) ?: 0
        long end5 = System.currentTimeMillis()
        def liveLimit = liveService.valateLiveLimit(appId)
        long end6 = System.currentTimeMillis()
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
        long end7 = System.currentTimeMillis()
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
            "beginTime":DateUtil.getTimestamp(liveRecord.beginTime),
            "msg" : pauseLog?.pauseMessage,//相关状态的提示信息"playUrl":  //flv 或者mp4, 直播流或者回放录像
            "playUrl":liveService.formatPlayUrl(liveRecord,vc,params),
            "role":livePgcService.getUserRole(liveRecord.liveId,userId)?:3,
            "pgcStatus":pgcStatus,//新接口使用
            "isOpenRemind":isOpenRemindStr,
            "brief": liveRecord.brief ,    //主播简介
            "briefHtml":liveRecord.briefHtml,
            "m3u8Url":liveService.formatM3u8Url(liveRecord,vc),
            "liveMode": liveRecord.liveMode,
            "introductionUrl": qcloudLiveService.abstractUrl+"?userid="+userId+"&foreshowId="+foreshowId+"&liveId="+liveRecord.liveId+"&time="+System.currentTimeMillis()+"&appId="+appId,    //直播简介
            "goodsUrl": "http://www.zhongsou.com",
            "liveLimit":liveLimit,//直播观看限制,
            "isMerge": isMerge,
            "serverCurrentTime":System.currentTimeMillis(),
            "isCollection": isCollection
        ]
        if(liveRecord.liveMode == LiveCommon.LIVE_MODE_3){
            //如果是付费直播，则要返回观看权限，商品url，邀请卡url，票价
            def userInfo = liveService.getUserInfo(userId)
            def liveRecordPay = [:]//qcloudLiveService.findLiveRecordPayInfo(liveRecord.liveId,userInfo,liveRecord,inviter)
            result.record << [
                price: liveRecordPay?.ticketPrice?Strings.getDivideNum(liveRecordPay.ticketPrice,"100"):0,
                sybPrice:liveRecordPay?.ticketPrice?:0,
                invitCardUrl:qcloudLiveService.inviteUrl+"?userid="+userId+"&foreshowId=+"+foreshowId+"&liveId="+liveRecord.liveId+"&appId="+appId,//生成当前邀请卡的appId，即请求当前接口的appId
//                viewAuthority: liveRecordPay?.viewAuthority ?: 0,
                viewAuthority: 1,
                appAccount: liveRecordPay?.mpAccount,
                tryTime:liveRecordPay?.tryTime?:0,
            ]
        }
        long end8 = System.currentTimeMillis()
        ApiUtils.log.info("/pgc/live.info.groovy findLiveRecordPayInfo_time:${end8-end7},findPauseLog_time:${end7-end6},liveLimit_time:${end6-end5},watchCount_time:${end5-end4},charmMapHost_time:${end4-end3}")
    }else if(liveRecordLog){
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

        def hostUserInfo = liveService.getUserInfo(liveRecordLog.userId)
        def charmMapHost = giftService.getUserCharmCount(hostUserInfo,appId)
        long end4 = System.currentTimeMillis()
        def shortUrl = ""
        if(liveRecordLog.liveMode == LiveCommon.LIVE_MODE_3){
//            shortUrl = shortURLService.getShortUrlLivePay([foreshowId:foreshowId, liveId: liveRecordLog.liveId,userId: userId, vc: vc,appId: appId])
        }else if(liveRecordLog.liveMode == LiveCommon.getLIVE_MODE_2()){
//            shortUrl = shortURLService.getShortUrlPgcForeshow([foreshowId:foreshowId, liveId: liveRecordLog.liveId,userId: userId, vc: vc,appId: appId])
        }else {
//            shortUrl = shortURLService.getShortUrlForeshow([foreshowId: foreshowId,liveMode:liveRecordLog.liveMode, userId: liveRecordLog.userId, vc: vc,appId: appId])
        }
        def watchCount= liveService.getLiveWatherTotalCount(liveRecordLog.liveId) ?: 0

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
            "beginTime":DateUtil.getTimestamp(liveRecordLog.beginTime),
            "msg" : "",//相关状态的提示信息"playUrl":  //flv 或者mp4, 直播流或者回放录像
            "playUrl":liveService.formatPlayUrl(liveRecordLog,vc,params)?:"",
            "role":livePgcService.getUserRole(liveRecordLog.liveId,userId)?:3,
            "pgcStatus":liveForeshow.foreshowStatus,//新接口使用
            "isOpenRemind":isOpenRemindStr,
            "brief": liveRecordLog.brief ,    //主播简介
            "briefHtml":liveRecordLog.briefHtml,
            "liveRecordUrl":urlList,
            "m3u8Url":liveService.formatM3u8Url(liveRecordLog,vc)?:"",
            "liveMode": liveRecordLog.liveMode,
            "introductionUrl": qcloudLiveService.abstractUrl+"?userid="+userId+"&foreshowId="+foreshowId+"&liveId="+liveRecordLog.liveId+"&time="+System.currentTimeMillis()+"&appId="+appId,    //直播简介
            "goodsUrl": "http://www.zhongsou.com",
            "isMerge": isMerge,
            "serverCurrentTime":System.currentTimeMillis(),
            "isCollection": isCollection
        ]
        if(liveRecordLog.liveMode == LiveCommon.LIVE_MODE_3){
            //如果是付费直播，则要返回观看权限，商品url，邀请卡url，票价
            def userInfo = liveService.getUserInfo(userId)
            def liveRecordPay = [:]//qcloudLiveService.findLiveRecordPayInfo(liveRecordLog.liveId,userInfo,liveRecordLog,inviter)
            result.record << [
                price: liveRecordPay?.ticketPrice?Strings.getDivideNum(liveRecordPay.ticketPrice,"100"):0,
                sybPrice:liveRecordPay?.ticketPrice?:0,
                invitCardUrl:qcloudLiveService.inviteUrl+"?userid="+userId+"&foreshowId=+"+foreshowId+"&liveId="+liveRecordLog.liveId+"&appId="+appId,//生成当前邀请卡的appId，即请求当前接口的appId
                viewAuthority: 1,//liveRecordPay?.viewAuthority ?: 0,
                appAccount: liveRecordPay?.mpAccount,
                tryTime:liveRecordPay?.tryTime?:0,
            ]
        }
        long end5 = System.currentTimeMillis()
        ApiUtils.log.info("/pgc/live.info.groovy findLiveRecordPayInfo_time:${end5-end4},charmMapHost_time:${end4-end3}")
    }else{
        result.record = [pgcStatus: LiveCommon.FORESHOW_STATUS_3]
    }
    return result
}
