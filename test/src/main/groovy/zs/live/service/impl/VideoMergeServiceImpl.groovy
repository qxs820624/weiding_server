package zs.live.service.impl

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.qcloud.Common.PullVd
import com.qcloud.Common.QcloudDown
import groovy.util.logging.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import zs.live.common.LiveCommon
import zs.live.dao.mysql.QcloudLiveRes
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.service.QcloudLiveService
import zs.live.service.VideoMergeService
import zs.live.utils.DateUtil
import zs.live.utils.ffmpeg.FFMpegUtil

/**
 * Created by Administrator on 2016/12/5 0005.
 */
@Slf4j
@Service
class VideoMergeServiceImpl implements VideoMergeService{
    public static final Logger timerLog = LoggerFactory.getLogger("timerLog");

    @Value('${live.video.ffmpeg}')
    String ffmpegUri //ffmpeg的全路径
    @Value('${live.video.download}')
    String downloadDir //下载的目录
    @Value('${live.video.upload}')
    String uploadDir //上传的目录
    @Value('${live.video.upload.url}')
    String uploadURL //上传的url(腾讯拉取)
    @Value('${live.video.callback.url}')
    String callbackURL //上传的url(腾讯拉取)

    @Value('${live.video.uploadMode}')
    String uploadMode //push：推送至云 pull：云端拉取

    @Autowired
    LiveQcloudRedis liveQcloudRedis;
    @Autowired
    QcloudLiveService qcloudLiveService

    @Autowired
    QcloudLiveRes qcloudLiveRes

    public String merge(List<String> files,long foreshowId,String appId){
        String localPath = this.mergeVideo(this.downloadVideo(files))
        if (uploadMode == LiveCommon.LIVE_UPLOADMODE_PUSH)
            multipartUploadVodFile(localPath,foreshowId,3,appId)
        else
            uploadVideo(localPath,foreshowId,appId)
    }

    /**
     * 视频合并
     * @param files 本地文件位置
     * @return 合并后的文件名，无路径
     */
    private String mergeVideo(List<String> files) {
        if(!files){
            return ""
        }
        String path = files.last()
        long start = System.currentTimeMillis()
        String fileName = path.substring(path.lastIndexOf("/")+1,path.lastIndexOf(".")) + ".mp4";
        String resultName = uploadDir + fileName
        File file = new File(resultName)
        if (!file.exists()){
            new FFMpegUtil(ffmpegUri).safeMergeFile(files, resultName);
            long end = System.currentTimeMillis()
            timerLog.info("mergeVideo 合并完成，耗时 ${end - start} 毫秒 >>> ${resultName}")
        }else {
            timerLog.info("mergeVideo 合并文件已存在 >>> ${resultName}")
        }
        return fileName
    }

    /**
     * 下载文件
     * @param files
     * @return
     */
    private List<String> downloadVideo(List<String> files) {
        timerLog.info("mergeVideo 下载文件 >>> ${files}")
        long start = System.currentTimeMillis()
        List<String> list = QcloudDown.download((String[])files.toArray(),downloadDir)
        long end = System.currentTimeMillis()
        timerLog.info("mergeVideo 下载完成，耗时 ${end - start} 毫秒 >>> ${list}")
        return list
    }

    /**
     * 上传文件
     * @param fileName
     * @param foreshowId
     */
    private void uploadVideo(String fileName, long foreshowId,String appId) {
        def qcloudInfo = qcloudLiveService.getQcloudInfo(appId)
        String secretId = qcloudInfo.secretId
        String secretKey = qcloudInfo.secretKey

        String url = uploadURL+fileName
        String realURL = "${callbackURL}?foreshowId=${foreshowId}"
        String result = PullVd.pullFile(url,fileName,realURL)
        JSONObject jsonObject = (JSONObject)JSON.parse(result);
        if (jsonObject.getInteger("code")==0){
            timerLog.info("mergeVideo foreshowId=${foreshowId}的文件 ${fileName} 上传成功 文件地址>>> ${url} 回调地址>>> ${realURL}")
            liveQcloudRedis.hsetVideoMergeRedis(foreshowId, "fileId", jsonObject.data[0].file_id as String)
        }else {
            timerLog.info("mergeVideo foreshowId=${foreshowId}的文件 ${fileName} 上传失败 失败信息>>> ${jsonObject.get("message")}")
            String fileId = liveQcloudRedis.hgetVideoMergeRedis(foreshowId, "fileId")
            if (fileId){
                timerLog.info("uploadVideo 预告 ${foreshowId} 文件 ${fileId} 已存在")
                String msg = QcloudLiveCommon.convertVodFile(secretId, secretKey,fileId,realURL)
                jsonObject = (JSONObject)JSON.parse(msg);
                timerLog.info("uploadVideo 预告 ${foreshowId} 转码文件 ${fileId}， 转码信息>>> ${jsonObject.get("message")}")
            }
        }
    }

