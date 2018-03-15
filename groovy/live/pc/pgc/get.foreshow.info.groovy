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
 * 获取直播信息接口
 *
 * */
ApiUtils.processNoEncry {
    Assert.isNotBlankParam(params, "foreshowId")
    String vc = params.vc ?: "5.5"

    long foreshowId = (params.foreshowId ?:0) as long
    long userId = (params.userId ?:0) as long
    String appId=Strings.getAppId(params)
    Map map = [userId: userId,appId: appId,vc: vc]
    LiveService liveService = bean(LiveService)

    Map result = liveService.getForehsowInfoByForeshowId(foreshowId,map)
    return result
}
