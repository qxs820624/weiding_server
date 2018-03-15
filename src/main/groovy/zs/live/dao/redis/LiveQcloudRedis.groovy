package zs.live.dao.redis

import com.mongodb.util.JSON
import groovy.util.logging.Slf4j
import io.netty.util.internal.StringUtil
import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import zs.live.entity.LiveForeshow
import zs.live.entity.LiveRecord
import zs.live.service.VestUserService
import zs.live.utils.DateUtil
import zs.live.utils.Parallel
import zs.live.utils.Strings

import java.util.List
import java.beans.VetoableChangeListener

/**
 * Created by kang on 2016/10/12.
 */
@Repository
@Slf4j
class LiveQcloudRedis {
    //马甲入群的最大人数，达到这个人数之后，不再发送马甲入群消息，只更新马甲入群的人数
    @Value('${max.vest.count}')
    int maxVestCount

    @Autowired
    LiveRedis liveRedis;
    @Autowired
    VestUserService vestUserService

    //生成signId key
    final static String LIVE_SIGN_ID_KEY = "live_sign_id_key_"
    //生成liveIdkey
    final static String LIVE_ID_KEY = "live_id_key_"
    //生成roomIdkey
    final static String LIVE_ROOM_ID_KEY = "live_room_id_key_"
    //用户房间对应关系 key
    final static String LIVE_ROOM_USER_KEY = "live_room_user_relation_key_"
    //正在直播列表 key
    final static String LIVE_START_BY_APP_KEY = "live_start_by_app_key_"
    //直播开始时间redis key
    final static String LIVE_BEGIN_KEY = "live_begin_key_"
    //直播心跳时间 redis key
    final static String LIVE_HEART_TIME_KEY = "live_heart_time_key_"
    //直播观众列表 redis key
    final static String LIVE_WATCHER_LIST_KEY = "live_watcher_list_key_"
    //直播观众信息 redis key
    final static String LIVE_WATCHER_KEY = "live_watcher_key_"
    //房间对应的直播（正在直播的key）
    final static String LIVE_ROOM_LIVE_ID_KEY = "live_room_live_id_key_"
    //房间观看总数（包含真实观众pv+马甲头像的pv+马甲数量的pv）
    final static String LIVE_WATHER_TOTAL_COUNT = "live_wather_total_count_"
    //马甲头像的PV(5.6版本及以上版本废弃)
    final static String LIVE_VEST_COUNT = "live_vest_count_"
    //马甲数量的pv（只增不减）(5.6版本及以上版本废弃)
    final static String LIVE_VEST_ONLY_COUNT_PV_KEY = "live_vest_only_count_pv_"
    //真实观众的pv
    final static String LIVE_REAL_WATCH_COUNT = "live_real_watch_count_"
    //头像的uv（真实观众+马甲头像）
    final static String LIVE_WATCH_COUNT_KEY = "live_watch_count_"
    //马甲数量的UV，马甲只增长数量，不增头像的缓存（可增可减）
    final static String LIVE_VEST_USER_COUNT_PERMINUTE = "live_vest_count_perminute_"
    //app的真实在线观看人数(某个app上所有直播观众数相加)
    final static String APP_WATHER_COUNT="app_wather_count_"
    //点赞数
    final static String LIVE_AVIMCMD_PRAISE_COUNT = "live_avimcmd_praise_count_"
    //马甲用户列表
    final static String VEST_USER_LIST = "vest_user_list_"
    //直播绑定的马甲用户列表
    final static String LIVE_VEST_USER_LIST_PRE = "live_vest_user_list_"
    //直播结束时错误代码
    final static String LIVE_STOP_ERROR_CODE_KEY = "live_stop_error_code_key_"
    //直播结束时错误代码
    final static String LIVE_STOP_ERROR_CODE_FIELD = "_@#@_"
    //直播评论redis队列
    final static String LIVE_COMMENT_REDIS_QUEUE = "live_comment_redis_queue"
    //删除直播评论redis队列
    final static String LIVE_DELETE_COMMENT_REDIS_QUEUE = "live_del_comment_redis_queue"
    //直播禁言缓存
    final static String LIVE_FORBID_COMMENT_KEY = "live_forbid_comment_"
    //直播踢人列表缓存
    final static String LIVE_KICK_MEMBER_KEY = "live_kick_member_"
//免费礼物列表缓存
    final static String LIVE_FREE_GIFT_LIST_KEY = "live_free_gift_list"
    //马甲评论库缓存
    final static String VEST_COMMENT_LIST_KEY = "vest_comment_list"
    //视频合并缓存（用来存储视频的fileId以及回调次数的计数）
    final static String VIDEO_MERGE_PRE = "video_merge_key_"

