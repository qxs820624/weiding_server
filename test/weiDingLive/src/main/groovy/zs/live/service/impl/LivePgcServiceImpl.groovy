package zs.live.service.impl

import com.alibaba.fastjson.JSON
import com.qcloud.Module.Live
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import zs.live.ApiException
import zs.live.common.LiveCommon
import zs.live.dao.mysql.LiveRes
import zs.live.dao.mysql.QcloudLiveRes
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.entity.LiveForeshow
import zs.live.entity.LiveRecord
import zs.live.service.*
import zs.live.utils.DateUtil
import zs.live.utils.Parallel
import zs.live.utils.Strings

import java.sql.Timestamp

/**
 * Created by Administrator on 2016/12/14.
 */
@Service
@Slf4j
class LivePgcServiceImpl implements LivePgcService {

    @Value('${live.env}')
    String liveEnv
    @Value('${live.defalut.srpid}')
    String liveDefalutSrpId
    @Value('${live.defalut.keyword}')
    String liveDefalutKeyword

    @Autowired
    QcloudLiveService qcloudLiveService
    @Autowired
    LiveQcloudRedis liveQcloudRedis
    @Autowired
    QcloudLiveRes qcloudLiveRes
    @Autowired
    LiveRes liveRes
    @Autowired
    ShortURLService shortURLService
    @Autowired
    QcloudPgcLiveService qcloudPgcLiveService
    @Autowired
    LiveService liveService

    @Autowired
    XiaoYuService xiaoYuService

    @Autowired
    LiveForeshowService liveForeshowService

    @Override
    def create(Map params) {
        log.info("创建直播传入参数：{}", params);
        Map msg=["msg":"创建失败", "liveId": 0]; //后台要求统一包括异常时的json格式
        params.host = Strings.parseJson(params.host)
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

        boolean extend = true
        if (params.liveMode && params.liveMode as int == LiveCommon.LIVE_MODE_3){
            //保存付费的扩展字段
            extend = extendLivePay(liveId, params);
        }
        if (create && extend) {
            msg.msg = "创建成功";
            msg.liveId = liveId
        }
        liveQcloudRedis.setLiveIdWithRoomId(roomId,liveId);
        Parallel.run([1],{
            //通知王龙直播统计 TODO 暂时只是与王龙联调，后期会删掉，统一由分析nginx日志的项目处理
            long createTime = DateUtil.getTimestamp(live.createTime)
            long beginTime = createTime
            if(live.liveMode != LiveCommon.FORESHOW_TYPE_1 && live.beginTime){
                beginTime = DateUtil.getTimestamp(live.beginTime)
            }
            qcloudLiveService.sendLiveDataToStatistic([jsonType:LiveCommon.STATISTIC_JSON_TYPE_1,liveId:live.liveId,foreshowId:live.foreshowId,roomId:live.roomId,
                                                       userId:live.userId.toString(),nickname:live.nickname,title:live.title,isPrivate:live.isPrivate,
                                                       liveMode:live.liveMode,appId:live.appId,vc:live.vc,createTime:createTime,beginTime:beginTime])
        },1)
        return msg;
    }
    /**保存付费的扩展字段*/
    private boolean extendLivePay(liveId, Map params){
        Map payMap = [
            "endTime":params.endTime,
            "speaker":params.speaker,
            "speakerTitle":params.speakerTitle,
            "speakerImage":params.speakerImage,
            "speakerIntro":params.speakerIntro,
            "orgs":params.orgs,
            "targetAudience":params.targetAudience,
            "ticketPrice":params.ticketPrice,
            "freeGroups":params.freeGroups,
            "vipChannel":params.vipChannel,
            "vipChannelTips":params.vipChannelTips,
            "mobileVerifyTips":params.mobileVerifyTips,
            "introduction":params.introduction,
            "mpAccount":params.mpAccount,    //腾迅公众号
            "commodityName":params.commodityName, //商品名称
            "shareTitle":params.shareTitle,       //分享页title
            "isSplit":params.isSplit,        //是否分账
            "splitPrice":params.splitPrice,        //分账金额
            "isSaleSplit":params.isSaleSplit, //是否促销分账，0：否，1：分账
            "saleSplitPrice":params.saleSplitPrice,//促销分账金额
            "tryTime":params.tryTime,//试看时间
        ]
        return qcloudLiveRes.addLiveRecordPayExtend(liveId,payMap)
    }

