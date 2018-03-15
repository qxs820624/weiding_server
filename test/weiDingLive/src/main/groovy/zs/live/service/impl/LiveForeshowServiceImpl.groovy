package zs.live.service.impl

import groovy.util.logging.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import zs.live.ApiException
import zs.live.common.LiveCommon
import zs.live.dao.mysql.LiveForeshowRes
import zs.live.dao.mysql.LiveRes
import zs.live.dao.mysql.QcloudLiveRes
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.entity.LiveForeshow
import zs.live.entity.LiveRecord
import zs.live.service.*
import zs.live.utils.DateUtil
import zs.live.utils.Http
import zs.live.utils.Parallel
import zs.live.utils.Strings
import zs.live.utils.VerUtils

import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

/**
 * Created by Administrator on 2016/12/14.
 */

@Slf4j
@Service
class LiveForeshowServiceImpl implements LiveForeshowService {
    public static final Logger callBackLog = LoggerFactory.getLogger("callBack")

    @Autowired
    LiveForeshowRes liveForeshowRes

    @Autowired
    QcloudLiveService qcloudLiveService


    @Autowired
    QcloudPgcLiveService qcloudPgcLiveService

    @Autowired
    LiveRes liveRes
    @Autowired
    LivePgcService livePgcService

    @Autowired
    LiveQcloudRedis  liveQcloudRedis

    @Autowired
    QcloudLiveRes    qcloudLiveRes


    @Autowired
    LiveService liveService

    @Autowired
    ShortURLService shortURLService

    @Autowired
    VestUserService vestUserService

    @Value('${live.test.user.list}')
    String testUser

    @Value('${live.foreshow.pay.status}')
    String liveForeshowPayStatusUrl

    @Value('${brief.url}')
    String briefUrl



    public LiveForeshow get(long foreshowId){
        return liveForeshowRes.get(foreshowId);
    }

    @Override
    int saveOrUpdate(Map paramMap) {
        LiveForeshow entity = null;
        if (paramMap.foreshowId){
            entity = liveForeshowRes.get(paramMap.foreshowId)
            if (!entity){
                return 0
            }
        }else {
            entity = new LiveForeshow();
        }
        entity.appId= paramMap.appId
        entity.foreshowId = paramMap.foreshowId
        entity.cateId = paramMap.cateId
        entity.userId = paramMap.userId
        entity.parentId = paramMap.parentId
        entity.userName = paramMap.userName
        entity.nickname = paramMap.nickname
        entity.userImage = paramMap.userImage
        entity.title = paramMap.title
        entity.imgUrl = paramMap.imgUrl
        entity.foreshowType = paramMap.foreshowType
        entity.beginTime = DateUtil.getDate(paramMap.beginTimeStr,DateUtil.FULL_DATE_PATTERN)
        entity.urlTag = paramMap.urlTag
        entity.url = paramMap.url
        entity.keyword = paramMap.keyword
        entity.srpId = paramMap.srpId
        entity.newsId = paramMap.newsId
        entity.pgcTitle = paramMap.pgcTitle
        entity.showTitleInList=paramMap.showTitleInList ?:0
        entity.notShowInClient=paramMap.notShowInClient ?:0
        entity.ruleJson=paramMap.columns
        int status = 0
        if (paramMap.foreshowId){
            status = liveForeshowRes.update(entity)
        }else {
            paramMap.foreshowId = liveForeshowRes.insert(entity)
            status = paramMap.foreshowId ? 1 : 0
        }
        log.info("foreshow update live_foreshow table result:{},forshowId:{}",status, paramMap.foreshowId)
        if (status && (paramMap.foreshowType == LiveCommon.FORESHOW_TYPE_2 || paramMap.foreshowType == LiveCommon.FORESHOW_TYPE_3) && paramMap.liveId) {
            //当预告类型是会议直播的时候，需要更新live_record表
            status = liveService.updateForeshowIdByLiveId(paramMap.liveId, paramMap.foreshowId, paramMap.appId)
        }
        liveQcloudRedis.updateLiveForeshowList(entity)
        //会议直播增加缓存 ---- 预告对应直播
        if (paramMap.foreshowType == LiveCommon.FORESHOW_TYPE_2 || paramMap.foreshowType == LiveCommon.FORESHOW_TYPE_3) {
            liveQcloudRedis.hsetPgcForeshowToLive(paramMap.foreshowId, paramMap.liveId)
        }
        if (status && !saveForeshowSrpRelation(paramMap)){
            return 0
        }
        Parallel.run([1],{
            //通知王龙直播统计 TODO 暂时只是与王龙联调，后期会删掉，统一由分析nginx日志的项目处理
            long createTime = System.currentTimeMillis()
            long beginTime = DateUtil.getTimestamp(paramMap.beginTimeStr)
            qcloudLiveService.sendLiveDataToStatistic([jsonType:LiveCommon.STATISTIC_JSON_TYPE_5,liveId:0,foreshowId:paramMap.foreshowId,roomId:0,
                                                       userId:entity.userId.toString(),nickname:entity.nickname,title:entity.title,isPrivate:0,
                                                       liveMode:entity.foreshowType,appId:entity.appId,vc:"0",createTime:createTime,beginTime:beginTime])
        },1)
        return status
    }

