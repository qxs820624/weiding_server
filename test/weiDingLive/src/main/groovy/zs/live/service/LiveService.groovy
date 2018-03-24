package zs.live.service

import zs.live.common.LiveCommon
import zs.live.entity.LiveRecord

import javax.servlet.http.HttpServletRequest

/**
 * Created by Administrator on 2016/7/25.
 */
interface LiveService {
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
    int getMyFansCount(long userId, int fansType, int followType, long lastId,String appId);

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
    int getMyFollowingsCount(long fansUserId, int fansType, int followType, long lastId,String appId);

    /**
     * 添加关注
     * @param fromUserId 动作发起方
     * @param toUserId   动作目标方
     * @param fansType  关注类型1:直播
     * @param appId
     * @return
     */
    int addFollow(long fromUserId, long toUserId, int fansType,String appId);

    /**
     * 取消关注
     * @param fromUserId 动作发起方
     * @param toUserId 动作目标方
     * @param fansType 关注类型1:直播
     * @param appId
     * @return
     */
    int cancelFollow(long fromUserId, long toUserId, int fansType,String appId);

    /**
     * 判断是否关注某人
     * @param fromUserId  动作发起方
     * @param toUserId 动作目标方
     * @param fansType 关注类型1:直播
     * @param appId
     * @return
     */
    def isFollow(long fromUserId, long toUserId, int fansType,String appId);

    /**
     * 根据liveId查询直播记录
     * @param liveId
     * @return
     */
    LiveRecord findLiveByLiveId(long liveId)

    /**
     * 根据liveId更新redis中的直播记录
     * @param liveId
     * @return
     */
    LiveRecord updateLiveRedisByLiveId(long liveId)

    /**
     * 根据userId获取用户信息
     * @param userId
     * @return
     */
    Map getUserInfo(Long userId)

    /**
     * 获取直播最新列表
     * @param map
     * @return
     */
    def findNewsLiveList(Map map)


    /**
     * 获取颜值直播分页列表
     * @param map
     * @return
     */
    def findFaceLiveList(Map map)
    /**
     * 通过预告id获取预告绑定的正在直播中的直播信息
     * @param userId
     * @param foreshowId
     * @param appId
     * @return
     */
    LiveRecord getLiveByForeshow( long foreshowId, String appId)
    /**
     * 获取回看地址
     * @param videoAddress
     * @param mapData
     * @return
     */
    def getVideoAddressUrlList(String videoAddress,Map mapData)
    /**
     * 获取回看历史列表
     * @param map
     * @return
     */
    def findLiveRecordList(Map map)
    /**
     * 获取我的回看历史列表
     * @param map
     * @return
     */
    def findMyLiveRecordList(Map map)
    /**
     * 获取我的回放列表，有预告的直播展示一条记录
     * @param map
     * @return
     */
    def findMyLiveRecordListNew(Map map)

    /**
     * 根据name得到相应的配置信息
     * @param appId
     * @param name
     * @return
     */
    def findLiveConfig(String appId, int configId)
    /**
     * 获取直播预告列表
     * @return
     */
    def findLiveForeshowList(Map map)

    /**
     * 获取未开始的直播预告列表
     * @return
     */
    def findNotStartedLiveForeshowList(Map map)

    /***
     * 对直播间中的用户进行校验
     * @param map
     * @return
     */
    int validateFans(Map map)
    /**
     * 通过腾讯云验证用户是否在直播间
     * @param userId
     * @param roomId
     * @param appId
     * @return
     */
    boolean validateMemberInGroup(long userId,int roomId,String appId)
    /***
     * 对直播回放记录进行排序
     * @param map
     * @return
     */
    int sortLiveRecord(Map map)

    /***
     * 对预约记录进行排序
     * @param map
     * @return
     */
    int sortForeshow(Map map)

    /***
     * 对预约记录进行排序
     * @param map
     * @return
     */
    int sortForeshowChild(Map map)

    /**
     * 用户预约直播
     * @param map
     * @return
     */
    int addAppointment(Map map)

    /**
     * 用户取消预约直播
     * @param map
     * @return
     */
    int cancelAppointment(Map map)

    /**
     * 根据liveId查询直播回放记录
     * @param liveId
     * @return
     */
    LiveRecord findLiveRecordByLiveId(long liveId)

    /**
     * 根据foreshowId查询直播回放记录
     * @param foreshowId
     * @return
     */
    LiveRecord findLiveRecordByForeshowId(long foreshowId)

    /**
     * 删除回放(用户删除回放、管理员删除回放)
     * @param map
     * @return
     */
    int delLiveRecord(Map map)

    /**
     * 删除预约
     * @param map
     * @return
     */
    int delForeshow(Map map)

