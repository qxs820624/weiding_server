import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 手机端用户关注/解除关注直播系列(end_time为空表示关注，end_time有值则表示取消关注的时间)
 * @param userId 用户id
 * @param foreshowId 直播系列Id
 * @param operType 操作类型 1:关注 0:解除关注
 */
ApiUtils.processNoEncry({
    Assert.isNotBlankParam(params as Map, "userId")
    Assert.isNotBlankParam(params as Map, "foreshowId")
    Assert.isNotBlankParam(params as Map, "operType")
    long userId = params.userId as long;
    long foreshowId = params.foreshowId as long;
    int operType = params.operType as int;
    String appId=Strings.getAppId(params)
    LiveService liveService = getBean(LiveService)
    int res = liveService.updateUserForeshow(userId, foreshowId, operType,appId);
    ["status":"success","msg":"成功",userForeshowStatus:res]
})
