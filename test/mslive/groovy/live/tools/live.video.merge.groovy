import com.alibaba.fastjson.JSON
import zs.live.ApiUtils
import zs.live.dao.mysql.LiveRes
import zs.live.service.LiveService
import zs.live.service.VideoMergeService
import zs.live.utils.Strings

    String foreshowIdStr=request.getParameter("foreshowId")
    String mergeFlagStr=request.getParameter("mergeFlag")
    String appId=request.getParameter("appId")?: Strings.APP_NAME_SOUYUE
    List urlList = []
    long foreshowId = (foreshowIdStr ?:0) as long
    if(foreshowId){
        LiveRes liveRes = ApiUtils.getBean(context,LiveRes)
        LiveService liveService = ApiUtils.getBean(context,LiveService)
        VideoMergeService vms = ApiUtils.getBean(context,VideoMergeService)
        def liveForeshow = liveRes.findLiveForeshowByForeshowId(foreshowId, appId)
        def liveRecordInfo = liveForeshow?.live_record_info
        def videoAddress = liveRecordInfo ? Strings.parseJson(liveRecordInfo as String).video_address : ""
        if(!videoAddress){  //当视频已经合并过不再进行合并
            //根据预告id查询出预告对应的所有直播，判断每个直播的回看地址是否都已经生成
            def liveRecordList = liveRes.findLiveRecordListByForeId([foreshowId: foreshowId,all: true,appId: appId])
            boolean flag = true
            int timeSpanAll = 0
            List fileIds = []
            String fileId = ""
            liveRecordList?.each{
                int timeSpan = it.time_span as int
                if(it.video_address){
                    Map videoAddressMap = liveService.getVideoAddressUrlList(it.video_address,[roomId: it.room_id, liveId: it.live_id,foreshowId: foreshowId,appId: appId])
                    timeSpanAll = (videoAddressMap.timeSpan ?:0) as int
                    List liveRecordUrl = videoAddressMap.url
                    fileId = videoAddressMap.fileId?.get(0)
                    fileIds.addAll(videoAddressMap.fileId)
                    liveRecordUrl?.each{
                        urlList.add(it.url)
                    }
                }
                ApiUtils.log.info("wangtf video merge 直播时长，foreshowId:{},统计的直播回放时长:{},腾讯云返回的时长：{}",foreshowId,timeSpan,timeSpanAll)
                int mergeFlag = (mergeFlagStr ?: 0) as int
                if(mergeFlag == 0){
                    if( timeSpan-timeSpanAll >= 10*60){
                        ApiUtils.log.info("wangtf video merge 对应的直播没有完全生成回看 foreshowId:{},liveId：{}",foreshowId,it.live_id)
                        flag = false
                    }
                }
            }
            //当所有直播的回看地址都已经生成，再进行合并
            if(flag && urlList){
                if(urlList.size() == 1){
                    //当只有一段视频地址时，不进行合并，也不更新预告表，由于更新之后会删除源文件的视频地址导致无法播放
                    ApiUtils.log.info("wangtf video merge 只有一段视频不合并并只更新 foreshowId:{},fileId：{},urlList:{}",foreshowId,fileId,urlList)
                    def urlListAll = []
                    urlList.each{
                        urlListAll.add(["url": it as String])
                    }
                    def videoAddressMap = [duration:timeSpanAll,fileId:fileId,playSet:urlListAll]
                    String videoAddressInfo = JSON.toJSON([videoAddressMap])
                    liveService.updateForeshowMergeVideoAddressInfo(foreshowId,videoAddressInfo,timeSpanAll,Strings.APP_NAME_SOUYUE)
                }else {
                    ApiUtils.log.info("wangtf video merge foreshowId:{},fileIds:{},urlList:{}",foreshowId,fileIds,urlList)
                    vms.concatVideo(foreshowId as long,fileIds,liveForeshow.title as String,["mp4","m3u8"],liveForeshow.app_id)
                }
            }else {
                ApiUtils.log.info("wangtf video merge 回放地址没有全部生成，foreshowId:{}",foreshowId)
            }
        }
    }
def html = """
    <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
    <html xmlns="http://www.w3.org/1999/xhtml">
    <head></head>
    <body>
        <form action="/live/tools/live.video.merge.groovy" method="post">
        foreshowId:
        <input type="text" name= "foreshowId" value ="${foreshowIdStr ?:""}"/>
        </br>
        mergeFlag:
        <input type="text" name= "mergeFlag" value = "${mergeFlagStr ?:""}" />
        </br>
        说明：mergeFlag的值为0或者1，当为0时合并视频时添加时长误差的判断条件，当为1时不添加此条件，默认值为0
        </br>
        <input type="submit" value="提交" />
        </br>
        urlList:</br>
        <textarea rows="10" cols="200">${urlList ?:""}</textarea>
"""
html+= """</form>
    </body>
    </html>
    """
out << html