    //Pgc暂停时间 long型
    final static String PAUSE_TIME= "pause_time_"
    //Pgc暂停时长
    final static String PAUSE_TIME_LONG = "pause_time_long_"
    //价值直播列表
    final static String LIVE_VALUE_LIST = "live_value_list_"
    //预告列表
    final static String LIVE_FORESHOW_LIST = "live_foreshow_list_"
    //价值直播分类列表
    final static String LIVE_CATEGORY_LIST = "live_category_list_"

    //预告对应直播的缓存 === 只有会议直播有 因为会议直播和预告是一一对应的
    final static String LIVE_PGC_FORESHOW_TO_LIVE = "live_pgc_foreshow_to_live_";

    //价值系列列表
    final static String LIVE_FORESHOWBYPARENT_LIST = "live_foreshowByParent_list_"

    //价值直播轮播列表
    final static String LIVE_BANNER_LIST = "live_banner_list"

    //圈子和预告的对应后的srpId列表
    final static String LIVE_FORESHOW_SRP_RELATION = "live_foreshow_srp_relation_"

    //腾讯云配置项信息（KEY）
    final static String LIVE_CLOUDE_INFO ="live_cloud_info_1.0_"

    //我购买的付费直播
    final static String USER_PAY_LIST ="user_pay_list_"

    static String SY_APPSTORE_SOUYUE_VERSION_PRE = "sy_appstore_souyue_version_pre_"
    static String SY_APPSTORE_SOUYUE_FIELD_PRE = "souyue_version_pre"

    static String SY_APPSTORE_SOUYUE_VERSION = "sy_appstore_souyue_version_"
    static String SY_APPSTORE_SOUYUE_FIELD = "souyue_version"

    void setSignId(String userId,String appId,String sdkappId,String signId) {
        //直播signId redis 一个月有效期
        liveRedis.set(LIVE_SIGN_ID_KEY+appId+sdkappId+userId,signId,30*24*60*60)
    }

    String getSignId(String userId,String appId,String sdkappId) {
        //直播signId redis 一个月有效期
        liveRedis.get(LIVE_SIGN_ID_KEY+appId+sdkappId+userId)
    }

    //生成liveId
    long getLiveId() {
        return liveRedis.setIncy(LIVE_ID_KEY, 1L);
    }
    //生成roomid
    int generateRoomId() {
        return liveRedis.setIncy(LIVE_ROOM_ID_KEY, 1L)
    }

    int getRoomId(long userId, String appId) {
        String roomid = liveRedis.get(LIVE_ROOM_USER_KEY + userId + "_" + appId)
        return roomid ? roomid as int : 0
    }

    void setRoomId(long userId, String appId, int roomId) {
        liveRedis.set(LIVE_ROOM_USER_KEY + userId + "_" + appId, roomId.toString())
    }
    /**
     * 更新直播列表(正在直播的信息)
     */
    void updateLiveList(LiveRecord live) {
        //因为是正在直播的 所以有效期放置3天应该够了 不可能一个直播这么长时间吧！！！
        liveRedis.hset(LIVE_START_BY_APP_KEY, live.liveId?.toString(), Strings.toJson(live), 3 * 24 * 60 * 60)
    }
    /**
     * 获取appId 下所有直播列表
     */
    def getLiveListInLive() {
        def liveList = liveRedis.hgetAll(LIVE_START_BY_APP_KEY)
        return liveList
    }

    /**
     * 获取直播信息
     * @param liveId
     * @param appId
     * @return
     */
    LiveRecord getLiveRecord(long liveId) {
        String livejson = liveRedis.hget(LIVE_START_BY_APP_KEY, liveId.toString());
        return Strings.parseJson(livejson, LiveRecord.class)
    }

    /**
     * 设置直播开始时间（推流时间）
     */
    void setLivePushTime(long liveId, long livePushTime) {
        liveRedis.set(LIVE_BEGIN_KEY + liveId, livePushTime.toString())
    }

