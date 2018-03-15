import zs.live.ApiUtils
import zs.live.common.LiveCommon
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

ApiUtils.processNoEncry{
    Assert.isNotBlankParam(params,"fileIds")
    Assert.isNotBlankParam(params,"appId")
    //flag:0,直接删除视频文件，1：修改数据库状态
    int flag = (params.flag ?: 0)as int
    String str = params.fileIds
    String appId = Strings.getAppId(params)
    String[] strs = str.split(",")
    LiveService liveService = getBean(LiveService)
    QcloudLiveService qcloudLiveService = bean(QcloudLiveService)
    int res = 0
    strs.each {
        if(flag == 0){
            String result = qcloudLiveService.deleteVodFile([it as String],appId)
            if(result.contains("Success")){
                res = 1
            }
        }else
            res = liveService.deleteRecodeLog(it as String)
    }
    return res
}

