package zs.live.service.impl

import com.alibaba.fastjson.JSON
import com.qcloud.Utilities.MD5
import groovy.util.logging.Slf4j
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import zs.live.ApiException
import zs.live.common.LiveCommon
import zs.live.common.LiveStatus
import zs.live.common.QcloudCommon
import zs.live.dao.kafka.KafkaRes
import zs.live.dao.kestrel.LiveKestrel
import zs.live.dao.mysql.DataBases
import zs.live.dao.mysql.LiveRes
import zs.live.dao.mysql.QcloudLiveRes
import zs.live.dao.qcloud.tls_sigature
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.entity.LiveRecord
import zs.live.service.GiftService
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.service.ShortURLService
import zs.live.service.VestUserService
import zs.live.utils.DateUtil
import zs.live.utils.Http
import zs.live.utils.ImageUtils
import zs.live.utils.Parallel
import zs.live.utils.QcloudBizidUtil
import zs.live.utils.Strings
import zs.live.utils.VerUtils
import zs.live.utils.ZURL

/**
 * Created by Administrator on 2016/10/11.
 */
@Service("qcloudLiveService")
@Slf4j
class QcloudLiveServiceImpl implements QcloudLiveService{

    public static final Logger imMsgLog = LoggerFactory.getLogger("imMsg")
    public static final Logger callBackLog = LoggerFactory.getLogger("callBack")
    @Value('${live.defalut.srpid}')
    String liveDefalutSrpId
    @Value('${live.defalut.keyword}')
    String liveDefalutKeyword
    @Value('${live.env}')
    String liveEnv
    @Value('${live.heart.task.time}')
    long liveHeartTaskTime
    @Value('${live.getuserpay.url}')
    String liveUserPayUrl
    @Value('${live.pay.abstract.url}')
    String abstractUrl
    @Value('${live.pay.invite.url}')
    String inviteUrl
    @Value('${live.pay.record.url}')
    String livePayOrderUrl
    @Value('${live.statistic.nginx.url}')
    String liveNginxLogUrl

    @Autowired
    LiveQcloudRedis liveQcloudRedis;
    @Autowired
    DataBases dataBases;
    @Autowired
    QcloudLiveRes qcloudLiveRes
    @Autowired
    LiveService liveService
    @Autowired
    ShortURLService shortURLService;
    @Autowired
    GiftService giftService;
    @Autowired
    LiveKestrel liveKestrel
    @Autowired
    KafkaRes kafkaRes
    @Autowired
    LiveRes liveRes
    @Autowired
    VestUserService vestUserService

    @Override
    String getSignId(String userId,String appId) {
        if(!appId){
            log.info("getSignId appId is null,userId=>{}",userId,appId)
            return null
        }
        def qcloudInfo = this.getQcloudInfo(appId)
        if(!qcloudInfo){
            log.info("getSignId qcloudInfo is null,userId=>{}",userId,appId)
            return null
        }
        String sdkappId = (qcloudInfo.sdkAppId?:"") as String
        if(!sdkappId){
            log.info("getSignId sdkappId is null,userId=>{}",userId,appId)
            return null
        }
        //redis获取signId
        String signId = liveQcloudRedis.getSignId(userId,appId,sdkappId)
        //强制获取signId
        if(signId == null || signId.length() == 0){
            signId = this.getSignIdFromQcloud(userId,appId)
        }
        return signId
    }

    @Override
    String  getSignIdFromQcloud(String userId,String appId) {
        if(!appId){
            return null
        }
        String signId
        try{
            def qcloudInfo = this.getQcloudInfo(appId)
            if(!qcloudInfo){
                return null
            }
            String privStr = qcloudInfo.privateKey
            String pubStr = qcloudInfo.publicKey
            Long sdkappId = (qcloudInfo.sdkAppId?:0) as long
            // generate signature
            tls_sigature.GenTLSSignatureResult result = tls_sigature.GenTLSSignatureEx(sdkappId, userId, privStr);
            if (0 == result.urlSig.length()) {
                log.info("getSignIdFromQcloud GenTLSSignatureEx failed: " + result.errMessage +"userId:"+userId)
                return;
            }
            signId = result.urlSig;  //生成signId

            // check signature //用私钥生成后 用公钥校验一次 成功后可以直接返回
            tls_sigature.CheckTLSSignatureResult checkResult = tls_sigature.CheckTLSSignatureEx(result.urlSig, sdkappId, userId, pubStr);
            if(checkResult.verifyResult == false) {
                log.info("getSignIdFromQcloud CheckTLSSignature failed: " + result.errMessage +"userId:"+userId)
                return;
            }
            log.info("\n---\ncheck sig ok -- expire time " + checkResult.expireTime + " -- init time " + checkResult.initTime + "\n---\n")
            if(signId == null || signId.length()<1){
                throw new ApiException(ApiException.STATUS_BUSINESS_ERROR,"获取失败！！！")
            }else {
                //填充redis
                liveQcloudRedis.setSignId(userId,appId,sdkappId as String,signId)
            }
            log.info("强制获取signId success,userId=>{},appId=>{},signId=>{}",userId,appId,signId )
        }catch(Exception e) {
            log.info("强制获取signId error,userId=>{},appId=>{},Exception=>{}",userId,appId,e.getMessage())
            e.printStackTrace();
        }
        return signId;
    }

