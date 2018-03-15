import zs.live.ApiUtils
import zs.live.dao.mysql.QcloudLiveRes
import zs.live.entity.LiveRecord
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.service.impl.QcloudLiveCommon
import zs.live.utils.Assert
import zs.live.utils.Strings

ApiUtils.processNoEncry {
    Assert.isNotBlankParam(params,"liveId")
    long liveId = params.liveId as long
    QcloudLiveService qcloudLiveService = getBean(QcloudLiveService)
    QcloudLiveRes qcloudLiveRes = getBean(QcloudLiveRes)
    String fileId = params.fileId
    String backString="";
    if(fileId){
        List lastList = new ArrayList()
        fileId?.split(",").each {
            String adrr = QcloudLiveCommon.getVodPlayListInfoByFileId(qcloudLiveService.secretId, qcloudLiveService.secretKey, it)
            List vo = Strings.parseJson(adrr,List)
            if(vo && vo.size() > 0){
                lastList.addAll(vo)
            }
        }
        //增加逻辑 kpc 16.11.24  先取fileId
        String videoAddress = Strings.toJson(lastList)
        ApiUtils.log.info("取回看记录开始 取到回看记录通过fileId： liveId={},fileId={},videoAddress={}",liveId,fileId,videoAddress)
        //修改liverecord_log表中vedio_adress字段的值
        qcloudLiveRes.updateLiveRecordVideoAddress(videoAddress, liveId,null)
        backString = videoAddress
    }else{
        LiveService liveService = getBean(LiveService)
        LiveRecord live = liveService.findLiveRecordByLiveId(liveId)
        backString  = qcloudLiveService.getBackInfo(live.roomId,liveId,live.foreshowId,live.appId,"手工调用重置回看地址")
    }

    return backString
}
