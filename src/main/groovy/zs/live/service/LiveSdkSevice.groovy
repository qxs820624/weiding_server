package zs.live.service

import zs.live.entity.LiveRecord

/**
 * Created by Administrator on 2017/4/28.
 */
interface LiveSdkSevice {

    /**
     * 创建直播
     * interface
     * @param param
     * @return
     */
    def create(Map param);

    /**
     * 推流开始
     * @param liveId
     * @param roomId
     * @param userId
     * @param appId
     * @return
     */
    Map start(long liveId,int roomId,long userId,String appId);

    /**
     * 结束直播
     * @param liveRecord
     * @param msg
     * @return
     */
    Map stop(long liveId,int roomId,long userId,String appId,String stopMsg)

    String getSyURL()
}
