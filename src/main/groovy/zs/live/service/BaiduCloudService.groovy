package zs.live.service

/**
 * Created by zhougc  on 2018/1/10.
 * @Description:
 */
interface BaiduCloudService {
    /**
     *
     * @param bucketName 要上传目录名称,测试：syvideotest,正式： syvideo
     * @param sourceFilePath 本地mp4绝对路径
     * @param targetFileName  上传后的访问名称。
     * @return  etage 上传成功后返回一个目标字符串
     * 上传成功后，访问路径为:测试：http://syvideotest.souyue.mobi/+targetFileName
     * 正式：http://syvideo.souyue.mobi/+targetFileName
     */
    String uploadFile(String bucketName,String sourceFilePath,String targetFileName)
    /**
     * 将mp4转化为m3u8
     * @param bucketName
     * @param sourceFileName  要转化的文件名，如 a.mp4
     * @param targetFileName  最终要转化的文件名，如： a.m3u8
     * @return  jobId  返回转化任务的jobId,转化需要一段时间
     */
    String mp4TranscodeM3u8(String bucketName,String sourceFileName, String targetFileName)
}