import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.entity.LiveRecord
import zs.live.service.GiftService
import zs.live.service.LiveService
import zs.live.service.ShortURLService
import zs.live.utils.Assert
import zs.live.utils.DateUtil
import zs.live.utils.Strings

/**
 * 直播信息_主播及直播相关信息（观看直播时调用）
 *
 * */
ApiUtils.process {
    Assert.isNotBlankParam(params, "liveId")
    Assert.isNotBlankParam(params, "userId")

    long liveId = (params.liveId ?:0) as long
    long userId = (params.userId ?:0) as long
    String appId=Strings.getAppId(params)
    String vc = params.vc ?: "5.4"

    LiveService liveService = bean(LiveService)
    GiftService giftService = bean(GiftService)
    ShortURLService shortURLService = bean(ShortURLService)

    LiveRecord liveRecord = liveService.findLiveByLiveId(liveId)

    Map result = [:]
    if(liveRecord){
        long start = System.currentTimeMillis()
        def userInfo = liveService.getUserInfo(liveRecord.userId)
        def charmMapHost = giftService.getUserCharmCount(userInfo,appId)
        long end1 = System.currentTimeMillis()
        def shortUrl = shortURLService.getShortUrlLive([liveId:liveRecord.liveId,liveMode: liveRecord.liveMode,userId: liveRecord.userId,roomId: liveRecord.roomId,vc: vc,appId: appId])
        //def admireCount = liveService.getLivePraise(liveId) ?: 0
        def watchCount= liveService.getLiveWatherCount(liveId) ?: 0
        long end2 = System.currentTimeMillis()
        def liveLimit = liveService.valateLiveLimit(appId)
        long end3 = System.currentTimeMillis()
        ApiUtils.log.info("/live/live.info.groovy charmMapHost_time:{},watchCount_time:{},liveLimit_time:{}",(end1-start),(end2-end1),(end3-end2))
        result.host=[
            "userId": liveRecord.userId,
            "userImage": liveRecord.userImage,
            "nickname": liveRecord.nickname,
            "charmCount": (charmMapHost?.charmCount ?:0) as int, //魅力值
        ]
        long time = System.currentTimeMillis()
        long pushTime = DateUtil.getTimestamp(liveRecord.createTime)
        result.record=[
            "liveId":liveRecord.liveId,
            "title": liveRecord.title,
            "liveBg": liveRecord.liveBg,
            "roomId": liveRecord.roomId,
            "chatId": liveRecord.chatId,
            "admireCount": -1, //热度
            "timeSpan": new BigDecimal((time-pushTime)/1000).toLong(),//单位：秒
            "watchCount": watchCount, //观看人数
            "shortUrl":shortUrl,//直播分享短链
            "liveStatus": 1,
            "chargeUrl":"www.baidu.com",
            "liveLimit":liveLimit,//直播观看限制
            "playUrl":liveRecord.playUrl  //直播观看url

        ]
    }else{
        long start = System.currentTimeMillis()
        liveRecord = liveService.findLiveRecordByLiveId(liveId)
        long end = System.currentTimeMillis()
        ApiUtils.log.info("/live/live.info.groovy liveRecord_time:{}",(end-start))
        if(liveRecord && liveRecord.liveStatus != 4){   //直播结束
            result.record = [liveStatus:0]
            result.host=[
                "userId": liveRecord.userId,
                "userImage": liveRecord.userImage,
                "nickname": liveRecord.nickname,
            ]
        }else{  //该直播已删除
            result.record = [liveStatus: 2]
        }

    }
    return result
}