    /**维护预告和圈子对应关系表以及对应缓存*/
    def saveForeshowSrpRelation(Map paramMap){
        //维护预告和圈子对应关系表
        List interestList = Strings.parseJson(paramMap.interestList)
        List srpList = Strings.parseJson(paramMap.srpList)

        //保存圈子和预告的对应
        def srpIdList=[]
        interestList?.each{
            if (it.srpId){
                srpIdList << it.srpId
            }
        }
        srpList?.each{
            if (it.srpId) {
                srpIdList << it.srpId
            }
        }
        liveQcloudRedis.addForeshowSrpRelation(paramMap.foreshowId, srpIdList)
        return liveRes.saveForeshowSrpRelation(paramMap.foreshowId, 0, 0, interestList, srpList)
    }

    @Override
    int remove(long foreshowId, String appId) {
        LiveForeshow entity = null;
        def res = 0
        if (foreshowId){
            entity = liveForeshowRes.get(foreshowId)
            if (!entity){
                return 0
            }
            if(entity.foreshowType.equals(5)){    //如果是系列
                //删除栏目
                res = liveRes.delForeshow(foreshowId,appId)
                //移除栏目下的预告类表的数据
                if(res > 0){
                    liveForeshowRes.updateParentId(foreshowId, appId);
                }
            }
        }
        return res
    }

    @Override
    int removeGroupDataList(long foreshowId, String appId) {
        int res = 0
        LiveForeshow liveForeshow = liveForeshowRes.get(foreshowId)
        if(foreshowId){
            res = liveForeshowRes.updateParentIdByForeshowId(foreshowId, appId);
            if (res){
                //更新live_forshow表中系列的开始时间为系列下预告列表中开始时间最大的时间
                LiveForeshow entity = liveForeshowRes.get(liveForeshow.parentId)
                updateForeshowGroupBeginTime(entity, liveForeshow.parentId, appId)
            }
        }
        return res
    }

    @Override
    int setForeshowCateId( long foreshowId, long cateId ) {

        return liveForeshowRes.setForeshowCateId( foreshowId, cateId );

    }

