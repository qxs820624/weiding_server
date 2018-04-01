package zs.live.dao.mysql

import groovy.sql.GroovyRowResult

/**
 * Created by Administrator on 2016/7/25.
 */
interface LiveRes {
    /**
     * 分页获取粉丝列表
     * @param userId  用户id
     * @param fansType  关注类型1:直播
     * @param followType  关注为1,互相关注为2 等于0是查询所有
     * @param lastId
     * @param pageSize
     * @param appId
     * @return
     */
    List getMyFans(long userId, int fansType, int followType, long lastId, int pageSize,String appId);

    /**
     * 获取粉丝数目
     * @param userId
     * @param fansType 关注类型1:直播
     * @param followType   关注为1,互相关注为2 等于0是查询所有
     * @param lastId
     * @param appId
     * @return
     */
    int getMyFansCount(long userId, int fansType, int followType,long lastId,String appId);

    /**
     * 分页获取我关注的用户列表
     * @param fansUserId 用户id
     * @param fansType   关注类型1:直播
     * @param followType 关注为1,互相关注为2 等于0是查询所有
     * @param lastId
     * @param pageSize
     * @param appId
     * @return
     */
    List getMyFollowings(long fansUserId, int fansType, int followType, long lastId, int pageSize,String appId);

    /**
     * 获取我关注的用户数目
     * @param fansUserId 用户id
     * @param fansType  关注类型1:直播
     * @param followType 关注为1,互相关注为2 等于0是查询所有
     * @param lastId
     * @param appId
     * @return
     */
    int getMyFollowingsCount(long fansUserId, int fansType, int followType,long lastId,String appId);

    /**
     * 添加关注
     * @param fromUserId 动作发起方
     * @param toUserId   动作目标方
     * @param fansType  关注类型1:直播
     * @param appId
     * @return
     */
    int addFollow(long fromUserId, long toUserId,int fansType,String appId);
    /**
     * 查询用户关注信息
     * @param fromUserId
     * @param toUserId
     * @param fansType
     * @param appId
     * @return
     */
    def getFollowInfoByUserId(long fromUserId, long toUserId,int fansType,String appId)

    /**
     * 取消关注
     * @param fromUserId 动作发起方
     * @param toUserId 动作目标方
     * @param fansType 关注类型1:直播
     * @param appId
     * @return
     */
    int cancelFollow(long fromUserId, long toUserId,int fansType,String appId);

    /**
     * 判断是否关注某人
     * @param fromUserId  动作发起方
     * @param toUserId 动作目标方
     * @param fansType 关注类型1:直播
     * @param appId
     * @return
     */
    def isFollow(long fromUserId, long toUserId,int fansType,String appId);
    /**
     * 获取直播的最新列表
     * @param map
     * @return
     */
    def findNewstLiveList(Map map);

    /**
     * 获取颜值直播的分页列表
     * @param map
     * @return
     */
    def findFaceLiveList(Map map);
    /**
     * 获取符合回看的直播预告列表
     * @param map
     * @return
     */
    def findLiveForeshowListForRecord(Map map)
    /**
     * 根据预告id获取预告信息
     * @param foreshowId
     * @param appId
     * @return
     */
    def findLiveForeshowByForeshowId(long foreshowId,String appId)
    /**
     * 根据预告id获取对应的所有历史回看列表
     * @param map
     * @return
     */
    def findLiveRecordListByForeId(Map map)
    /**
     * 获取我的直播回看历史列表
     * @param map
     * @return
     */
    def findMyLiveRecordList(Map map)
    /**
     * 获取我的直播回看历史列表,只有颜值直播的列表
     * @param map
     * @return
     */
    def findMyLiveRecordListForUgc(Map map)
    /**
     * 获取我的直播回放列表，将预告合并为一条记录进行展示
     * @param map
     * @return
     */
    def findMyLiveRecordListNew(Map map)
    /**
     * 根据liveId查询直播记录
     * @param liveId
     * @return
     */
    GroovyRowResult findLiveByLiveId(long liveId)
    /**
     * 根据预告id获取预告绑定的正在直播中的直播信息
     * @param foreshowId
     * @param appId
     * @return
     */
    def findLiveByForeshowId(long foreshowId, String appId)
    /**
     * 获取直播列表标签配置信息
     * @param appId
     * @param name
     * @return
     */
    def fingLiveConfig(String appId,int configId)
    def addLiveConfig(Map map)

