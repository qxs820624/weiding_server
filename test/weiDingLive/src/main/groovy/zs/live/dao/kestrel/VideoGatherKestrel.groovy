package zs.live.dao.kestrel

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository

/**
 * 视频下载，与直播无关
 */
@Repository
class VideoGatherKestrel {

    @Autowired
    Kestrel kestrel

    @Value('${live.kestrel.video.gather}')
    String VIDEO_DOWNLOAD_KEY

    @Value('${live.kestrel.video.cms.gather}')
    String VIDEO_DOWNLOAD_CMS_KEY

    /**
     * 取kestrel 取待下载的视频
     * @return
     */
    String getGatherMsg(){
        return kestrel.getMsg(VIDEO_DOWNLOAD_KEY)
    }

    /**
     * 取kestrel 取待下载的视频
     * @return
     */
    String getCMSGatherMsg(){
        return kestrel.getMsg(VIDEO_DOWNLOAD_CMS_KEY)
    }

    /**
     * 发送失败的下载信息
     * @param gatherMsg
     * @return
     */
    def sendGatherMsg(String gatherMsg){
        kestrel.sendMsg(VIDEO_DOWNLOAD_KEY,gatherMsg)
    }

    /**
     * 发送失败的下载信息
     * @param cMSGatherMsg
     * @return
     */
    def sendCMSGatherMsg(String cMSGatherMsg){
        kestrel.sendMsg(VIDEO_DOWNLOAD_CMS_KEY,cMSGatherMsg)
    }
}