    @Override
    def saveOrUpdateGroup(Map paramMap) {
        def res = [status:0]
        LiveForeshow entity = null;
        long foreshowId = (paramMap.foreshowId?:0) as long
        if (foreshowId){
            entity = liveForeshowRes.get(foreshowId)
            if (!entity){
                return res
            }
        }else {
            entity = new LiveForeshow();
            entity.setForeshowType(LiveCommon.FORESHOW_TYPE_5) //5直播系列
        }
        entity.cateId = paramMap.cateId
        entity.title = paramMap.title
        entity.imgUrl = paramMap.imgUrl
        entity.description = paramMap.description
        entity.descriptionHtml = paramMap.descriptionHtml
        entity.isCost = paramMap.isCost
        entity.isSale = paramMap.isSale
        entity.notShowInClient = paramMap.showIn
        entity.appId = paramMap.appId
        entity.foreshowStatus=LiveCommon.FORESHOW_STATUS_2
        Date date = DateUtil.addDateByMonth(new Date(),-12*20)//提前20年
        entity.beginTime = entity.beginTime ?: date
        int status = 0
        if (foreshowId){
            status = liveForeshowRes.update(entity)
        }else {
            foreshowId = liveForeshowRes.insert(entity)
            if (foreshowId){
                status = 1
                paramMap.foreshowId = foreshowId;
            }
        }

        if (entity.isCost == 1){
            //保存付费的扩展字段
            qcloudLiveRes.updateLiveRecordPayExtend(foreshowId,[price:paramMap.price,salePrice:paramMap.salePrice,
                                                             saleStartTime:paramMap.saleStartTime,
                                                             saleEndTime:paramMap.saleEndTime,type:1,
                                                             isSplit:paramMap.isSplit,splitPrice:paramMap.splitPrice,
                                                             isSaleSplit:paramMap.isSaleSplit,saleSplitPrice:paramMap.saleSplitPrice,
                                                             tryTime:paramMap.tryTime])
        }else{
            //当系列由付费变更为免费时，需要删除付费扩展表中的记录。以免后面导致是否付费与价格等之间的不一致问题
            qcloudLiveRes.delLiveRecordPayExtend(foreshowId,1)
        }

        if (status && !saveForeshowSrpRelation(paramMap)){
            return res
        }

        return [status:1,foreshowId:foreshowId]
    }

    @Override
    int fillLiveForeshow(long foreshowId, List<Long> subForeshowIdList, String appId) {

        if (!foreshowId || !subForeshowIdList){
            return 0
        }
        LiveForeshow entity = liveForeshowRes.get(foreshowId)
        if (!entity) {
            return 0
        }
        long cateId = entity.cateId
        int res = liveForeshowRes.fillLiveForeshow(foreshowId, cateId, subForeshowIdList, appId)
        if(subForeshowIdList.size() == res){
            //更新live_forshow表中系列的开始时间为系列下预告列表中开始时间最大的时间
            updateForeshowGroupBeginTime(entity, foreshowId, appId)
        }
        return res
    }

    //更新live_forshow表中系列的开始时间为系列下预告列表中开始时间最大的时间
    @Override
    def updateForeshowGroupBeginTime (LiveForeshow liveForeshow, long foreshowId, String appId){
        List foreshowList = liveForeshowRes.findForeshowListByParentId(foreshowId,appId)
        log.info("生成回看时调用，foreshowList==>{}，foreshowId:{}",Strings.toJson(foreshowList),foreshowId)
        if(foreshowList && foreshowList.size()>0){
            def beginTime = foreshowList.get(0).begin_time
            log.info("生成回看时调用，beginTime==>{}，foreshowId:{}",beginTime,foreshowId)
            if(beginTime){
                log.info("调用开始，foreshowId:{}",foreshowId)
                SimpleDateFormat sim=new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                Date d=sim.parse(Strings.getDayOfTime(beginTime));
                liveForeshow.beginTime = d
                def result = liveForeshowRes.update(liveForeshow)
                log.info("wangtf uodate foreshow group beginTime foreshowId:{},beginTime:{},result={}", foreshowId, liveForeshow.beginTime,result)
            }
        }
    }

    @Override
    def updateForeshowGroupBeginTime (long foreshowId, String appId){
        List foreshowList = liveForeshowRes.findForeshowListByParentId(foreshowId,appId)
        log.info("生成回看时调用，foreshowList==>{}，foreshowId:{}",Strings.toJson(foreshowList),foreshowId)
        if(foreshowList && foreshowList.size()>0){
            def beginTime = foreshowList.get(0).begin_time
            log.info("生成回看时调用，beginTime==>{}，foreshowId:{}",beginTime,foreshowId)
            if(beginTime){
                log.info("调用开始，foreshowId:{}",foreshowId)
                SimpleDateFormat sim=new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                Date d=sim.parse(Strings.getDayOfTime(beginTime));
                def result = liveForeshowRes.updateUpdateTime(foreshowId,d)
                log.info("wangtf uodate2 foreshow group beginTime foreshowId:{},beginTime:{},result={}", foreshowId, beginTime,result)
            }
        }
    }