    /**
     * 重试上传
     * @param url
     * @param foreshowId
     */
    public void reTryUploadVideo(String url, long foreshowId) {
        String title = url.substring(url.lastIndexOf("/")+1,url.lastIndexOf("?"));
        String realURL = "${callbackURL}?foreshowId=${foreshowId}"
        String result = PullVd.pullFile(url,title,realURL)
        JSONObject jsonObject = (JSONObject)JSON.parse(result);
        if (jsonObject.getInteger("code")==0){
            timerLog.info("mergeVideo retry foreshowId=${foreshowId}提交拉取成功 文件地址>>> ${url} 回调地址>>> ${realURL}")
            liveQcloudRedis.hsetVideoMergeRedis(foreshowId, "fileId", jsonObject.data[0].file_id as String)
        }else {
            timerLog.info("mergeVideo retry foreshowId=${foreshowId}提交拉取失败 失败信息>>> ${jsonObject.get("message")}")
        }
    }

    public String multipartUploadVodFile(String fileName, long foreshowId,int max,String appId){
        def qcloudInfo = qcloudLiveService.getQcloudInfo(appId)
        String secretId = qcloudInfo.secretId
        String secretKey = qcloudInfo.secretKey

        String fileId = liveQcloudRedis.hgetVideoMergeRedis(foreshowId, "fileId")
        String filePath = uploadDir + fileName
        String result = fileId
        String realURL = "${callbackURL}?foreshowId=${foreshowId}"
        timerLog.info("multipartUpload 开始上传 foreshowId >> ${foreshowId} 文件名 >> ${filePath}")
        if (!fileId) {
            result = QcloudLiveCommon.multipartUpload(secretId, secretKey, filePath)
            if (result){
                liveQcloudRedis.hsetVideoMergeRedis(foreshowId, "fileId", result)
            }
        }
        timerLog.info("multipartUpload 上传结束 foreshowId >> ${foreshowId} 本地路径 >> ${filePath} fileID >> ${result} 回调地址 >> ${realURL}")
        if (result){
            Thread.sleep(10*1000)
            String msg = QcloudLiveCommon.convertVodFile(secretId, secretKey,result,realURL)
            timerLog.info("multipartUpload 提交转码 foreshowId >> ${foreshowId} fileID >> ${result} 提交结果 >> ${msg}")
        }else {
            if (max>0){
                max--
                timerLog.info("multipartUpload 上传重试 还有 ${max} 次机会")
                multipartUploadVodFile(fileName, foreshowId,max,appId)
            }
            timerLog.info("multipartUpload 重试3次上传失败")
        }
    }

    @Override
    String concatVideo(long foreshowId,List<String> fileIds, String name, List<String> dstType,String appId) {
        def qcloudInfo = qcloudLiveService.getQcloudInfo(appId)
        if(!qcloudInfo){
            log.info("concatVideo qcloudInfo is null,appId=>{}",appId)
            return null
        }
        String secretId = qcloudInfo.secretId
        String secretKey = qcloudInfo.secretKey
        def result = QcloudLiveCommon.concatVideo(secretId, secretKey, fileIds, name, dstType)
        JSONObject jsonObject = JSON.parse(result)
        if (jsonObject.code==0){
            liveQcloudRedis.hsetVideoMergeRedis(jsonObject.vodTaskId, "foreshowId", foreshowId as String)
            def id = liveQcloudRedis.hgetVideoMergeRedis(jsonObject.vodTaskId as String, "foreshowId")
            timerLog.info("concatVideo 合并任务 任务标识 >> ${jsonObject.vodTaskId} 预告ID >> ${id} 开始时间 >> ${DateUtil.getTodayDate(DateUtil.FULL_DATE_PATTERN)}")

            def liveEvent = [
                taskId:jsonObject.vodTaskId,
                appId:appId
            ]
            qcloudLiveRes.addLiveEvent(liveEvent)
        }
        return result
    }

}
