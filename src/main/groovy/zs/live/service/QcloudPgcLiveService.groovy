package zs.live.service

import zs.live.entity.LiveRecord

/**
 * Created by Administrator on 2016/12/15.
 */
interface QcloudPgcLiveService {

    /**
     * 填充直播需要请求腾讯云的参数
     * @param params
     * @param liveId
     * @param roomId
     * @return
     */
    Map fillPgcQcloudParams(Map params,long liveId,int roomId)
    /**
     * 更新腾讯云回看地址
     * @param liveId
     */
    String updateBackVideoAddressByQcloud(LiveRecord live)

    /**
     * 删除腾讯云频道
     */
    void stopQcloudChannel(LiveRecord liveRecord,String msg)

    /**
     * 启动腾讯云频道
     * @param live
     */

    void startQcloudChannel(LiveRecord live)


    /**
     * 暂停腾讯云频道
     * @param live
     */

    void pauseQcloudChannel(LiveRecord live)

    /**
     * 获取频道id
     */
    String getStreamId(int bizid,long liveId, long roomId)

    /**
     * 开启推流（直播码模式）
     * @param live
     * @return
     */
    def startLiveChannel(LiveRecord live)

    /**
     * 关闭推流（直播码模式）
     * @param live
     * @return
     */
    def stopLiveChannel(LiveRecord live)
}