    @Override
    public List listForeShowByParentId(Map params){
        long liveSerieId = params.parentId
        int psize = params.psize
        String sortInfo = params.sortInfo
        long userId = params.userId
        String appId = params.appId
        def status = params.status
        String vc = params.vc


        def testUserList = testUser ?  Arrays.asList(testUser.split(",")): null;
        List retList=[];
        if (!sortInfo || sortInfo=='{}') {//如果读取第一页，读取\写入缓存
            if(!testUserList || !testUserList.contains(userId as String)) {//如果是测试用户，不走缓存
                retList = liveQcloudRedis.getForeshowByParentList(appId,liveSerieId,vc)
            }
        }
        if(retList && retList.size()>0) {
            return retList
        }


        long startTime = System.currentTimeMillis()
        List subForeshowList = liveForeshowRes.listForeShowByParentId(params)
        def openRemindList = liveService.listOpenRemind(userId, appId)//预约列表

        retList=[];
        if(subForeshowList){
            long foreshowIdTemp=0;
            Map entryTemp=null;
            for(int i=0;i<subForeshowList.size();i++){
                if(subForeshowList.get(i).foreshowId!=foreshowIdTemp){

                    if(entryTemp){
                        entryTemp.remove("totalWatchCount");
                        entryTemp.remove("flag");
                        retList <<entryTemp;
                    }

                    entryTemp=subForeshowList.get(i);
                    foreshowIdTemp=subForeshowList.get(i).foreshowId

                    entryTemp.viewType= 10002
                    if (openRemindList.contains(entryTemp.foreshowId)){
                        entryTemp.appointment =true   //已预约
                    }



                    entryTemp.timeSpan = 0
                    entryTemp.watchCount = 0
                    entryTemp.flag=false;

                    def liveRecordInfo =  Strings.parseJson(subForeshowList.get(i).liveRecordInfo)
                    def videoAddress = liveRecordInfo ? liveRecordInfo.video_address : ""

                    if (entryTemp.foreshowType == LiveCommon.FORESHOW_TYPE_1){//互动直播
                        entryTemp.invokeType = LiveCommon.INVOKE_TYPE_LIVE_FORESHOW_RECORD

                        if (videoAddress){
                            entryTemp.flag=true;
                            entryTemp.timeSpan = liveRecordInfo.time_span as int
                            entryTemp.watchCount=liveRecordInfo.total_watch_count?:0 as int
                        }else{

                            Map dataMap = [
                                liveId    : subForeshowList.get(i).liveId,
                                roomId    : subForeshowList.get(i).roomId,
                                foreshowId: subForeshowList.get(i).foreshowId,
                                appId     : appId,
                                vc        : vc
                            ]
                            Map videoAddressMap = liveService.getVideoAddressUrlList(subForeshowList.get(i).videoAddress as String, dataMap)
                            entryTemp.timeSpan = (videoAddressMap?.timeSpan  ?: 0) as int

                            try {
                                entryTemp.watchCount =subForeshowList.get(i).totalWatchCount
                            }catch (Exception e){
                                entryTemp.watchCount = 0
                            }
                        }
                    }else if (entryTemp.foreshowType == LiveCommon.FORESHOW_TYPE_2 || entryTemp.foreshowType == LiveCommon.FORESHOW_TYPE_3){//会议直播，人数必须从缓存取
                        entryTemp.invokeType = LiveCommon.INVOKE_TYPE_LIVE_FORESHOW_VALUE_RECORD

                        Map dataMap = [
                            liveId    : subForeshowList.get(i).liveId,
                            roomId    : subForeshowList.get(i).roomId,
                            foreshowId: subForeshowList.get(i).foreshowId,
                            appId     : appId,
                            vc        : vc
                        ]

                        //取时长
                        if (videoAddress){
                            entryTemp.flag=true;
                            entryTemp.timeSpan = liveRecordInfo.time_span as int
                        }else {
                            Map videoAddressMap = liveService.getVideoAddressUrlList(subForeshowList.get(i).videoAddress as String, dataMap)
                            entryTemp.timeSpan = (videoAddressMap?.timeSpan  ?: 0) as int
                        }

                        //取人数
                        try {
                            entryTemp.watchCount = liveQcloudRedis.getLiveWatherTotalCount(subForeshowList.get(i).liveId)
                        }catch (Exception e){
                            entryTemp.watchCount = 0
                        }
                    }

                }else{
                    if (subForeshowList.get(i).foreshowType == LiveCommon.FORESHOW_TYPE_1){//互动直播
                        if(!entryTemp.flag){
                            Map dataMap = [
                                liveId    : subForeshowList.get(i).liveId,
                                roomId    : subForeshowList.get(i).roomId,
                                foreshowId: subForeshowList.get(i).foreshowId,
                                appId     : appId,
                                vc        : vc
                            ]
                            Map videoAddressMap = liveService.getVideoAddressUrlList(subForeshowList.get(i).videoAddress as String, dataMap)
                            entryTemp.timeSpan += (videoAddressMap?.timeSpan  ?: 0) as int

                            def tempWatchCount = 0
                            try {
                                tempWatchCount=subForeshowList.get(i).totalWatchCount
                            }catch (Exception e){
                                tempWatchCount = 0
                            }
                            entryTemp.watchCount += tempWatchCount
                        }
                    }else if(subForeshowList.get(i).foreshowType == LiveCommon.FORESHOW_TYPE_2 || entryTemp.foreshowType == LiveCommon.FORESHOW_TYPE_3){//会议直播，人数必须从缓存取
                        if(!entryTemp.flag){
                            Map dataMap = [
                                liveId    : subForeshowList.get(i).liveId,
                                roomId    : subForeshowList.get(i).roomId,
                                foreshowId: subForeshowList.get(i).foreshowId,
                                appId     : appId,
                                vc        : vc
                            ]
                            Map videoAddressMap = liveService.getVideoAddressUrlList(subForeshowList.get(i).videoAddress as String, dataMap)
                            entryTemp.timeSpan += (videoAddressMap?.timeSpan  ?: 0) as int

                            def tempWatchCount = 0
                            try {
                                tempWatchCount = liveQcloudRedis.getLiveWatherTotalCount(subForeshowList.get(i).liveId)
                            }catch (Exception e){
                                tempWatchCount = 0
                            }
                            entryTemp.watchCount += tempWatchCount
                        }else {
                            def tempWatchCount = 0
                            try {
                                tempWatchCount = liveQcloudRedis.getLiveWatherTotalCount(subForeshowList.get(i).liveId)
                            }catch (Exception e){
                                tempWatchCount = 0
                            }
                            entryTemp.watchCount += tempWatchCount
                        }
                    }

                }

            }
            if(foreshowIdTemp && entryTemp ){
                entryTemp.remove("flag");
                entryTemp.remove("totalWatchCount");
                retList <<entryTemp;
            }
        }else {
            retList=[]
        }

        if (!sortInfo || sortInfo=='{}') {//如果读取第一页，读取\写入缓存
            if(retList && (!testUserList || !testUserList.contains(userId as String))) {//如果是测试用户，不走缓存
                liveQcloudRedis.setForeshowByParentList(appId,liveSerieId, retList,vc)
            }
        }
        return retList;
    }

