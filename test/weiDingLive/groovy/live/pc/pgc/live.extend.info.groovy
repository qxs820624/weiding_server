import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.common.LiveCommon
import zs.live.dao.mysql.QcloudLiveRes
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
    Assert.isNotBlankParam(params, "liveId")

    long liveId = (params.liveId ?:0) as long
    int type = (params.type ?: 0) as int

    String appId=Strings.getAppId(params)

    QcloudLiveRes qcloudLiveRes = bean(QcloudLiveRes)
    return qcloudLiveRes.findLiveRecordPayExtendByLiveId(liveId,type)
}
