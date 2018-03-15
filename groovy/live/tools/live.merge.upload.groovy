import com.qcloud.Common.VodDemo
import zs.live.ApiUtils
import zs.live.service.VideoMergeService
import zs.live.utils.Strings

/**
 * Created by Administrator on 2017/1/12 0012.
 */
ApiUtils.processNoEncry{

    String fileName = params.fileName
    long foreshowId = params.foreshowId as long
    String appId=Strings.getAppId(params)

    VideoMergeService videoMergeService = bean(VideoMergeService)
    videoMergeService.multipartUploadVodFile(fileName,foreshowId,3,appId)
}
