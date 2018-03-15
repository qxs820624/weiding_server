import redis.clients.jedis.params.Params
import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Strings

/**
 * 应用场景：显示正在直播的列表
 *
 */
ApiUtils.process {
    LiveService liveService = bean(LiveService)
    String appId = Strings.getAppId(params)
    int configId = (params.configId ?: 1) as int
    return liveService.findLiveConfig(appId,configId)
}

