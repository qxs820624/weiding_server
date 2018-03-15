import zs.live.ApiUtils
import zs.live.entity.LiveRecord
import zs.live.service.LiveService
import zs.live.utils.Assert


/**
 * 直播状态
 * status = 1 直播存在
 * status = 2 直播不存在
 */
ApiUtils.process({
    Assert.isNotBlankParam(params,"liveId")
    Assert.isNotBlankParam(params,"roomId")
    long liveId = params.liveId ? params.liveId as long : 0
    int roomId = params.roomId ? params.roomId as int : 0
    ApiUtils.log.info("live.status.groovy 调用开始 liveId=>{},roomId=>{}",liveId,roomId)
    LiveService liveService = getBean(LiveService)
    //获取正在直播的 直播数据
    LiveRecord live = liveService.findLiveByLiveId(liveId);
    if(live){
        ApiUtils.log.info("live.status.groovy liveId=>{},roomId=>{}, 调用返回值==>{}",liveId,roomId,1)
        return ["status":1]
    }else{
        ApiUtils.log.info("live.status.groovy liveId=>{},roomId=>{}, 调用返回值==>{}",liveId,roomId,2)
        return ["status":2]
    }

})