    @Override
    def modify(Map params) {
        log.info("修改直播传入参数：{}", params);
        String msg="";
        long liveId = (params.liveId ?: 0) as long
        //查询直播中的表
        def liveRecordRet = liveRes.findLiveByLiveId(liveId)
        LiveRecord  liveRecord = liveService.toLiveRecord(liveRecordRet)
        //查询回放表
        if(!liveRecord){
            liveRecord = liveService.findLiveRecordByLiveId(liveId)
        }
        if (!liveRecord) {
            throw new ApiException(700, "指定会议记录不存在");
        }
        long beginTimeAssert=  params.beginTime as long;
       if(DateUtil.getTimestamp(liveRecord.beginTime)<= System.currentTimeMillis() && DateUtil.getTimestamp(liveRecord.beginTime)!= beginTimeAssert){
           throw new ApiException(700, "会议开始后不能修改开始时间");
       }

        int pgcType = liveRecord.pgcType;
        if (pgcType == 2) {  //修改小鱼的
            Map<String, Object> data = new HashMap<String, Object>();
            String title = params.title as String;
            if (title && title.size() > 32)
                title = title.substring(0, 32);
            data.put("title", title);
            //confNo为会议室id，在一个会议室中可以有多个会议直播
            data.put("confNo", params.confNo);
            //直播开始和终止时间，似乎没用
            long beginTime = params.beginTime ? params.beginTime as long : System.currentTimeMillis() + 1000 * 60
            data.put("startTime", beginTime);
            data.put("endTime", beginTime + 10 * 24 * 60 * 60 * 1000);  //10天后结束
            data.put("autoRecording", true);
            data.put("autoPublishRecording", true);

            String brief = params.brief as String;
            if (brief && brief.size() > 128)
                brief = brief.substring(0, 128);
            data.put("detail", brief);
            data.put("location", "");

            Map xiaoyuRet = xiaoYuService.updateLive(params.xiaoyuId, liveRecord.xiaoYuLiveId, data);
            if (!xiaoyuRet || !xiaoyuRet.statusCode || xiaoyuRet.statusCode != 204) {
                throw new ApiException(700, "修改小鱼直播信息失败");
            }
        }

        Map updateParam = [:];

        updateParam.title = params.title.length() > 200 ? params.title.substring(0, 200) : params.title;
        updateParam.xiaoyuId = params.xiaoyuId as String;
        updateParam.beginTime = new Timestamp(params.beginTime as long);
        updateParam.liveBg = params.liveBg as String;
        updateParam.compereUserId = params.compereUserId as String;
        updateParam.fieldControlId = params.fieldControlId as String;
        updateParam.brief = params.brief as String;
        updateParam.briefHtml = params.briefHtml as String;
        updateParam.liveId = liveId;


        log.info("修改会议 updateParam:{}", updateParam);
        qcloudLiveRes.updateLiveRecordForPgc(updateParam)
        qcloudLiveRes.updateLiveRecordLogForPgc(updateParam)

        //更新liveRecord相关缓存，
        liveService.updateLiveRedisByLiveId(liveId)

        boolean extend = true
        if (params.liveMode && params.liveMode as int == LiveCommon.LIVE_MODE_3){
            //更新付费的扩展字段
            Map payMap = [
                "endTime":params.endTime,
                "speaker":params.speaker,
                "speakerTitle":params.speakerTitle,
                "speakerImage":params.speakerImage,
                "speakerIntro":params.speakerIntro,
                "orgs":params.orgs,
                "targetAudience":params.targetAudience,
                "ticketPrice":params.ticketPrice,
                "freeGroups":params.freeGroups,
                "vipChannel":params.vipChannel,
                "vipChannelTips":params.vipChannelTips,
                "mobileVerifyTips":params.mobileVerifyTips,
                "introduction":params.introduction,
                "mpAccount":params.mpAccount,    //腾迅公众号
                "commodityName":params.commodityName, //商品名称
                "shareTitle":params.shareTitle,        //分享页title
                "isSplit":params.isSplit,        //是否分账
                "splitPrice":params.splitPrice,        //分账金额
                "isSaleSplit":params.isSaleSplit, //是否促销分账，0：否，1：分账
                "saleSplitPrice":params.saleSplitPrice,//促销分账金额
                "tryTime":params.tryTime,//试看时间
            ]
            extend = qcloudLiveRes.updateLiveRecordPayExtend(liveId,payMap)
        }
        if (!extend) {
            msg = "更新付费的扩展字段失败";
            return msg;
        }

        if (liveRecord.foreshowId) { //更改会议的时候同步修改live_foreshow表的pgc_title和开始时间
            Map updateParamForeshow = [:];
            updateParamForeshow.beginTime = new Timestamp(params.beginTime as long);
            updateParamForeshow.pgcTitle = updateParam.title as String
            updateParamForeshow.foreshowId = liveRecord.foreshowId;
            log.info("修改会议 updateParamForeshow:{}", updateParamForeshow);
            boolean updateForeshow = qcloudLiveRes.updateLiveRecordForPgcRelateForeshow(updateParamForeshow);
            if (!updateForeshow) {
                msg = "更新关联预告表失败";
                return msg;
            }
        }
        Parallel.run([1],{
            //通知王龙直播统计 TODO 暂时只是与王龙联调，后期会删掉，统一由分析nginx日志的项目处理
            long createTime = DateUtil.getTimestamp(liveRecord.createTime)
            long beginTime = params.beginTime as long
            qcloudLiveService.sendLiveDataToStatistic([jsonType:LiveCommon.STATISTIC_JSON_TYPE_1,liveId:liveRecord.liveId,foreshowId:liveRecord.foreshowId,roomId:liveRecord.roomId,
                                                       userId:liveRecord.userId.toString(),nickname:liveRecord.nickname,title:updateParam.title,isPrivate:liveRecord.isPrivate,
                                                       liveMode:liveRecord.liveMode,appId:liveRecord.appId,vc:liveRecord.vc,createTime:createTime,beginTime:beginTime])
        },1)
        msg = "更新成功";
        return msg
    }