    /**
     * 得到直播开始时间（推流时间）
     */
    long getLivePushTime(long liveId) {
        String pushTimeStr = liveRedis.get(LIVE_BEGIN_KEY + liveId)
        return pushTimeStr ? pushTimeStr as long : 0L
    }

    /**
     * 删除直播记录
     * @param appId
     * @param liveId
     */
    void delLiveRecord(String appId, long liveId) {
        liveRedis.hdel(LIVE_START_BY_APP_KEY, liveId.toString())
    }
    /**
     * 设置心跳时间
     * @param liveId
     */
    void setHeartTime(long liveId) {
        liveRedis.set(LIVE_HEART_TIME_KEY + liveId, System.currentTimeMillis().toString())
    }
    /**
     * 设置主播离开心跳时间 主播离开的时候心跳延长30s
     */
    void setHeartTimeHostLeave(long liveId) {
        long hostLeaveTime = System.currentTimeMillis() - (30 * 1000)
        liveRedis.set(LIVE_HEART_TIME_KEY + liveId, hostLeaveTime.toString())
    }
    /**
     * 获取心跳时间
     */
    long getHeartTime(long liveId) {
        String timeStr = liveRedis.get(LIVE_HEART_TIME_KEY + liveId)
        return timeStr ? timeStr as long : 0L
    }

    /**
     * 设置直播观众
     * @param liveId
     * @param watherJson 用户信息json格式，userId、userName、userImage、nickname
     * @param userId
     */
    void setLiveWather(long liveId, String watherJson, long userId) {
        //如果json中昵称是空的 就不往redis里放
        def userInfo = Strings.parseJson(watherJson)
        if (userInfo && userInfo.nickname) {
            liveRedis.set(LIVE_WATCHER_KEY + userId, Strings.toJson(userInfo)) //两天过期
            liveRedis.zadd(LIVE_WATCHER_LIST_KEY + liveId, System.currentTimeMillis(), String.valueOf(userId))
        }
        Parallel.run([1],{
            //观众头像的redis列表保持在100左右
            vestUserService.cutterLiveWatchList(liveId)
        },1)
    }

    /**
     * 删除直播观众
     * @param liveId
     * @param watherJson 用户信息json格式，userId、userName、userImage、nickname、liveId
     * @param userId
     */
    void delLiveWather(long liveId, long userId) {
        liveRedis.del(LIVE_WATCHER_KEY + userId)
        liveRedis.zrem(LIVE_WATCHER_LIST_KEY + liveId, String.valueOf(userId))
    }
    /**
     * 删除直播观众，只删除观众列表缓存，不删除用户信息缓存（给马甲退出房间专用）
     * @param liveId
     * @param watherJson 用户信息json格式，userId、userName、userImage、nickname、liveId
     * @param userId
     */
    void delVestWather(long liveId, long userId) {
        liveRedis.zrem(LIVE_WATCHER_LIST_KEY + liveId, String.valueOf(userId))
    }

    /**
     * 判断用户是否在该直播间
     * @param liveId
     * @param userId
     * @return
     */
    boolean validateLiveWather(long liveId, long userId) {
        boolean res = false
        def score = liveRedis.zscore(LIVE_WATCHER_LIST_KEY + liveId, userId.toString())
        if (score) {
            res = true
        }
        return res
    }

    /**
     * 分页获取直播观众列表
     */
    def getLiveWather(long liveId, String lastUserId, int psize) {
        def userList = []
        // 正序获取指定区间内的观众
        String score = "+inf"
        if (StringUtils.isNotBlank(lastUserId)) {
            score = liveRedis.zscore(LIVE_WATCHER_LIST_KEY + liveId, lastUserId) ?: ""
            if (!score) {
                score = "+inf"
            }
        }
        LinkedHashSet<String> sets = liveRedis.zrevrangeByScore(LIVE_WATCHER_LIST_KEY + liveId, score, psize)
        List userIdList = [];
        sets?.each {
            userIdList.add(LIVE_WATCHER_KEY + it)
        }
        // 根据id取用户信息
        List<String> list = liveRedis.mget(userIdList);
        list.each {
            def userJson = JSON.parse(it)
            Map userInfo = [
                userId  : userJson?.userId ?: "",
                nickname: userJson?.nickname ?: "",
                userImage: userJson?.userImage ?: ""
            ]
            if (userInfo.userId && userInfo.nickname) {
                userList.add(userInfo)
            }
        }
        return userList
    }
    /**
     * 获取马甲的pv数(马甲头像的pv+马甲数量的pv)
     */
    int getVestCount(long liveId) {
        String vestStr = liveRedis.get(LIVE_VEST_COUNT + liveId)
        return (vestStr ? vestStr as int : 0) + (getVestOnlyCountRedis(liveId)) //存redis的马甲列表 + 每分钟的马甲列表
    }
    /**
     * 增加马甲数
     */
    void addVestCount(long liveId) {
        liveRedis.setIncy(LIVE_VEST_COUNT + liveId)
    }

