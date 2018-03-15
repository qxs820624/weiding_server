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
    Assert.isNotBlankParam(params, "pushUrl");    //String
    Assert.isNotBlankParam(params, "title");      //String
    Assert.isNotBlankParam(params, "beginTime");  //long
    Assert.isNotBlankParam(params, "description");//String
    Assert.isNotBlankParam(params, "partnerId");  //String
    Assert.isNotBlankParam(params, "sign");  //String

    PartnerService partnerService = getBean(PartnerService)
    def partner = partnerService.getPartner(params.partnerId)
    if (!partner){
        throw new ApiException(700,"请重新登录")
    }

    def paramMap = [pushUrl:params.pushUrl,title:params.title,beginTime:params.beginTime,description:params.description,partnerId:params.partnerId]
    if (! SignUtils.checkSign(paramMap,params.sign as String,partner.secret)){
        throw new ApiException(701,"签名不下正确")
    }

    LivePgcService livePgcService = getBean(LivePgcService)

    return livePgcService.createThirdParty(params)
})
