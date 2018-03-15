package zs.live.service

/**
 * Created by zhougc  on 2017/4/7.
 * @Description: 马甲用户相关操作
 */
interface VestUserService {
    /**
     * 将所有马甲用户放入缓存
     * @param vestList
     */
    void initAllVestUserInRedis(List vestList)
    /**
     * 从某个直播对应的redis中获取马甲信息
     * @param userId
     * @return
     */
    def getVestUserFromRedis(long liveId,long userId)
    /**
     * 从所有的马甲用户列表中获取几个马甲用户列表
     * @param userCount
     * @return
     */
    List getVestUserList(int userCount)
    /**
     * 设置某个直播的马甲
     * @param liveId
     * @param vestList
     */
    void setVestUserListByLiveId(long liveId, List vestList)
    /**
     * 获取针对某个直播的马甲头像列表
     * @param liveId
     * @param vestCount
     * @return
     */
    List getVestUserListByLiveId(long liveId, int vestCount)
    /**
     * 获取针对某个直播的马甲头像数的uv
     * @param liveId
     * @param vestCount
     * @return
     */
    long getVestUserCountByLiveId(long liveId)
    /**
     * 获得包括虚拟数和真实数的总马甲数的uv
     * @param liveId
     * @return
     */
    long getVestUserVirtualAndRealCountByLiveId(long liveId)
    /**
     * 获得虚拟数字的uv
     * @param liveId
     * @return
     */
    long getVestUserVirtualCountByLiveId(long liveId)
    /**
     * 删除马甲用户列表
     * @param liveId
     * @return
     */
    def delVestUserByLiveIdAndUserId (long liveId,def userInfo)
    /**
     * 删除互动直播下所有的马甲
     * @param liveId
     */
    void delAllVestUserByLiveId(long liveId)

    /**
     * 获取当前房间内的观众人数
     * @param liveId
     * @return
     */
    long getLiveRoomWatchCount(long liveId)

    /**
     * 处理用户头像
     * @param userImage
     * @return
     */
    String fillVestUserImage(String userImage)
    /**
     * 如果直播观众列表中的记录超过100个，则进行裁剪，只保留100个
     * @param liveId
     */
    void cutterLiveWatchList(long liveId)

}