    /**
     * 获取直播回看配置信息
     */
    def findLiveBackVedioConfig(String appId);
    /**
     * 获取直播预告列表
     * @return
     */
    def getLiveForeshowListForLive(Map map)

    /**
     * 获取未开始的直播预告列表
     * @return
     */
    def getNotStartedLiveForeshowList(Map map)

    /**
     * 修改回看排序
     * @param liveId
     * @param sortNum
     * @param appId
     * @return
     */
    def updateLiveRecordSortNum(long liveId,int sortNum,String appId)
    /**
     * 修改预告排序
     * @param foreshowId
     * @param sortNum
     * @param appId
     * @return
     */
    def updateForeshowSortNum(long foreshowId,int sortNum,String appId)

    /**
     *
     */
    def updateForeshowChildSortNum(long foreshowId,int sortNum,String appId)

    /**
     * 新增预约
     * @param foreshowId
     * @param userId
     * @param appId
     * @return
     */
    def addAppointment(long foreshowId,long userId,String appId)

    /**
     * 删除预约
     * @param foreshowId
     * @param userId
     * @param appId
     * @return
     */
    def delAppointment(long foreshowId,long userId,String appId)


    /**
     * 根据liveId查询直播回放记录
     * @param liveId
     * @return
     */
    GroovyRowResult findLiveRecordByLiveId(long liveId)

    /**
    * 根据foreshowId查询直播回放记录
    * @param foreshowId
    * @return
    */
    GroovyRowResult  findLiveRecordByForeshowId(long foreshowId)

    /**
     * 根据liveId删除直播回放
     * @param liveId
     * @param liveStatus
     * @param appId
     * @return
     */
    def delLiveRecordByLiveId(long liveId,int liveStatus,String appId)

    /**
     * 根据预告id删除直播回放
     * @param foreshowId
     * @param liveStatus
     * @param appId
     * @return
     */
    def delLiveRecordByForeshowId(long foreshowId,int liveStatus,String appId)

    /**
     * 删除直播预约
     * @param foreshowId
     * @param appId
     * @return
     */
    def delForeshow(long foreshowId,String appId)

    /**
     * 预告删除时，需更新直播表、直播回放表中预告id为0 且直播类型变更为非官方
     * @param foreshowId
     * @param appId
     * @return
     */
    def updateLiveForeshowId(long foreshowId,String appId)
    /**
     * 验证用户是否开启预约提醒
     * @param userId
     * @param foreId
     * @return
     */
    int isOpenRemind(String appId, long userId, long foreshowId)
    /**
     * 取出用户的预约列表
     * @param userId
     * @param foreId
     * @return
     */
    List<Long> listOpenRemind(String appId, long userId)
    /**
     * 根据预告id获取开启提醒的用户列表
     * @param foreshowId
     * @param lastId
     * @param appId
     * @return
     */
    def findAppointmentUserlist(long foreshowId, int lastId, String appId, int psize)
    /**
     * 根据预告id获取正在直播中的直播
     * @return
     */
    def findLiveRecordByForeId(long foreshowId,String appId)
    /**
     * 更新直播预告状态值为结束
     * @param foreshowId
     * @param appId
     * @return
     */
    def updateLiveForeshowStatus(long foreshowId,int status,String appId)
    /**
     * 预告结束后，更新预告表中的回看信息
     * @param liveRecordInfo
     * @param foreshowId
     * @param appId
     * @return
     */
    def updateLiveForeshowLiveRecordInfo(def liveRecordInfo,int foreshowStatus,long foreshowId,String appId)
    /**
     * 获取所有的马甲用户列表
     * @return
     */
    def findAllVestUserList()
    /**
     * 获取马甲每次入群人数的配置信息
     * @param liveId
     * @param appId
     * @return
     */
    def findVestCountConfigInfo(long liveId, String name,String appId)
    /**
     * 获取马甲每分钟入群数量的全局配置
     * @param appId
     * @return
     */
    def findVestCountGlobalConfigInfo(String name,String appId)

