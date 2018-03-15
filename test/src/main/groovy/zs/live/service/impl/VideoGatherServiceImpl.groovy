package zs.live.service.impl

import com.qcloud.Common.PullVd
import com.qcloud.Common.QcloudDown
import groovy.util.logging.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import zs.live.dao.mysql.QcloudLiveRes
import zs.live.service.QcloudLiveService
import zs.live.service.VideoGatherService
import zs.live.utils.Http
import zs.live.utils.Parallel
import zs.live.utils.Strings

/**
 * 视频下载，与直播无关
 */
@Slf4j
@Service
class VideoGatherServiceImpl implements VideoGatherService{
    public static final Logger videoGatherLog = LoggerFactory.getLogger("videoGatherLog");

    @Value('${live.video.upload}')
    String downloadDir      //下载的目录
    @Value('${live.video.gather.callback.url}')
    String callbackURL      //回调地址
    @Value('${live.cms.key}')
    String cmsKey
    @Value('${live.cms.server}')
    String cmsServer
    @Autowired
    QcloudLiveService qcloudLiveService
    @Autowired
    QcloudLiveRes qcloudLiveRes

    /**
     * 文件下载
     * @param files
     */
    List<String> downloadFiles(List<String> files){
        videoGatherLog.info("GatherVideo 下载文件 >>> ${files}")
        long start = System.currentTimeMillis()
        List<String> list = QcloudDown.download((String[])files.toArray(),downloadDir)
        long end = System.currentTimeMillis()
        videoGatherLog.info("GatherVideo 下载完成，下载地址 >>> ${files} 耗时 ${end - start} 毫秒 保存地址 >>> ${list}")
        return list
    }

    /**
     * 文件上传
     * @param fileName
     */
    void multipartUpload(String fileName,Map params){
        String realURL = "${callbackURL}"
        String filePath = fileName
        try {
            videoGatherLog.info("GatherVideo 开始上传 文件名 >> ${filePath}")
            def qcloudInfo = qcloudLiveService.getQcloudInfo(params.appId)
            String secretId = qcloudInfo.secretId
            String secretKey = qcloudInfo.secretKey
            String result = QcloudLiveCommon.multipartUpload(secretId, secretKey, filePath, params?.title)
            videoGatherLog.info("GatherVideo 上传结束 本地路径 >> ${filePath} fileID >> ${result} 回调地址 >> ${realURL}")
            if (result){
                Thread.sleep(10*1000)
                Parallel.run([1],{
                    dealUploadFile(result,params)
                    String msg = QcloudLiveCommon.convertVodFile(secretId, secretKey,result,realURL)
                    videoGatherLog.info("GatherVideo 提交转码 fileID >> ${result} 提交结果 >> ${msg}")
                    def jsonObject = Strings.parseJson(msg)
                    def liveEvent = [
                        taskId:jsonObject.vodTaskId,
                        appId:params.appId
                    ]
                    qcloudLiveRes.addLiveEvent(liveEvent)

                },1)
            }else {
                throw new Exception("GatherVideo 上传失败")
            }
        }finally {
            File f = new File(filePath);
            def status = f.delete();
            videoGatherLog.info("GatherVideo 临时文件${filePath} 删除状态 >> ${status} ")
        }
    }

    /**
     * 提交给CMS视频信息
     * @param fileId
     * @param params
     * @return
     */
    def dealUploadFile(String fileId,Map params){
        String response
        if (!fileId){
            fileId = ''
        }
        String sign = Strings.md5(fileId + params?.newsLink + cmsKey)
        def request = [
            fileId:fileId,
            title:params?.title,
            newsLink:Strings.getUrlEncode(params?.newsLink,'UTF-8'),
            source:params?.source,
            pubtime:params?.pubtime,
            sign: sign
        ]
        try {
            if (params?.videoId){//CMS的手动抓取视频
                request.videoId = params?.videoId
                response = Http.post("${cmsServer}/api/dealUserDoVideo",request)
            }else {//自动抓取的视频
                response = Http.post("${cmsServer}/api/dealUploadFile",request)
            }
        } catch (Exception e) {
            e.printStackTrace()
            videoGatherLog.info("GatherVideo 上传结束调用CMS异常 Request >> ${request} Response >> ${response}")
        }
        videoGatherLog.info("GatherVideo 上传结束调用CMS Request >> ${request} Response >> ${response}")
    }

    /**
     * 回调CMS
     * @param params
     * @return
     */
    def uploadCallBack(Map params){
        String response
        try {
            response = Http.postJSON("${cmsServer}/DealVideo/VideoReFun",Strings.toJson(params))
            videoGatherLog.info("uploadCallBack 调用CMS Request >> ${params} Response >> ${response}")
        } catch (Exception e) {
            e.printStackTrace()
            videoGatherLog.info("uploadCallBack 调用CMS异常 Request >> ${params} Response >> ${response}")
        }
    }
}