    /**
     * 客户端参数：  创建人uid，srpId，title
     根据uid创建房间，
     查询redis缓存 是否创建过房间 key--> uid
     如果没创建过生成新的房间号, 返回 （使用redis计数器生成房间id），填充redis缓存，异步插入表live_user_av_room
     如果已经创建过房间 取现房间号，
     判断该房间有没有没结束的直播，查询live_record 表
     有的话先结束该直播，然后再返回房间号
     没有则直接返回
     生成liveId,(redis计数器)
     插入live_record 表，,异步插入。
     调用IM接口创建群组（v4/group_open_http_svc/create_group）参数 groupId =roomId
     更新心跳 直播列表redis (这里更新主要为了 能监控到 客户端只发起了直播 没有开始直播这种情况，此时需要及时删除live_record表)
     * @param param
     * @return
     */
    @Override
    Map createLive(def params) {
        def statusMap = [:]
        statusMap.put("status",QcloudCommon.QCLOUD_SUCCESS)
        long userId = params.userId as long
        boolean isStopRoom = false;
        String appId = Strings.getAppId(params)
        long foreshowId = (params.foreshowId ?:0) as long
        if(!foreshowId){//云平台需求，颜值直播需要进行直播权限的校验
            int code = liveService.checkLiveAccessByUserId(userId, appId)
            if(code != 1){
                throw new ApiException(700, "您没有直播权限，欢迎联系客服开通呦！")
            }
        }
        Map dealMap = dealLastErrorStop(userId,appId,"创建直播")
        if(dealMap && dealMap.size() > 0){
            return dealMap
        }

        //创建直播先创建房间（因为要判断是否有没结束的房间 先结束了）
//        int roomId = getRoomId(userId,appId)
//        if(roomId != 0){
//            // 如果发起过直播先查询是否有未停止的直播
//            long liveId = liveQcloudRedis.getLiveIdByRoomId(roomId)
//            if(liveId == 0){
//                liveId = qcloudLiveRes.getLiveIdByRoomId(roomId)
//            }
//            //如果有调用停止直播逻辑
//            if(liveId !=0){
//                Map stopMap = stopLiveComm(liveId,roomId,userId,appId,"begin")
//                int status = stopMap.status as int
//                if(status != QcloudCommon.QCLOUD_SUCCESS){ //如果不成功则返回失败原因 客户端重试
//                    return stopMap
//                }
//                isStopRoom = true
//                //发送通知消息
//                sendMemberExit(userId,roomId,liveId,appId);
////                Thread.sleep(2*1000) //歇息两秒
//            }
//
//        }else{
//            roomId = createRoom(userId,appId)
//        }
        //直播码 模式改为 每次create的时候新增一个roomId 不用原来的了
        int roomId = createNewRoom(userId,appId)

        // 创建直播时返回预告状态，放在操作腾讯云下面 如果腾讯云操作没问题了 再进行后续操作
        def foreShow = liveRes.findLiveForeshowByForeshowId(foreshowId as long,appId)
        if(foreShow){
            int foreshowStatus = foreShow.foreshow_status? foreShow.foreshow_status as int :0
            if(foreshowStatus != 0 && foreshowStatus != 1){
                statusMap.put("status",QcloudCommon.QCLOUD_HAVE_FORESHOW)
                statusMap.put("msg","有预告")
            }
        }
        //组织直播数剧
        LiveRecord live = getLiveRecord(params,roomId);
        //创建直播
        boolean create = qcloudLiveRes.createLiveRecord(live)
        //调用IM接口创建群聊
        //createIMGroup(roomId,live)
        //更新正在直播列表
        liveQcloudRedis.updateLiveList(live);
        //更新房间对应正在直播的缓存
        liveQcloudRedis.setLiveIdWithRoomId(roomId,live.liveId);
        //设置心跳时间（严格来说心跳是第一次心跳来了设置时间的 但是时间太长，而且不用那么精确 所以就选择这里设置时间）
        liveQcloudRedis.setHeartTime(live.liveId)
        def backMap = [:]
        backMap.put("roomId",live.roomId)
        backMap.put("chatId",live.chatId)
        backMap.put("liveId",live.liveId)
        String shortUrl = shortURLService.getShortUrlLive([liveId:live.liveId,liveMode: live.liveMode,userId: live.userId,roomId: live.roomId, vc: live.vc, appId: appId])
        backMap.put("shortUrl",shortUrl)
        backMap.put("degreeFixed",1)
        //魅力值
        def userInfo = liveService.getUserInfo(userId)
        def gift = giftService.getUserCharmCount(userInfo,appId)
        backMap.put("charmCount",gift?.charmCount?:0)
        if(isStopRoom){
            backMap.put("status",QcloudCommon.QCLOUD_STOPBYBEIGN_SLEEP_ERROR)
            backMap.put("msg","结束推流休息两秒返回")
        }else{
            backMap.put("status",statusMap.status)
            backMap.put("msg",statusMap.msg)
        }

        log.info("backMap"+backMap)
        Parallel.run([1],{
            //通知新增直播帖子，通知发IM消息,延迟20秒
            liveService.addBlog(Long.valueOf(live.liveId))
        },1)
        Parallel.run([1],{
            //由于通知新增直播帖子，通知发IM消息的代码中有延迟20秒的操作，为避免影响其他的操作，固重新单独一个异步
            //通知王龙直播统计 TODO 暂时只是与王龙联调，后期会删掉，统一由分析nginx日志的项目处理
            long createTime = DateUtil.getTimestamp(live.createTime)
            long beginTime = createTime
            if(live.liveMode == LiveCommon.FORESHOW_TYPE_2){
                beginTime = DateUtil.getTimestamp(live.beginTime)
            }
            sendLiveDataToStatistic([jsonType:LiveCommon.STATISTIC_JSON_TYPE_1,liveId:live.liveId,foreshowId:live.foreshowId,roomId:live.roomId,
                                     userId:live.userId.toString(),nickname:live.nickname,title:live.title,isPrivate:live.isPrivate,
                                     liveMode:live.liveMode,appId:live.appId,vc:live.vc,createTime:createTime,beginTime:beginTime])
        },1)

        return backMap
    }
    /**
     * 处理上次结束直播异常结束的流程
     */
    Map dealLastErrorStop(long userId,String appId,String callEnv){
        def qcloudInfo = this.getQcloudInfo(appId)
        String secretId = qcloudInfo.secretId
        String secretKey = qcloudInfo.secretKey
        String liveAdminIdentifier = qcloudInfo.identifier
        Long sdkappId = (qcloudInfo.sdkAppId?:0) as long
        // 判断上次结束是否异常
        Map closeErrorMap = liveQcloudRedis.getLiveStopErrorCode(userId,appId)
        if(closeErrorMap){
            //先删除redis缓存，只强制停止一次，如果不成功就记录日志就行了 避免一直强制结束发生死循环
            liveQcloudRedis.delLiveStopErrorCode(userId,appId)
            int errorStatus = closeErrorMap.status? closeErrorMap.status as int : 0
            if(errorStatus !=0 && errorStatus != QcloudCommon.QCLOUD_SUCCESS){
                //有问题的话 用channelId重新结束一遍
                //TODO 这两个失败都应该记日志 入库
                //这里需要先解散房间 再结束推流，结束录制（反正是强制结束 不会因为没有房间结束不了）
                String desLog = QcloudLiveCommon.destroyIMGroup(closeErrorMap.roomId as int,getSignId(liveAdminIdentifier,appId),liveAdminIdentifier,sdkappId)
                log.info("流程：{},roomId={},调用强制结束IM记录直播日志:{}",callEnv,closeErrorMap.roomId,desLog)
//改成直播码模式 暂时不处理频道  以后可以改成调用直播码的关闭频道的接口
//                if(closeErrorMap.channelId){
//                    String stopLVBChannel = QcloudLiveCommon.stopLVBChannel(secretId,secretKey,closeErrorMap.channelId)
//                    log.info("流程：{},channelId={},userId={},强制停止频道日志:{}",callEnv,closeErrorMap.channelId,userId,stopLVBChannel)
//                    String deleteLVBChannel = QcloudLiveCommon.deleteLVBChannel(secretId,secretKey,closeErrorMap.channelId)
//                    log.info("流程：{},channelId={},userId={},强制删除频道日志:{}",callEnv,closeErrorMap.channelId,userId,deleteLVBChannel)
//                }else{
//                    log.info("流程：{},强制结束channelId为空",callEnv)
//                }
            }
        }
    }

    /**
     * {
     "liveBg":"直播背景图",
     "liveThumb":"直播列表缩略图",
     "title": "我发起了一个直播，快来看看吧！",
     "host": {
     "avatar": "http://user-pimg.b0.upaiyun.com/selfcreate/default/default_77.jpg!sy",
     "userId": "3476812",
     "userName": "qb0005"
     },
     "isPrivate": 0,
     "srpInfo":[
     {
     "srpId": "",
     "keyword":""
     }
     ]

     * @param param
     * @return
     */

