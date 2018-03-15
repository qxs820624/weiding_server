import redis.clients.jedis.params.Params
import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 根据appid获取腾讯云配置
 *
 */
ApiUtils.process {
    Assert.isNotBlankParam(params,"appId")

    String appId=Strings.getAppId(params)
    int op_role = (params.op_role ?:0 ) as long

    QcloudLiveService qcloudLiveService = bean(QcloudLiveService)
    def result = [:]
    def config = qcloudLiveService.getQcloudInfo(appId)
    if(op_role == 100){//研发人员查询redis数据
        result = config
    }else{//客户端业务使用
        result.sdkAppId = config.sdkAppId
        result.identifierType = config.identifierType
    }
    return result
}