    /**
     * 仅修复数据使用
     * @param liveId
     * @param count
     */
    void setVestCountRedis(long liveId, long count) {
        liveRedis.set(LIVE_VEST_COUNT + liveId, count as String)
    }
    /**
     * 最大参与的观众数的总数
     */
    void addWatherTotalCount(long liveId, long count) {
        liveRedis.setIncy(LIVE_WATHER_TOTAL_COUNT + liveId, count)
    }

    /**
     * 获取最大参与的观众数的总数
     * @param liveId
     * @return
     */
    long getLiveWatherTotalCount(long liveId) {
        //腾讯云回调计数+超过1000的马甲增长数
        long totalWatchCount = (liveRedis.get(LIVE_WATHER_TOTAL_COUNT + liveId) ?: 0) as long
        return totalWatchCount
    }

    /**
     * 获取正在参于的直播观众数（观众退出直播会减掉数）
     */
    int getLiveWatherCount(long liveId) {
        return (liveRedis.zcard(LIVE_WATCHER_LIST_KEY + liveId) as int) + (getVestCountPerMinute(liveId))
    }

    long getLiveWatchListUv(long liveId){
        return (liveRedis.zcard(LIVE_WATCHER_LIST_KEY + liveId) ?: 0) as long
    }

    /**
     * 增加appId的观众数
     */
    void addAppWatcherCount(String appId,int count){
        liveRedis.setIncy(APP_WATHER_COUNT+appId,count as long, 60*60*24*3)
    }

    /**
     * 删除appId的观众数
     */
    void delAppWatcherCount(String appId){
        liveRedis.del(APP_WATHER_COUNT+appId)
    }

    /**
     * 获取appId的观众数
     */
    def getAppWatcherCount(String appId){
        return (liveRedis.get(APP_WATHER_COUNT+appId) ?:0) as int
    }

    /**
     * 房间对应直播列表
     */
    void setLiveIdWithRoomId(int roomId, long liveId) {
        liveRedis.set(LIVE_ROOM_LIVE_ID_KEY + roomId, liveId.toString())
    }
    /**
     * 获取房间对应的正在直播的liveId
     */
    long getLiveIdByRoomId(int roomId) {
        String liveIdStr = liveRedis.get(LIVE_ROOM_LIVE_ID_KEY + roomId)
        return liveIdStr ? liveIdStr as long : 0
    }
    /**
     * 房间对应直播
     */
    void delLiveIdByRoomID(int roomId) {
        liveRedis.del(LIVE_ROOM_LIVE_ID_KEY + roomId)
    }

