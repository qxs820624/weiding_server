package zs.live.service

/**
 * 视频下载，与直播无关
 */
interface VideoGatherService {

    /**
     * 文件下载
     * @param files
     */
    List<String> downloadFiles(List<String> files)

    /**
     * 文件上传
     * @param fileName
     */
    void multipartUpload(String fileName,Map params)

    /**
     * 提交给CMS视频信息
     * @param fileId
     * @param params
     * @return
     */
    def dealUploadFile(String fileId,Map params)

    /**
     * 回调CMS
     * @param params
     * @return
     */
    def uploadCallBack(Map params)
}
