import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.service.LivePgcService
import zs.live.service.PartnerService
import zs.live.utils.Assert
import zs.live.utils.SignUtils

/**
 * Created by Administrator on 2016/12/22.
 */
ApiUtils.process({
    Assert.isNotBlankParam(params, "liveId");        //long
    Assert.isNotBlankParam(params, "partnerId");  //String
    Assert.isNotBlankParam(params, "status");  //int
    Assert.isNotBlankParam(params, "sign");  //String

    PartnerService partnerService = getBean(PartnerService)
    def partner = partnerService.getPartner(params.partnerId)
    if (!partner){
        throw new ApiException(700,"请重新登录")
    }

    def paramMap = [liveId:params.liveId, partnerId:params.partnerId, status:params.status]
    if (! SignUtils.checkSign(paramMap,params.sign as String,partner.secret)){
        throw new ApiException(701,"签名不下正确")
    }
    LivePgcService livePgcService = getBean(LivePgcService)

    return livePgcService.modifyThirdParty(params)
})