    //更新点赞数
    void setLivePraise(long liveId) {
        liveRedis.setIncy(LIVE_AVIMCMD_PRAISE_COUNT + liveId)
    }
    //获取点赞数
    long getLivePraise(long liveId) {
        String livepStr = liveRedis.get(LIVE_AVIMCMD_PRAISE_COUNT + liveId)
        return livepStr ? livepStr as long : 0L
    }
    /**
     * 获取所有马甲用户列表
     * @param start
     * @param end
     * @return
     */
    List getAllVestUserList(int start, int end) {
        LinkedHashSet<String> sets = liveRedis.zrange(VEST_USER_LIST, start, end);
        List userList = []
        sets?.each {
            userList << it
        }
        return userList
    }
    /**
     * 获取一定数量的马甲用户列表
     * @param start
     * @param end
     * @return
     */
    List getVestUserList(String key ,int start, int end) {
        LinkedHashSet<String> sets = liveRedis.zrange(key, start, end);
        List userList = []
        sets?.each {
            userList << it
        }
        return userList
    }
    /**
     * 把所有马甲用户列表放入缓存，为了能随机获取马甲列表中的某一范围内的数据，采用有序的缓存方法
     * @param vestList
     * @return
     */
    def setAllVestUserList(List vestList) {
        //TODO 马甲用户列表缓存删除操作执行时，由于value值太大，导致执行时间过长，redis会有问题，因此去掉此操作，有可能会影响此缓存的更新
//        liveRedis.del(VEST_USER_LIST)
        vestList.each {
            double sort = it.userId as double
            liveRedis.zadd(VEST_USER_LIST, sort, Strings.toJson(it))
        }
    }
    /**
     * 从马甲列表中获取某个马甲的用户信息
     * @param userId
     * @return
     */
    def getVestUserInfoFromRedis(long userId){
        liveRedis.zrangeByScore(VEST_USER_LIST, userId as String, userId as String)
    }
    /**
     * 从马甲列表中获取某个马甲的用户信息
     * @param userId
     * @return
     */
    def getVestUserInfoFromRedis(String key,long userId){
        liveRedis.zrangeByScore(key, userId as String, userId as String)
    }
    /**
     * 获取直播已绑定的马甲用户列表
     * @param liveId
     * @param start
     * @param end
     * @return
     */
    List getVestUserListByLiveId(long liveId, int start, int end) {
        LinkedHashSet<String> sets = liveRedis.zrange(LIVE_VEST_USER_LIST_PRE + liveId, start, end);
        List userList = []
        sets?.each {
            userList << it
        }
        return userList
    }
    /**
     * 直播中的直播绑定的马甲用户的列表，为了发普通马甲消息时，随机获取马甲列表中的某一范围内的数据，采用有序的缓存方法
     * @param liveId
     * @param vestList
     * @return
     */
    def setVestUserListByLiveId(long liveId, List vestList) {
        liveRedis.zaddWithPipeline(LIVE_VEST_USER_LIST_PRE + liveId,vestList)
    }
    /**
     * 获取直播所绑定的马甲列表中的某个马甲的用户信息
     * @param liveId
     * @param userId
     * @return
     */
    def getVestUserInfoByLiveIdAndUserId(long liveId, long userId){
        return liveRedis.zrangeByScore(LIVE_VEST_USER_LIST_PRE + liveId, userId as String, userId as String)
    }
    /**
     * 删除马甲用户列表
     * @param liveId
     * @param userId
     * @return
     */
    def delVestUserByLiveIdAndUserId (long liveId, def user){
        liveRedis.zrem(LIVE_VEST_USER_LIST_PRE + liveId, String.valueOf(user))
    }
    /**
     * 获取直播绑定的马甲数量
     * @param liveId
     * @return
     */
    long getVestUserCountByLiveId(long liveId) {
        long count = 0
        if (!liveId) {
            return count
        }
        count = liveRedis.zcard(LIVE_VEST_USER_LIST_PRE + liveId)
        return count
    }
    /**
     * 增加马甲数量的uv
     * @param liveId
     * @param count
     * @return
     */
    def setVestCountPerMinute(long liveId, int count, int second = 0) {
        liveRedis.setIncy(LIVE_VEST_USER_COUNT_PERMINUTE + liveId, count as long, second)    //设置过期时间为1天
    }
    /**
     * 当马甲数大于1000时，获取每分钟马甲入群的数量
     * @param liveId
     * @return
     */
    long getVestCountPerMinute(long liveId) {
        long count = 0
        if (!liveId) {
            return count
        }
        count = (liveRedis.get(LIVE_VEST_USER_COUNT_PERMINUTE + liveId) ?: 0) as long
        return count
    }
    /**
     * 根据liveId获取当前马甲的数量
     * @param liveId
     * @return
     */
    long getVestAllCount(long liveId) {
        long count = 0
        if (!liveId) {
            return count
        }
        count = (liveRedis.get(LIVE_VEST_USER_COUNT_PERMINUTE + liveId) ?: 0) as long
        count += getVestUserCountByLiveId(liveId)
        return count
    }
    /**
     * 马甲只增数量缓存（只增不减）
     * @param liveId
     * @param count
     * @param second
     * @return
     */
    def setVestOnlyCountRedis(long liveId, int count, int second = 0) {
        if(second){
            liveRedis.setIncy(LIVE_VEST_ONLY_COUNT_PV_KEY + liveId, count as long, second)    //设置过期时间为1天
        }else {
            liveRedis.setIncy(LIVE_VEST_ONLY_COUNT_PV_KEY + liveId, count as long)
        }
    }
    /**
     * 仅修复数据使用
     * @param liveId
     * @param count
     * @return
     */
    def setVestOnlyCountRedisByLiveId(long liveId, long count) {
       liveRedis.set(LIVE_VEST_ONLY_COUNT_PV_KEY + liveId, count as String)
    }
    /**
     * 获取马甲只增数量的缓存pv
     * @param liveId
     * @return
     */
    long getVestOnlyCountRedis(long liveId) {
        long count = 0
        if (!liveId) {
            return count
        }
        count = (liveRedis.get(LIVE_VEST_ONLY_COUNT_PV_KEY + liveId) ?: 0) as long
        return count
    }
    /**
     * 直播结束时删除马甲绑定的马甲用户列表
     * @param liveId
     * @return
     */
    def delVestUserCountByLiveId(long liveId) {
        liveRedis.del(LIVE_VEST_USER_LIST_PRE + liveId)
    }
    /**
     * 直播未结束成功时的错误代码
     */
    void setLiveStopErrorCode(long userId, String appId, Map errorMap) {
        liveRedis.hset(LIVE_STOP_ERROR_CODE_KEY, userId + LIVE_STOP_ERROR_CODE_FIELD + appId, Strings.toJson(errorMap))
    }