    /**
     * 查询所有回放的fileId
     */
    def findBackVedioAddress();

    /**
     * 获取我的直播提醒
     * @param map
     * @return
     */
    def getMyLiveNotice(Map map)

    /**
     * 查询直播中预告列表
     * @param map
     * @return
     */
    def findLivingForeshowList(Map map)

    /**
     * 查询暂停中预告列表
     * @param map
     * @return
     */
    def findPausedForeshowList(Map map)

    /**
     * 查询直播预告列表
     * @param map
     * @return
     */
    def findForeshowList(Map map)

    /**
     * 查询回放中预告列表
     * @param map
     * @return
     */
    def findOverForeshowList(Map map)

    /**
     * 查询直播类别列表
     * @param map
     * @return
     */
    def findLiveCategroyList(Map map)

    /**
     * 添加 用户关注直播系列/预告
     * @param userId
     * @param foreshowId
     * @return
     */
    def addUserForeshow(long userId, long foreshowId,String appId)

    /**
     * 解除用户关注直播系列/预告
     * @param userId
     * @param foreshowId
     * @return
     */
    def removeUserForeshow(long userId, long foreshowId,String appId)

    /**
     * 用户关注直播系列列表
     * @param userId
     * @param foreshowType 1互动直播 2会议直播，5直播系列
     * @param psize
     * @param sortInfo
     * @return
     */
    def listUserForeshow(long userId,int foreshowType, int psize, String sortInfo,String appId)
    /**
     *
     * @param userId
     * @param foreshowId
     * @return
     */
    def getUserForeshow(long userId,long foreshowId,String appId)

    /**
     * 获取价值直播系列最新一条
     * @param foreshowId
     * @return
     */
    def findLastForeShow(long foreshowId,int foreshowStatus,String vc)

    /**
     * 根据liveId更新foreshow_id字段
     * @param liveId
     * @param foreshowId
     * @param appId
     * @return
     */
    def updateForeshowIdByLiveId(long liveId,long foreshowId, String appId)

    /**
     * 修改场控信息（追加的方式）
     * @param params
     * @return
     */
    def updateFieldControlByAdd(Map params)

    /**
     * 修改场控信息（整体修改的方式）
     * @param params
     * @return
     */
    def updateFieldControlByAll(Map params)
    /**
     * 获取马甲评论的列表
     * @return
     */
    def findVestCommentList(String appId)

    /**
     * 直播回看
     * @param foreshowId
     * @param foreshowStatus
     * @return
     */
    def updateForeshowStatus(long foreshowId,int foreshowStatus);
    /**
     * 将到开始时间的未开始预告状态改为开始，定时任务执行
     * @return
     */
    def updateForeshowStatusBegin()

    /**
     * 获取已经结束的会议直播列表
     */
    def findStopedPgcList();

    /**
     * 根据liveId修改直播状态
     * @param liveId
     * @param status
     * @return
     */
    def updateLivePgcStatusByLiveId(long liveId,int pgcStatus)
    /***
     * 得到当前时间点以前没有开始的直播记录
     * @return
     */
    def getNotBeginLiveRecordsByTime()
    /**
     * 查询当前时间点，已经开始了但状态还未改为开始的直播预告
     * @return
     */
    def findNotBeginForeshowListByTime()
    /**
     * 将数据库中字段转为bean对象
     * @param obj
     * @return
     */
    def toForeshowBean(def obj)

    /**
     * 根据fileId查询出直播回看信息
     * @param fileId
     * @return
     */
    def findLiveRecordLogByFileId(String fileId)
    /**
     * 查询出已结束的预告列表（视频合并使用）
     * @return
     */
    List findForeshowIdListForMerge()

    /**
     * 查询出指定时间的已结束的预告列表（视频合并校验使用）
     * @return
     */
    List findForeshowIdListForMergeValidate()

