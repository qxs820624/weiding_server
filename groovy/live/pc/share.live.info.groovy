import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.entity.LiveRecord
import zs.live.service.GiftService
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.DateUtil
import zs.live.utils.Strings

/**
 * 直播信息_主播及直播相关信息（分享落地页观看直播时调用）
 *
 * */
ApiUtils.processNoEncry {
    Assert.isNotBlankParam(params, "liveId")

    long liveId = (params.liveId ?:0) as long
    long hostUserId = (params.hostUserId ?:0) as long
    String appId=Strings.getAppId(params)

    LiveService liveService = bean(LiveService)
    GiftService giftService = bean(GiftService)

    LiveRecord liveRecord = liveService.findLiveByLiveId(liveId)
    Map result = [:]
    int watchCount  = 0
    int liveStatus = -1
    if(!liveRecord){
        liveRecord = liveService.findLiveRecordByLiveId(liveId)
        if(liveRecord){
            liveStatus = 0
            watchCount = (liveRecord.totalWatchCount ?: 0) as int
        }
    }else{
        liveStatus = 1
        watchCount = liveService.getLiveWatherCount(liveId)
        long pushTime = DateUtil.getTimestamp(liveRecord.createTime)
        liveRecord.timeSpan = new BigDecimal((System.currentTimeMillis()-pushTime)/1000).toLong()
    }
    hostUserId = liveRecord ? liveRecord.userId : hostUserId
    String msg = ""
    Map userInfo = liveService.getUserInfo(hostUserId)
    def charmMapHost = giftService.getUserCharmCount(userInfo,appId)
    result.host=[
        "userId": userInfo?.userId?: "",
        "userImage": userInfo ?.userImage ?: "",
        "nickname": userInfo ?.nickname ?: "",
        "charmCount": (charmMapHost?.charmCount ?:0) as int, //魅力值
    ]
    if(!liveRecord){
        msg = "直播不存在"
        result.record=[
            "liveStatus": -1,
            "msg":msg
        ]
        return result
    }
    if(liveRecord.liveStatus ==1){
        msg = "符合回看规则"
    }else if(liveRecord.liveStatus ==2){
        msg = "不符合回看规则"
    }else if(liveRecord.liveStatus == 4){
        msg = "该直播已删除"
    }
    result.record=[
        "liveId":liveRecord.liveId,
        "title": liveRecord.title,
        "liveBg": liveRecord.liveBg,
        "roomId": liveRecord.roomId,
        "chatId": liveRecord.chatId,
        "admireCount": liveRecord.admireCount, //热度
        "timeSpan": liveRecord.timeSpan,//单位：秒
        "watchCount": watchCount, //累积观看人数
        "channelId":liveRecord.channelId?:"",
        "m3u8Url":liveRecord.m3u8Url?:"",
        "createTime":DateUtil.getTimestamp(liveRecord.createTime ?: ""),
        "liveStatus": liveStatus,
        "msg":msg
    ]
    return result
}
