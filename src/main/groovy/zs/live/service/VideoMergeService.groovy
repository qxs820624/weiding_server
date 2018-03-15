package zs.live.service

/**
 * Created by Administrator on 2016/12/5 0005.
 */
interface VideoMergeService {

    /**
     * 合并视频
     * @param files 待合并视频下载地址
     * @param foreshowId 回调传回的id
     * @return
     */
    public String merge(List<String> files,long foreshowId,String appId)

    /**
     * 视频重新拉取
     * @param url
     * @param foreshowId
     */
    public void reTryUploadVideo(String url, long foreshowId)

    /**
     * 分段上传
     * @param fileName
     * @param foreshowId
     * @param max 重试次数
     * @return
     */
    public String multipartUploadVodFile(String fileName, long foreshowId,int max,String appId)

    /**
     * 腾讯云视频合并
     * @param foreshowId
     * @param fileIds
     * @param name  预告标题
     * @param dstType mp4 m3u8
     * @param appId
     * @return
     */
    public String concatVideo(long foreshowId,List<String> fileIds,String name,List<String> dstType,String appId)
}
