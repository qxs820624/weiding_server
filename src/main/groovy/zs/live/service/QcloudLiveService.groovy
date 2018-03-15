package zs.live.service

import zs.live.entity.LiveRecord


/**
 * Created by kang on 2016/10/11.
 * 互动直播跟腾讯云交互接口文档
 */
interface QcloudLiveService {

    /**
     * 获取signId
     * 因为是腾讯云的接口所以里面包一层异常 状态用map返回，不用项目本身的异常机制了
     * @return Map
     */
    String getSignId(String userId,String appId);

    /**
     * 强制获取去signId
     * 客户端发现signId不一致时 强制从腾讯云获取signId
     */
    String getSignIdFromQcloud(String userId,String appId);

    /**
     *创建直播
     */
    Map createLive(def param);

    /**
     * 开始直播
     */
    Map beginLive(long liveId,int roomId,long userId,String appId);

    /**
     * 停止直播
     * @param liveId
     * @return
     */
    Map stopLive(long liveId,int roomId,long userId,String appId)

    Map stopLiveComm(long liveId,int roomId,long userId,String appId,String stopMsg)

    /**
     * 强制结束直播，结束房间 根据userId he appId
     */
    Map dealLastErrorStop(long userId,String appId,String callEnv)
    /**
     * 直播心跳
     * @param liveId
     * @return
     */
    int sendHeadRate(long liveId,long timespan);

    /**
     * 礼物打赏发送IM消息
     * @param map
     * @return
     */
    String sendGiftImMsg(Map map)
    /**
     * 获取用户在群组中的身份信息
     * @param map
     * @return
     */
    String getGroupMemberInfo(Map map)
    /**
     * 发送马甲进入房间消息
     * @param map
     * @return
     */
    String sendVestIntoRoomMsg(Map map)
    /**
     * 发送如直播间的马甲人数的消息
     * @param map
     * @return
     */
    String sendVestCountMsg(Map map)
    /**
     * 马甲发送普通消息
     * @param map
     * @return
     */
    String sendVestMsg(Map map)
    /**
     * 将某个成员禁言
     * @param map
     * @return
     */
    def forbidComment(Map map)
    /**
     * 马甲各种操作下发消息
     * @param map
     * @return
     */
    String sendVestDoPrimeMsg(Map map)

    /**
     * 将某个成员踢出群聊
     * @param map
     * @return
     */
    def kickGroupMember(Map map)
    /**
     * 通用IM消息，暂时未启用
     * @param map
     * @return
     */
    String sendVestImMsg(Map map)
    /**
     * 回调调用结束直播
     * @param liveId
     * @param roomId
     * @param userId
     * @param appId
     * @param stopMsg
     */
    void stopByCallBy(long liveId,int roomId,long userId,String appId,String logmsg)
    /**
     * 删除腾讯云视频
     */
    def deleteVodFile(List<String> fileIds,String appId);
    /**
     * 提供更新mp4回看列表
     * msg ：标识调用位置
     */
    String getBackInfo(int roomId, long liveId,long foreshowId,String appId,String msg);

    /**
     * 获取roomId
     * @param userId
     * @param appId
     * @return
     */
    int getRoomId(long userId,String appId)
    /**
     * 生成直播
     * @param userId
     * @param appId
     * @return
     */
    int createRoom(long userId,String appId);
    /**
     * 会议直播发送暂停消息
     * @param map
     * @return
     */
    String sendPgcMsg(Map map)
    /**
     * 获取会议直播评论列表
     * @param map
     * @return
     */
    def findLiveCommentList(Map map)
    /**
     * 获取会议直播回放的评论列表
     * @param map
     * @return
     */
    def findLiveRecordCommentList(Map map)
    /**
     * 直播相关数据统计
     * @return
     */
    def sendLiveDataToStatistic(Map dateMap)

    /**
     * 根据appId获取直播的key和secret
     * 以及公钥、私钥、accountType、帐号名称、adkappid
     * @param appId
     * @return
     */
    def getQcloudInfo(String appId)

    /**
     * 根据appId获取直播首页设置信息
     * @param appId
     * @return
     */
    def getAppConfigSetInfo(String appId)
    /**
     * 删除腾讯云直播相关配置数据
     * @param appId
     * @return
     */
    def delQcloudInfo(String appId)

    /**
     * 获取所有腾讯云配置数据可用的appId
     * @return
     */
    def getAllQcloudInfo()

    def findLiveRecordPayInfo(long liveId, def userInfo,LiveRecord liveRecord,long inviter)
}
