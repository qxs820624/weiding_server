import zs.live.ApiUtils
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.utils.Assert

/**
 * watcherCount 正在观看人数
 * watcherTotalCount 累积观看总数,pv
 * vestCount 马甲数
 * userCount 真实用户数
 */
ApiUtils.processNoEncry({
    Assert.isNotBlankParam(params,"liveId")

    LiveQcloudRedis liveQcloudRedis = getBean(LiveQcloudRedis)
    long liveId = params.liveId as long
    long userCount = liveQcloudRedis.getLiveRealWatchCount(liveId)
    long watcherTotalCount = liveQcloudRedis.getLiveWatherTotalCount(liveId)

    return ["vestCount":watcherTotalCount-userCount,"userCount":userCount,"watcherTotalCount":watcherTotalCount]
})