    @Override
    int getLiveWatcherCountByParentId(long foreshowId, int status) {
        List liveIdList = liveForeshowRes.listLiveIdByParentId(foreshowId, status)
        int watchCount = 0;
        liveIdList.each{
            int c = 0
            try {
//                c = liveQcloudRedis.getLiveWatherCount(it)
                c = vestUserService.getLiveRoomWatchCount(it)
            }catch (Exception e){
                c = 0
            }
            watchCount+=c
        }
        return watchCount
    }

    int getLiveWatcherCountById(long foreshowId, int status) {
        List liveIdList = liveForeshowRes.listLiveIdById(foreshowId, status)
        int watchCount = 0;
        liveIdList.each{
            int c = 0
            try {
//                c = liveQcloudRedis.getLiveWatherCount(it)
                c = vestUserService.getLiveRoomWatchCount(it)
            }catch (Exception e){
                c = 0
            }
            watchCount+=c
        }
        return watchCount
    }

    def pausePgc(long foreshowId,String appId,LiveRecord liveRecord,LiveForeshow liveForeshow,String message,String vc){
/*        SimpleDateFormat format = new SimpleDateFormat(FULL_DATE_PATTERN);
        Date date = new Date();*/

        long beginTimeLong =DateUtil.getTimestamp(liveRecord?.beginTime)
        if(beginTimeLong>System.currentTimeMillis()){
            return [ "status":"1"]
        }

       // LiveRecord liveRecord = liveService.getLiveByForeshow(foreshowId,appId)
        if(LiveCommon.FORESHOW_STATUS_1!=liveRecord.pgcStatus) return
        liveQcloudRedis.setPgcPaushTime(foreshowId,System.currentTimeMillis());
      //  System.out.println(System.currentTimeMillis())
        //System.out.println("pausePgc----->"+roomId);


        Parallel.run([1], {
            qcloudLiveService.sendPgcMsg([roomId: liveRecord?.roomId,fromAccount: liveForeshow?.userId,appId:liveRecord.appId,
                                          msgInfo:[pgcStatus:LiveCommon.FORESHOW_STATUS_6, message: message,liveId:liveRecord?.liveId]])
            if (VerUtils.toIntVer(vc) >= VerUtils.toIntVer("5.5") ) {//5.5以后版本
                qcloudPgcLiveService.pauseQcloudChannel(liveRecord)
            }else {
                qcloudPgcLiveService.stopLiveChannel(liveRecord)
            }
             } ,TimeUnit.SECONDS.toMillis(5)
        )


        liveRecord.setPgcStatus(LiveCommon.FORESHOW_STATUS_6)
        liveQcloudRedis.updateLiveList(liveRecord);

       // qcloudPgcLiveService.startQcloudChannel(liveRecord)

        liveForeshowRes.updateStatus(foreshowId,LiveCommon.FORESHOW_STATUS_6)

        liveForeshowRes.insertPauseLog([
            foreshowId:foreshowId,
            liveId:liveRecord.liveId,
            pauseMessage:message
        ])
        return [ "status":"1"]
    }

