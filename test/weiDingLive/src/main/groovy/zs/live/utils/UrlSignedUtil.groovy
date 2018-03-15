package zs.live.utils

import groovy.util.logging.Slf4j
import org.apache.commons.codec.binary.Hex
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import zs.live.service.QcloudLiveService

import java.security.MessageDigest

/**
 * Created by Administrator on 2017/9/15.
 */
@Slf4j
@Component
class UrlSignedUtil {
    @Value('${qcloud.play.sign.key}')
    String qcloudPlayKey
    @Value('${pili.sign.key}')
    String piliSignKey

    @Autowired
    QcloudLiveService qcloudLiveService

    public String getSignedUrl(String url,int timeSpan,String appId,String vc) {
        if(!url){
            return url
        }
        //处理url，去掉?之后的内容(url失效后客户端在重新请求地址时会带着加密参数)
        if(url.indexOf("?") >= 0 ){
            url = url.substring(0,url.indexOf("?"))
        }
        //timeSpan为空时，表示根据app的设置取失效时间；
        // timeSpan有值时，表示所给time就是有效时间（主要给多段视频的时候处理失效时间的问题）
        int qcloudTime = timeSpan
        int piliTime= timeSpan
        if(!timeSpan){
            //根据appId获取url防盗链失效时间
            def qcloudInfo = qcloudLiveService.getQcloudInfo(appId)
            if(qcloudInfo){
                qcloudPlayKey= qcloudInfo.playKey ?:this.qcloudPlayKey
                piliSignKey= qcloudInfo.piliKey ?:this.piliSignKey
                qcloudTime = (qcloudInfo.qcloudTime ?: 0 ) as int
                piliTime = (qcloudInfo.piliTime ?: 0 ) as int
            }else{
                log.info("getSignedUrl qcloudInfo is null,appId=>{}",appId)
            }
        }
        if(!qcloudPlayKey || !piliSignKey){
            log.info("getSignedUrl key is null,qcloudPlayKey=>{},piliSignKey=>{}",qcloudPlayKey,piliSignKey)
            return url
        }

        //默认过期时间
        if(!qcloudTime){
            if(VerUtils.toIntVer(vc) < VerUtils.toIntVer("5.7.0")){
                qcloudTime = 12*60*60
            }else{
                qcloudTime = 30
            }
        }
        if(!piliTime){
            piliTime = 10
//            if(VerUtils.toIntVer(vc) < VerUtils.toIntVer("5.7.0")){
//                piliTime = 12*60*60
//            }else{
//            }
        }
        if(url.contains("liveplay.myqcloud.com")){//腾讯云直播拉流
            url = this.getQcloudLivePushSignedUrl(qcloudPlayKey,url,qcloudTime)
        }else if(url.contains("vod2.myqcloud.com")){//腾讯云点播（中搜账号）
            url = this.getQcloudLivePlaySignedUrl(qcloudPlayKey,url,qcloudTime)
        }else if(url.contains("pili-live")){
            url = this.getPiliSignedUrl(piliSignKey,url,piliTime)
        }
        return url
    }

    /**
     * 腾讯云直播推流、拉流url防盗链
     * @param url
     * @param timeSpan 视频的播放时长，单位：秒
     * @return
     */
    String getQcloudLivePushSignedUrl(String key,String url,int timeSpan){
        //非腾讯云数据不需要加密
        if(url.indexOf("liveplay.myqcloud.com") < 0 ){
            return url
        }
        boolean replaceFlag = false
        if(url.startsWith("rtmp")){//为后面方便处理，统一将地址更换为http的，最后再替换回去就可以了
            url = url.replaceAll("rtmp","http")
            replaceFlag = true
        }
        String fileName = url.substring(url.lastIndexOf('/')+1);
        int index = fileName.lastIndexOf(".")
        String streamId = fileName
        if(index >=0 ){
            streamId = fileName.substring(0,index)
        }
        Long time = System.currentTimeMillis()/1000+timeSpan
        String signUrl = QcloudBizidUtil.getSafeUrl(key, streamId, time)
        URL urlObj = new URL(url);
        if (urlObj.getQuery() != null) {
            url = String.format("%s&%s", url,signUrl);
        } else {
            url = String.format("%s?%s", url,signUrl);
        }
        if(replaceFlag){
            url = url.replaceAll("http","rtmp")
        }
        return url
    }

    /**
     * 腾讯云点播url防盗链
     * @param url
     * @param timeSpan 视频的播放时长，单位：秒
     * @return
     */
    String getQcloudLivePlaySignedUrl(String key,String url,int timeSpan){
        //非腾讯云数据不需要加密
        if(url.indexOf("qcloud.com") < 0 ){
            return url
        }
        boolean replaceFlag = false
        if(url.startsWith("rtmp")){//为后面方便处理，统一将地址更换为http的，最后再替换回去就可以了
            url = url.replaceAll("rtmp","http")
            replaceFlag = true
        }
        URL urlObj = new URL(url);
        String partUrl = urlObj.getPath()
        String[] array = partUrl.split("/")
        if(array.length<=2){//直播1.0的数据不支持加密，即直播数据没有分级目录，如：http://200006652.vod.myqcloud.com/200006652_cb732f444eb3450397bd075115bbb24e.f0.mp4
            return url
        }else{
            //该判断只是为了记录一下日志而已，目前看的url在腾讯云中正常都是两级目录
            if(array.length!=4){
                log.info("url目录异常，url=>{}",url)
            }
            //sign = md5(KEY + dir + t + exper + us)
            String dir = partUrl.substring(partUrl.indexOf("/"),partUrl.lastIndexOf("/")+1)
            Long time = System.currentTimeMillis()/1000+timeSpan
            String t = Long.toHexString(time)
            String exper = "0" //试看时间0表示不试看，即返回完整视频
            String us = Strings.shortUUID()
            String sign = Strings.md5(key+dir+t+exper+us)
            if (urlObj.getQuery() != null) {
                url = String.format("%s&t=%s&exper=%s&us=%s&sign=%s", url, t, exper,us,sign);
            } else {
                url = String.format("%s?t=%s&exper=%s&us=%s&sign=%s", url, t, exper,us, sign);
            }
        }
        if(replaceFlag){
            url = url.replaceAll("http","rtmp")
        }
        return url
    }

    /**
     * 七牛url防盗链
     * @param url
     * @param timeSpan 视频的播放时长，单位：秒
     * @return
     */
    String getPiliSignedUrl(String key,String url,int timeSpan){
        //非腾讯昆仑决的七牛不需要加密
        if(!(url.contains("pili-live") && url.contains("gedoushijie"))){
            return url
        }
        boolean replaceFlag = false
        if(url.startsWith("rtmp")){//为后面方便处理，统一将地址更换为http的，最后再替换回去就可以了
            url = url.replaceAll("rtmp","http")
            replaceFlag = true
        }
        URL urlObj = new URL(url);
        String partUrl = urlObj.getPath()
        //sign = md5(KEY + dir + t )
        Long time = System.currentTimeMillis()/1000+timeSpan
        String t = Long.toHexString(time)
        String sign = Strings.md5(key+partUrl+t)
        if (urlObj.getQuery() != null) {
            url = String.format("%s&sign=%s&t=%s", url, sign, t);
        } else {
            url = String.format("%s?sign=%s&t=%s", url, sign, t);
        }
        if(replaceFlag){
            url = url.replaceAll("http","rtmp")
        }
        return url
    }

}
