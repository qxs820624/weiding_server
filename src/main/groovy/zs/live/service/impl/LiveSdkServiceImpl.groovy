package zs.live.service.impl

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import zs.live.common.LiveCommon
import zs.live.common.QcloudCommon
import zs.live.dao.mysql.LiveRes
import zs.live.dao.mysql.QcloudLiveRes
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.entity.LiveRecord
import zs.live.service.LiveSdkSevice
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.service.ShortURLService
import zs.live.service.VestUserService
import zs.live.utils.DateUtil
import zs.live.utils.Parallel
import zs.live.utils.QcloudBizidUtil
import zs.live.utils.Strings

/**
 * Created by Administrator on 2017/4/28.
 */
@Slf4j
@Service
class LiveSdkServiceImpl implements LiveSdkSevice {

    @Value('${live.env}')
    String liveEnv
    @Value('${live.souyue.api.url}')
    String syUrl

    @Autowired
    LiveQcloudRedis liveQcloudRedis
    @Autowired
    QcloudLiveService   qcloudLiveService
    @Autowired
    QcloudLiveRes qcloudLiveRes
    @Autowired
    ShortURLService    shortURLService
    @Autowired
    VestUserService    vestUserService
    @Autowired
    LiveService    liveService
    @Autowired
    LiveRes  liveRes

    @Override
    def create(Map params) {
        log.info("创建直播传入参数：{}", params);
        def backMap=[:]
        Map msg=["msg":"创建失败", "liveId": 0]; //后台要求统一包括异常时的json格式
        long userId = params.host.userId as long
        String appId = Strings.getAppId(params)
        //创建直播先创建房间（因为要判断是否有没结束的房间 先结束了）
        int roomId = getRoomId(userId,appId);
        //生成liveId
        long liveId = getLiveId();

        fillParams(params, liveId, roomId);

        LiveRecord live = getMeetingLiveRecord(params, roomId, liveId);
        log.info("liveId = {},roomId={},直播创建组织数据：{}", liveId, roomId, Strings.toJson(live))

        //创建直播
        boolean create = qcloudLiveRes.createLiveRecord(live)
        //更新正在直播列表
        liveQcloudRedis.updateLiveList(live);
        //更新房间对应正在直播的缓存
        liveQcloudRedis.setLiveIdWithRoomId(roomId,live.liveId);
        //设置心跳时间（严格来说心跳是第一次心跳来了设置时间的 但是时间太长，而且不用那么精确 所以就选择这里设置时间）
        liveQcloudRedis.setHeartTime(live.liveId)
        /**
         * "roomId":""
         "rtmpUrl":"rtmp://...."//推流url
         "liveId": "7099",
         "shortUrl": "http://tt.zhongsou.com/u/wlB."
         "status":int
         "msg":"错误信息"
         */
        backMap.put("roomId",roomId)
        backMap.put("rtmpUrl",params.rtmpUrl)
        backMap.put("liveId",liveId)
        backMap.put("status",QcloudCommon.QCLOUD_SUCCESS)
        String shortUrl = shortURLService.getShortUrlLive([liveId:live.liveId,liveMode: live.liveMode,userId: live.userId,roomId: live.roomId, vc: live.vc, appId: appId])
        backMap.put("shortUrl",shortUrl)

        return backMap
    }

    @Override
    Map start(long liveId, int roomId, long userId, String appId) {
        log.info("liveId=={},roomId=={},userId=={},appId=={},推流成功！！！",liveId,roomId,userId,appId);
        return ["status":QcloudCommon.QCLOUD_SUCCESS]
    }