    def beginPgc(long foreshowId,String appId,LiveRecord liveRecord,LiveForeshow liveForeshow,String message,String vc) {

       // LiveRecord liveRecord = liveService.getLiveByForeshow(foreshowId,appId)
        //不到直播开始时间，不修改状态
        long beginTimeLong =DateUtil.getTimestamp(liveRecord?.beginTime)
        if(beginTimeLong>System.currentTimeMillis()){
            return [ "status":"1"]
        }


        long time = liveQcloudRedis.getPgcPaushTime(foreshowId);//上一次放入的时间
      //  System.out.println(System.currentTimeMillis())
       // System.out.println("beginPgc----->1"+[roomId: roomId, hostUid: userId, msgInfo: [pgcStatus: 1, message: message]]);
        long newTimeLong = time?System.currentTimeMillis() - time:0;
        long oldTimeLong = liveQcloudRedis.getPgcPaushTimeLong(foreshowId);//历史时长

        liveQcloudRedis.setPgcPaushTimeLong(foreshowId, newTimeLong + oldTimeLong)


        //LiveForeshow  foreshow = get(foreshowId);
        long timeSpan = System.currentTimeMillis() - (liveForeshow?.beginTime?.getTime()?:0) //总时长
        timeSpan = timeSpan - newTimeLong - oldTimeLong //总时间 - 暂停时间



        Parallel.run([1], {
             qcloudLiveService.sendPgcMsg([roomId:liveRecord?.roomId, fromAccount: liveForeshow?.userId,appId:liveRecord.appId,
                                           msgInfo: [pgcStatus: LiveCommon.FORESHOW_STATUS_1, message: message,timeSpan: timeSpan/1000,liveId:liveRecord?.liveId]])
          } ,TimeUnit.SECONDS.toMillis(5)
        )

        liveRecord.setPgcStatus(LiveCommon.FORESHOW_STATUS_1)
        liveQcloudRedis.updateLiveList(liveRecord);

       // LiveForeshow  liveForeshow = get(foreshowId);
        if(liveForeshow && liveForeshow.foreshowStatus == LiveCommon.FORESHOW_STATUS_6){
            if (VerUtils.toIntVer(vc) >= VerUtils.toIntVer("5.5") ) {//5.5以后版本
                qcloudPgcLiveService.startQcloudChannel(liveRecord)
            }else {
                qcloudPgcLiveService.startLiveChannel(liveRecord)
            }
            def res = liveForeshowRes.updateStatus(foreshowId,LiveCommon.FORESHOW_STATUS_1)
            if (res){
                //给客户端发直播开始消息
                def msgList=[]
                Parallel.run([1], {
                    def msgMap = [operType:1,userId:liveForeshow.userId,srpId: liveForeshow.srpId,title: liveForeshow.title,foreshowId: liveForeshow.foreshowId,appId: liveForeshow.appId,imgUrl:bean.imgUrl]
                    if (liveForeshow.foreshowType==LiveCommon.FORESHOW_TYPE_2){
                        msgMap.category = "meeting"
                    }
                    msgList<< msgMap
                    log.info("zs.live.service.impl.LiveForeshowServiceImpl.beginPgc 手动执行pushLiveStartMsg方法，msgList={}",Strings.toJson(msgList))
                    liveService.pushLiveStartMsg(msgList)
                }, TimeUnit.SECONDS.toMillis(5)
                )
            }
        }

        return [ "status":"1"]
    }