    Map getLiveStopErrorCode(long userId, String appId) {
        Map backMap = [:]
        String errorCodeStr = liveRedis.hget(LIVE_STOP_ERROR_CODE_KEY, userId + LIVE_STOP_ERROR_CODE_FIELD + appId)
        if (errorCodeStr) {
            backMap = Strings.parseJson(errorCodeStr)
        }
        return backMap
    }

    void delLiveStopErrorCode(long userId, String appId) {
        liveRedis.hdel(LIVE_STOP_ERROR_CODE_KEY, userId + LIVE_STOP_ERROR_CODE_FIELD + appId)
    }

    Map getAllLiveStopErrorCode() {
        Map m = liveRedis.hgetAll(LIVE_STOP_ERROR_CODE_KEY)
        return m
    }

    def pushCommentDataToRedis(String value) {
        return liveRedis.rpush(LIVE_COMMENT_REDIS_QUEUE, value)
    }
    def pushDeleteCommentDataToRedis(String value) {
        return liveRedis.rpush(LIVE_DELETE_COMMENT_REDIS_QUEUE, value)
    }

    //直播禁言缓存
    def hsetForbidCommentUserInfo(long liveId, long userId, String value) {
        liveRedis.hset(LIVE_FORBID_COMMENT_KEY + liveId as String, userId as String, value)
    }

    def hgetForbidCommentUserInfo(long liveId, long userId) {
        return liveRedis.hget(LIVE_FORBID_COMMENT_KEY + liveId as String, userId as String)
    }

    //直播踢人缓存
    def hsetKickGroupMember(long liveId, long userId, String value) {
        liveRedis.hset(LIVE_KICK_MEMBER_KEY + liveId as String, userId as String, value)
    }

    def hgetKickGroupMember(long liveId, long userId) {
        return liveRedis.hget(LIVE_KICK_MEMBER_KEY + liveId as String, userId as String)
    }

    List getFreeGiftListRedis() {
        return Strings.parseJson(liveRedis.get(LIVE_FREE_GIFT_LIST_KEY), List.class)
    }

    List setFreeGiftListRedis(String value) {
        //获取缓存失效时间（第二天凌晨2:30失效）
        int seconds = DateUtil.getExpireTimeForNextDay(2, 30) - System.currentTimeMillis() / 1000
        liveRedis.set(LIVE_FREE_GIFT_LIST_KEY, value, seconds)
    }

    //马甲评论列表缓存
    def setVestCommentListRedis(String value) {
        liveRedis.set(VEST_COMMENT_LIST_KEY, value, 6*60*60)
    }

    List getVestCommentList() {
        def value = liveRedis.get(VEST_COMMENT_LIST_KEY) ?: null
        return Strings.parseJson(value)
    }

    def delVestCommentListRedis() {
        liveRedis.del(VEST_COMMENT_LIST_KEY)
    }

    //视频合并相关缓存
    def hsetVideoMergeRedis(def key, String field, String value) {
        liveRedis.hset(key, field, value, 60 * 60 * 24);
    }

    def hgetVideoMergeRedis(def key, String field) {
        return liveRedis.hget(key, field);
    }