    LiveRecord getLiveRecord(def params,int roomId){
        long liveid = liveQcloudRedis.getLiveId();
        LiveRecord live = new LiveRecord();
        live.liveId = liveid;
        live.appModel =params.appModel;
        live.appId = Strings.getAppId(params);
        live.chatId = roomId
        live.roomId = roomId
        live.createTime = Strings.getDayOfTime(new Date())
        live.userId=params.host.userId as long
        live.userImage=params.host.userImage
        live.nickname=params.host.nickname
        live.liveBg=params.liveBg
        live.liveThump=params.liveThumb
        live.title=params.title
        live.isPrivate = params.isPrivate? params.isPrivate as int :0;
        live.srpId = params.srpInfo? params.srpInfo.srpId && Strings.toListString(params.srpInfo.srpId)?Strings.toListString(params.srpInfo.srpId):liveDefalutSrpId:liveDefalutSrpId
        live.keyword = params.srpInfo?params.srpInfo.keyword && Strings.toListString(params.srpInfo.keyword,LiveCommon.KEYWORD_SEPARATOR)?Strings.toListString(params.srpInfo.keyword,LiveCommon.KEYWORD_SEPARATOR):liveDefalutKeyword:liveDefalutKeyword
        live.foreshowId = (params.foreshowId ?: 0) as long
        if(live.foreshowId){  //默认非官方直播 有预告id的为官方直播
            live.liveType = 1
            live.isPrivate = 0 //官方直播只能是公开类型的直播
        }
        live.vc = params.vc ?: "5.5.0"
        live.m3u8Url= getPlayUrl(live.appId,live.roomId,live.userId,"m3u8")
        live.playUrl= getPlayUrl(live.appId,live.roomId,live.userId,"flv")
        return live
    }

    String getPlayUrl(String appId,int roomId, long userId, String suffix ){

        def config = getQcloudInfo(appId)
        int bizid = config.bizid as int
        String md5String = MD5.stringToMD5(roomId+"_"+userId+"_"+"main");
        String url = """http://"""+bizid+"""."""+QcloudCommon.LIVE_PLAY_URL + bizid+"""_"""+md5String+"""."""+suffix
        return url
    }
    /**
     * 获取房间号
     * @param userId
     * @param appId
     * @return
     */
    int getRoomId(long userId,String appId){
        int roomId = liveQcloudRedis.getRoomId(userId,appId)
        //如果取不到roomId则到mysql中取一次roomId,然后更新到redis
        if(roomId == 0){
            //取roomId
            roomId = qcloudLiveRes.getRoomIdByUserIdAndAppId(userId,appId)
        }
        return roomId
    }
    /**
     * 生成直播
     * @param userId
     * @param appId
     * @return
     */
    int createRoom(long userId,String appId){
        //生成roomId
        int roomId = liveQcloudRedis.generateRoomId();
        //更新redis //因为唯一性要先查redis 所以先更新redis 这样入库就可以异步了
        liveQcloudRedis.setRoomId(userId,appId,roomId)
        Parallel.run([1],{
            //入库
            qcloudLiveRes.insertRoomId(userId,appId,roomId)
        },1)
        return roomId
    }

    /**
     * 生成直播
     * @param userId
     * @param appId
     * @return
     */
    int createNewRoom(long userId,String appId){
        //生成roomId
        int roomId = liveQcloudRedis.generateRoomId();
        return roomId
    }

    /**
     * 开始直播接口 直播推流加录制
     * @param liveId
     * @return
     */
    @Override
    Map beginLive(long liveId,int roomId,long userId,String appId) {
        if(true){
            return ["status":QcloudCommon.QCLOUD_SUCCESS]
        }
        def qcloudInfo = this.getQcloudInfo(appId)
        Long sdkappId = (qcloudInfo.sdkAppId?:0) as long
        log.info("推流开始：liveId===>"+liveId);
        Map resultMap = [:]
        Map updateParam = [:]
        //create成功，但是start过程中，客户端的定时任务发起了结束直播，导致live_record表中没有记录
        def findRes = liveRes.findLiveByLiveId(liveId as long)
        if(!findRes){
            log.info("beginLive LIVE_NOT_EXIST,liveId=>{}",liveId)
            return ["status":QcloudCommon.LIVE_NOT_EXIST_APP_STOP]
        }
        //直播推流
        // 更新推流时间
        long pushTime = System.currentTimeMillis()
        liveQcloudRedis.setLivePushTime(liveId,pushTime)   //不管成功不成功都需要修改推流时间 否则推流失败的时候 直播时长没法计算了
        updateParam.put("pushTime",new Date(pushTime))

        String signId = getSignId(userId as String,appId)
        String tuiliuReturn = QcloudLiveCommon.tuiliuBegin(liveId,roomId,sdkappId,userId,signId,liveEnv)
        def tuiliuObject = JSON.parse(tuiliuReturn)
        String err = tuiliuObject?.rspbody?.rsp_0x6?.str_errorinfo
        int errCode = tuiliuObject?.rspbody?.rsp_0x6?.uint32_result? tuiliuObject?.rspbody?.rsp_0x6?.uint32_result as int:0
        if(err != null && err.trim().length() > 0){
            if(errCode == QcloudCommon.QCLOUD_NOUSER_ERROR){
                resultMap.put("status",QcloudCommon.QCLOUD_NOUSER_ERROR)
            }else if (errCode == QcloudCommon.QCLOUD_NOROOM_CODE){
                resultMap.put("status",QcloudCommon.QCLOUD_NOROOM_ERROR)
            }else{
                resultMap.put("status",QcloudCommon.QCLOUD_NORESEAON_ERROR)
            }
            resultMap.put("msg",tuiliuReturn)
            //TODO 记日志
            boolean  updateLive = qcloudLiveRes.updateLiveRecord(updateParam)
            log.info("参数:{},liveId = {},推流失败更新推流时间返回updateLive = {}",updateParam,liveId,updateLive)
            return resultMap;
        }
        //腾讯云返回错误格式 // {"ActionStatus":"FAIL","ErrorCode":60015,"ErrorInfo":"body account invalid"}
        String actionStatus = tuiliuObject?.ActionStatus
        if(actionStatus != null && actionStatus.trim().length() >0){
            resultMap.put("status",QcloudCommon.QCLOUD_NORESEAON_ERROR)
            resultMap.put("msg",tuiliuReturn)
            boolean  updateLive = qcloudLiveRes.updateLiveRecord(updateParam)
            log.info("参数:{},liveId = {},推流失败更新推流时间返回updateLive = {}",updateParam,liveId,updateLive)
            return resultMap;
        }
        //调用IM接口创建群聊
        //createIMGroup(roomId,live)
        // QcloudLiveCommon.createIMGroup(roomId,signId,userId,sdkappId)
        //推流成功后

        // 更新直播列表
        String channelId = tuiliuObject?.rspbody?.rsp_0x6?.uint64_channel_id
        updateParam.put("channelId",channelId)
        def m3u8UrlList = tuiliuObject?.rspbody?.rsp_0x6?.msg_live_url?.string_play_url
        String m3u8Url = "";
        if(m3u8UrlList.size()!=0){
            m3u8Url = m3u8UrlList[0]
        }
        updateParam.put("m3u8Url",m3u8Url)
        updateParam.put("liveId",liveId)

        log.info("updateParam:"+updateParam);
        boolean  updateLive = qcloudLiveRes.updateLiveRecord(updateParam)
        if(!updateLive){
            log.info("liveId:{},更新live_record表失败！",liveId)
            return ["status":QcloudCommon.LIVE_NOT_EXIST_APP_STOP]
        }
        //更新直播redis
        LiveRecord liveRecord = liveQcloudRedis.getLiveRecord(liveId as long)
        liveRecord.pushTime = DateUtil.getFormatDteByLong(pushTime)
        liveRecord.channelId = channelId
        liveRecord.m3u8Url = m3u8Url
        liveQcloudRedis.updateLiveList(liveRecord)

        //其他业务 异步开始即可
        Parallel.run([1],{
            //直播录制
            String luzhiReturn = QcloudLiveCommon.luzhiBegin(liveId,roomId,sdkappId,userId,signId)
        },10)
        log.info("推流正常返回：liveId===>"+liveId)
        return ["status":QcloudCommon.QCLOUD_SUCCESS]
    }

