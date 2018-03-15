import com.alibaba.fastjson.JSON
import zs.live.ApiUtils
import zs.live.service.LivePgcService
import zs.live.service.impl.QcloudLiveCommon
import zs.live.utils.Assert
import zs.live.utils.DateUtil
import zs.live.utils.Http
import zs.live.utils.HttpQcould
import zs.live.utils.Strings
import zs.live.utils.UrlSignedUtil
import zs.live.utils.VerUtils

ApiUtils.process {
    Assert.isNotBlankParam(params, "url");
    Assert.isNotBlankParam(params, "vc");

    String url = params.url
    String vc = params.vc
    int timeSpan = (params.timeSpan ?:0) as int
    String appId=Strings.getAppId(params)
    UrlSignedUtil urlSignedUtil = getBean(UrlSignedUtil)
    String signUrl = urlSignedUtil.getSignedUrl(url,timeSpan,appId,vc)
    return [url:signUrl]
}