    @Override
    def stopForeshow(long foreshowId,String appId,LiveRecord liveRecord,LiveForeshow liveForeshow){
        log.info("后台结束预告,foreshowId=>{},appId=>{}",foreshowId,appId)
        long beginTimeLong =DateUtil.getTimestamp(liveRecord?.beginTime)
        if(beginTimeLong>System.currentTimeMillis()){
            return [ "status":"1"]
        }
        if(!liveRecord){
          //  throw new ApiException(700, "未找到关联会议，结束失败");
            log.info("foreshowId==>{},未找到关联会议，结束失败",foreshowId)
            return [ "status":"0",msg:"未找到关联会议，结束失败"]
        }

        if(liveRecord){
            log.info("后台结束预告存在直播记录,foreshowId=>{},liveId={},appId={}",foreshowId,liveRecord.liveId,appId)
            livePgcService.stop(liveRecord,"后台预告结束调用")
            //预告结束时，状态先设置为5：暂时没有回放记录，即没有回放记录时列表中则不显示该官方回放
            liveRes.updateLiveForeshowStatus(foreshowId,LiveCommon.FORESHOW_STATUS_5, appId)
            //通知IM直播结束
            qcloudLiveService.sendPgcMsg([roomId: liveRecord.roomId,fromAccount: liveRecord.userId,appId:liveRecord.appId,
                                          msgInfo:[pgcStatus:LiveCommon.FORESHOW_STATUS_5, message: "预告结束",liveId:liveRecord.liveId]])
            //Parallel.run([1],{
               // Thread.sleep(2*1000);
                //如果此预告属于某个系列，更新系列的结束时间
//            log.info("预告结束后调用，foreshowId={}",foreshowId)
//                LiveForeshow liveForeshow=this.get(foreshowId);
//                log.info("预告结束后异步调用，foreshowId={},liveForeshow对象={}",foreshowId,Strings.toJson(liveForeshow))
//                if(liveForeshow.parentId){
//                    //updateUpdateTime(liveForeshow.parentId,liveForeshow.updateTime);
//                    updateForeshowGroupBeginTime(liveForeshow.parentId,appId)
//                }
            Parallel.run([1],{
                //通知王龙直播统计 TODO 暂时只是与王龙联调，后期会删掉，统一由分析nginx日志的项目处理
                qcloudLiveService.sendLiveDataToStatistic([jsonType:LiveCommon.STATISTIC_JSON_TYPE_3,liveId:liveRecord.liveId,foreshowId:liveRecord.foreshowId,roomId:liveRecord.roomId,
                                                           userId:liveRecord.userId.toString(),nickname:liveRecord.nickname,title:liveRecord.title,isPrivate:liveRecord.isPrivate,
                                                           liveMode:liveRecord.liveMode,appId:liveRecord.appId,vc:liveRecord.vc,createTime:System.currentTimeMillis()])
            },1)
                //获取腾讯云回看地址
                log.info("roomId==>{},获取会议直播回看开始===》",liveRecord.roomId)
                String videoAddress= livePgcService.updateBackVideoAddress(liveRecord)
                log.info("会议直播回放地址生成 ，liveId={},roomId={},videoAddress={}",liveRecord.liveId,liveRecord.roomId,videoAddress)
                if (videoAddress) {
                    qcloudLiveRes.updateLiveRecordVideoAddress(videoAddress, liveRecord.liveId, null)
                    if (liveRecord.foreshowId) {
                        log.info("更新预告状态为回看状态，foreshowId==>{}", liveRecord.foreshowId);
                        liveService.updatePgcForeshowMergeInfo(liveRecord.foreshowId,liveRecord.appId)
                    }
                    //通知Im消息生成回看
                    qcloudLiveService.sendPgcMsg([roomId: liveRecord.roomId, fromAccount: liveRecord.userId,appId:liveRecord.appId,
                                                  msgInfo: [pgcStatus: LiveCommon.FORESHOW_STATUS_2, message: "回看生成",liveId:liveRecord.liveId]])
                }
           // },10)
        }
        return [ "status":"1"]
    }