    @Override
    Map stopLive(long liveId,int roomId,long userId,String appId) {
        return stopLiveComm(liveId,roomId,userId,appId,"接口调用直播结束")
    }
    Map stopLiveComm(long liveId,int roomId,long userId,String appId,String stopMsg){
        def qcloudInfo = this.getQcloudInfo(appId)
        String secretId = qcloudInfo.secretId
        String secretKey = qcloudInfo.secretKey
        String liveAdminIdentifier = qcloudInfo.identifier
        Long sdkappId = (qcloudInfo.sdkAppId?:0) as long
        String logmsg = stopMsg;
        if("begin".equals(stopMsg)){
            logmsg = "开始直播调用=="
        }
        log.info("流程： {} 直播结束,liveId={}",logmsg,liveId)
        //定义结束推流异常存储
        def exceptionFlagMap = [:]
        //默认成功
        exceptionFlagMap.put("status",QcloudCommon.QCLOUD_SUCCESS)
        //先查出直播对象 保存下来
        LiveRecord nowLive = liveService.findLiveByLiveId(liveId)
        //如果拿不到直播信息的话 说明直播已经通过服务器结束过了 所以只调用腾讯云接口，不需要操作数据库和redis
        if(nowLive){  //结束直播 先操作数据库记录 再调用腾讯云接口  保证多次调用时本地数据不会乱
            //插入live_record_log表 删除live_record表
            //插入live_record_log表
            Map liveRecordMap = [:]
            //直播时长
//            long pushTime = liveQcloudRedis.getLivePushTime(liveId)
//            long timespan = 0L
//            if(pushTime){
//                timespan = (System.currentTimeMillis() - pushTime)/1000
//                nowLive.timeSpan = timespan
//            }
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
            def conf = liveRes.findLiveBackVedioConfig(appId)
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
            //通知删除直播贴
            def kestrelMap = [:]
            kestrelMap.put("operType",2)
            kestrelMap.put("userId",userId)
            kestrelMap.put("liveId",liveId)
            kestrelMap.put("avRoomId",roomId)
            kestrelMap.put("chatRoomId",roomId)
            kestrelMap.put("admireCount",admireCount)
            kestrelMap.put("watchCount",totalWatchCount)
            kestrelMap.put("timeSpan",timespan)
            kestrelMap.put("status",3)  //2，是有回访 3，是无回放（跟目前定义的字段不一样）
            kestrelMap.put("vc","5.4.0")
            def liveArray = []
            liveArray.add(0,kestrelMap)
            liveKestrel.sendLiveBlogMsg(Strings.toJson(liveArray))
        }
//kpc    去掉互动直播结束录制 结束推流逻辑
//        String signId = getSignId(userId as String,appId)
//
//        //先结束录制
//        String luzhiendReturn = QcloudLiveCommon.luzhiEnd(liveId,roomId,sdkappId,userId,signId)
//        log.info("流程：{},录制结束返回值：{},liveId{}",logmsg,luzhiendReturn,liveId);
//        //录制结束后需要回填录制地址,
//        //如果结束录制有问题，则放入队列，定时任务去取录制地址
//        def luzhiObject = JSON.parse(luzhiendReturn)
//        String err = luzhiObject?.rspbody?.rsp_0x5?.str_errorinfo
//        String actionStatus = luzhiObject?.ActionStatus
//        String videoAddress = "";
//        //videoAddress = QcloudLiveCommon.getVodPlayListInfo(secretId,secretKey,liveId,roomId)
//        if(StringUtils.isBlank(err) && StringUtils.isBlank(actionStatus)){
//            def str_fileID = luzhiObject?.rspbody?.rsp_0x5?.str_fileID
//            String fileId = StringUtils.join(str_fileID, ",");
//            //拿到fileId之后先去更新一下live_record_log 防止定时取回看的任务比下面异步取回看的先执行时 取不到fileId
//            qcloudLiveRes.updateLiveRecordVideoAddress(null, liveId,fileId)
//            // 异步结束录制
//            Parallel.run([1],{
//                Thread thread = Thread.currentThread();
//                thread.sleep(5*1000);
//                log.info("liveId==>{},roomId==>{},异步通过fileId取回看信息开始",liveId,roomId)
//
//                List lastList = new ArrayList();
//                fileId?.split(",").each {
//                    videoAddress = QcloudLiveCommon.getVodPlayListInfoByFileId(secretId, secretKey, it)
//                    List vo = Strings.parseJson(videoAddress,List)
//                    lastList.addAll(vo)
//                }
//                qcloudLiveRes.updateLiveRecordVideoAddress(Strings.toJson(lastList), liveId,null)
////                videoAddress = QcloudLiveCommon.getVodPlayListInfoByFileId(secretId, secretKey, fileId)
////                qcloudLiveRes.updateLiveRecordVideoAddress(videoAddress, liveId,null)
//                if(nowLive && nowLive.foreshowId){
//                    log.info("调用合并预告,foreshowId:{}",nowLive.foreshowId)
//                    liveService.updateForeshowMergeInfo(Long.valueOf(nowLive.foreshowId),appId)
//                }
//            },1)
//
//        }
//
//        //再结束推流
//        //推流异常时需要记录标记
//        String tuiliuEndRetrun = QcloudLiveCommon.tuiliuEnd(liveId,roomId,sdkappId,userId,signId);
//        log.info("流程：{},推流结束返回结果,liveId=>{},tuiliuEndString=>{}",logmsg,liveId,tuiliuEndRetrun)
//        def tuiliuObject = JSON.parse(tuiliuEndRetrun)
//        //腾讯云返回错误格式 // {"ActionStatus":"FAIL","ErrorCode":60015,"ErrorInfo":"body account invalid"}
//        String tuiliuActionStatus = tuiliuObject?.ActionStatus
//        if(tuiliuActionStatus != null && tuiliuActionStatus.trim().length() >0){
//            // throw new ApiException(ApiException.STATUS_BUSINESS_ERROR,"stop:"+tuiliuEndRetrun)
//            exceptionFlagMap.put("status",QcloudCommon.QCLOUD_STOPTUILIU_ERROR)
//            exceptionFlagMap.put("msg",tuiliuEndRetrun)
//            //TODO 结束推流失败 应该取出channelId 返回 服务器端或者客户端 用channelId结束直播
//            //情况不会很多 所以直接查下库就行
//            LiveRecord liveRecord = liveService.findLiveRecordByLiveId(liveId)
//            exceptionFlagMap.put("channelId",liveRecord.channelId)
//            exceptionFlagMap.put("roomId",liveRecord.roomId)
//            //设置redis存储 失败原因，开始直播的时候调用一下
//            liveQcloudRedis.setLiveStopErrorCode(userId,appId,exceptionFlagMap)
//        }
//
//        String tuiliuErr = tuiliuObject?.rspbody?.rsp_0x6?.str_errorinfo
//        int errCode = tuiliuObject?.rspbody?.rsp_0x6?.uint32_result as int
//        if(tuiliuErr != null && tuiliuErr.trim().length() > 0){
//            //如果有错误则强制结束
//            //情况不会很多 所以直接查下库就行
//            LiveRecord liveRecord = liveService.findLiveRecordByLiveId(liveId)
//            String channelId = liveRecord.channelId
//            if(channelId){
//                String stopLVBChannel = QcloudLiveCommon.stopLVBChannel(secretId,secretKey,channelId)
//                log.info("流程：{},channelId={},userId={},liveId={},强制停止频道日志(结束推流失败直接调用):{}",logmsg,channelId,userId,liveId,stopLVBChannel)
//                def stopObj = Strings.parseJson(stopLVBChannel)
//                int code = stopObj?.code ? stopObj?.code as int : 0
//                if(code !=0){
//                    //TODO 落地日志
//                    exceptionFlagMap.put("status",QcloudCommon.QCLOUD_STOPTUILIU_ERROR)
//                    exceptionFlagMap.put("msg",stopLVBChannel)
//                    exceptionFlagMap.put("roomId",liveRecord.roomId)
//                    exceptionFlagMap.put("channelId",channelId)
//                    liveQcloudRedis.setLiveStopErrorCode(userId,appId,exceptionFlagMap)
//                }
//                String deleteLVBChannel = QcloudLiveCommon.deleteLVBChannel(secretId,secretKey,channelId)
//                log.info("流程：{},channelId={},userId={},liveId={},强制删除频道日志（结束推流失败直接调用）:{}",logmsg,channelId,userId,liveId,deleteLVBChannel)
//                def delObj = Strings.parseJson(deleteLVBChannel)
//                int delCode = delObj?.code ? delObj?.code as int : 0
//                if(delCode !=0){
//                    //TODO 落地日志
//                    exceptionFlagMap.put("status",QcloudCommon.QCLOUD_STOPTUILIU_ERROR)
//                    exceptionFlagMap.put("msg",deleteLVBChannel)
//                    exceptionFlagMap.put("roomId",liveRecord.roomId)
//                    exceptionFlagMap.put("channelId",channelId)
//                    liveQcloudRedis.setLiveStopErrorCode(userId,appId,exceptionFlagMap)
//                }
//            }
//        }


        if(!"begin".equals(stopMsg)){
            //解散IM群组, 必须在最后 要不然结束推流和结束录制的时候会报no room
            String destroyIMGroup = QcloudLiveCommon.destroyIMGroup(roomId,getSignId(liveAdminIdentifier,appId),liveAdminIdentifier,sdkappId)
            log.info("流程："+logmsg+"liveId:"+liveId+"roomId:"+roomId+"强制解散房间返回:"+ destroyIMGroup)
            def desObj = destroyIMGroup?Strings.parseJson(destroyIMGroup):null
            //错误   {"ActionStatus":"FAIL","ErrorCode":10010,"ErrorInfo":"this group does not exist"} //房间不存在也默认为已经结束成功了
            //正确  {"ActionStatus": "OK","ErrorInfo": "","ErrorCode": 0}
            int errorCode = desObj ?desObj.ErrorCode as int : -1 //强制解散房间异常
            if(errorCode !=0 && errorCode != QcloudCommon.QCLOUD_IM_NO_ROOM_CODE){
                exceptionFlagMap.put("status",QcloudCommon.QCLOUD_NORESEAON_ERROR)
                exceptionFlagMap.put("msg",destroyIMGroup)
                //情况不会很多 所以直接查下库就行
                LiveRecord liveRecord = liveService.findLiveRecordByLiveId(liveId)
                exceptionFlagMap.put("channelId",liveRecord.channelId)
                exceptionFlagMap.put("roomId",liveRecord.roomId)
                liveQcloudRedis.setLiveStopErrorCode(userId,appId,exceptionFlagMap)
            }
        }

        Parallel.run([1],{
            //通知王龙直播统计add by liaojing 20170315 TODO 暂时只是与王龙联调，后期会删掉，统一由分析nginx日志的项目处理
            Map params = [
                liveId:liveId,
                roomId:roomId,
                createTime:System.currentTimeMillis(),
                userId:userId as String,
                userAction:QcloudCommon.AVIMCMD_DESTORY,
                msgType:QcloudCommon.CALLBACK_AFTERGROUP_DESTROYED,
                jsonType:LiveCommon.STATISTIC_JSON_TYPE_2
            ]
            this.sendLiveDataToStatistic(params)
            //通知appId人数减去真实观众数，云平台需求
            liveService.addAppLimitInfo(liveId,0)
        },1)
        return exceptionFlagMap
    }