    /**
     * 获取直播回看的信息
     * @param map
     * @return
     */
    def getLiveRecordInfo(Map map)
    /**
     * 获取预告跳转信息
     * @param foreshowId
     * @param appId
     * @return
     */
    def getLiveForeshowSkipInfo(long foreshowId, long userId, String appId,String vc)
    /**
     * 验证用户是否开启直播预约
     * @param userId
     * @param foreshowId
     * @param appId
     * @return
     */
    int validateIsOpenRemind(long userId, long foreshowId, String appId)

    /**
     * 获取开启直播预告提醒的用户列表
     * @param foreshowId
     * @param lastId
     * @param appId
     * @param psize
     * @return
     */
    def findaAppointmentUserList(long foreshowId, int lastId, String appId, int psize)

    /**
     * 结束直播预告
     * @param foreshowId
     * @return
     */
    int updataLiveForeshowStatus(long foreshowId,String appId)

    /**
     * 根据liveId分页获取观众信息
     * @param liveId
     * @param lastUserId
     * @param psize
     * @return
     */
    def findWatchList(long liveId,String lastUserId,int psize)

    /**
     * 根据liveid获取直播热度（点赞数）
     * @param liveId
     * @return
     */
    def getLivePraise(long liveId)

    /**
     * 根据liveid获取直播观看人数
     * @param liveId
     * @return
     */
    def getLiveWatherCount(long liveId)

    /**
     * 根据liveid获取回放观看人数
     * @param liveId
     * @return
     */
    def getLiveWatherTotalCount(long liveId)

    /**
     * 停止直播页返回数据
     * @param liveId
     * @param userId
     * @param toUserId
     * @param appId
     * @return
     */
    def getStopPageInfo(long liveId,long userId,long toUserId,String appId)

    /**
     * 整合官方直播的回放信息到预告表中
     * @param foreshowId
     * @param appId
     * @return
     */
    def updateForeshowMergeInfo(long foreshowId,String appId)

    def updatePgcForeshowMergeInfo(long foreshowId,String appId);

    /**
     * 更新预告状态
     */
    def updateForeshowStatus(long foreshowId, int foreshowStatus);
    /**
     * 更新预告中合并之后的视频地址
     * @param foreshowId
     * @param url
     * @param appId
     * @return
     */
    def updateForeshowMergeVideoAddressInfo(long foreshowId, String url ,int timeSpan,String appId)
    /**
     * 通知搜悦增加直播帖子
     * @param liveId
     * @return
     */
    def addBlog(long liveId)

    /**
     * 获取我的直播提醒
     * @param userId
     * @param appId
     * @param version app版本
     * @return
     */
    def getMyLiveNotice(long userId,String appId ,String version)

    /**
     * 获取直播分类信息列表
     * @return
     */
    def getLiveCategroyList(Long userId,String appId)

    /**
     * 获取直播列表（价值）
     * @param map
     * @return
     */
    def getValueLiveList(Map map)
    /**
     * 查询直播推荐数据
     * @param map
     * @return
     */
    def getRecommendLiveList(map)
    /**
     * 根据srpId获取直播列表
     */
    def getValueLiveListBySrp(Map map)
    def getLiveListForSmallProgram(Map map)
    /**
     * 禁言
     * @param userId
     * @param forbidUserId
     * @param roomId
     * @param liveId
     * @param appId
     * @return
     */
    def forbidCommentToGroupMember(long userId,long forbidUserId,int roomId,long liveId,String appId)
    /**
     * 踢人
     * @param userId
     * @param kickUserId
     * @param roomId
     * @param liveId
     * @param appId
     * @return
     */
    def kickGroupMember(long userId, long kickUserId, int roomId, long liveId, String appId)

    /**
     * 手机端用户关注/解除关注直播系列
     * @param userId
     * @param foreshowId
     * @param operType 1:关注 2:解除关注
     * @return
     */
    def updateUserForeshow(long userId, long foreshowId, int operType,String appId);

    /**
     * 手机端用户关注系列列表
     * @param userId
     * @param psize
     * @param sortInfo
     * @return
     */
    def listUserForeshow(long userId, int psize, String sortInfo,String appId,String vc);

    /**
     * 取出一条关注
     * @param userId
     * @param forshowId
     * @return
     */
    def getUserForeshow(long userId,long forshowId,String appId);

    /**
     * 是否预约
     * @param userId
     * @param foreshowId
     * @param appId
     * @return
     */
    boolean isOpenRemind(long userId,long foreshowId,String appId)

    /**
     * 取出一个用户的预约列表
     * @param userId
     * @param foreshowId
     * @param appId
     * @return
     */
    List<Long> listOpenRemind(long userId,String appId)

    /**
     * 转换成直播对象
     * @param obj  //数据库查出来的groovy对象
     * @return
     */
    LiveRecord toLiveRecord(def obj)

