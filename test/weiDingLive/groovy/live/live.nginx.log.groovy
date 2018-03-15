import zs.live.ApiUtils
import zs.live.dao.mysql.QcloudLiveRes
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 *  为统计系统nginx收集日志使用
 */
ApiUtils.processNoEncry {
    return [msg:"ok"]
}
