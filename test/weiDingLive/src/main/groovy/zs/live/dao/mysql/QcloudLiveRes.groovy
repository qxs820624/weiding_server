package zs.live.dao.mysql

import com.qcloud.Module.Live
import zs.live.entity.LiveRecord

/**
 * Created by Administrator on 2016/10/13.
 */
interface QcloudLiveRes {

    /**
     * 创建直播
     * @param liveRecord
     * @return
     */
    boolean createLiveRecord(LiveRecord liveRecord)
    /**
     * 更新直播信息表
     * @param updateParam
     * @return
     */
    boolean updateLiveRecord(Map updateParam);

    /**
     * 更新会议直播相关信息(直播中)
     * @param updateParam
     * @return
     */
    boolean updateLiveRecordForPgc(Map updateParam);

    /**
     * 更新会议直播相关信息(回放)
     * @param updateParam
     * @return
     */
    boolean updateLiveRecordLogForPgc(Map updateParam);

    /**
     * 更新会议直播时，如果已经创建了预告，同步修改预告表的begin_time
     * @param updateParam
     * @return
     */
    boolean updateLiveRecordForPgcRelateForeshow(Map updateParam);

    /**
     * 获取roomId
     * @param userId
     * @param appId
     * @return
     */
    int getRoomIdByUserIdAndAppId(long userId,String appId)

    /**
     * roomId入库
     * @param userId
     * @param appId
     * @param roomId
     */
    boolean insertRoomId(long userId,String appId, int roomId)

    /**
     * 更新历史记录
     * @param liveRecord
     * @return
     */
    boolean insertLiveRecordLog(LiveRecord liveRecord,Map liveRecordMap)
    /**
     * 删除直播记录
     * @param liveId
     * @return
     */
    boolean deleteLiveRecord(long liveId)

    /**
     * 获取roomId 是否有正在直播的liveId
     */
    int getLiveIdByRoomId(int roomId);

    /**
     * 根据roomid获取最后一条直播liveid
     */
    int getLiveIdByRoomIdFromLog(int roomId);

    /**
     * 更新直播贴回看地址
     * @param videoAddress
     * @param liveId
     * @return
     */
    int updateLiveRecordVideoAddress(String videoAddress ,long liveId,String fileId);

    /**
     * 获取当天直播记录中没有 回看信息的列表
     */
    List<LiveRecord> getNoBackVedioLiveRecordListToday();

    /**
     * 获取会议直播没有回看的直播
     */
    List<LiveRecord> getPgcNoBackVedioLiveRecordList();

    /**
     * 获取没有回看的直播
     */
    List<LiveRecord> getQcloudNoBackVedioLiveRecordList()

    /**
     * 腾讯云回调数据入库
     * @param params
     * @return
     */
    def addQcloudMsg(Map params);

    /**
     * 保存第三方直播的合作者id
     * @param liveId
     * @param partnerId
     * @return
     */
    boolean savePartner(long liveId, String partnerId)

    /**
     * 获取会议直播评论列表
     * @param params
     * @return
     */
    def findQcloudMsgList(Map params)
    /**
     * 查询出某个直播中真实用户的pv
     * @param params
     * @return
     */
    long findQcloudJoinRoomUserList(Map params)
    /**
     * 获取互动直播回放的评论列表
     * @param params
     * @return
     */
    def findQLiveRecordMsgList(Map params)
    /**
     * 删除直播的某条评论
     * @param time
     * @param liveId
     * @return
     */
    boolean deleteLiveComment(long time, long liveId)
    /**
     * 保存付费的扩展字段
     * @param liveId
     * @param payMap 付费直播的参数
     * @return
     */
    def addLiveRecordPayExtend(long liveId,Map payMap)

    /**
     * 根据liveId或者foreshowId删除付费扩展表中的记录
     * @param liveId
     * @param type
     * @return
     */
    def delLiveRecordPayExtend(long liveId,int type)
    /**
     * 更新付费的扩展字段
     * @param liveId
     * @param payMap
     * @return
     */
    def updateLiveRecordPayExtend(long liveId,Map payMap)
    /**
     * 根据liveId获取直播扩展信息
     * @param liveId
     * @return
     */
    def findLiveRecordPayExtendByLiveId(long liveId,int type)

    /**
     * 根据appId查询腾讯云信息
     *
     */
    def getQcloudInfo(String appId);

    /**
     * 获取所有腾讯云配置数据可用的appId
     * @return
     */
    def getAllQcloudInfo()

    /**
     * 保存腾讯事件通知
     * @param liveEvent
     * @return
     */
    def addLiveEvent(Map liveEvent)

    /**
     * 获取单条事件通知
     * @param taskId
     * @return
     */
    def getLiveEventByTaskId(String taskId)
}