    /**
     * 回调调用结束直播
     * @param liveId
     * @param roomId
     * @param userId
     * @param appId
     * @param stopMsg
     */
    void stopByCallBy(long liveId,int roomId,long userId,String appId,String logmsg){
        def qcloudInfo = this.getQcloudInfo(appId)
        String secretId = qcloudInfo.secretId
        String secretKey = qcloudInfo.secretKey
        Long sdkappId = (qcloudInfo.sdkAppId?:0) as long

        String signId = getSignId(userId as String,appId)
        //先结束录制
        String luzhiendReturn = QcloudLiveCommon.luzhiEnd(liveId,roomId,sdkappId,userId,signId)
        log.info("流程：{},录制结束返回值：{},liveId{}",logmsg,luzhiendReturn,liveId);

        //再结束推流
        //推流异常时需要记录标记
        String tuiliuEndRetrun = QcloudLiveCommon.tuiliuEnd(liveId,roomId,sdkappId,userId,signId);
        log.info("流程：{},推流结束返回结果,liveId=>{},tuiliuEndString=>{}",logmsg,liveId,tuiliuEndRetrun)
        def tuiliuObject = JSON.parse(tuiliuEndRetrun)


        String tuiliuErr = tuiliuObject?.rspbody?.rsp_0x6?.str_errorinfo
        if(tuiliuErr != null && tuiliuErr.trim().length() > 0){
            //如果有错误则强制结束
            //情况不会很多 所以直接查下库就行
            LiveRecord liveRecord = liveService.findLiveRecordByLiveId(liveId)
            String channelId = liveRecord.channelId
            if(channelId){
                String stopLVBChannel = QcloudLiveCommon.stopLVBChannel(secretId,secretKey,channelId)
                log.info("流程：{},channelId={},userId={},liveId={},强制停止频道日志(结束推流失败直接调用):{}",logmsg,channelId,userId,liveId,stopLVBChannel)

                String deleteLVBChannel = QcloudLiveCommon.deleteLVBChannel(secretId,secretKey,channelId)
                log.info("流程：{},channelId={},userId={},liveId={},强制删除频道日志（结束推流失败直接调用）:{}",logmsg,channelId,userId,liveId,deleteLVBChannel)

            }
        }
    }