    @Override
    def delete(Map params) {
        long liveId = (params.liveId ?: 0) as long
        //先查出直播对象 保存下来
        def liveRecordRet = liveRes.findLiveByLiveId(liveId)
        LiveRecord  liveRecord = liveService.toLiveRecord(liveRecordRet)

        if (!liveRecord) {
            throw new ApiException(700, "指定会议记录不存在,无法删除");
        }

        if (liveRecord.foreshowId) {   //预告存在且没有被删除时 不允许删除会议
            LiveForeshow liveForeshow=liveForeshowService.get(liveRecord.foreshowId);
            if(liveForeshow && liveForeshow.foreshowStatus!=LiveCommon.FORESHOW_STATUS_3)
            throw new ApiException(ApiException.STATUS_FORESHOW_CREATED_CANNOT_DELETE, "指定会议已经关联预告,无法删除");
        }

        if (liveRecord.pgcType == 2) {
            try {
                Map xiaoyuRet = xiaoYuService.removeLive(liveRecord.xiaoYuId, liveRecord.xiaoYuLiveId);
                liveRecord.liveStatus = 4; //后台定时任务不再扫描试图获得回放列表

            } catch (Exception xiaoYuEx) {
                log.error("delete xiaoyu failed liveId:{} ", liveId, xiaoYuEx);
            }
        } else if (liveRecord.pgcType == 3) {
            //删除直播时删除腾讯云频道
            qcloudPgcLiveService.stopQcloudChannel(liveRecord, "后台删除会议直播");
        }

        //获取服务器时间
        long timeSpan = 0;
        if (liveRecord.foreshowId) {
            long paushTime = liveQcloudRedis.getPgcPaushTimeLong(liveRecord.foreshowId) //暂停时间
            timeSpan = System.currentTimeMillis() - DateUtil.getTimestamp(liveRecord.beginTime)  //总时长
            timeSpan = timeSpan - paushTime //总时间 - 暂停时间
            liveRecord.timeSpan = timeSpan
        } else {
            timeSpan = System.currentTimeMillis() - DateUtil.getTimestamp(liveRecord.beginTime) //直播时间
            if(timeSpan > 0){
                liveRecord.timeSpan = timeSpan
            }

        }

        //createTime 统一格式

        qcloudLiveRes.insertLiveRecordLog(liveRecord, [:]);

        //删除live_record表
        qcloudLiveRes.deleteLiveRecord(liveId)
        //删除删除直播列表redis缓存
        String appId = Strings.getAppId(params)
        liveQcloudRedis.delLiveRecord(appId, liveId)

        liveQcloudRedis.delLiveIdByRoomID(liveRecord.roomId)
        return  "删除成功";
    }

