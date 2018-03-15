import zs.live.ApiUtils
import zs.live.dao.redis.LiveRedis

/**
 * 监控redis写功能是否正常，目前该集群经常发送主从切换不成功问题
 * 202.108.1.140:7379,202.108.1.142:7379,202.108.1.144:7379,202.108.1.145:7379'
 */
ApiUtils.processNoEncry({
    LiveRedis liveRedis = getBean(LiveRedis)
    def key=params.key
    liveRedis.set(key,"1",1)
    return [msg:"ok"]
})