    @Override
    int sendHeadRate(long liveId,long timespan) {
        log.info("接收到心跳，liveId："+liveId+",timespan:"+timespan)
        liveQcloudRedis.setHeartTime(liveId)
        //当网络波动较大的时候，服务端长时间收不到心跳已经结束了直播，并通知了客户端，
        // 但是因为没网络，客户端收不到，造成主播方还在直播，也一直在发心跳
        if(liveQcloudRedis.getLiveRecord(liveId as long)){
            return LiveStatus.SUCCESS.getStatus()
        }else{
            return QcloudCommon.LIVE_NOT_EXIST_SERVER_STOP
        }
    }

    @Override
    String sendGiftImMsg(Map map) {
        def qcloudInfo = this.getQcloudInfo(map.appId)
        String liveAdminIdentifier = qcloudInfo.identifier
        Long sdkappId = (qcloudInfo.sdkAppId?:0) as long

        long fromAccount = (map.fromAccount?:0) as long
        String sigId = getSignId(liveAdminIdentifier,map.appId)
        int userAction = QcloudCommon.AVIMCMD_GIFT
        Map actionParam = [ userInfo: map.userInfo, anchorInfo:map.anchorInfo,giftInfo: map.giftInfo, giftCount: map.giftCount ]
        String res = QcloudLiveCommon.sendIMMsg(map.roomId as int, sigId,sdkappId,liveAdminIdentifier,fromAccount,userAction,actionParam)
        imMsgLog.info("礼物打赏IM消息发送，map=>{},res=>{}",map,res)
        return res
    }

    @Override
    String  getGroupMemberInfo(Map map) {
        def qcloudInfo = this.getQcloudInfo(map.appId)
        String liveAdminIdentifier = qcloudInfo.identifier
        Long sdkappId = (qcloudInfo.sdkAppId?:0) as long

        String sigId = getSignId(liveAdminIdentifier,map.appId)
        String res = QcloudLiveCommon.getGroupMemberInfo(map.roomId as int,map.offset as int,sigId,sdkappId,liveAdminIdentifier)
        imMsgLog.info("获取用在群组中的身份信息，map=>{},res=>{}",map,res)
        return res
    }

    @Override
    String sendVestIntoRoomMsg(Map map) {
        def qcloudInfo = this.getQcloudInfo(map.appId)
        String liveAdminIdentifier = qcloudInfo.identifier
        Long sdkappId = (qcloudInfo.sdkAppId?:0) as long

        long fromAccount = map.fromAccount as long
        String sigId = getSignId(liveAdminIdentifier,map.appId)
        int userAction = QcloudCommon.AVIMCMD_ENTERLIVE_FILL_DATA
        def actionParam = map.userInfo
        String res = QcloudLiveCommon.sendIMMsg(map.roomId as int, sigId,sdkappId,liveAdminIdentifier,fromAccount,userAction,actionParam)
        imMsgLog.info("马甲入群消息发送,res=>{},map=>{}",res,map)
        return res
    }

    @Override
    String sendVestCountMsg(Map map) {
        def qcloudInfo = this.getQcloudInfo(map.appId)
        String liveAdminIdentifier = qcloudInfo.identifier
        Long sdkappId = (qcloudInfo.sdkAppId?:0) as long

        long fromAccount = map.fromAccount as long
        String sigId = getSignId(liveAdminIdentifier,map.appId)
        int userAction = QcloudCommon.AVIMCMD_SYNCHRONIZING_INFORMATION
        Map actionParam = map.msgInfo
        String res = QcloudLiveCommon.sendIMMsg(map.roomId as int, sigId,sdkappId,liveAdminIdentifier,fromAccount,userAction,actionParam)
        imMsgLog.info("增加马甲数量消息发送,res=>{},map=>{}",res,map)
        return res
    }

    @Override
    String sendVestMsg(Map map) {
        def qcloudInfo = this.getQcloudInfo(map.appId)
        String liveAdminIdentifier = qcloudInfo.identifier
        Long sdkappId = (qcloudInfo.sdkAppId?:0) as long

        long fromAccount = map.fromAccount as long
        String sigId = getSignId(liveAdminIdentifier,map.appId)
        int userAction = QcloudCommon.AVIMCMD_SEND_MESSAGE
        Map actionParam = map.msgInfo
        String res = QcloudLiveCommon.sendIMMsg(map.roomId as int, sigId,sdkappId,liveAdminIdentifier,fromAccount,userAction,actionParam)
        imMsgLog.info("马甲评论消息发送,res=>{},map=>{}",res,map)
        return res
    }
    @Override
    String sendVestDoPrimeMsg(Map map){
        def qcloudInfo = this.getQcloudInfo(map.appId)
        String liveAdminIdentifier = qcloudInfo.identifier
        Long sdkappId = (qcloudInfo.sdkAppId?:0) as long

        long fromAccount = map.fromAccount as long
        String sigId = getSignId(liveAdminIdentifier,map.appId)
        int userAction = QcloudCommon.AVIMCMD_USER_OPEAR
        Map actionParam = map.msgInfo
        String res = QcloudLiveCommon.sendIMMsg(map.roomId as int, sigId,sdkappId,liveAdminIdentifier,fromAccount,userAction,actionParam)
        imMsgLog.info("马甲点赞或者关注消息,res=>{},map=>{}",res,map)
        return res
    }
    @Override
    def forbidComment(Map map) {
        def qcloudInfo = this.getQcloudInfo(map.appId)
        String liveAdminIdentifier = qcloudInfo.identifier
        Long sdkappId = (qcloudInfo.sdkAppId?:0) as long

        long forbidUserId = map.forbidUserId as long
        String sigId = getSignId(liveAdminIdentifier,map.appId)
        def res = QcloudLiveCommon.forbidComment(map.roomId as int, sigId, sdkappId, liveAdminIdentifier, forbidUserId)
        log.info("wangtf forbid member comment, res=>{}, map=>{}",res, map)
        return res
    }

    @Override
    def kickGroupMember(Map map) {
        def qcloudInfo = this.getQcloudInfo(map.appId)
        String liveAdminIdentifier = qcloudInfo.identifier
        Long sdkappId = (qcloudInfo.sdkAppId?:0) as long

        long kickUserId = map.kickUserId as long
        String sigId = getSignId(liveAdminIdentifier,map.appId)
        def res = QcloudLiveCommon.kickGroupMember(map.roomId as int, sigId, sdkappId, liveAdminIdentifier, kickUserId)
        log.info("wangtf kick group member, res=>{}, map=>{}",res, map)
        return res
    }

