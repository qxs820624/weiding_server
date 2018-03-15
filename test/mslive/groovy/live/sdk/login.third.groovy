import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.service.LiveSdkSevice
import zs.live.service.impl.LiveSdkServiceImpl
import zs.live.utils.Assert
import zs.live.utils.ClientType
import zs.live.utils.Http
import zs.live.utils.Strings

//
//该接口是第三方登录接口，主要针对外边合作单位调用。
//
//
//参数：
//thirdUserId：第三方的uid，为字符串。
//nick：第三方用户昵称
//image：第三方用户头像地址
//source：类型，xuzhouribao:徐州日报
//imei：设备编号，用于数据合并。

//var foo = {
//	head : {
//		status : 200
//	},
//	body : {
//		/*用户资料*/
//      userId :
//		image : "头像地址",
//		name : "用户昵称",
//      image: ""
//		token : ""
//	}
//}

ApiUtils.process({
    Assert.isNotBlankParam(params, "thirdUserId")
    Assert.isNotBlankParam(params, "nick")
    Assert.isNotBlankParam(params, "source") //用户中心给不同app分配的固定值
    Assert.isNotBlankParam(params, "imei") //imei号
    Assert.isNotBlankParam(params,"appId")
    String vc=params.vc?:"5.6"
    def clientIp=Http.getIpAddr(request)
    def clientType = ClientType.isAndroidOs(request)?'android':'ios'
    osType = "ios".equalsIgnoreCase(clientType)?1:2

    LiveSdkSevice liveSdkSevice=getBean(LiveSdkSevice)
    def syLoginUrl=liveSdkSevice.getSyURL()+"/d3api2/user/login.third.groovy"
    def loginMap=[thirdUserId:params.thirdUserId,nick:params.nick,source:params.source,
                  imei:params.imei,appId:params.appId,vc:vc,appName:params.appId,
                  province:params.province,city:params.city,
                  clientIp:clientIp,osType:osType]
    def back=Http.post(syLoginUrl,loginMap)
    ApiUtils.log.info("loginThird params:"+Strings.toJson(loginMap)+" back:"+back)
    def loginInfo
    if(back){
        def backData= Strings.parseJson(back)
        if(backData.head.status==200){
            loginInfo=backData.body
        }
    }
    if(!loginInfo){
        throw new ApiException(ApiException.STATUS_GET_BODY_MSG, '登录失败');
    }
    return loginInfo
})
