import com.alibaba.fastjson.JSON
import zs.live.ApiUtils
import zs.live.dao.mysql.LiveRes
import zs.live.service.LiveService
import zs.live.utils.Strings

ApiUtils.processNoEncry({

    long foreshowId = (params.foreshowId ?: 0) as int

    LiveRes liveRes = bean(LiveRes)
    LiveService liveService = bean(LiveService)

    if(foreshowId >0){
        def liveForeshow = liveRes.findLiveForeshowByForeshowId(foreshowId,Strings.APP_NAME_SOUYUE)
        this.updateForeshowIdVideoAddress(foreshowId, liveForeshow, liveRes, liveService)
    }else {
        List liveList = liveRes.findForeshowIdListForMerge()
        for (def liveForeshow : liveList){
            foreshowId = (liveForeshow.foreshow_id ?: 0) as long
            this.updateForeshowIdVideoAddress(foreshowId, liveForeshow, liveRes, liveService)
        }
    }
})


def updateForeshowIdVideoAddress(long foreshowId, def liveForeshow,LiveRes liveRes,LiveService liveService){
    def liveRecordInfo = liveForeshow.live_record_info
    def videoAddress = liveRecordInfo ? Strings.parseJson(liveRecordInfo as String).video_address : ""
    if(videoAddress){
        def liveRecordList = liveRes.findLiveRecordListByForeId([foreshowId: foreshowId,all: true,appId: Strings.APP_NAME_SOUYUE])
        List urlList = []
        int timeSpanAll = 0
        String fileId = ""
        liveRecordList?.each{
            if(it.video_address){
                Map videoAddressMap = liveService.getVideoAddressUrlList(it.video_address,[roomId: it.room_id, liveId: it.live_id,foreshowId: foreshowId,appId: Strings.APP_NAME_SOUYUE])
                timeSpanAll = (videoAddressMap.timeSpan ?:0) as int
                List liveRecordUrl = videoAddressMap.url
                fileId = videoAddressMap.fileId?.get(0)
                liveRecordUrl?.each{
                    urlList.add(it.url)
                }
            }
        }
        //当所有直播的回看地址都已经生成，再进行合并
        if(urlList.size() == 1){
            def urlListAll = []
            urlList.each{
                urlListAll.add(["url": it as String])
            }
            def videoAddressMap = [duration:timeSpanAll,fileId:fileId,playSet:urlListAll]
            String videoAddressInfo = JSON.toJSON([videoAddressMap])
            liveService.updateForeshowMergeVideoAddressInfo(foreshowId,videoAddressInfo,timeSpanAll,Strings.APP_NAME_SOUYUE)
            ApiUtils.log.info("wangtf video merge 只有一段视频更新 foreshowId:{},fileId：{},urlList:{}",foreshowId,fileId,urlList)
        }
    }
}