    @Override
    String sendVestImMsg(Map map){
        def qcloudInfo = this.getQcloudInfo(map.appId)
        String liveAdminIdentifier = qcloudInfo.identifier
        Long sdkappId = (qcloudInfo.sdkAppId?:0) as long

        long fromAccount = map.fromAccount as long
        String sigId = getSignId(liveAdminIdentifier,map.appId)
        int userAction = QcloudCommon.AVIMCMD_SHOW_INFO
        Map actionParam = map.msgInfo
        String res = QcloudLiveCommon.sendIMMsg(map.roomId as int, sigId,sdkappId,liveAdminIdentifier,fromAccount,userAction,actionParam)
        imMsgLog.info("马甲发送点赞消息,res=>{},map=>{}",res,map)
        return res
    }
    /**
     *
     * @return
     */
    String sendMemberExit(long fromAccount,int roomId,long liveId,String appId){
        def qcloudInfo = this.getQcloudInfo(appId)
        String liveAdminIdentifier = qcloudInfo.identifier
        Long sdkappId = (qcloudInfo.sdkAppId?:0) as long

        String sigId = getSignId(liveAdminIdentifier,map.appId)
        int userAction = QcloudCommon.AVIMCMD_ROOM_EXIT
        Map actionParam = ["liveId":liveId,roomId: roomId]
        String res = QcloudLiveCommon.sendIMMsg(roomId, sigId,sdkappId,liveAdminIdentifier,fromAccount,userAction,actionParam)
        log.info("liveId={},roomId={}, 发送成员退出房间消息返回：{}",liveId,roomId,res)
    }

    def deleteVodFile(List<String> fileIds,String appId){
        def qcloudInfo = this.getQcloudInfo(appId)
        String secretId = qcloudInfo.secretId
        String secretKey = qcloudInfo.secretKey
        return QcloudLiveCommon.deleteVodFile(secretId,secretKey,fileIds);
    }
    @Override
    String getBackInfo(int roomId, long liveId,long foreshowId,String appId,String msg) {
        log.info("流程： {}： liveId={},按前缀 roomId+liveId 取回放开始",msg,liveId)
        def qcloudInfo = this.getQcloudInfo(appId)
        String secretId = qcloudInfo.secretId
        String secretKey = qcloudInfo.secretKey
        //按前缀取 首先按名称前缀取 roomId_liveId 取MP4格式
        String prefix = roomId+"_"+liveId
        String videoAddress = QcloudLiveCommon.getVodPlayListInfo(secretId, secretKey, liveId,roomId,prefix)
        if(videoAddress){
            log.info("流程： {}： liveId={},按前缀 roomId+liveId 取回放返回：{}" ,msg, liveId ,  videoAddress)
        }
        if(videoAddress){
            //修改liverecord_log表中vedio_adress字段的值
            qcloudLiveRes.updateLiveRecordVideoAddress(videoAddress, liveId,null)
            if(foreshowId){
                log.info("流程： {}  调用合并预告,liveId=>{},foreshowId:{}",msg, liveId, foreshowId)
                liveService.updateForeshowMergeInfo(foreshowId,appId)
            }
        }
        return videoAddress
    }

    @Override
    String sendPgcMsg(Map map) {
        def qcloudInfo = this.getQcloudInfo(map.appId)
        String liveAdminIdentifier = qcloudInfo.identifier
        Long sdkappId = (qcloudInfo.sdkAppId?:0) as long

        long fromAccount = map.fromAccount as long
        String sigId = getSignId(liveAdminIdentifier,map.appId)
        int userAction = QcloudCommon.AVIMCMD_PGC_STATUS
        Map actionParam = map.msgInfo
        String res = QcloudLiveCommon.sendIMMsg(map.roomId as int, sigId,sdkappId,liveAdminIdentifier,fromAccount,userAction,actionParam)
        imMsgLog.info("会议直播操作返回值,res=>{},map=>{}",res,map)
        return res
    }

    @Override
    def findLiveCommentList(Map map) {
        List commentList = qcloudLiveRes.findQcloudMsgList(map)
        List resultList = []
        commentList?.each{
            def commentData = Strings.parseJson(it.msg as String)
            def data = commentData?.MsgBody?.MsgContent?.Data ?:[]
            if(data){
                def dataMap = Strings.parseJson(data.get(0))
                dataMap.id = it.create_time as long
                resultList.add(dataMap)
            }
        }
        return resultList
    }
    @Override
    def findLiveRecordCommentList(Map map) {
        long liveId = (map.liveId ?: 0) as long
        long foreshowId = (map.foreshowId ?: 0 ) as long
        List liveIdList = []
        if(foreshowId){
            List liveRecordList = liveRes.findLiveRecordListByForeId([foreshowId: foreshowId,appId:map.appId])
            liveRecordList.each{
                liveIdList.add(it.live_id as long)
            }
        }else {
            liveIdList.add(liveId)
        }
        map.put("liveIdList", liveIdList)
        List commentList = qcloudLiveRes.findQLiveRecordMsgList(map)
        List resultList = []
        commentList?.each{
            def commentData = Strings.parseJson(it.msg as String)
            def data = commentData?.MsgBody?.MsgContent?.Data ?:null
            System.out.println(commentData?.MsgBody?.MsgContent?.Data)
            def dataMap = [:]
            if(data){
                dataMap = Strings.parseJson(data.get(0)) ?: [:]
            }
            if(it.user_action as int == -1){
                def message = ""
                if(data && dataMap){
                    message = commentData?.MsgBody?.MsgContent?.Text.get(0).get(0) ?: ""
                    try{
                        def actionParam = dataMap.actionParam
                        dataMap.actionParam = [message: message, userId: actionParam.userId, nickname: actionParam.nickname, userImage: ImageUtils.getSmallImg(actionParam.userImage)]
                    }catch(Exception e){
                        def actionParam = dataMap.actionParam.get(0)
                        dataMap.actionParam = [message: message, userId: actionParam.userId, nickname: actionParam.nickname, userImage: ImageUtils.getSmallImg(actionParam.userImage)]
                    }
                }else {
                    message = commentData?.MsgBody?.MsgContent?.Text.get(0) ?: ""
                    long userId = (it.user_id ?: 0) as long
                    def userInfo = liveService.getUserInfo(userId)
                    dataMap.userAction = QcloudCommon.AVIMCMD_Comment
                    dataMap.actionParam = [message: message, userId: userInfo.userId, nickname: userInfo.nickname, userImage: ImageUtils.getSmallImg(userInfo.userImage)]
                }
            }
            dataMap.id = it.create_time as long
            resultList.add(dataMap)
        }
        return resultList
    }

    @Override
    def sendLiveDataToStatistic(Map dateMap){
        if(!kafkaRes.liveStatisticStart || !dateMap){
            return
        }
        long liveId = (dateMap.liveId?:0) as long
        String user_id = (dateMap.userId?:"") as String
        int user_action = (dateMap.userAction?:0) as int
        int jsonType = (dateMap.jsonType?:0) as int
        if(user_action==QcloudCommon.AVIMCMD_ENTERLIVE_FILL_DATA
            || user_action==QcloudCommon.AVIMCMD_SYNCHRONIZING_INFORMATION
            || user_action==QcloudCommon.AVIMCMD_USER_OPEAR){
            return
        }
        try{
            //jsonType 1:表示创建直播，2：表示腾讯云回调，3：直播结束
            dateMap.remove("msg")//王龙的统计不需要msg的内容
            dateMap.put("sourceFrom","app")
            String dataJson = JSON.toJSON(dateMap)
            callBackLog.info("liaojing sendLiveDataToStatistic to kafka,dataJson=>{}",dataJson)
            kafkaRes.sendLiveData(dataJson)
//            由于统计系统暂时无人员进行下一步开发，固暂时不改规则，还是统一入到kafka中
//            if(jsonType == LiveCommon.STATISTIC_JSON_TYPE_2){//由于腾讯云回调量较大，所以直接使用kafka
//                callBackLog.info("liaojing sendLiveDataToStatistic to kafka,dataJson=>{}",dataJson)
//                kafkaRes.sendLiveData(dataJson)
//            }else{
//                callBackLog.info("liaojing sendLiveDataToStatistic to nginx,dataJson=>{}",dataJson)
//                Http.post(liveNginxLogUrl,[dataJson:dataJson])
//            }
        }catch (Exception e){
            callBackLog.error("sendLiveDataToStatistic error,Exception=>{}",e.getMessage())
        }
    }

