import zs.live.ApiUtils
import zs.live.dao.mysql.LiveRes
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 接口用途：结束直播预告
 * 逻辑：
 *  1.验证该预告是否有绑定正在直播中的直播，如果有则结束该直播
 *  2.更改预告表中的foreshow_status的状态值
 *  3.回写直播回看信息，其中包括：    //此步骤暂时忽略
 *      a.主播的用户信息，host：host_uid,host_avatar,host_username
 *      b.回看信息，live_record：video_address,watch_count,max_online_num,time_span
 */
ApiUtils.processNoEncry({
    Assert.isNotBlankParam(params,"foreshowId")
    long foreshowId = params.foreshowId as long
    String appId = Strings.getAppId(params)
    LiveService liveService = bean(LiveService)
    int status = liveService.updataLiveForeshowStatus(foreshowId,appId)
    return ["status":status]
})
