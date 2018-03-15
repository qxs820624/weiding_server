import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.entity.LiveForeshow
import zs.live.entity.LiveRecord
import zs.live.service.LiveForeshowService
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 更新预告直播的状态
 * 包括：开始直播，暂停  结束
 */
ApiUtils.processNoEncry{
    Assert.isNotBlankParam(params,"foreshowId") //预告Id
    Assert.isNotBlankParam(params,"operateType") //1:开始 2:暂停 3:结束

    long foreshowId=params.foreshowId as long
    def operateType=params.operateType as int
    def message=params.message?:""
    def vc=params.vc?:"1.0"
    String appId=Strings.getAppId(params)

    LiveForeshowService liveForeshowService = getBean(LiveForeshowService)
    LiveService liveService = getBean(LiveService)
    LiveForeshow liveForeshow=liveForeshowService.get(foreshowId);
    LiveRecord liveRecord = liveService.getLiveByForeshow(foreshowId,appId)
    if(!liveRecord){
        throw new ApiException(700,"预告对应的直播不存在")
    }
    if(!liveForeshow){
        throw new ApiException(700,"预告不存在")
    }
    def ret;
    switch (operateType){
        case 1://开始
            ret=   liveForeshowService.beginPgc(foreshowId,appId,liveRecord,liveForeshow,message,vc)
            break;
        case 2://暂停
           ret=  liveForeshowService.pausePgc(foreshowId ,appId,liveRecord, liveForeshow  ,message,vc)
            break;
        case 3://结束
            ret=  liveForeshowService.stopForeshow(foreshowId,appId,liveRecord, liveForeshow)
            break;
        default:
            break;
    }

    return ret;
}
