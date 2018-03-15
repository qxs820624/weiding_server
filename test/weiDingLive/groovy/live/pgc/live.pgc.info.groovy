import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.common.LiveCommon
import zs.live.entity.LiveForeshow
import zs.live.entity.LiveRecord
import zs.live.service.LiveForeshowService
import zs.live.service.LivePgcService
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 手机端查询会议直播推流地址,点击“开启直播”时调用
 * @author liyongguang
 * @date 2017/2/8.
 */
ApiUtils.process {
    Assert.isNotBlankParam(params, "foreshowId")
    Assert.isNotBlankParam(params, "userId")

    long foreshowId = (params.foreshowId ?:0) as long
    long userId = (params.userId ?:0) as long
    String vc = params.vc ?: "5.4"


    String appId=Strings.getAppId(params)

    LiveService liveService = bean(LiveService)

    LiveForeshowService liveForeshowService = bean(LiveForeshowService)
    LivePgcService livePgcService = bean(LivePgcService)

    Map result = [:]
    LiveForeshow liveForeshow = liveForeshowService.get(foreshowId);
    if(!liveForeshow){
        throw new ApiException(700,"预告不存在")
    }
    if (liveForeshow.userId != userId){
        throw new ApiException(700,"主播才能开始/结束直播")
    }
    LiveRecord liveRecord = liveService.getLiveByForeshow(foreshowId,appId) //正在直播中
    if(liveRecord){
        result.liveId = liveRecord.liveId
        result.roomId = liveRecord.roomId
        result.chatId = liveRecord.chatId
        result.rtmpUrl = liveRecord.rtmpUrl
    } else {
        throw new ApiException(700,"直播不存在");
    }
    return result
}
