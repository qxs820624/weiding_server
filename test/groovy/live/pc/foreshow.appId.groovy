import zs.live.ApiUtils
import zs.live.entity.LiveRecord
import zs.live.service.LiveService

/**
 * 根据liveId或者foreshowId获取可推送的appId
 * 互动直播推送给所有appId，官方直播（即带预告的直播）根据共享分类查询有效的推送appId
 */
ApiUtils.processNoEncry {
    long liveId = (params.liveId ?:0) as long
    long foreshowId = (params.foreshowId ?:0) as long

    LiveService liveService = bean(LiveService.class)
    def res =[:]
    def result = []
    if (liveId){
        LiveRecord liveRecord = liveService.findLiveByLiveId(liveId)
        if(!liveRecord){
            ApiUtils.log.info("foreshow.appId error,liveRecord is null.params=>{}",params)
            return res
        }
        int live_type = liveRecord.liveType
        if(live_type == 1){//官方直播
            foreshowId = liveRecord.foreshowId
            if(!foreshowId){
                ApiUtils.log.info("foreshow.appId error,foreshowId is null.params=>{}",params)
                return res
            }
        }else{
            result = liveService.getAppIdsFromConfig()
        }
    }
    if(foreshowId){
        result = liveService.getAppIdsByForeshow([foreshowId:foreshowId])
    }
    def appIds = []
    result.each {
        appIds << it.app_id
    }
    ApiUtils.log.info("foreshow.appId params=>{},appIds=>{}",params,appIds)
    return [appIds:appIds]
}