    /**
     * 直播结束
     * @param liveRecord
     * @return
     */
    Map stop(LiveRecord liveRecord, String msg) {
        def result = [:]
        //获取服务器时间
        long timeSpan = 0;
        if (liveRecord.foreshowId) {
            long paushTime = liveQcloudRedis.getPgcPaushTimeLong(liveRecord.foreshowId) //暂停时间
            timeSpan = System.currentTimeMillis() - DateUtil.getTimestamp(liveRecord.beginTime) //总时长
            timeSpan = timeSpan - paushTime //总时间 - 暂停时间
            liveRecord.timeSpan = timeSpan / 1000
        } else {
            timeSpan = System.currentTimeMillis() - DateUtil.getTimestamp(liveRecord.beginTime) //直播时间
            liveRecord.timeSpan = timeSpan / 1000
        }
        //createTime 统一格式
        liveRecord.liveStatus = 1 //会议直播暂时不受控制
        qcloudLiveRes.insertLiveRecordLog(liveRecord, [:]);

        //删除live_record表
        qcloudLiveRes.deleteLiveRecord(liveRecord.liveId)
        //删除删除直播列表redis缓存
        liveQcloudRedis.delLiveRecord(liveRecord.appId, liveRecord.liveId)
        if (liveRecord.pgcType == 3) {
            //会议直播修改停止逻辑 注释掉频道托管 用直播码暂停
            //qcloudPgcLiveService.stopQcloudChannel(liveRecord, msg);
            qcloudPgcLiveService.stopLiveChannel(liveRecord)
        }
       // liveQcloudRedis.delLiveIdByRoomID(liveRecord.roomId)
        Parallel.run([1],{
            //通知appId人数减去真实观众数，云平台需求
            liveService.addAppLimitInfo(liveId,0)
        },1)
        return result
    }

    @Override
    def getUserRole(Long liveId, Long userId) {
        int role = LiveCommon.LIVE_ROLE_3
        LiveRecord liveRecord = liveService.findLiveByLiveId(liveId)
        if (!liveRecord) {
            //会议直播可永久进行评论，可能会出现redis和licerecord表中查不到记录的情况(结束的会议直播)，固需要查询一下liverecord_log表
            liveRecord = liveService.findLiveRecordByLiveId(liveId)
            if(!liveRecord){
                return role
            }
        }
        String compereUserId = "," + liveRecord.compereUserId + ","
        String fieldControlId = "," + liveRecord.fieldControlId + ","
        if (compereUserId.contains("," + userId + ",")) {
            role = LiveCommon.LIVE_ROLE_1
        } else if (fieldControlId.contains("," + userId + ",")) {
            role = LiveCommon.LIVE_ROLE_2
        } else if (liveRecord.userId == userId) {
            role = LiveCommon.LIVE_ROLE_4
        }
        return role
    }

    @Override
    def updateFieldControl(Map params) {
        Map res = [status: 1, msg: "成功"]
        try {
            Long op_role = (params.op_role ?: 0) as long
            String operType = params.operType ?: ""
            if (op_role == 100) {
                //pc的请求，设置场控
                liveRes.updateFieldControlByAll(params)
            } else {
                int user_role = getUserRole(params.liveId, params.userId as long)
                int toUser_role = getUserRole(params.liveId, params.toUserId as long)
                //客户端的请求设置场控，判断当前用户是否为主持人或主播，只有主持人、主播有权限设置场控
                if (LiveCommon.LIVE_ROLE_1 != user_role && LiveCommon.LIVE_ROLE_4 != user_role) {
                    res.status = 2
                    res.msg = "您没有权限设置场控"
                    return res
                } else if ("add".equals(operType) && LiveCommon.LIVE_ROLE_2 == toUser_role) {
                    res.status = 1
                    res.msg = "该用户已经是场控"
                    return res
                }
                if ("add".equals(operType)) {
                    liveRes.updateFieldControlByAdd(params)
                } else if ("del".equals(operType)) {
                    //删除采取的是查询数据库记录，去掉需要取消的场控，然后统一重新修改场控字段
                    LiveRecord liveRecord = liveService.findLiveByLiveId(params.liveId)
                    if(!liveRecord){
                        liveRecord = liveService.findLiveRecordByLiveId(params.liveId)
                    }
                    if (liveRecord && liveRecord.getFieldControlId()) {
                        List<String> list = Strings.splitToList(liveRecord.getFieldControlId())
                        Iterator<String> iter = list.iterator();
                        while (iter.hasNext()) {
                            String s = iter.next();
                            if (s.equals(params.toUserId)) {
                                iter.remove();
                            }
                        }
                        params.put("toUserId", Strings.toListString(list))
                        liveRes.updateFieldControlByAll(params)
                    }
                }
            }
            //修改直播信息的缓存
            liveService.updateLiveRedisByLiveId(params.liveId)
        } catch (Exception e) {
            res.status = 0
            res.msg = "java内部错误"
            log.info("updateFieldControl java内部错误，Exception=>{}", e.getMessage())
            e.printStackTrace()
        }
        return res
    }