    def delVideoMergeRedis(def key) {
        liveRedis.del(key)
    }
    //视频截图相关缓存
    def hsetSnapshotRedis(def key, String field, String value) {
        liveRedis.hset(key, field, value, 60 * 60 * 24);
    }

    def hgetSnapshotRedis(def key, String field) {
        return liveRedis.hget(key, field);
    }

    /**
     * 存储暂停时间
     */
    def setPgcPaushTime(long foreshowId,long time) {
         liveRedis.set(PAUSE_TIME + foreshowId,time?.toString(),0)
       // return pushTimeStr ? pushTimeStr as long : 0L
    }

    /**
     * 获取会议直播时间
     */
    long getPgcPaushTime(long foreshowId) {
        String pushTimeStr = liveRedis.get(PAUSE_TIME + foreshowId)
        return pushTimeStr ? pushTimeStr as long : 0L
    }
    /**
     * 存储会议直播暂停时长
     */
    def setPgcPaushTimeLong(long foreshowId,long timeLong) {
        liveRedis.set(PAUSE_TIME_LONG + foreshowId,timeLong?.toString(),0)
     //   return pushTimeStr ? pushTimeStr as long : 0L
    }


    /**
     * 获取会议直播暂停时长
     */
    long getPgcPaushTimeLong(long foreshowId) {
        String pushTimeStr = liveRedis.get(PAUSE_TIME_LONG + foreshowId)
        return pushTimeStr ? pushTimeStr as long : 0L
    }

    /**
     * 存储价值直播回放列表
     * @param liveList
     * @return
     */
    def setLiveValueList(long categoryId, String appId,List liveList,String vc=''){
        String key = LIVE_VALUE_LIST + categoryId + "_" + appId + vc
        liveRedis.set(key,Strings.toJson(liveList),60)
    }

    /**
     * 获取价值直播回放列表
     * @return
     */
    List getLiveValueList(long categoryId, String appId,String vc=''){
        String key = LIVE_VALUE_LIST + categoryId + "_" + appId + vc
        return liveRedis.get(key)?Strings.parseJson(liveRedis.get(key), List.class):null
    }

    /**
     * 修改直播预告缓存列表
     * @param liveForeshow
     * @return
     */
    def updateLiveForeshowList(LiveForeshow liveForeshow){
        liveRedis.hset(LIVE_FORESHOW_LIST, liveForeshow.foreshowId?.toString(), Strings.toJson(liveForeshow), 24 * 60 * 60)
    }

    /**
     * 获取直播预告
     * @param foreshowId
     * @return
     */
    LiveForeshow getLiveForeshow(long foreshowId){
        String livejson = liveRedis.hget(LIVE_FORESHOW_LIST, foreshowId.toString());
        return Strings.parseJson(livejson, LiveForeshow.class)
    }

    /**
     * 存储价值直播分类
     * @param liveList
     * @return
     */
    def setLiveCategoryList(List categoryList,String appId){
        liveRedis.set(LIVE_CATEGORY_LIST+appId,Strings.toJson(categoryList),60)
    }

    /**
     * 获取价值直播回放列表
     * @return
     */
    List getLiveCategoryList(String appId){
        return liveRedis.get(LIVE_CATEGORY_LIST+appId)?Strings.parseJson(liveRedis.get(LIVE_CATEGORY_LIST+appId), List.class):null
    }


    /**
     * 设置会议直播直播预告和直播对应关系
     * @param
     * @return
     */
    def hsetPgcForeshowToLive(long foreshowId,long liveId){
        String key = LIVE_PGC_FORESHOW_TO_LIVE + foreshowId
        liveRedis.hset(key.toString(),liveId.toString(),liveId.toString(),60*60*24*7)  //设置7天缓存
    }

    /**
     * 获取会议直播对应的直播信息
     */
    Map hgetAllPgcForeshowToLive(long foreshowId){
        String key = LIVE_PGC_FORESHOW_TO_LIVE + foreshowId
        return liveRedis.hgetAll(key)
    }

    /**
     * 存储系列列表
     * @param liveList
     * @return
     */
    def setForeshowByParentList(String appId,long liveSerieId,List foreshowList,String vc=''){
        String key = LIVE_FORESHOWBYPARENT_LIST + liveSerieId + "_" + appId + vc
        liveRedis.set(key,Strings.toJson(foreshowList),60)
    }