    /**
     * 将到开始时间的未开始直播状态改为开始，定时任务执行
     * @return
     */
    def updateLivePgcStatusBegin()
    /**
     * 根據liveId更新直播表中的foreshow_id字段，并刪除該直播緩存
     * @param liveId
     * @param foreshowId
     * @param appId
     * @return
     */
    def updateForeshowIdByLiveId(long liveId,long foreshowId,String appId)

    /**
     * 根据配置参数取不同的播放地址
     * @param liveRecord
     * @return
     */
    def formatPlayUrl(LiveRecord liveRecord,String vc,def params)

    /**
     * 根据配置参数取不同的m3u8地址
     * @param liveRecord
     * @return
     */
    def formatM3u8Url(LiveRecord liveRecord,String vc)
    /**
     * 将到开始时间的未开始直播预告状态改为开始，定时任务执行
     * @return
     */
    def updateForeshowStatusBegin()

    /**
     * 直播开始时向粉丝推消息
     * @param paramList json
         [{
         "category":"meeting",// foreshow,live,meeting
         "operType":1,
         "userId":123,
         "srpId":"abc",
         "title":"abc",
         "liveId":123,
         "foreshowId":123,
         "appId":"souyue",
         "imgUrl":"http://sns-img.."
         }]
     * @return
     */
    def pushLiveStartMsg(List<Map<String,Object>> paramList)

    /**
     * 删除腾讯云的视频文件，并进行修改数据库的状态值
     * @param fileId
     * @return
     */
    def deleteRecodeLog(String fileId)

    /**
     * 获取价值直播轮播图
     * @return
     */
    def getRollImgList(Map map)

    /**
     * 查看用户正在进行的直播
     * @param userId 当前用户
     * @param toUserId 要查看的用户
     * @param appId
     * @return
     */
    def findUserLive(long userId,long toUserId,String appId)

    /**
     * 根据appId查询直播人数限制
     * @param appId
     * @return
     */
    def valateLiveLimit(String appId)

    /**
     * 真实观众进出房间时根据appId设置直播人数
     * @param liveId
     * @param count
     * @return
     */
    def addAppLimitInfo(long liveId,int count)

    /**
     * 我购买的付费直播
     * @param userId
     * @param appId
     * @return
     */
    def getUserPaidList(long userId,String appId,int pageSize,String sortInfo)

    /**
     * 根据用户id获取直播商品列表
     * @return
     */
    def getLiveGoodsList(Map map)

    /**
     * 根据预告查询appid
     * @param params
     * @return
     */
    def getAppIdsByForeshow(Map params)

    /**
     * 查询以上上线的所有appId
     * @return
     */
    def getAppIdsFromConfig()

    /**
     * 查询某srpId下有多少个预告
     * @return
     */
    def getForeshowNumberForSrpId(String srpId);
    /**
     * 根据关键词匹配标题，查询直播列表
     * @param keyword
     * @param appId
     * @return
     */
    def searchLiveListByKeyWord(long userId, String keyword,int psize,def sortInfo,String appId,String vc)

    /**
     * 用户直播权限校验
     * @param userId
     * @param appId
     * @return
     */
    def checkLiveAccessByUserId(long userId,String appId)

    /***
     * 通知第三方同步关注数据
     * @param map
     * @return
     */
    def fansSyncData(def map)

    /**
     * 请求腾讯云，获取直播的截图
     * @param map
     * @return
     */
    def getSnapshotUrl(Map map)

    /**
     * 根据liveId更新直播回看的截图
     * @param liveId
     * @param data
     * @return
     */
    def updateLiveThumpByLiveId(long liveId, def data)

    /**
     * 校验预告表中回放地址的json（修复数据使用）
     * @return
     */
    def scanForeshowDataForMistake()

    /**
     * 直播收藏
     * @param userId
     * @param foreshowId
     * @param type
     * @return
     */
    def collectLive(long userId, long foreshowId, int type,String appId)
    /**
     * 获取直播收藏列表
     * @param userId
     * @param appId
     * @return
     */
    def getLiveCollectionListByUserId(Map map, String appId)
    /**
     * 返回是否收藏了直播
     * @param userId
     * @param foreshowId
     * @param appId
     * @return
     */
    boolean isCollectLive(long userId,long foreshowId,String appId)

    /**
     * 对某个直播进行推荐或取消推荐
     * @param type
     * @param forehsowId
     * @param liveId
     * @param operType
     * @return
     */
    int updateLiveRecommend(int type, long forehsowId, long liveId,int operType)

    /**
     *根据预告id获取预告详情
     * @param foreshowId
     * @param map
     * @return
     */
    def getForehsowInfoByForeshowId(long foreshowId,Map map)

    /**
     * 根据appid获取中文包名
     * @param appId
     * @return
     */
    def getAppName(String appId)
    def payOrder(Map map)
}
