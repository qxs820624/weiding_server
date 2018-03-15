import zs.live.ApiUtils
import zs.live.dao.mysql.LiveRes
import zs.live.dao.redis.LiveQcloudRedis

ApiUtils.process({
    LiveQcloudRedis liveQcloudRedis = bean(LiveQcloudRedis)
    LiveRes liveRes = getBean(LiveRes)
    //获取真实观众的pv
    List liveList = liveRes.findLiveRecordListForUpdateCountRedis()
    List liveIds = []
    liveList?.each{
        long liveId = it.live_id as long
        long watchCount = liveQcloudRedis.getLiveRealWatchCount(liveId)
        if(watchCount < 0){
            liveIds.add(liveId)
        }
    }
    return [count : liveIds.size(),liveIds:liveIds]
})