    /**
     * 获取系列列表
     * @return
     */
    List getForeshowByParentList(String appId,long liveSerieId,String vc=''){
        String key = LIVE_FORESHOWBYPARENT_LIST + liveSerieId + "_" + appId + vc
        return liveRedis.get(key)?Strings.parseJson(liveRedis.get(key), List.class):null
    }

    /**
     * 价值直播轮播列表
     * @param bannerList
     * @return
     */
    def setLiveBannerList(List bannerList){
        liveRedis.set(LIVE_BANNER_LIST,Strings.toJson(bannerList),30)
    }

    /**
     * 获取价值直播轮播列表
     * @return
     */
    List getLiveBannerList(){
        return liveRedis.get(LIVE_BANNER_LIST)?Strings.parseJson(liveRedis.get(LIVE_BANNER_LIST), List.class):null
    }

    /**
     * 保存圈子和预告的对应
     * @param forshowId
     * @param srpIdList
     * @return
     */
    def addForeshowSrpRelation(long forshowId,List<String> srpIdList){
        srpIdList?.each{
            liveRedis.set(LIVE_FORESHOW_SRP_RELATION+it, "$forshowId")
        }
    }

    /**
     * 根据appid查询腾讯云redis缓存
     * @param appId
     * @return
     */
    def getCloudInfoRedis(String appId){
        return liveRedis.get(LIVE_CLOUDE_INFO+appId as String);
    }

    /**
     * 设置腾讯云缓存
     * @param appId
     * @param config
     * @return
     */
    def setCloudInfoRedis(String appId,Map config){
        liveRedis.set(LIVE_CLOUDE_INFO+appId as String,Strings.toJson(config));
    }

    /**
     * 删除腾讯云缓存
     * @param appId
     * @return
     */
    def  delCloudInfoRedis(String appId){
        liveRedis.del(LIVE_CLOUDE_INFO+appId as String)
        liveRedis.del(LIVE_CLOUDE_INFO+"all" as String)
    }

    /**
     * 查询腾讯云redis缓存
     * @return
     */
    def getAllCloudInfoRedis(){
        return liveRedis.get(LIVE_CLOUDE_INFO + "all" as String);
    }

    /**
     * 设置腾讯云缓存
     * @param appIds
     * @return
     */
    def setAllCloudInfoRedis(List appIds){
        liveRedis.set(LIVE_CLOUDE_INFO + "all" as String,Strings.toJson(appIds),60*60);
    }

    /**
     * 增加真实观众的pv
     * @param liveId
     * @param count
     * @param seconds
     * @return
     */
    def addLiveRealWatchCount(long liveId,long count, int seconds = 0){
        liveRedis.setIncy(LIVE_REAL_WATCH_COUNT+liveId,count,seconds)
    }
    def setLiveRealWatchCount(long liveId,long count, int seconds = 0){
        liveRedis.set(LIVE_REAL_WATCH_COUNT+liveId,count as String,seconds)
    }
    /**
     * 获取真实观众的pv
     * @param liveId
     * @return
     */
    long getLiveRealWatchCount(long liveId){
        return (liveRedis.get(LIVE_REAL_WATCH_COUNT+liveId) ?: 0) as long
    }
    /**
     * 增加头像的uv
     * @param liveId
     * @param count
     * @param seconds
     * @return
     */
    def addLiveWatchCount(long liveId,long count, int seconds = 0){
        liveRedis.setIncy(LIVE_WATCH_COUNT_KEY+liveId,count,seconds)
    }
    def setLiveWatchCount(long liveId,long count, int seconds = 0){
        liveRedis.set(LIVE_WATCH_COUNT_KEY+liveId,count as String,seconds)
    }
    /**
     * 获取头像的uv
     * @param liveId
     * @return
     */
    long getLiveWatchCount(long liveId){
        return (liveRedis.get(LIVE_WATCH_COUNT_KEY+liveId) ?: 0) as long
    }

    /**
     * 保存我购买的直播列表
     * @param bannerList
     * @return
     */
    def setUserPayList(long userId,List liveList){
        String key = USER_PAY_LIST + userId
        liveRedis.set(key,Strings.toJson(liveList),60)
    }

    /**
     * 获取我购买的直播列表
     * @return
     */
    List getUserPayList(long userId){
        String key = USER_PAY_LIST + userId
        return liveRedis.get(key)?Strings.parseJson(liveRedis.get(key), List.class):null
    }
}
