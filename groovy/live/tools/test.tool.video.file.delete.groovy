import zs.live.ApiUtils
import zs.live.common.LiveCommon
import zs.live.entity.LiveRecord
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.utils.Assert

ApiUtils.processNoEncry{
    long liveId = params.liveId as long

    LiveService liveService = getBean(LiveService)
    LiveRecord liveRecord = liveService.findLiveRecordByLiveId(liveId)
    long foreshowId = liveRecord.foreshowId
    int liveMode = liveRecord.liveMode
    int res = 0
    if(liveMode == 1){//互动直播
        res = liveService.delLiveRecord([liveId:liveId,appId:liveRecord.appId,op_role:100])
        ApiUtils.log.info("wangtf 删除互动直播数据，删除直播数据，liveId:{},foreshowId:{},res:{}", liveId,foreshowId,res)
    }else {//会议直播
        if(foreshowId){
            res = liveService.delForeshow([foreshowId:foreshowId,appId: liveRecord.appId])
            ApiUtils.log.info("wangtf 删除会议直播数据，删除预告：liveId:{},foreshowId:{},res:{}", liveId,foreshowId,res)
            res =liveService.delLiveRecord([liveId: liveId,appId: liveRecord.appId,op_role: 100])
            ApiUtils.log.info("wangtf 删除会议直播数据，删除直播数据，liveId:{},foreshowId:{},res:{}", liveId,foreshowId,res)
        }else {
            res = liveService.delLiveRecord([liveId: liveId,appId: liveRecord.appId,op_role: 100])
            ApiUtils.log.info("wangtf 删除会议直播数据，没有绑定预告，删除直播数据，liveId:{},foreshowId:{},res:{}", liveId,foreshowId,res)
        }

    }

}