    @Override
    List listInfoByParentIds(List<Long> parentIdList, int status,String appId,String vc) {
        liveForeshowRes.listInfoByParentIds(parentIdList,status,appId, vc)
    }

    @Override
    def updateUpdateTime(long foreshowId,Date updateTime){
        liveForeshowRes.updateUpdateTime(foreshowId, updateTime);
    }

    @Override
    def findPauseLog(long foreshowId, long liveId) {
        def result = liveForeshowRes.findPauseLog(foreshowId,liveId)
        return [
            pauseMessage:result?.pause_message
        ]
    }

    @Override
    def updateStatus(long foreshowId, int foreshowStatus) {
        liveForeshowRes.updateStatus(foreshowId, foreshowStatus)
    }

    @Override
    def updateForeshowStarted(Map paramMap) {
        LiveForeshow entity = liveForeshowRes.get(paramMap.foreshowId)
        if (!entity){
            return 0
        }

        entity.cateId = paramMap.cateId
        entity.parentId = paramMap.parentId
        entity.title = paramMap.title
        entity.imgUrl = paramMap.imgUrl
        entity.showTitleInList=paramMap.showTitleInList ?:0
        entity.notShowInClient=paramMap.notShowInClient ?:0
        entity.ruleJson = paramMap.columns
        int status = liveForeshowRes.update(entity);

        if (status && !saveForeshowSrpRelation(paramMap)){
            return 0
        }
        return status
    }

    @Override
    def writebackForeshowWatchCount(){
        // TODO 定时回写会议直播观看人数到foreshow表，以优化直播列表和直播系列列表查询效率
    }

    @Override
    def checkForeshowPayment(Map params){
        def result = [:]
        try {
            def userInfo = liveService.getUserInfo(params.userId as long)
            def foreshowIds = params.foreshowIds //1009051,1009052
            if (userInfo){
//                def url = "${liveForeshowPayStatusUrl}?liveId=${foreshowIds}&userName=${Strings.getUrlEncode(userInfo.userName,"UTF-8")}"
                Map map = [liveId: foreshowIds, userName: Strings.getUrlEncode(userInfo.userName,"UTF-8")]
                def response = Http.post(liveForeshowPayStatusUrl, map)    //{"head":{"code":200,"msg":"成功"},"body":{"1009051":"1","1009052":0}}
                log.info("checkForeshowPayment url >>> ${liveForeshowPayStatusUrl}   map >>> ${map}")
                log.info("checkForeshowPayment response >>> ${response}")
                def json = Strings.parseJson(response)
                if (json.head.code == 200){
                    json.body.each{k,v ->
                        result.put(k as String,v)
                    }
                }
            }
        } catch (Exception e) {
            result = [:]
        }
        log.info("checkForeshowPayment result >>> ${result}")
        return result
    }
}