    @Override
    Map stop(long liveId,int roomId,long userId,String appId,String stopMsg) {

        def qcloudInfo = qcloudLiveService.getQcloudInfo(appId)
        String liveAdminIdentifier = qcloudInfo.identifier
        Long sdkappId = (qcloudInfo.sdkAppId?:0) as long
        log.info("流程： {} 直播结束,liveId={}",stopMsg,liveId)
        //定义结束推流异常存储
        def exceptionFlagMap = [:]
        //默认成功
        exceptionFlagMap.put("status",QcloudCommon.QCLOUD_SUCCESS)
        //先查出直播对象 保存下来
        LiveRecord nowLive = liveService.findLiveByLiveId(liveId)
        if(nowLive){  //结束直播 先操作数据库记录 再调用腾讯云接口  保证多次调用时本地数据不会乱
            //插入live_record_log表 删除live_record表
            //插入live_record_log表
            Map liveRecordMap = [:]

            //直播时长修改成按create_time 算直播 因为有没有推流的情况
            long createTime =  DateUtil.getTimestamp(nowLive.createTime)
            long timespan = (System.currentTimeMillis() - createTime)/1000
            nowLive.timeSpan = timespan

            //直播观众数
            long watcherCount = liveQcloudRedis.getLiveRealWatchCount(liveId)
            nowLive.watchCount = watcherCount   //观看数等于真实观众数加马甲数
            long totalWatchCount = liveQcloudRedis.getLiveWatherTotalCount(liveId)
            nowLive.totalWatchCount = totalWatchCount   //总数
            long vestCount = totalWatchCount - watcherCount  //马甲数
            nowLive.vestCount = vestCount

            //点赞数
            int admireCount = liveQcloudRedis.getLivePraise(liveId)
            nowLive.admireCount = admireCount

            //更新直播回看状态  这个应该放在插入的时候去做，放在这里可以提高效率，但是会有延时
            def conf = liveRes.findLiveBackVedioConfig(appId);
            long confTime = conf? (conf.time? conf.time as long : 0L): 0L
            int peoples = conf?(conf.peoples? conf.peoples as int :0) : 0
            int liveStatus = 2 // 默认不可以回放 1符合回看,2不符合回看,3用户删除，4彻底删除即腾讯云删除
            //符合回看条件 或者是官方直播 都可以回看
            //真实观众数=pv数-马甲数
            int realCount = totalWatchCount - vestCount
            if((timespan >confTime && peoples <= realCount) || nowLive.foreshowId != 0){
                liveStatus = 1
            }
            nowLive.liveStatus = liveStatus

            //createTime 统一格式
            qcloudLiveRes.insertLiveRecordLog(nowLive,liveRecordMap)
            //删除live_record表
            qcloudLiveRes.deleteLiveRecord(liveId)
            //删除删除直播列表redis缓存
            liveQcloudRedis.delLiveRecord(appId,liveId)
            //删除直播对应room
            liveQcloudRedis.delLiveIdByRoomID(roomId)
            //删除马甲相关
            vestUserService.delAllVestUserByLiveId(liveId)
        }

        //解散IM群组, 必须在最后 要不然结束推流和结束录制的时候会报no room
        String destroyIMGroup = QcloudLiveCommon.destroyIMGroup(roomId,qcloudLiveService.getSignId(liveAdminIdentifier,appId),liveAdminIdentifier,sdkappId)
        log.info("流程："+stopMsg+"liveId:"+liveId+"roomId:"+roomId+"强制解散房间返回:"+ destroyIMGroup)

        return ["status":QcloudCommon.QCLOUD_SUCCESS]
    }

    int getRoomId(long userId,String appId) {
        int roomId = liveQcloudRedis.generateRoomId();

        def qcloudInfo = qcloudLiveService.getQcloudInfo(appId)
        String liveAdminIdentifier = qcloudInfo.identifier
        Long sdkappId = (qcloudInfo.sdkAppId?:0) as long
        //调用IM接口创建群聊
        String signId = qcloudLiveService.getSignId(liveAdminIdentifier,appId)
        QcloudLiveCommon.createIMGroup(roomId, signId, userId, sdkappId, liveAdminIdentifier)
        //后面发消息会涉及到from_account账号可能不存在的问题（账号从未登录过客户端的），
        // 固在创建房间的时候增加一个步骤：异步将主播导入到腾讯云im
        Parallel.run([1],{
            try{
                //腾讯云IM账号导入
                QcloudLiveCommon.importIMAccount(roomId, signId, userId, sdkappId, liveAdminIdentifier)
            }catch(Exception e){}
        },1)
        return roomId;
    }

    long getLiveId() {
        //生成liveId
        long liveId = liveQcloudRedis.getLiveId();
        return liveId
    }