    /**
     * 获取轮播数据
     * @return
     */
    def getRollImgList(String appId)

    /**
     * 根据id获取预告
     * @param foreshowId
     * @return
     */
    def getForeshowById(long foreshowId)

    /**
     * 测试环境中所有的未删除的直播数据
     * @return
     */
    def findTestLiveRecordList()
    /**
     * 正式环境所有未删除的直播数据
     * @return
     */
    def findZstestLiveRecordList()

    /**
     * 获取所有正在直播和直播已结束的直播列表，修复人数缓存
     * @return
     */
    def findLiveRecordListForUpdateCountRedis()
    /**
     * 根据sql语句查询出数据列表
     * @param sql
     * @return
     */
    def findDataListBySql(String sql)

    /**
     * 查找用户正在进行的直播
     * @param appId
     * @param toUserId 要查询的用户
     * @param isFollow 当前用户是否已经关注
     * @return
     */
    def getUserLiveRecord(String appId, long toUserId, boolean isFollow);

    /**
     * 维护预告和圈子对应关系表
     * @param foreshowId
     * @param liveId
     * @param parentId
     * @param interestList
     * @param srpList
     * @return
     */
    def saveForeshowSrpRelation(long foreshowId, long liveId, long parentId, List interestList, List srpList)

    /**
     * 根据ID获取预告
     * @param ids
     * @return
     */
    def getListByIds(List ids)

    /**
     * 根据预告查询appid
     * @param params
     * @return
     */
    def getAppIdsByForeshow(Map params)

    /**
     * 查询live_app_config表中所有有效的appId
     * @return
     */
    def getAppIdsFromConfig()


    /**
     * 查询某srpId下有多少个预告
     * @return
     */
    def getForeshowNumberForSrpId(String srpId);

    /**
     * 根据关键词匹配直播标题，查询直播列表
     * @param keyword
     * @param appId
     * @return
     */
    def searchLiveListByKeyword(Map map)
    /**
     * 更新互动直播对应的截图
     * @param liveId
     * @param liveThump
     * @return
     */
    def updateLiveThumpByLiveId(long liveId, String liveThump)

    /**
     * 更新直播统计信息
     * @param liveId
     * @param statisticsInfo
     * @return
     */
    def updateStatisticsInfoByLiveId(long liveId, String statisticsInfo)
    /**
     * 校验预告表中回放地址的json（修复数据使用）
     * @return
     */
    def getAllForeshowList()

    /**
     * 获取收藏直播记录
     * @param userId
     * @param forehsowId
     * @param appId
     * @return
     */
    def findCollectLiveByuserIdAndForeshowId(long userId, long forehsowId,String appId)
    /**
     * 收藏/取消收藏直播
     * @param userId
     * @param foreshowId
     * @param type
     * @param appId
     * @return
     */
    def collectLive(long userId ,long foreshowId,String appId)
    def updateCollectLive(long userId ,long foreshowId,int status,String appId)
    /**
     * 获取直播收藏列表
     * @param userId
     * @param appId
     * @return
     */
    def findLiveCollectionListByUserId(Map map, String appId)

    /**
     * 根据foreshowId删除收藏记录
     * @param foreshowId
     * @param appId
     * @return
     */
    def delCollectionByForeshowId(long foreshowId)

    def updateLiveRecommendStatus(String tableName,long foreshowId, long liveId,int status,int operType)

    /**
     * 根据appId、类型获取共享的直播或预告
     * @param appId
     * @param type
     * @return
     */
    def findShareLiveByAppId(String appId,Integer type)

    /**
     * 查询出直播中的颜值和价值直播，发短信通知运营人员
     * @return
     */
    List findNoticeLive()

    /**
     * 查询需要发信息通知的手机号码
     * @return
     */
    List findNoticeMobie()

    /**
     * 生成购买直播的订单
     * @param map
     * @return
     */
    def addPayOrder(Map map)

    /**
     * 获取直播支付结果
     * @param map
     * @return
     */
    def getLivePayOrderInfo(Map map)
}


