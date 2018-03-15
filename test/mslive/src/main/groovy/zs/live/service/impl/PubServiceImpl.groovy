package zs.live.service.impl

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import zs.live.ApiException
import zs.live.service.PubService
import zs.live.utils.Http
import zs.live.utils.Strings
import zs.live.utils.VerUtils

/**
 * 公共服务接口的实现类
 * Created by ls on 2015/6/18.
 */
@Service("pubService")
@Slf4j
class PubServiceImpl implements PubService {

    @Value('${live.env}')
    String test // 是否是测试环境
    @Value('${test.address}')
    String testAddress

    @Override
    def upgradeVc(String appName,String channel,String vc,boolean isAndroid){
        int status=0
        def backMap=[status:status]
        def upgradeVc="5.6"
        //当前版本已经是最新版本则不需要抛出800
        if(VerUtils.toIntVer(vc)>= VerUtils.toIntVer(upgradeVc)){
            return backMap
        }
        String osType=isAndroid ? "android":"ios"
        def desc="您的版本过低,请升级到"+upgradeVc
        def skipHtml5="http://souyue.mobi/"
        //只有搜悦才给出升级提醒
        if(VerUtils.toIntVer(vc)<VerUtils.toIntVer(upgradeVc)&&"souyue".equals(appName)){
            //status=ApiException.STATUS_UPGRADE_SKIP_HTML5
           status=ApiException.STATUS_UPGRADE_REMIND
        }
        def p=""
        if("souyue".equals(appName)){
            p="android".equals(osType)?"souyue":"中搜搜悦"
            p = URLEncoder.encode(p,"UTF-8")
        }

        if(status==ApiException.STATUS_UPGRADE_REMIND){//直接升级
            def url="test".equals(test)?"http://103.29.134.224:8888":"http://api2.souyue.mobi"
           // def url = "http://api2.souyue.mobi"
            url=url+"/d3api2/checkVersion.groovy?vc=${vc}&c=${channel}&e=${osType}&p=${p}"
            log.info("url===>"+url)
           def checkVc=Http.get(url)
           def checkData=Strings.parseJson(checkVc)
           if(checkData?.head?.status==200){
               backMap=checkData.body
           }
            backMap.status=status
        }
        if(status==ApiException.STATUS_UPGRADE_SKIP_HTML5){//直接跳转到html5
            backMap.status=status
            backMap.url=skipHtml5
            backMap.desc=desc
        }
        return backMap
    }
}
