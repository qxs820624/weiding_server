import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.service.QcloudLiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * Created by Administrator on 2016/10/11.
 */
ApiUtils.process ({
    Assert.isNotBlankParam(params,"userId")
    String appId=Strings.getAppId(params)

    String userId = params.userId ?:""

    QcloudLiveService qcloudLiveService = getBean(QcloudLiveService)
    int isForce = params.isForce? params.isForce as int : 0  //是否强制获取 1表示强制获取
    String signId;
    if(isForce == 1){
        signId = qcloudLiveService.getSignIdFromQcloud(userId,appId)
    }else{
        signId = qcloudLiveService.getSignId(userId,appId)
    }
    return ["signId":signId];
})

