import com.alibaba.fastjson.JSON
import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.service.GiftService
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 礼物扣费
 *
 * */
ApiUtils.process {
    Assert.isNotBlankParam(params, "token")
    Assert.isNotBlankParam(params, "giftId")
    Assert.isNotBlankParam(params, "userId")
    Assert.isNotBlankParam(params, "toUserId")
    Assert.isNotBlankParam(params, "liveId")
    Assert.isNotBlankParam(params, "openId")
    Assert.isNotBlankParam(params, "opId")
    Assert.isNotBlankParam(params, "serialNumber")
    Assert.isNotBlankParam(params, "encData")

    String token = params.token ?: ""  //用户名称
    String giftId = params.giftId ?: ""
    long userId = (params.userId ?: 0) as long //用户id
    long toUserId = (params.toUserId ?: 0) as long //被打赏用户id（主播id）
    long liveId = (params.liveId ?: 0) as long //直播id
    int giftCount = (params.giftCount ?: 1) as long //礼物数量，默认1个
    def giftInfo = params.giftInfo
    String openId = params.openId ?: ""
    String opId = params.opId ?: ""
    String serialNumber = params.serialNumber ?: ""
    String encData = params.encData ?: ""
    String appId=Strings.getAppId(params)

    LiveService liveService = getBean(LiveService)
    GiftService giftService = getBean(GiftService)

    def userInfo = liveService.getUserInfo(userId)
    def toUserInfo = liveService.getUserInfo(toUserId)

    //校验用户打赏是否合法
    int res = liveService.validateFans([userId:userId,toUserId:toUserId,liveId:liveId,appId:appId])
    String msg = "送礼成功"
    if(res ==1){
        //校验成功，开始打赏
        Map paramsMap = [
            token:token,
            giftId:giftId,
            userId:userId,
            userName:userInfo ?.userName ?: "",
            userNickname:userInfo ?.nickname ?: "",
            userImage:userInfo ?.userImage ?: "",
            toUserId:toUserId,
            toUserName:toUserInfo?.userName ?: "",
            liveId:liveId,
            openId:openId,
            opId:opId,
            serialNumber:serialNumber,
            giftCount:giftCount,
            giftInfo:giftInfo,
            encData:encData,
            appId:appId
        ]
        def resMap = giftService.giftPayment(paramsMap)
        res = (resMap?.status?:0) as int
        msg = resMap?.msg?:""
        if(res == 1){//打赏成功
            Map userInfoMap = [
                sybCount:(resMap?.sybCount ?:0) as int//打赏者剩余搜悦币
            ]
            Map anchorInfoMap = [
                charmCount:(resMap?.charmCount ?:0) as int//主播魅力值
            ]
            return ["status":res,msg:msg,giftInfo:giftInfo,"userInfo":userInfoMap,"anchorInfo":anchorInfoMap]
        }else if(res == ApiException.STATUS_GET_BODY_MSG){//账号被冻结造成打赏失败，需要通知给客户端
            throw new ApiException(ApiException.STATUS_GET_BODY_MSG, msg);
        }else{//其他原因造成打赏失败，不需要通知给客户端，接口展示出来，方便定位问题
            return ["status":res,msg:msg]
        }
    }else if (res ==2) {
//        msg="打赏用户不存在"
        msg ="打赏失败（2）"
        throw new ApiException(ApiException.STATUS_GET_BODY_MSG, msg);
    } else if (res ==3) {
//        msg="被打赏用户不存在"
        msg ="打赏失败（3）"
        throw new ApiException(ApiException.STATUS_GET_BODY_MSG, msg);
    } else if (res ==4) {
//        msg="直播不存在"
        msg ="打赏失败（4）"
        throw new ApiException(ApiException.STATUS_GET_BODY_MSG, msg);
    } else if (res ==5) {
//        msg="主播不在该直播间"
        msg ="打赏失败（5）"
        throw new ApiException(ApiException.STATUS_GET_BODY_MSG, msg);
    } else if (res ==6) {
//        msg="打赏人不在该直播间"
        msg ="打赏失败（6）"
        throw new ApiException(ApiException.STATUS_GET_BODY_MSG, msg);
    }
    return ["status":res,msg:msg]
}