    @Override
    def updateBackVideoAddress(LiveRecord live) {
        def videoAddress;
        if (live.pgcType == 3 || live.pgcType == 1) {
            //videoAddress = qcloudPgcLiveService.updateBackVideoAddressByQcloud(live)
           // videoAddress = qcloudPgcLiveService.
            def config = qcloudLiveService.getQcloudInfo(live.appId)
            log.info("qcloudInfo====>"+Strings.toJson(config))
            int bizid = config.bizid as int
            String qcloudAppid = config.qcloudAppid
            String channelId = qcloudPgcLiveService.getStreamId(bizid,live.liveId,live.roomId)
            long txTime = System.currentTimeMillis() + 1000*60*60*24 //开始时间的 一天以后
            String apiKey = config.apiKey
            videoAddress =  QcloudLiveCommon.liveTapeGetFilelist(qcloudAppid,channelId,apiKey,txTime)
        } else if (live.pgcType == 2) { //取小鱼的回放地址


            def xiaoYuRet = xiaoYuService.getVideosWithDuration(live.xiaoYuId, live.xiaoYuLiveId);
            if (xiaoYuRet?.statusCode != 200) {
                log.info("updateBackVideoAddress of xiaoyu failed,xiaoYuRet :{}", xiaoYuRet);
                return null;
            }
            List xiaoYuRetList = xiaoYuRet?.list
            if (xiaoYuRetList) {
                def retList = [];
                xiaoYuRetList.each {
                    def node = [duration: String.valueOf((long)(it.durationMs?:0)/1000), fileId: "", fileName: "", image_url: "", status: "", vid: ""];
                    node.playSet = [[definition: 0, vbitrate: 0, vheight: 0, vwidth: 0]];
                    node.playSet[0].url = it.url;
                    retList << node;
                }
                videoAddress = JSON.toJSONString(retList);
            }
        }
        return videoAddress;
    }

    Map fillParams(Map params, long liveId, int roomId) {
        int pgcType = (params.pgcType ?: 0) as int
        if (pgcType == 3 ) {
          qcloudPgcLiveService.fillPgcQcloudParams(params, liveId, roomId)
        } else if (pgcType == 2) {

                Map<String, Object> data = new HashMap<String, Object>();
                String title = params.title as String;
                if (title && title.size() > 32)
                    title = title.substring(0, 32);
                data.put("title", title);
                //confNo为会议室id，在一个会议室中可以有多个会议直播
                data.put("confNo", params.confNo);
                //直播开始和终止时间，似乎没用
                long beginTime = params.beginTime ? params.beginTime as long : System.currentTimeMillis() + 1000 * 60
                data.put("startTime", beginTime);
                data.put("endTime", beginTime + 5 * 24 * 60 * 60 * 1000);  //5天后结束
                data.put("autoRecording", true);
                data.put("autoPublishRecording", true);

                String brief = params.brief as String;
                if (brief && brief.size() > 128)
                    brief = brief.substring(0, 128);
                data.put("detail", brief);
                data.put("location", "");

                Map fillMap =  xiaoYuService.reserveLive(params.xiaoyuId as String, data);

                if (fillMap?.statusCode != 200) {
                        String  msg = "小鱼号或会议号错误";
                        throw new ApiException(700, msg);
                } else {
                        params.xiaoYuLiveId = fillMap.liveId as String
                        params.playUrl = fillMap.flv
                        params.m3u8Url = fillMap.hls
                        params.xiaoYuId = fillMap.nemoNumber as String
                        params.xiaoYuConfNo = fillMap.confNo as String;

                        params.m3u8Standard =fillMap.hls
                        params.m3u8High = fillMap.hls
                        params.flvStandard = fillMap.flv
                        params.flvHigh = fillMap.flv

                  }

        }else if(pgcType == 4){
           params.m3u8Standard =params.m3u8Url
           params.m3u8High =    params.m3u8Url
           params.flvStandard = params.playUrl
           params.flvHigh =     params.playUrl
        }else if (pgcType == 1){
           qcloudPgcLiveService.fillPgcQcloudParams(params, liveId, roomId)
        }
        return params;
    }