    @Override
    def getQcloudInfo(String appId){
        def map = [:]
        try{
            def result = liveQcloudRedis.getCloudInfoRedis(appId);
            if(result){
//                log.info("从Redis中获取腾讯云配置项,appId=>{},config=>{}",appId,result);
                def rs = Strings.parseJson(result)
                return rs;
            }
            def cloudResult= qcloudLiveRes.getQcloudInfo(appId)
            if(cloudResult !=null){
                def configInfo = Strings.parseJson(cloudResult.config_info)
                map.put("secretId",configInfo?.secret_id?:"")
                map.put("secretKey",configInfo?.secret_key?:"")
                map.put("sdkAppId",configInfo?.sdkappid?:"")
                map.put("identifier",configInfo?.account?:"")
                map.put("identifierType",configInfo?.account_type?:"")
                map.put("publicKey",configInfo?.public_key?:"")
                map.put("privateKey",configInfo?.private_key?:"")
                map.put("bizid",configInfo?.bizid?:"")
                map.put("pushKey",configInfo?.push_key?:"")
                map.put("playKey",configInfo?.playKey?:"")
                map.put("piliKey",configInfo?.piliKey?:"")
                map.put("apiKey",configInfo?.api_key?:"")
                map.put("qcloudAppid",configInfo?.qcloud_appid?:"")
                map.put("liveshareValue",cloudResult?.liveshare_value?:"")
                map.put("mallUrl",cloudResult.mall_url)
                map.put("faceListLimit",cloudResult.face_list_limit)
                map.put("appId",cloudResult.app_id)

                log.info("从数据库中获取腾讯云配置项,appId=>{},config=>{}",appId,map);
                liveQcloudRedis.setCloudInfoRedis(appId,map);
            }else{
                log.info("腾讯云配置项不完善,appId=>{}",appId);
            }
        }catch (Exception e){
            e.printStackTrace()
        }
        return map;
    }

    @Override
    def getAppConfigSetInfo(String appId) {
        def map = [:]
        try{
            def cloudResult= qcloudLiveRes.getQcloudInfo(appId)
            def configSetInfo = Strings.parseJson(cloudResult?.config_set_info)
            if(configSetInfo !=null){
                map.put("homeFormat",configSetInfo?.home_format?:"")
                map.put("priceliveTitle",configSetInfo?.pricelive_title?:"")
                map.put("normalliveTitle",configSetInfo?.normallive_title?:"")
                map.put("recommendTitle",configSetInfo?.recommend_title?:"")
                map.put("classifyChannel",configSetInfo?.classify_channel?:"")
                map.put("toplabelStyle",configSetInfo?.toplabel_style?:"")
                map.put("navbarColor",configSetInfo?.navbar_color?:"")
                map.put("statusStyle",configSetInfo?.status_style?:"")
                map.put("classifyStyle",configSetInfo?.classify_style?:"")
                map.put("masterColor",configSetInfo?.master_color?:"")
                map.put("slideshowStatus",configSetInfo?.slideshow_status?:"")
                map.put("liveClassify",configSetInfo?.live_classify?:"")
                map.put("tipImage",configSetInfo?.tip_image?:"")
                map.put("playerButton",configSetInfo?.player_button?:"")
                map.put("liveButton",configSetInfo?.live_button?:"")
                map.put("liveButtonImage",configSetInfo?.live_button_image?:"")
                map.put("styleCategory",configSetInfo?.style_category?:"")
                map.put("liveshareValue",cloudResult?.liveshare_value?:"")
                map.put("mallUrl",cloudResult.mall_url)
                map.put("faceListLimit",cloudResult.face_list_limit)
                map.put("appId",cloudResult.app_id)

                log.info("从数据库中获取腾讯云配置项,appId=>{},config=>{}",appId,map);
            }else{
                log.info("腾讯云配置项不完善,appId=>{}",appId);
            }
        }catch (Exception e){
            e.printStackTrace()
        }
        return map;
    }

    @Override
    def delQcloudInfo(String appId){
        liveQcloudRedis.delCloudInfoRedis(appId);
        return  true;
    }

    @Override
    def getAllQcloudInfo(){
        def result = liveQcloudRedis.getAllCloudInfoRedis();
        if(result){
            def rs = Strings.parseJson(result)
            return rs;
        }

        def appIds = []
        qcloudLiveRes.getAllQcloudInfo().each {
            appIds << it.appId
        }
        liveQcloudRedis.setAllCloudInfoRedis(appIds)
        return appIds
    }

    @Override
    def findLiveRecordPayInfo(long liveId, def userInfo,LiveRecord liveRecord,long inviter) {
        def liveRecordPay = qcloudLiveRes.findLiveRecordPayExtendByLiveId(liveId,0)
        try{
            double price = (liveRecordPay?.ticketPrice as double)*100
            if(price.intValue()==0){
                liveRecordPay.viewAuthority = 1
                Parallel.run([1],{
                    Map mapData = [
                        userName: userInfo.userName,
                        userId:userInfo.userId,
                        clientOprateOrder: userInfo.userId+"_"+liveId+"_"+"order",
                        giftName: liveRecord.title,
                        giftPrice: 0,
                        giftNum: 1,
                        liveId: liveId,
                        liveTitle: liveRecord.title,
                        appAccount: liveRecordPay.mpAccount,
                        sign: ZURL.generateMD5(userInfo.userId+""+liveId+"XSDLAKSIJDUSHNUA@@1!!423"),
                        inviter: inviter,
                        appId: liveRecord.appId
                    ]
                    Http.post(livePayOrderUrl,mapData)
                },1)
            }else {
                Map params = [userName:userInfo.userName, liveId: liveId,phone:userInfo.mobile,appId: liveRecord.appId,liveName:liveRecord.title]
//                Map params = [userName:userInfo.userName, liveId: liveId]
                def resultStr = Http.post(liveUserPayUrl, params)
                def result = Strings.parseJson(resultStr)
                log.info("wangtf getuserpay liveId:{},userId:{},userName:{},res:{}",liveId,userInfo.userId,userInfo.userName,result)
                if(result &&  "200".equals(result?.head?.code as String) ){
                    liveRecordPay.viewAuthority = 1
                }else {
                    liveRecordPay.viewAuthority = 0
                }
            }
        }catch (Exception e){
            e.printStackTrace()
        }
        return liveRecordPay
    }
}