    Map fillParams(Map params, long liveId, int roomId){
        def config = qcloudLiveService.getQcloudInfo(Strings.getAppId(params))
        log.info("qcloudInfo====>"+Strings.toJson(config))
        int bizid = config.bizid as int
        String key = config.pushKey
        //不同前缀取值
        String stream_id=getStreamId(bizid,liveId,roomId)
        long txTime = System.currentTimeMillis()/1000 + 60*60*24 //开始时间的 一天以后
        String lastString = QcloudBizidUtil.getSafeUrl(key, stream_id, txTime)
        // System.out.println(getSafeUrl(key, stream_id, txTime));
        String rtmpUrl = """rtmp://"""+bizid+"""."""+QcloudCommon.LIVE_PUSH_URL + stream_id+"""?bizid="""+bizid+"""&"""+lastString;
        String m3u8Url = """http://"""+bizid+"""."""+QcloudCommon.LIVE_PLAY_URL + stream_id+""".m3u8"""
        String playUrl = """http://"""+bizid+"""."""+QcloudCommon.LIVE_PLAY_URL + stream_id+""".flv"""

        params.put("rtmpUrl",rtmpUrl)
        params.put("m3u8Url",m3u8Url)
        params.put("playUrl",playUrl)
        params.put("channelId",stream_id)
        log.info("rtmpUrl========>"+rtmpUrl)
        log.info("m3u8Url========>"+m3u8Url)
        log.info("playUrl========>"+playUrl)
    }

    /**
     *{"liveBg":"直播背景图",
     "liveThumb":"直播列表缩略图",
     "title": "我发起了一个直播，快来看看吧！",
     "host": {"avatar": "http://user-pimg.b0.upaiyun.com/selfcreate/default/default_77.jpg!sy",
     "userId": "3476812",
     "userName": "qb0005"},
     "isPrivate": 0,
     "srpInfo":[{"srpId": "",
     "keyword":""}]

     * @param param 组织直播数据
     * @return
     */

    LiveRecord getMeetingLiveRecord(def params, int roomId, long liveId) {

        LiveRecord live = new LiveRecord();
        live.liveId = liveId;
        live.playUrl = params.playUrl
        live.m3u8Url = params.m3u8Url
        live.appModel = params.appModel;
        live.appId = Strings.getAppId(params);
        live.chatId = roomId
        live.roomId = roomId
        live.createTime = Strings.getDayOfTime(new Date())
        live.userId = params.host.userId as long
        live.userImage = params.host.userImage
        live.nickname = params.host.nickname
        live.liveBg = params.liveBg
        live.liveThump = params.liveBg
        live.title = params.title
        live.isPrivate = params.isPrivate ? params.isPrivate as int : 0;
        if(params.srpInfo){
            live.srpId = params.srpInfo.srpId && Strings.toListString(params.srpInfo.srpId)?Strings.toListString(params.srpInfo.srpId):liveDefalutSrpId
            live.keyword = params.srpInfo.keyword && Strings.toListString(params.srpInfo.keyword,LiveCommon.KEYWORD_SEPARATOR)?Strings.toListString(params.srpInfo.keyword,LiveCommon.KEYWORD_SEPARATOR):liveDefalutKeyword
        }
        live.foreshowId = params.foreshowId? params.foreshowId as int:0
        if(live.foreshowId){  //默认非官方直播 有预告id的为官方直播
            live.liveType = 1
            live.isPrivate = 0 //官方直播只能是公开类型的直播
        }
        live.vc = params.vc
        live.channelId = params.channelId ?: ""
        live.compereUserId = params.compereUserId //主持人
        live.fieldControlId = params.fieldControlId //场控
        live.beginTime = Strings.getDayOfTime(new Date())
        live.rtmpUrl = params.rtmpUrl  //直播推流地址
        live.pgcStatus = 0//会议直播的直播状态 1.未开始，2.直播中，3直播暂停
       // live.pgcType = params.pgcType as int//1.手机 2.小鱼 3.搜悦推流
        live.brief = params.brief//直播简介
        live.liveMode = LiveCommon.LIVE_MODE_1
        live.liveFromSdk = 1;

        live.viewJson=[
            m3u8:params.m3u8Url ?:"",
            m3u8Standard:params.m3u8Standard?:"",
            m3u8High: params.m3u8High?:"",
            flv: params.playUrl ?:"",
            flvStandard:params.flvStandard?:"",
            flvHigh:params.flvHigh?:""
        ]
        live.briefHtml=params?.briefHtml ?:""  //主播提示语

        return live
    }

    String getStreamId(int bizid,long liveId, long roomId){
        String streamId=bizid+"_";
        if("test".equals(liveEnv)){
            streamId = streamId+"test_"+liveId+"_"+roomId
        }else if("pre".equals(liveEnv)){
            streamId = streamId+"pre_"+liveId+"_"+roomId
        }else{
            streamId = streamId+"online_"+liveId+"_"+roomId
        }

        return streamId
    }
    String getSyURL(){
        return syUrl
    }
}