    long getLiveId() {
        //生成liveId
        long liveId = liveQcloudRedis.getLiveId();
        return liveId
    }

    @Override
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
        //live.srpId = params.srpInfo.srpId && Strings.toListString(params.srpInfo.srpId)?Strings.toListString(params.srpInfo.srpId):liveDefalutSrpId
        // live.keyword = params.srpInfo.keyword && Strings.toListString(params.srpInfo.keyword,LiveCommon.KEYWORD_SEPARATOR)?Strings.toListString(params.srpInfo.keyword,LiveCommon.KEYWORD_SEPARATOR):liveDefalutKeyword
        //live.foreshowId = params.foreshowId? params.foreshowId as int:0
//        if(live.foreshowId){  //默认非官方直播 有预告id的为官方直播
//            live.liveType = 1
//            live.isPrivate = 0 //官方直播只能是公开类型的直播
//        }
        //live.vc = params.vc
        live.channelId = params.channelId ?: ""
        live.liveType = 1
        live.compereUserId = params.compereUserId //主持人
        live.fieldControlId = params.fieldControlId //场控
        live.beginTime = Strings.getDayOfTime(new Date(params.beginTime as long))
        live.rtmpUrl = params.rtmpUrl  //直播推流地址
        live.pgcStatus = 0//会议直播的直播状态 1.未开始，2.直播中，3直播暂停
        live.pgcType = params.pgcType as int//1.手机 2.小鱼 3.搜悦推流
        live.brief = params.brief//直播简介
        live.liveMode = params?.liveMode?params.liveMode as int:LiveCommon.LIVE_MODE_2

        live.xiaoYuLiveId = params?.xiaoYuLiveId ?: "";
        live.xiaoYuId = params?.xiaoYuId ?: "";
        live.xiaoYuConfNo = params?.xiaoYuConfNo ?: "";
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

    @Override
    Map createThirdParty(Map params) {
        //创建直播先创建房间（因为要判断是否有没结束的房间 先结束了）
        String appId = Strings.getAppId(params)
        int roomId = getRoomId(0,appId);

        LiveRecord live = new LiveRecord();
        long liveId = getLiveId();
        live.liveId = liveId;
        live.playUrl = params.pushUrl
        live.beginTime = Strings.getDayOfTime(new Date(params.beginTime as long))
        live.appId = appId
        live.chatId = roomId
        live.roomId = roomId
        live.createTime = Strings.getDayOfTime(new Date())
        live.userId = 0
        live.userImage = ''
        live.nickname = ''
        live.title=params.title
        live.pgcType = 4
        live.pgcStatus = LiveCommon.PGC_STATUS_0
        live.brief = params.description//直播简介
        live.liveBg = ''
        Map backMap = [:]
        //创建直播
        boolean create = qcloudLiveRes.createLiveRecord(live)
        //更新正在直播列表
        liveQcloudRedis.updateLiveList(live);
        boolean saved = qcloudLiveRes.savePartner(liveId,params.partnerId)
        if(create && saved){
            backMap.msg="成功";
            backMap.liveId = liveId
            backMap.status = live.pgcStatus
        }else {
            backMap.msg="失败";
        }
        return backMap
    }

    @Override
    Map modifyThirdParty(Map params) {
        long liveId=(params.liveId ?: 0) as long
        LiveRecord liveRecord=liveService.findLiveByLiveId(liveId)
        if(!liveRecord){
            throw new ApiException(700,"指定会议记录不存在");
        }
        int status = (params.status ?: 0) as int
        String partnerId = params.partnerId
        long foreshowId = liveRecord.foreshowId
  /*      switch (status){
            case 1://1开始
                liveForeshowService.beginPgc(foreshowId,liveRecord.appId,liveRecord.roomId,liveRecord.userId,LiveCommon.FORESHOW_STATUS_1,liveRecord.brief)
                break;
            case 2://3直播暂停
                liveForeshowService.pausePgc(foreshowId,liveRecord.appId,liveRecord.roomId,liveRecord.userId,LiveCommon.FORESHOW_STATUS_6,liveRecord.brief)
                break;
            case 3: //4结束
                liveForeshowService.stopForeshow(foreshowId)
                break;
            default: break;
        }*/

        return [msg: "修改成功"]
    }
}
