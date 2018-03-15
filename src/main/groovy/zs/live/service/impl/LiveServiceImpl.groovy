package zs.live.service.impl

import com.mongodb.DBObject
import com.mongodb.util.JSON
import com.qcloud.Module.Live
import groovy.sql.GroovyRowResult
import groovy.util.logging.Slf4j
import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import zs.live.ApiException
import zs.live.common.LiveCommon
import zs.live.common.QcloudCommon
import zs.live.dao.kestrel.LiveKestrel
import zs.live.dao.mysql.LiveForeshowRes
import zs.live.dao.mysql.LiveRes
import zs.live.dao.mysql.QcloudLiveRes
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.dao.redis.LiveRedis
import zs.live.entity.LiveForeshow
import zs.live.entity.LiveRecord
import zs.live.service.LiveForeshowService
import zs.live.service.LivePgcService
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.service.ShortURLService
import zs.live.service.VestUserService
import zs.live.service.VideoMergeService
import zs.live.utils.DateUtil
import zs.live.utils.Http
import zs.live.utils.ImageUtils
import zs.live.utils.Parallel
import zs.live.utils.Strings
import zs.live.utils.UrlSignedUtil
import zs.live.utils.VerUtils
import zs.live.utils.ZURL

import javax.servlet.http.HttpServletRequest
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

/**
 * Created by Administrator on 2016/7/25.
 */
@Slf4j
@Service
class LiveServiceImpl implements LiveService {
    @Value('${user.info.souyue.url}')
    String userInfoUrl;
    @Value('${live.test.user.list}')
    String testUser
    @Value('${live.info.play.rate}')
    int playRate
    @Value('${live.user.pay.list.url}')
    String userPaidListUrl
    @Value('${live.message.url}')
    String messageUrl
    @Value('${live.access.check.url}')
    String liveAccessUrl
    @Value('${live.access.check.appIds}')
    String liveAccessAppIds
    @Value('${live.fans.sync.url}')
    String liveFansSyncUrl
    @Value('${live.fans.sync.appIds}')
    String liveFansSyncAppIds
    @Value('${live.get.appname.url}')
    String liveGetAppNameUrl
    @Value('${live.get.appname.signkey}')
    String liveGetAppNameSignkey

    @Autowired
    LiveRes liveRes;
    @Autowired
    LiveQcloudRedis liveQcloudRedis
    @Autowired
    ShortURLService shortURLService;
    @Autowired
    QcloudLiveService qcloudLiveService
    @Autowired
    LiveKestrel liveKestrel
    @Autowired
    LivePgcService livePgcService
    @Autowired
    LiveForeshowService liveForeshowService
    @Autowired
    VideoMergeService videoMergeService
    @Autowired
    LiveRedis liveRedis
    @Autowired
    VestUserService vestUserService
    @Autowired
    QcloudLiveRes qcloudLiveRes
    @Autowired
    UrlSignedUtil urlSignedUtil
    @Autowired
    LiveForeshowRes liveForeshowRes
    @Override
    public List getMyFans(long userId, int fansType, int followType, long lastId, int pageSize, String appId) {
        def list = liveRes.getMyFans(userId, fansType, followType, lastId, pageSize, appId);
        if (list) {
            list.each {
                def userMap = this.getUserInfo(it.userId);
                it.userName = userMap?.userName ?: "";
                it.nickname = userMap?.nickname ?: "";
                it.userImage = userMap?.userImage ?: "";
            }
        }
        return list;
    }

    @Override
    public int getMyFansCount(long userId, int fansType, int followType, long lastId, String appId) {
        return liveRes.getMyFansCount(userId, fansType, followType, lastId, appId);
    }

    public List getMyFollowings(long fansUserId, int fansType, int followType, long lastId, int pageSize, String appId) {
        def list = liveRes.getMyFollowings(fansUserId, fansType, followType, lastId, pageSize, appId);
        if (list) {
            list.each {
                def userMap = this.getUserInfo(it.userId);
                it.userName = userMap?.userName ?: "";
                it.nickname = userMap?.nickname ?: "";
                it.userImage = userMap?.userImage ?: "";
            }
        }
        return list;
    }

    @Override
    public int getMyFollowingsCount(long fansUserId, int fansType, int followType, long lastId, String appId) {
        return liveRes.getMyFollowingsCount(fansUserId, fansType, followType, lastId, appId);
    }

    @Override
    public int addFollow(long fromUserId, long toUserId, int fansType, String appId) {
        return liveRes.addFollow(fromUserId, toUserId, fansType, appId)
    }

    @Override
    public int cancelFollow(long fromUserId, long toUserId, int fansType, String appId) {
        return liveRes.cancelFollow(fromUserId, toUserId, fansType, appId);
    }

    @Override
    public def isFollow(long fromUserId, long toUserId, int fansType, String appId) {
        return liveRes.isFollow(fromUserId, toUserId, fansType, appId);
    }
    @Override
    LiveRecord findLiveByLiveId(long liveId){
        LiveRecord liveRecord = null
        try {
            liveRecord = liveQcloudRedis.getLiveRecord(liveId)
            if(!liveRecord){
                def result = liveRes.findLiveByLiveId(liveId)
                liveRecord = this.toLiveRecord(result)
                if(liveRecord && liveRecord.liveId){
                    liveQcloudRedis.updateLiveList(liveRecord)
                }
            }
        }catch (Exception e){
            e.printStackTrace()
        }
        return liveRecord
    }

    @Override
    LiveRecord updateLiveRedisByLiveId(long liveId){
        LiveRecord liveRecord = null
        try {
            def result = liveRes.findLiveByLiveId(liveId)
            liveRecord = this.toLiveRecord(result)
            if(liveRecord && liveRecord.liveId){
                liveQcloudRedis.updateLiveList(liveRecord)
            }
        }catch (Exception e){
            e.printStackTrace()
        }
        return liveRecord
    }

    @Override
    def findNewsLiveList(Map map) {
        int psize = map.psize as int
        def resultList = []
        while( resultList.size()<psize){
            List liveList = liveRes.findNewstLiveList(map)
            def testUserList = testUser.split(",")
            liveList?.each {
                if(!testUserList.contains(map.userId as String) && (it.title as String).startsWith("zstest")){
                }else{
                    def data = getLiveReturn(it)
                    resultList << data
                }
            }
            if(liveList.size() < psize) //当取到的列表的数量少于一页的数量时，表示没有数据了，跳出循环
                break;
            def lastOne = liveList.size() > 0 ? toLiveRecord(liveList.get(liveList.size()-1)) : null
            def sortInfo = lastOne ? [orderId: lastOne.newliveSortNum, createTime: DateUtil.getTimestamp(lastOne.createTime as String)] : null
            map.sortInfo = Strings.toJson(sortInfo)
        }
        return resultList
    }

    @Override
    def findFaceLiveList(Map map) {
        def resultList = []
        List liveList = liveRes.findFaceLiveList(map)
        liveList?.each {live->
            LiveRecord liveRecord = this.toLiveRecord(live)
            String shortUrl = shortURLService.getShortUrlLive([liveId:liveRecord.liveId,liveMode: liveRecord.liveMode,userId: liveRecord.userId,roomId: liveRecord.roomId,vc: liveRecord.vc,appId: map.appId])
            int watchCount = vestUserService.getLiveRoomWatchCount(liveRecord.liveId)
            Map data= [
                    invokeType: liveRecord.liveFromSdk==1? LiveCommon.INVOKE_TYPE_LIVE_FROM_SDK : LiveCommon.INVOKE_TYPE_LIVE,
                    viewType: LiveCommon.VIEW_TYPE_LIVE,
                    liveId: liveRecord.liveId,
                    title: liveRecord.title,
                    liveRoom: [
                            roomId: liveRecord.roomId,
                            chatId: liveRecord.chatId,
                            watchCount: watchCount < 0 ? 0 : watchCount
                    ],
                    anchorInfo: [
                            nickname: liveRecord.nickname,
                            userImage: liveRecord.userImage,
                            userId: liveRecord.userId
                    ],
                    sortInfo: [
                        isLiving: 1,
                            orderId: liveRecord.liveId,
                            createTime: DateUtil.getTimestamp(liveRecord.createTime)
                    ],
                    shortUrl: shortUrl,
                    liveThumb: liveRecord.liveThump ?: ImageUtils.getLiveListImg(liveRecord.userImage),
                    isLiving: 1
            ]
            resultList << data
        }

        return resultList
    }

    def getLiveReturn (def live){
        LiveRecord liveRecord = this.toLiveRecord(live)
        String shortUrl = shortURLService.getShortUrlLive([liveId:liveRecord.liveId,liveMode: liveRecord.liveMode,userId: liveRecord.userId,roomId: liveRecord.roomId, vc: liveRecord.vc,appId: liveRecord.appId])
        int watchCount = vestUserService.getLiveRoomWatchCount(liveRecord.liveId)
        return [
            invokeType: LiveCommon.INVOKE_TYPE_LIVE,
            viewType: LiveCommon.VIEW_TYPE_LIVE,
            liveId: liveRecord.liveId,
            title: liveRecord.title,
            liveRoom: [
                roomId: liveRecord.roomId,
                chatId: liveRecord.chatId,
                watchCount: watchCount
            ],
            anchorInfo: [
                nickname: liveRecord.nickname,
                userImage: liveRecord.userImage,
                userId: liveRecord.userId
            ],
            sortInfo: [
                orderId: liveRecord.newliveSortNum,
                createTime: DateUtil.getTimestamp(liveRecord.createTime)
            ],
            shortUrl: shortUrl,
            liveThumb: liveRecord.liveThump ?: ImageUtils.getLiveListImg(liveRecord.userImage),
        ]
    }
    @Override
    def findLiveRecordList(Map map) {
        List resultList = []
        int psize = map.psize ? map.psize as int : 10
        def testUserList = testUser.split(",")
        while (resultList.size() < psize){
            List foreshowList = liveRes.findLiveForeshowListForRecord(map)
            if(!foreshowList){
                break
            }
            foreshowList?.each { foreshow ->
                if(!testUserList.contains(map.userId as String) && (foreshow.title as String).startsWith("zstest")){
                }else{
                    def liveRecordInfo = Strings.parseJson(foreshow.live_record_info as String)
                    int totalWatchCount = (liveRecordInfo?.total_watch_count ?: 0) as int
                    int timeSpan = (liveRecordInfo?.time_span ?: 0) as int
                    def shortUrl = ""
                    if(foreshow.foreshow_type as int == LiveCommon.FORESHOW_TYPE_3){
                        def liveRecordLog = liveRes.findLiveRecordByForeshowId(foreshow.foreshow_id as long)
                        shortUrl = shortURLService.getShortUrlLivePay([foreshowId: foreshow.foreshow_id, liveId: liveRecordLog.live_id,userId: map.userId,vc: map.vc,appId: foreshow.app_id])
                    }else if(foreshow.foreshow_type as int == LiveCommon.getLIVE_MODE_2()){
                        shortUrl = shortURLService.getShortUrlPgcForeshow([foreshowId: foreshow.foreshow_id, liveId: liveRecordLog.live_id,userId: map.userId,vc: map.vc,appId: foreshow.app_id])
                    }else {
                        shortUrl = shortURLService.getShortUrlForeshow([foreshowId:foreshow.foreshow_id,liveMode:foreshow.foreshow_type,userId: foreshow.user_id as long, vc: map.vc,appId: foreshow.app_id])
                    }
                    def data =[
                        invokeType: LiveCommon.INVOKE_TYPE_LIVE_FORESHOW_RECORD,
                        viewType: LiveCommon.VIEW_TYPE_LIVE_FORESHOW_RECORD,
                        foreshowId: foreshow.foreshow_id as long,
                        title: foreshow.title,
                        watchCount: totalWatchCount,
                        timeSpan: timeSpan,
                        anchorInfo: [
                            nickname: foreshow.nickname,
                            userImage: foreshow.user_image,
                            userId: foreshow.user_id
                        ],
                        sortInfo: [
                            orderId: foreshow.sort_num,
                            createTime: DateUtil.getTimestamp(foreshow.begin_time as String)
                        ],
                        shortUrl: shortUrl,
                        liveThumb: foreshow.img_url,
                        beginTime: DateUtil.getTimestamp(foreshow.begin_time as String)
                    ]
                    resultList << data
                }
            }
            def lastOne = foreshowList.size() > 0 ? foreshowList.get(foreshowList.size()-1) : null
            def sortInfo = lastOne ? [orderId: lastOne.sort_num, createTime: DateUtil.getTimestamp(lastOne.begin_time as String)] : null
            map.sortInfo = Strings.toJson(sortInfo)
        }
        return resultList
    }

    @Override
    def findMyLiveRecordList(Map map) {
        def liveList = liveRes.findMyLiveRecordList(map)
        return getMyLiveRecordReturn(liveList,map.vc)
    }

    @Override
    def findMyLiveRecordListNew(Map map) {
        List liveList = []
        def liveList1 = liveRes.findMyLiveRecordListForUgc(map)
        if(liveList1){
            liveList.addAll(liveList1)
        }
        if (!map.onlyUgc){
            def liveList2 = liveRes.findMyLiveRecordListNew(map)
            if(liveList2){
                liveList.addAll(liveList2)
            }
        }
        if(liveList){
            sortByFileName(liveList,"lastId", 1)    //按照lastId降序排列
        }
        return getMyLiveRecordReturn(liveList, map.vc)
    }

    def getMyLiveRecordReturn(List liveRecordList, String vc){
        def resultList = []
        liveRecordList?.each {
            if(it.lastId){
                it.create_time = it.lastId
            }
            LiveRecord liveRecord = this.toLiveRecord(it)
            String shortUrl = shortURLService.getShortUrlLive([liveId:liveRecord.liveId,liveMode: liveRecord.liveMode,userId: liveRecord.userId,roomId: liveRecord.roomId, vc: liveRecord.vc,appId: liveRecord.appId])
            def data = [:]
            if(liveRecord.foreshowId){
                data.put("foreshowId",liveRecord.foreshowId)
            }else {
                data.put("liveId",liveRecord.liveId)
            }
            if (VerUtils.toIntVer(vc) >= VerUtils.toIntVer("5.6.7") && liveRecord.foreshowId) {
                if(liveRecord.liveMode==LiveCommon.LIVE_MODE_1){
                    data.put("invokeType", LiveCommon.INVOKE_TYPE_LIVE_FORESHOW_RECORD)
                }else {
                    data.put("invokeType", LiveCommon.INVOKE_TYPE_LIVE_FORESHOW_VALUE_RECORD)
                }
            }else if (VerUtils.toIntVer(vc) >= VerUtils.toIntVer("5.6.7")) {
                data.put("invokeType", LiveCommon.INVOKE_TYPE_LIVE_FORESHOW_RECORD)
            }
            long watchCount = liveRecord.totalWatchCount ?: liveQcloudRedis.getLiveWatherTotalCount(liveRecord.liveId)
            data.putAll( [
                title: liveRecord.title ?: "我发起了一个直播，快来看看吧！",
                watchCount: watchCount < 0 ? 0: watchCount,
                anchorInfo: [
                    nickname: liveRecord.nickname,
                    userImage: liveRecord.userImage,
                    userId: liveRecord.userId
                ],
                sortInfo: [
                    isLiving: 0,
                    orderId: liveRecord.newliveSortNum,
                    createTime: DateUtil.getTimestamp(liveRecord.createTime)
                ],
                shortUrl: shortUrl,
                isPrivate: liveRecord.isPrivate,
                liveThump: liveRecord.userImage,
                isLiving: 0
            ])
            resultList << data
        }
        return resultList
    }

    @Override
    def findLiveConfig(String appId, int configId) {
        return liveRes.fingLiveConfig(appId,configId)
    }

    @Override
    def findLiveForeshowList(Map map) {
        long startTime = System.currentTimeMillis()
        def preList = liveRes.getLiveForeshowListForLive(map)
        def resultList = []
        def testUserList = testUser.split(",")
        preList?.each {
            if(!testUserList.contains(map.userId as String) && (it.title as String).startsWith("zstest")){
            }else{
                long beginTime =  DateUtil.getTimestamp(it.begin_time ? it.begin_time as String : "")
                int isHost = (map.userId as String).equals(it.user_id as String) ? 1:0
                int invokeType=it.url_tag as int
                long blogId
                String category = ""
                if(invokeType==20||invokeType==24){
                    blogId=it.url as long
                    category = "interest"
                }else if(invokeType == 10){
                    category = "srp"
                }
                int isOpenRemind
                if(isHost == 0){
                    isOpenRemind = liveRes.isOpenRemind(map.appId, map.userId as long, it.foreshow_id)
                }
                LiveRecord liveRecord = null
                int watchCount
                if(beginTime <= System.currentTimeMillis()){
                    invokeType = LiveCommon.INVOKE_TYPE_LIVE_FORESHOW
                    liveRecord = getLiveByForeshow(it.foreshow_id as long, map.appId)
                    long liveId = liveRecord?.liveId ?: 0
                    if(liveId){
                        watchCount = vestUserService.getLiveRoomWatchCount(liveId)
                    }
                }
                String shortUrl = shortURLService.getShortUrlLive([liveId: liveRecord?.liveId ?: 0,liveMode: liveRecord?.liveMode ?:0,userId:it.user_id as long,roomId: liveRecord?.roomId ?: 0, vc: liveRecord?.vc, appId: liveRecord?.appId])
                resultList << [
                    invokeType: invokeType,
                    viewType: LiveCommon.VIEW_TYPE_LIVE_FORESHOW,
                    foreshowId: it.foreshow_id as long,
                    category: category,
                    watchCount: watchCount,
                    liveThumb: it.img_url,
                    title: it.title,
                    keyword: it.keyword,
                    srpId: it.srp_id,
                    url: it.url,
                    blogId: blogId,
                    beginTime: beginTime,
                    liveStatus: (beginTime > System.currentTimeMillis()) ? 0:1, //0:未开始，1：直播中
                    isHost: isHost,
                    isOpenRemind: isOpenRemind,
                    shortUrl: shortUrl
                ]
            }
        }
        log.info("获取直播预告列表的总时长===>{}", System.currentTimeMillis()-startTime)
        return resultList
    }

    @Override
    def findNotStartedLiveForeshowList(Map map) {
        String appId = map.appId
        long startTime = System.currentTimeMillis()
        //查询预告
        def preList = liveRes.getNotStartedLiveForeshowList(map)
        def resultList = []
        preList?.each {
               long beginTime =  DateUtil.getTimestamp(it.begin_time ? it.begin_time as String : "")
                int isHost = 0
                if(map.userId ==it.user_id){
                  if(!it.live_mode|| !it.pgc_type || (it.live_mode==2 && it.pgc_type==1 )||it.live_mode==1){
                      isHost=1;
                  }
                }
                int invokeType=it.url_tag as int
                // 会议直播预告和付费会议直播预告,在非souyue中除H5以外统一跳转到房间
                if(!Strings.APP_NAME_SOUYUE.equals(appId) && invokeType != 120 && invokeType != 85){
                    invokeType = LiveCommon.INVOKE_TYPE_LIVE_FORESHOW_START_AND_PAUSE
                }
                long blogId=0
                String category = ""
                if(invokeType==20||invokeType==24){
                    blogId=it.url as long
                    category = "interest"
                }else if(invokeType == 10){
                    category = "srp"
                }
                int isOpenRemind=it.appoint_id ? 1:0;
                int watchCount=0
                long liveId = it.live_id ?: 0L ;

                def liveStatus = 0
                if (VerUtils.toIntVer(map.vc) >= VerUtils.toIntVer("5.6")){
                    liveStatus = beginTime > System.currentTimeMillis()+3600_000 ? LiveCommon.FORESHOW_STATUS_0 : beginTime > System.currentTimeMillis() ? LiveCommon.FORESHOW_STATUS_3: LiveCommon.FORESHOW_STATUS_1 //0:未开始，1：直播中
                }else {
                    liveStatus = (beginTime > System.currentTimeMillis()) ? LiveCommon.PGC_STATUS_0:LiveCommon.PGC_STATUS_1
                }
                resultList << [
                    invokeType: invokeType,
                    viewType: LiveCommon.VIEW_TYPE_LIVE_FORESHOW,
                    foreshowId: it.foreshow_id as long,
                    category: category,
                    watchCount: 0,
                    liveThumb: it.img_url,
                    title: it.title,
                    keyword: it.keyword,
                    srpId: it.srp_id,
                    url: it.url,
                    blogId: blogId,
                    beginTime: beginTime,
                    liveStatus: liveStatus,//0:未开始，1：直播中
                    isHost: isHost,
                    isOpenRemind: isOpenRemind,
                    shortUrl: "",
                    foreshowType:it.foreshow_type,
                    liveId:liveId,
                    showTitle:it.show_title_in_list,
                    chargeType:it.foreshow_type==3 ? 1 :2,
                    serverCurrentTime:System.currentTimeMillis()
                ]

        }
        log.info("获取未开始的直播预告列表的总时长===>{}", System.currentTimeMillis()-startTime)
        return resultList
    }

    @Override
    LiveRecord getLiveByForeshow( long foreshowId, String appId){
        def live = liveRes.findLiveByForeshowId(foreshowId,appId)
        LiveRecord liveRecord = toLiveRecord(live)
        return liveRecord
    }
    /**
     *
     * @param videoAddress
     * @param mapData [liveId,roomId,foreshowId,appId]
     * @return
     */
    @Override
    def getVideoAddressUrlList(String videoAddress,Map mapData){
        List address = videoAddress ? Strings.parseJson(videoAddress) : []
        String addrString;
        //如果有flv格式的回看 重新去腾讯云取一遍看是否有mp4格式的回看
        if(address &&  mapData.size()>0){
            for(def it: address){
                List playSets = it.playSet
                if(playSets?.size()>0 ){
                    for(def palySet: playSets) {
                        String url = (palySet?.url ?: "") as String
                        if(url.indexOf("?") >= 0 ){//由于url防盗链改的是底层方法，为防止合并后的地址被加密固增加改方法
                            url = url.substring(0,url.indexOf("?"))
                        }
                        if (url.endsWith("flv")) {
                            int roomId = mapData.roomId ? mapData.roomId as int : 0;
                            long liveId = mapData.liveId ? mapData.liveId as long : 0
                            long foreshowId = mapData.foreshowId ? mapData.foreshowId as long : 0
                            if(roomId !=0 && liveId != 0){
                                addrString = qcloudLiveService.getBackInfo( roomId,liveId,foreshowId,mapData.appId,"回看列表点击调用");
                                if(addrString){
                                    break;
                                }
                            }
                        }
                    }
                    if(addrString){
                        break;
                    }
                }
            }
        }
        List urlList = []
        List fileIds = []
        int timeSpanAll = 0
        List addressSenc = []
        if(addrString){
            addressSenc = addrString ? Strings.parseJson(addrString) : []
        }else{
            addressSenc = videoAddress ? Strings.parseJson(videoAddress) : []
        }
        if(addressSenc){
//            sortByFileName(addressSenc,"fileId") //不能用fileId排序，会造成多段直播的乱序问题
            addressSenc?.each {
                int timeSpan = (it.duration ?: 0) as int
                String fileId = it.fileId ?: ""
                List playSets = it.playSet
                Map mapUrl = [:]
                String urlStr = ""
                playSets?.each {
                    String url = (it.url ?: "") as String
                    if(url.indexOf("?") >= 0 ){//由于url防盗链改的是底层方法，为防止合并后的地址被加密固增加改方法
                        url = url.substring(0,url.indexOf("?"))
                    }
                    if(url.endsWith("flv"))
                        mapUrl.flvUrl = url
                    else if(url.endsWith(".f10.mp4"))
                        mapUrl.f10Mp4Url = url
                    else if(url.endsWith(".f20.mp4"))
                        mapUrl.f20Mp4Url = url
                    else if(url.endsWith("f210.av.m3u8"))
                        mapUrl.f210M3u8Url = url
                    else if(url.endsWith("f220.av.m3u8"))
                        mapUrl.f220M3u8Url = url
                    else if(url.endsWith("f0.mp4"))
                        mapUrl.f0Mp4Url = url
                    else if(url.endsWith(".mp4"))
                        mapUrl.mp4Url = url
                    else if(url.endsWith(".m3u8"))
                        mapUrl.m3U8=url
                }
                urlStr = getVideoUrlByUrlStyle(mapData,mapUrl)
                if(urlStr){
                    urlList << [url: urlStr, timeSpan: timeSpan]
                    timeSpanAll = timeSpanAll+timeSpan
                }
                fileIds << fileId
            }
        }
        urlList?.eachWithIndex {it,i ->
            if(VerUtils.toIntVer(mapData.vc) >= VerUtils.toIntVer("5.7.0")){
                //url防盗链，多段直播防止暂停后播放不了，固此处第一段是取默认失效时间（30秒），之后每小段的失效时间是整个视频的时长
                if(i == 0){
                    it.url = urlSignedUtil.getSignedUrl(it.url,0,mapData.appId,mapData.vc)
                }else{
                    it.url = urlSignedUtil.getSignedUrl(it.url,timeSpanAll*2,mapData.appId,mapData.vc)
                }
            }else{
                it.url = urlSignedUtil.getSignedUrl(it.url,0,mapData.appId,mapData.vc)
            }
        }
        return [url: urlList, fileId: fileIds,timeSpan: timeSpanAll]
    }

    //回看的播放地址按照fileId升序排列
    void sortByFileName(List<Map<String, String>> data, String fileName, int flag = 0) {
        Collections.sort(data, new Comparator<Map>() {
            public int compare(Map o1, Map o2) {
                String a = (String) o1.get(fileName);
                String b = (String) o2.get(fileName);
                if(flag > 0){// 降序
                    return b.compareTo(a);
                }else {// 升序
                    return a.compareTo(b);
                }
            }
        });
    }
    def getVideoUrlByUrlStyle(Map mapData,Map urlMap){
        int clarity	= (mapData.clarity ?: 2) as int		//1: 普通，2：高清
        String fileType = mapData.fileType ?: ""   // "flv","mp4","m3u8"
        String url = ""
        switch (fileType) {
            case "flv":
                url =  urlMap.flvUrl
                break;
            case "mp4":
                if(clarity == 1){
                    url =  urlMap.f10Mp4Url ?: urlMap.mp4Url
                }else {
                    url =  urlMap.f20Mp4Url ?: urlMap.mp4Url
                }
                break;
            case "m3u8":
                if(clarity == 1){
                    url =  urlMap.f210M3u8Url ?: urlMap.m3U8
                }else {
                    url =  urlMap.f220M3u8Url ?: urlMap.m3U8
                }
                break;
            default:
                url =  (urlMap.f220M3u8Url ?: urlMap.f210M3u8Url)?:urlMap.m3U8
                if(!url){
                    url = (((urlMap.f0Mp4Url ?: urlMap.f20Mp4Url) ?: urlMap.f10Mp4Url) ?: urlMap.mp4Url) ?: urlMap.flvUrl
                }
        }
        return url
    }
    @Override
    LiveRecord toLiveRecord(def obj){
        LiveRecord liveRecord = null
        try{
            liveRecord = obj ? new LiveRecord(
                liveId: (obj.containsKey("live_id") && obj.live_id?obj.live_id: 0) as long,
                foreshowId: (obj.containsKey("foreshow_id") && obj.foreshow_id?obj.foreshow_id: 0) as long,
                title: obj.containsKey("title") && obj.title ?obj.title: "",
                srpId: obj.containsKey("srp_id") && obj.srp_id ?obj.srp_id: "",
                keyword: obj.containsKey("keyword") && obj.keyword ?obj.keyword: "",
                m3u8Url: obj.containsKey("m3u8_url") && obj.m3u8_url ?obj.m3u8_url: "",
                appId: obj.containsKey("app_id") && obj.app_id?obj.app_id: "",
                appModel: obj.containsKey("app_model") && obj.app_model ?obj.app_model: "",
                channelId: obj.containsKey("channel_id") && obj.channel_id ?obj.channel_id: "",
                taskId: obj.containsKey("task_id") && obj.task_id?obj.task_id: "",
                liveThump: obj.containsKey("live_thump") && obj.live_thump ?obj.live_thump: "",
                liveBg: obj.containsKey("live_bg") && obj.live_bg?obj.live_bg: "",
                userId: (obj.containsKey("user_id") && obj.user_id ?obj.user_id: 0) as long,
                userImage: obj.containsKey("user_image") && obj.user_image ?obj.user_image: "",
                nickname: obj.containsKey("nickname") && obj.nickname ?obj.nickname: "",
                roomId: (obj.containsKey("room_id") && obj.room_id ?obj.room_id: 0) as long ,
                chatId: (obj.containsKey("chat_id") && obj.chat_id ?obj.chat_id: 0) as long,
                admireCount: (obj.containsKey("admire_count") && obj.admire_count?obj.admire_count: 0) as long,
                watchCount: (obj.containsKey("watch_count") && obj.watch_count ?obj.watch_count: 0) as long,
                totalWatchCount: (obj.containsKey("total_watch_count") && obj.total_watch_count ?obj.total_watch_count: 0) as long,
                vestCount: (obj.containsKey("vest_count") && obj.vest_count ?obj.vest_count: 0) as long,
                timeSpan: (obj.containsKey("time_span") && obj.time_span ?obj.time_span: 0) as long,
                createTime: obj.containsKey("create_time") && obj.create_time ?String.valueOf(obj.create_time): "",
                updateTime: obj.containsKey("update_time") && obj.update_time ?String.valueOf(obj.update_time): "",
                pushTime: obj.containsKey("push_time") && obj.push_time ?String.valueOf(obj.push_time): "",
               liveStatus: (obj.containsKey("live_status") && obj.live_status ?obj.live_status: 0) as int,
                videoAddress: obj.containsKey("video_address") && obj.video_address?obj.video_address: "",
                isPrivate: (obj.containsKey("is_private") && obj.is_private ?obj.is_private: 0) as int,
                newliveSortNum: (obj.containsKey("newlive_sort_num") && obj.newlive_sort_num ?obj.newlive_sort_num: 0) as int,
                backliveSortNum: (obj.containsKey("backlive_sort_num") && obj.backlive_sort_num ?obj.backlive_sort_num: 0) as int,
                liveType: (obj.containsKey("live_type") && obj.live_type ?obj.live_type: 0) as int,
                vc: obj.containsKey("vc") && obj.vc ? obj.vc : "",
                fileId: obj.containsKey("file_id") && obj.file_id ? obj.file_id : "",
                playUrl:obj.containsKey("play_url") && obj.play_url ? obj.play_url : "",
                compereUserId:obj.containsKey("compere_user_id") && obj.compere_user_id ? obj.compere_user_id : "",
                fieldControlId:obj.containsKey("field_control_user_id") && obj.field_control_user_id ? obj.field_control_user_id : "",
                beginTime:obj.containsKey("begin_time") && obj.begin_time ? obj.begin_time : "",
                rtmpUrl: obj.containsKey("rtmp_url") && obj.rtmp_url ? obj.rtmp_url : "",
                pgcStatus: obj.containsKey("pgc_status")&&obj.pgc_status ?obj.pgc_status : 1,
                pgcType:obj.containsKey("pgc_type")&&obj.pgc_type ?obj.pgc_type : 1,
                brief: obj.containsKey("brief") && obj.brief ? obj.brief : "",
                liveMode:obj.containsKey("live_mode")&&obj.live_mode ?obj.live_mode : 1,
                xiaoYuLiveId:obj.containsKey("xiaoyu_live_id") && obj.xiaoyu_live_id ? obj.xiaoyu_live_id : "",
                xiaoYuId: obj.containsKey("xiaoyu_id") && obj.xiaoyu_id ? obj.xiaoyu_id : "",
                xiaoYuConfNo:obj.containsKey("xiaoyu_conf_no") && obj.xiaoyu_conf_no? obj.xiaoyu_conf_no  : "",
                viewJson: obj.containsKey("view_json") && obj.view_json? Strings.parseJson(obj.view_json ) : [:],
                briefHtml: obj.containsKey("brief_html") && obj.brief_html? obj.brief_html :"",
                liveFromSdk: (obj.containsKey("live_from_sdk") && obj.live_from_sdk? obj.live_from_sdk : 0) as int,
                liveRecommend:obj.containsKey("live_recommend")&&obj.live_recommend ?obj.live_recommend : 0,
                recommendTime:obj.containsKey("recommend_time") && obj.recommend_time ? obj.recommend_time : "",
                isShowLiveHomepage:obj.containsKey("is_show_live_homepage") && obj.is_show_live_homepage ? obj.is_show_live_homepage:0
            ) : null
        } catch (Exception e) {
            log.error("toLiveRecord,error:{}", e.getMessage())
            e.printStackTrace()
        }
        return liveRecord
    }

    @Override
    Map getUserInfo(Long userId){
        Map userInfo = [:]
        try {
            if(userId > 1000000000 ){
                userInfo.userName = "游客";
                userInfo.nickname = "游客";
                userInfo.userImage = "";
                userInfo.userId = userId
                userInfo.signature = "";
            }else{
                String res = Http.post(userInfoUrl,["userId":userId])
                def resJson = Strings.parseJson(res)
                userInfo.userName = resJson?.body.userName ?: "";
                userInfo.nickname = resJson?.body.nickName ?: "游客";
                userInfo.userImage = ImageUtils.getSmallImg(resJson?.body.imageUrl ?: "");
                userInfo.userId = userId
                userInfo.signature = resJson?.body.signature ?: "";
                userInfo.mobile = resJson?.body.mobile ?: ""
            }
        }catch (Exception e){}
        return userInfo
    }

    @Override
    int validateFans(Map map){
        int res = 1
        long userId = (map.userId ?:0) as long
        long toUserId = (map.toUserId ?:0) as long
        long liveId = (map.liveId ?:0) as long
        if(!this.getUserInfo(userId)){
            return 2 //打赏人不存在
        }
        if(!this.getUserInfo(toUserId)){
            return 3 //被打赏人不存在
        }
        LiveRecord liveRecord = this.findLiveByLiveId(map.liveId)
        if(!liveRecord){
            liveRecord = this.findLiveRecordByLiveId(map.liveId)
            if(!liveRecord){
                return 4 //直播不存在
            }
        }
        if(toUserId != liveRecord.userId){
            return 5 //主播不在该直播间
        }
        if(!liveQcloudRedis.validateLiveWather(liveId,userId)){
            boolean flag = validateMemberInGroup(userId,liveRecord.roomId,liveRecord.appId)
            log.info("gift payment validateMemberInGroup userId:{},roomId:{},flag:{}",userId,toUserId,flag)
            if(!flag){
                return 6 //打赏人不在该直播间
            }
        }
        return res
    }
    boolean validateMemberInGroup(long userId, int roomId, String appId){
        int i = 0
        boolean flag = false
        try {
            while (true) {
                int offset = i * 100
                i++
                String result = qcloudLiveService.getGroupMemberInfo([roomId: roomId, offset: offset, appId: appId])
                def json = Strings.parseJson(result)
                if ("OK".equals(json.ActionStatus)) {
                    if ((json.MemberNum as int) > 0) {
                        List memberList = json.MemberList
                        for(def member : memberList) {
                            if (userId == (member.Member_Account as long)) {
                                flag = true
                                break
                            }
                        }
                        if (flag) {
                            break
                        }
                    }
                    if ((json.MemberNum as int) < 100) {
                        break;
                    }
                } else
                    break;
            }
        }catch(Exception e){
            e.printStackTrace()
        }
        return flag
    }
    @Override
    int sortLiveRecord(Map map){
        int res = 0
        long liveId = (map.liveId ?:0) as long
        int sortNum = (map.sortNum ?:0) as int
        try {
            res = liveRes.updateLiveRecordSortNum(liveId,sortNum,map.appId)
        }catch (Exception e){
            e.printStackTrace()
        }
        return res
    }

    @Override
    int sortForeshow(Map map){
        int res = 0
        long foreshowId = (map.foreshowId ?:0) as long
        int sortNum = (map.sortNum ?:0) as int
        try {
            res = liveRes.updateForeshowSortNum(foreshowId,sortNum,map.appId)
        }catch (Exception e){
            e.printStackTrace()
        }
        return res
    }

    @Override
    int sortForeshowChild(Map map){
        int res = 0
        long foreshowId = (map.foreshowId ?:0) as long
        int sortNum = (map.sortNum ?:0) as int
        try {
            //res = liveRes.updateForeshowSortNum(foreshowId,sortNum,map.appId)
            res = liveRes.updateForeshowChildSortNum(foreshowId,sortNum,map.appId)
        }catch (Exception e){
            e.printStackTrace()
        }
        return res
    }

    @Override
    int addAppointment(Map map){
        int res = 0
        long foreshowId = (map.foreshowId ?:0) as long
        long userId = (map.userId ?:0) as long
        try {
            res = liveRes.addAppointment(foreshowId,userId,map.appId)
        }catch (Exception e){
            e.printStackTrace()
        }
        Parallel.run([1],{
            //通知王龙直播统计 TODO 暂时只是与王龙联调，后期会删掉，统一由分析nginx日志的项目处理
            long createTime = System.currentTimeMillis()
            qcloudLiveService.sendLiveDataToStatistic([jsonType:LiveCommon.STATISTIC_JSON_TYPE_4,userId:userId,
                                                       foreshowId:foreshowId,operType:"add",createTime:createTime])
        },1)
        return res
    }

    @Override
    int cancelAppointment(Map map){
        int res = 0
        long foreshowId = (map.foreshowId ?:0) as long
        long userId = (map.userId ?:0) as long
        try {
            res = liveRes.delAppointment(foreshowId,userId,map.appId)
        }catch (Exception e){
            e.printStackTrace()
        }
        Parallel.run([1],{
            //通知王龙直播统计 TODO 暂时只是与王龙联调，后期会删掉，统一由分析nginx日志的项目处理
            long createTime = System.currentTimeMillis()
            qcloudLiveService.sendLiveDataToStatistic([jsonType:LiveCommon.STATISTIC_JSON_TYPE_4,userId:userId,
                                                       foreshowId:foreshowId,operType:"cancel",createTime:createTime])
        },1)
        return res
    }

    @Override
    LiveRecord findLiveRecordByLiveId(long liveId){
        LiveRecord liveRecord = null
        try {
            def result = liveRes.findLiveRecordByLiveId(liveId)
            liveRecord = this.toLiveRecord(result)
        }catch (Exception e){
            e.printStackTrace()
        }
        return liveRecord
    }

    @Override
    LiveRecord findLiveRecordByForeshowId(long foreshowId) {
        LiveRecord liveRecord = null
        try {
            def result = liveRes.findLiveRecordByForeshowId(foreshowId)
            //System.out.println("===========result:"+result)
            liveRecord = this.toLiveRecord(result)
        }catch (Exception e){
            e.printStackTrace()
        }
        return liveRecord
    }

    @Override
    public int delLiveRecord(Map map) {
        def res = 1;
        int op_role = (map.op_role ?: 0) as int
        Long liveId = (map.liveId ?: 0) as long
        Long foreshowId = (map.foreshowId ?: 0) as long
        Long userId = (map.userId ?: 0) as long


        //删除直播历史
        int liveStatus = LiveCommon.LIVERECORD_STATUS_3 //用户删除
        if(op_role == 100){
            liveStatus = LiveCommon.LIVERECORD_STATUS_4  //彻底删除，包括腾讯云删除
        }
        if(liveId && !foreshowId){//客户端删除不带预告的回放+php后台彻底删除单个回放
            LiveRecord liveRecord =this.findLiveRecordByLiveId(liveId)
            if(!liveRecord){
                return res
            }
            if(op_role != 100){//超级管理员没有用户校验
                Long hostUid = (liveRecord.userId ?: 0 ) as long
                //校验用户
                if(userId != hostUid){
                    throw new ApiException(700, "操作用户非法");
                }
            }
            //增加判断如果会议直播时需要先删除预告再删除直播 add kpc 2016.12.26
            if(liveRecord.liveMode == 2 || liveRecord.liveMode == 3){
                if(liveRecord.foreshowId != 0){ //如果有预告则不让删除
                    throw new ApiException(700, "删除会议直播需要先删除对应的预告！");
                }
            }
            res = liveRes.delLiveRecordByLiveId(liveId,liveStatus,map.appId)
            //if(op_role == 100 && res ==1) 由于删除的定时任务调用腾讯云接口失败，固重新跑数据是他tus=4的记录
            if(op_role == 100){//管理员成功删除数据库记录时，需要同步删除腾讯云直播数据
                //异步删除腾讯云,参数fileId，格式list
                Parallel.run([1], {
                    List fileIdList = getVideoAddressUrlList(liveRecord.videoAddress, [:])?.fileId?:[]
                    if(fileIdList){
                        qcloudLiveService.deleteVodFile(fileIdList,map.appId)
                        log.info("liaojing delLiveRecord qcloud,liveInfo=>{}",map)
                    }
                }, 1)
            }
        }else if(!liveId && foreshowId && op_role!=100){//客户端删除带预告的回放
            def liveForeshow = liveRes.findLiveForeshowByForeshowId(foreshowId,map.appId)
            if(!liveForeshow){
                return res
            }
            if(op_role != 100){//超级管理员没有用户校验
                Long hostUid = (liveForeshow.user_id ?: 0 ) as long
                //校验用户
                if(userId != hostUid){
                    throw new ApiException(700, "操作用户非法");
                }
            }
            int foreshowType = (liveForeshow.foreshow_type ?: 0 ) as int
            //增加判断如果会议直播时需要先删除预告再删除直播 add kpc 2016.12.26
            if(foreshowType == 2){
                throw new ApiException(700, "删除会议直播需要先删除对应的预告！");
            }
            res = liveRes.delLiveRecordByForeshowId(foreshowId,liveStatus,map.appId)
        }else if(foreshowId){//删除预告回放
            if(op_role == 100){//管理员成功删除数据库记录时，需要同步删除腾讯云直播数据
                //删除预告合并后的腾讯云文件
                delForeshowQcloud(foreshowId,map.appId)
            }
            //修改预告表状态
            res = liveRes.delForeshow(foreshowId,map.appId)
            //更新直播表、直播回放表预告id及官方、非官方状态
            liveRes.updateLiveForeshowId(foreshowId,map.appId);
            //删除预告对应的收藏
            liveRes.delCollectionByForeshowId(foreshowId)
        }else{
            res = 0
            log.info("liaojing delLiveRecord mysql fail,参数有误=>{}",map)
        }
        log.info("liaojing delLiveRecord success,liveInfo=>{},res=>{}",map,res)
        return 1;
    }

    @Override
    int delForeshow(Map map){
        int res = 0
        long foreshowId = (map.foreshowId ?:0) as long
        try {
            //结束直播
            def live = liveRes.findLiveRecordByForeId(foreshowId,map.appId)
            if(live){
                if(live.live_mode as int==LiveCommon.FORESHOW_TYPE_1){
                     qcloudLiveService.stopLiveComm(live.live_id as long, live.room_id as int, live.user_id as long, live.app_id,"结束预告")
                }
            }
            //删除预告合并后的腾讯云文件
            delForeshowQcloud(foreshowId,map.appId)
            //删除预约表记录
            res = liveRes.delForeshow(foreshowId,map.appId)
            //更新直播表、直播回放表预告id及官方、非官方状态
            if(res == 1){
                liveRes.updateLiveForeshowId(foreshowId,map.appId);
                //删除直播收藏表中的记录
                liveRes.delCollectionByForeshowId(foreshowId)
            }
        }catch (Exception e){
            e.printStackTrace()
        }
        return res
    }

    @Override
    def getLiveRecordInfo(Map map) {
        long foreshowId = (map.foreshowId ?: 0) as long
        long liveId = (map.liveId ?: 0) as long
        def host = null
        def urlList = []
        int timeSpan = 0
        String liveBg = ""
        int liveStatus = 1
        String fileIds = "";
        long totalWatchCount = 0
        String shortUrl = ""
        def splitTime = []
        long statistic_liveId = 0
        long statistic_roomId = 0
        String vc = map.vc
        if(foreshowId){
            def liveForeshow = liveRes.findLiveForeshowByForeshowId(foreshowId as long,map.appId as String)
            def liveRecordInfo =  Strings.parseJson(liveForeshow.live_record_info)
            def videoAddress = liveRecordInfo ? liveRecordInfo.video_address : ""
            boolean flag =  videoAddress ? true: false
            if(flag){
                //获取时间时，如果timeSpan为0，从回放表中获取视频回放信息
                timeSpan = liveRecordInfo.time_span as int
                if(timeSpan == 0){
                    flag = false
                }else {
                    Map videoAddressMap = getVideoAddressUrlList(videoAddress, map)
                    urlList.addAll(videoAddressMap.url)
                    if(liveForeshow.foreshow_type as int != LiveCommon.FORESHOW_TYPE_1){
                        def liveRecordLog = liveRes.findLiveRecordByForeshowId(foreshowId)
                        liveId = (liveRecordLog.live_id ?: 0) as long
                        totalWatchCount = (liveQcloudRedis.getLiveWatherTotalCount(liveId) ?: 0)as long
                    }else
                        totalWatchCount = (liveRecordInfo.total_watch_count ?:0)as long
                    host = [userId: liveForeshow.user_id as long,userImage: liveForeshow.user_image,nickname: liveForeshow.nickname]
                }
            }
            List liveRecordList = liveRes.findLiveRecordListByForeId(map)
            if(!liveRecordList) {
                liveStatus = 3
            }else {

                int statistic_timespan = 0
                liveRecordList?.each {
                    //add by kpc 增加参数
                    Map dataMap = [
                        liveId: it.live_id,
                        roomId: it.room_id,
                        foreshowId: it.foreshow_id,
                        appId: it.app_id,
                        vc   : vc
                    ]
                    Map videoAddressMap = getVideoAddressUrlList(it.video_address as String, dataMap)
                    int it_timeSpan = (videoAddressMap ?videoAddressMap.timeSpan : 0) as int
                    if(it_timeSpan > statistic_timespan){//直播统计使用 add by liaojing 20170307
                        statistic_timespan = it_timeSpan
                        statistic_liveId = it.live_id
                        statistic_roomId = it.room_id
                    }
                    splitTime.add([timeSpan: it_timeSpan, beginTime: DateUtil.getTimestamp(it.push_time ? it.push_time as String : "")])
                    if(!flag){
                        if(!host){
                            host = [userId: it.user_id as long,userImage: it.user_image,nickname: it.nickname]
                        }
                        urlList.addAll(videoAddressMap.url)
                        timeSpan += it_timeSpan
                        totalWatchCount += (it.total_watch_count as long)
                        if(!liveBg){
                            liveBg = it.video_address ? Strings.parseJson(it.video_address)?.get(0)?.image_url : ""
                        }
                    }
                    if(liveForeshow.foreshow_type as int == LiveCommon.FORESHOW_TYPE_3){
                        shortUrl = shortURLService.getShortUrlLivePay([foreshowId: foreshowId,liveId:it.live_id,userId: map.userId,vc: map.vc,appId: liveForeshow.app_id])
                    }else if(liveForeshow.foreshow_type as int == LiveCommon.FORESHOW_TYPE_2){
                        shortUrl = shortURLService.getShortUrlPgcForeshow([foreshowId: foreshowId,liveId:it.live_id,userId: map.userId,vc: map.vc,appId: liveForeshow.app_id])
                    }else {
                        shortUrl = shortURLService.getShortUrlForeshow([foreshowId: foreshowId,liveMode: liveForeshow.foreshow_type, userId: host.userId as long, vc: map.vc,appId: liveForeshow.app_id])
                    }
                }
            }
        }else if(liveId){
            def liveRecord = findLiveRecordByLiveId(liveId)
            if(liveRecord){
                liveStatus = liveRecord.liveStatus
                host = [userId: liveRecord.userId,userImage: liveRecord.userImage,nickname: liveRecord.nickname]
                //add by kpc 增加参数
                Map dataMap = [
                    liveId: liveRecord.liveId,
                    roomId: liveRecord.roomId,
                    foreshowId: liveRecord.foreshowId,
                    appId: liveRecord.appId,
                    vc   : vc
                ]
                Map videoAddressMap = getVideoAddressUrlList(liveRecord.videoAddress, dataMap)
                splitTime.add([timeSpan: videoAddressMap.timeSpan as int, beginTime: DateUtil.getTimestamp(liveRecord.pushTime)])
                timeSpan = videoAddressMap.timeSpan
                urlList = videoAddressMap.url
                fileIds = videoAddressMap.fileId
                totalWatchCount = liveRecord.totalWatchCount
                String liveAddress = liveRecord.videoAddress
                def liveAddressList = [];
                if(liveAddress){
                    liveAddressList = Strings.parseJson(liveRecord.videoAddress)
                }
                if(liveAddressList.size() >0){
                    liveBg = liveAddressList.get(0)?.image_url
                }
                shortUrl = shortURLService.getShortUrlLive([liveId:liveRecord.liveId,liveMode: liveRecord.liveMode,userId: liveRecord.userId,roomId: liveRecord.roomId, vc: liveRecord.vc,appId: liveRecord.appId])
                //直播统计使用 add by liaojing 20170307
                statistic_liveId = liveRecord.liveId
                statistic_roomId = liveRecord.roomId
            }
        }
        Parallel.run([1],{
            //通知王龙直播统计add by liaojing 20170307 TODO 暂时只是与王龙联调，后期会删掉，统一由分析nginx日志的项目处理
            Map params = [
                liveId:statistic_liveId,
                roomId:statistic_roomId,
                createTime:System.currentTimeMillis(),
                userId:(map ?.userId?: "0") as String,
                userAction:QcloudCommon.AVIMCMD_EnterLive,
                msgType:QcloudCommon.CALLBACK_AFTERNEWMEMBER_JOIN,
                jsonType:LiveCommon.STATISTIC_JSON_TYPE_2
            ]
            qcloudLiveService.sendLiveDataToStatistic(params)
        },1)
        return urlList && liveStatus == 1 ? [
            liveStatus: liveStatus,
            foreshowId: foreshowId,
            liveId: liveId ?:"",//客户端是String类型，判断为空时有问题，0不为空
            liveRecordUrl: urlList,
            timeSpan: timeSpan as int,
            watchCount: totalWatchCount,
            liveBg: liveBg,
            anchorInfo: [
                nickname: host?.nickname ?: "",
                userImage: host?.userImage ?: "",
                userId: (host?.userId ?: 0)as long
            ],
            fileIds:  fileIds,
            shortUrl: shortUrl,
            splitTime: splitTime
        ] : [liveStatus: 3]
    }

    @Override
    int validateIsOpenRemind(long userId, long foreshowId, String appId) {
        boolean result = liveRes.isOpenRemind(appId,userId,foreshowId)
        return result ? 1: 0
    }

    @Override
    def getLiveForeshowSkipInfo(long foreshowId, long userId, String appId,String vc) {
        int liveStatus = 0
        def result = [:]
        def liveForeshow = liveRes.findLiveForeshowByForeshowId(foreshowId,appId)
        String shortUrl = ""
        if(liveForeshow){
            int foreshowStatus = (liveForeshow.foreshow_status ?: 0) as int
            long beginTime = DateUtil.getTimestamp(liveForeshow.begin_time as String)
            if (foreshowStatus == 3){
                liveStatus = 5   //预告已删除
            }else {
                if (beginTime > System.currentTimeMillis()){
                    liveStatus = 0  //预告中
                }else {
                    if(foreshowStatus >= 2){    //预告结束
                        def liveRecordInfo = Strings.parseJson(liveForeshow.live_record_info as String)
                        int totalWatchCount = (liveRecordInfo?.total_watch_count ?: 0) as int
                        def host = [:]
                        if (foreshowStatus ==4 || foreshowStatus == 5){
                            liveStatus = 3  //预告已结束，但无回放
                            List list = liveRes.findLiveRecordListByForeId([foreshowId: foreshowId,appId: appId])
                            long liveId = list ? ((Strings.parseJson(list.last())?.live_id  ?: 0)as long):0
                            def isFollow = isFollow(userId, liveForeshow.user_id as long, 1, appId)  //与粉丝关注情况
                            host = this.getUserInfo(liveForeshow.user_id as long)
                            result << [
                                liveId: liveId,
                                watchCount: totalWatchCount,
                                isHaveBack: 0,
                                isFollow: isFollow,
                                anchorInfo: [
                                    nickname: host?.nickname ?: "",
                                    userImage: host?.userImage ?: "",
                                    userId: (host?.userId ?: 0)as long
                                ]
                            ]
                        }else if (foreshowStatus == 2){
                            liveStatus = 4  //预告已结束，且有回放
                            List list = liveRes.findLiveRecordListByForeId([foreshowId: foreshowId,appId: appId])
                            int timeSpan = 0
                            List urlList = []
                            String liveBg = ""
                            list.each{
                                LiveRecord liveRecord = toLiveRecord(it)
                                //add by kpc 增加参数
                                Map dataMap = [
                                    liveId: liveRecord.liveId,
                                    roomId: liveRecord.roomId,
                                    foreshowId: liveRecord.foreshowId,
                                    appId: liveRecord.appId,
                                    vc : vc
                                ]
                                Map videoAddressMap = getVideoAddressUrlList(liveRecord.videoAddress,dataMap)
                                urlList.addAll(videoAddressMap.url)
                                timeSpan += videoAddressMap.timeSpan as int
                                if(!host){
                                    host = [userId: liveRecord.userId,userImage: liveRecord.userImage,nickname: liveRecord.nickname]
                                }
                                if(!liveBg){
                                    liveBg = liveRecord.videoAddress ? Strings.parseJson(liveRecord.videoAddress)?.get(0)?.image_url : ""
                                }
                                if(liveForeshow.foreshow_type as int == LiveCommon.FORESHOW_TYPE_3){
                                    shortUrl = shortURLService.getShortUrlLivePay([foreshowId: foreshowId,liveId: liveRecord.liveId,userId: userId, vc: vc,appId: liveForeshow.app_id])
                                }else if(liveForeshow.foreshow_type as int == LiveCommon.getLIVE_MODE_2()){
                                    shortUrl = shortURLService.getShortUrlPgcForeshow([foreshowId: foreshowId,liveId: liveRecord.liveId,userId: userId, vc: vc,appId: liveForeshow.app_id])
                                }else {
                                    shortUrl = shortURLService.getShortUrlForeshow([foreshowId: foreshowId,liveMode: liveForeshow.foreshow_type,userId: (host?.userId ?: 0)as long, vc: vc,appId: liveForeshow.app_id])
                                }
                            }
                            result <<  [
                                foreshowId: foreshowId,
                                liveRecordUrl: urlList,
                                timeSpan: timeSpan,
                                watchCount: totalWatchCount,
                                liveBg: liveBg,
                                anchorInfo: [
                                    nickname: host?.nickname ?: "",
                                    userImage: host?.userImage ?: "",
                                    userId: (host?.userId ?: 0)as long
                                ],
                                shortUrl: shortUrl
                            ]
                        }
                    }else {  //直播中
                        def live = liveRes.findLiveRecordByForeId(foreshowId,appId)
                        if(live) {
                            liveStatus = 1  //直播中，且有直播
                            result << getLiveReturn(live)
                        }else {
                            liveStatus = 2  //直播中，但无直播
                        }
                    }
                }
            }
        }
        return [liveStatus: liveStatus, liveInfo: result]
    }

    @Override
    def findaAppointmentUserList(long foreshowId, int lastId, String appId, int psize) {
        return liveRes.findAppointmentUserlist(foreshowId,lastId,appId,psize)
    }

    @Override
    int updataLiveForeshowStatus(long foreshowId,String appId) {
        def live = liveRes.findLiveRecordByForeId(foreshowId,appId)
        int status = 1
        if(live){
            //结束直播
            Map statusMap = qcloudLiveService.stopLiveComm(live.live_id as long, live.room_id as int, live.user_id as long, appId,"结束预告")
            status = statusMap?.status as int
        }
        //预告结束时，状态先设置为5：暂时没有回放记录，即没有回放记录时列表中则不显示该官方回放
        status = liveRes.updateLiveForeshowStatus(foreshowId,5,appId)
        Parallel.run([1], {
//            log.info("互动预告结束后调用，foreshowId={}",foreshowId)
//            LiveForeshow liveForeshow=liveForeshowService.get(foreshowId);
//            log.info("互动预告结束后异步调用，foreshowId={},liveForeshow对象={}",foreshowId,Strings.toJson(liveForeshow))
//            if(liveForeshow.parentId){
//                //updateUpdateTime(liveForeshow.parentId,liveForeshow.updateTime);
//                liveForeshowService.updateForeshowGroupBeginTime(liveForeshow.parentId,appId)
//            }
            //异步回写数据库，将预告对应的直播的人数和直播时长放入数据库中
            this.updateForeshowMergeInfo(foreshowId,appId)
        }, 1)
        return status
    }

    @Override
    def findWatchList(long liveId,String lastUserId,int psize){
        def userList = liveQcloudRedis.getLiveWather(liveId,lastUserId,psize)
        userList?.each{
            if(it && it.userImage){
                it.userImage = ImageUtils.getSmallImg(it.userImage)
            }
        }
        return userList
    }

    @Override
    def getLivePraise(long liveId){
        liveQcloudRedis.getLivePraise(liveId)
    }

    def getLiveWatherCount(long liveId){
        vestUserService.getLiveRoomWatchCount(liveId)
    }

    def getLiveWatherTotalCount(long liveId){
        liveQcloudRedis.getLiveWatherTotalCount(liveId)
    }

    def getStopPageInfo(long liveId,long userId,long toUserId,String appId){
        def stopPageInfo = [:]
        long start = System.currentTimeMillis()
        LiveRecord l = findLiveRecordByLiveId(liveId)
        long end1 = System.currentTimeMillis()
        if(l){
            if(StringUtils.isNotBlank(l.videoAddress)){
                stopPageInfo.put("isHaveBack",1)
            }
            //结束页面展示pv
            stopPageInfo.put("watchCount",l.totalWatchCount)
            //与粉丝关注情况
            def isFollow = isFollow(userId, toUserId, 1, appId);
            stopPageInfo.put("isFollow",isFollow)
        }else{
            stopPageInfo =["isHaveBack":0,"watchCount":0,"isFollow":0]
        }
        long end2 = System.currentTimeMillis()
        log.info("liveId={},user={},toUserId={},appId={},直播结束页返回数据：{},findLiveRecordByLiveId time=>{},isFollow time=>{}",liveId,userId,toUserId,appId,Strings.toJson(stopPageInfo),(end1-start),(end2-end1))
        return stopPageInfo
    }

    @Override
    def updateForeshowMergeInfo(long foreshowId,String appId){
        //将预告对应的直播的人数和直播时长放入数据库中
        def liveForeshow = liveRes.findLiveForeshowByForeshowId(foreshowId,appId)
        int foreshowStatus = (liveForeshow?.foreshow_status ?: 0) as int

        if(foreshowStatus == LiveCommon.FORESHOW_STATUS_0  || foreshowStatus == LiveCommon.FORESHOW_STATUS_1){
            log.info("updateForeshowMergeInfo foreshow is not end,foreshowId=>{},foreshowStatus=>{}",foreshowId,foreshowStatus)
            return
        }
        List liveRecordList = liveRes.findLiveRecordListByForeId([foreshowId: foreshowId, appId: appId,all:true])
        def liveRecord
        int timeSpan = 0
        int timeSpanAll = 0
        int watchCountAll = 0
        int watchCountTotalAll = 0
        int vestCountAll = 0
        int watchCount = 0
        int watchCountTotal = 0
        int vestCount = 0
        def urlList = []
        //根据已结束的直播合并观众人数及直播时长
        if(liveRecordList){
            //判断已结束的直播是否已产生了回放，若所有直播均没有回放，则官方回放列表不显示该预告
            boolean hasVideo = false
            liveRecordList?.each {
                timeSpanAll += (it.time_span as int)
                watchCountTotalAll += (it.total_watch_count as int)
                watchCountAll += (it.watch_count as int)
                vestCountAll += (it.vest_count as int)
                if(it.video_address && StringUtils.isNotBlank(it.video_address)){
                    Map dataMap = [liveId: it.live_id,roomId:it.room_id,foreshowId:foreshowId,appId:it.app_id]
                    Map videoAddressMap = getVideoAddressUrlList(it.video_address,dataMap)
                    urlList.addAll(videoAddressMap.url)
                    timeSpan += (it.time_span as int)
                    watchCountTotal += (it.total_watch_count as int)
                    watchCount += (it.watch_count as int)
                    vestCount += (it.vest_count as int)
                    if(!hasVideo){
                        hasVideo = true
                    }
                }
            }
            if(!hasVideo){
            /*当预告所有直播的回放都在生成中时，数据使用都有直播的所有，
            若有回放地址了，则使用有回放地址的直播数据总和，以保持列表和详情中看到的数值保持一致*/
                watchCount = watchCountAll
                watchCountTotal = watchCountTotalAll
                vestCount = vestCountAll
                timeSpan = timeSpanAll
            }
            liveRecord = [watch_count:watchCount,total_watch_count:watchCountTotal,vest_count:vestCount,time_span:timeSpan]
            def liveRecordInfo = Strings.toJson(liveRecord)
            foreshowStatus = LiveCommon.FORESHOW_STATUS_5
            if(hasVideo){
                foreshowStatus = LiveCommon.FORESHOW_STATUS_2
            }
            liveRes.updateLiveForeshowLiveRecordInfo(liveRecordInfo,foreshowStatus,foreshowId,appId)
            log.info("updateForeshowMergeInfo success,foreshowId=>{},status:{}",foreshowId,foreshowStatus)
            long parentId = (liveForeshow?.parent_id ?: 0) as long
            log.info("合并回看 live == >{}, parentId={},foreshowStatus={}",Strings.toJson(liveForeshow),parentId,foreshowStatus)
            if(parentId && foreshowStatus == 2){
                liveForeshowService.updateForeshowGroupBeginTime(parentId,appId)
            }
            //当预告结束的时候，将视频合并
//            try{
//                if(foreshowStatus == 2){
//                    List urls = []
//                    urlList?.each{
//                        if(it?.url){
//                            urls << it.url
//                        }
//                    }
//                    Parallel.run([1], {
//                        if(urls){
//                            log.info("wangtf 预告结束的时候合并视频，foreshowId:{},urls:{}",foreshowId ,urls)
//                            videoMergeService.merge(urls, foreshowId)
//                        }
//                    }, 1)
//                }
//            }catch (Exception e){
//                e.printStackTrace()
//            }

        }else{
            log.info("updateForeshowMergeInfo liveRecordList is null,foreshowId=>{}",foreshowId)
        }
    }

    @Override
    def updatePgcForeshowMergeInfo(long foreshowId,String appId){
        //将预告对应的直播的人数和直播时长放入数据库中
        def liveForeshow = liveRes.findLiveForeshowByForeshowId(foreshowId,appId)
        int foreshowStatus = (liveForeshow?.foreshow_status ?: 0) as int
        if(foreshowStatus == LiveCommon.FORESHOW_STATUS_0  || foreshowStatus == LiveCommon.FORESHOW_STATUS_1){
            log.info("updateForeshowMergeInfo foreshow is not end,foreshowId=>{},foreshowStatus=>{}",foreshowId,foreshowStatus)
            return
        }
        List liveRecordList = liveRes.findLiveRecordListByForeId([foreshowId: foreshowId, appId: appId,all:true])
        def liveRecord
        int timeSpan = 0
        int timeSpanAll = 0
        def urlList = []
        //根据已结束的直播合并观众人数及直播时长
        if(liveRecordList){
            //判断已结束的直播是否已产生了回放，若所有直播均没有回放，则官方回放列表不显示该预告
            boolean hasVideo = false
            liveRecordList?.each {
                timeSpanAll += (it.time_span as int)
                if(it.video_address && StringUtils.isNotBlank(it.video_address)){
                    Map dataMap = [liveId: it.live_id,roomId:it.room_id,foreshowId:foreshowId,appId:it.app_id]
                    Map videoAddressMap = getVideoAddressUrlList(it.video_address,dataMap)
                    urlList.addAll(videoAddressMap.url)
                    timeSpan += (it.time_span as int)
                    if(!hasVideo){
                        hasVideo = true
                    }
                }
            }
            if(!hasVideo){
                /*当预告所有直播的回放都在生成中时，数据使用都有直播的所有，
                若有回放地址了，则使用有回放地址的直播数据总和，以保持列表和详情中看到的数值保持一致*/
                timeSpan = timeSpanAll
            }
            liveRecord = [time_span:timeSpan]
            def liveRecordInfo = Strings.toJson(liveRecord)
            foreshowStatus = LiveCommon.FORESHOW_STATUS_5
            if(hasVideo){
                foreshowStatus = LiveCommon.FORESHOW_STATUS_2
            }
            liveRes.updateLiveForeshowLiveRecordInfo(liveRecordInfo,foreshowStatus,foreshowId,appId)
            log.info("updateForeshowMergeInfo success,foreshowId=>{},status:{}",foreshowId,foreshowStatus)
            long parentId = (liveForeshow?.parent_id ?: 0) as long
            log.info("合并回看 live == >{}, parentId={},foreshowStatus={}",Strings.toJson(liveForeshow),parentId,foreshowStatus)
            if(parentId && foreshowStatus == 2){
                liveForeshowService.updateForeshowGroupBeginTime(parentId,appId)
            }
//            //当预告结束的时候，将视频合并
//            try{
//                if(foreshowStatus == 2){
//                    List urls = []
//                    urlList?.each{
//                        if(it?.url){
//                            urls << it.url
//                        }
//                    }
//                    Parallel.run([1], {
//                        if(urls){
//                            log.info("wangtf 预告结束的时候合并视频，foreshowId:{},urls:{}",foreshowId ,urls)
//                            videoMergeService.merge(urls, foreshowId)
//                        }
//                    }, 1)
//                }
//            }catch (Exception e){
//                e.printStackTrace()
//            }
        }else{
            log.info("updateForeshowMergeInfo liveRecordList is null,foreshowId=>{}",foreshowId)
        }
    }

    @Override
    def updateForeshowStatus(long foreshowId, int foreshowStatus){
        liveRes.updateForeshowStatus(foreshowId,foreshowStatus)
    }

    @Override
    def updateForeshowMergeVideoAddressInfo(long foreshowId, String url,int timeSpan,String appId){
        int res = 0
        if(timeSpan == 0){
            return res
        }
        if(foreshowId && url ){
            def liveForeshow = liveRes.findLiveForeshowByForeshowId(foreshowId, appId)
            def liveRecordInfoStr = Strings.parseJson(liveForeshow?.live_record_info ?: null)
//            if((liveRecordInfoStr.time_span as int) -timeSpan > 60*10){
//                return
//            }
            int foreshowType = liveForeshow.foreshow_type as int
            long totalWatchCount = (liveRecordInfoStr ? liveRecordInfoStr.total_watch_count?:0 : 0)as long
            if (totalWatchCount == 0 && foreshowType == LiveCommon.FORESHOW_TYPE_1){
                updateForeshowMergeInfo(foreshowId,appId)
            }
            if(liveRecordInfoStr || (!liveRecordInfoStr && (foreshowType == LiveCommon.FORESHOW_TYPE_2 || foreshowType == LiveCommon.FORESHOW_TYPE_3))){
                def liveRecord = [watch_count:liveRecordInfoStr?.watch_count,total_watch_count:liveRecordInfoStr?.total_watch_count,
                                      vest_count:liveRecordInfoStr?.vest_count,time_span:timeSpan,video_address: url]
                def liveRecordInfo = Strings.toJson(liveRecord)
                log.info("wangtf updateForeshowMergeVideoAddressInfo success, foreshowId:{}, liverecordInfo:{}",foreshowId, liveRecordInfo)
                res = liveRes.updateLiveForeshowLiveRecordInfo(liveRecordInfo, liveForeshow.foreshow_status as int,foreshowId, appId)
            }else{
                log.info("wangtf updateForeshowMergeVideoAddressInfo fail, foreshowId:{}, liverecordInfo:{}",foreshowId, liveRecordInfo)
            }
        }
        return res
    }
    def addBlog(long liveId){
        //延迟20秒再发送
        Thread.sleep(20000)
        //异步通知api 创建直播贴
        // {[operTye:1,userId:Long型,srpId:String型,title:String型,liveId:Long型,avRoomId:Long型,chatRoomId:Long型,isPrivate:int型]}
        //查询直播相关信息
        if(!liveId){
            return null
        }
        LiveRecord live = this.findLiveByLiveId(liveId)
        if(!live){
            return null
        }
        List<String> srpIdList = Strings.splitToList(live.srpId)
        def liveArray = []
        if(srpIdList){
            srpIdList.each{
                def kestrelMap = [:]
                kestrelMap.put("operType",1)
                kestrelMap.put("userId",live.userId)
                kestrelMap.put("srpId",it)
                kestrelMap.put("title",live.title)
                kestrelMap.put("liveId",live.liveId)
                kestrelMap.put("avRoomId",live.roomId)
                kestrelMap.put("chatRoomId",live.chatId)
                kestrelMap.put("isPrivate",live.isPrivate)
                kestrelMap.put("vc",live.vc)
                kestrelMap.put("liveMode",live.liveMode ?:1)
                kestrelMap.put("appId",live.appId ?:"souyue")
                liveArray.add(kestrelMap)
            }
        }else{
            def kestrelMap = [:]
            kestrelMap.put("operType",1)
            kestrelMap.put("userId",live.userId)
            kestrelMap.put("srpId","")
            kestrelMap.put("title",live.title)
            kestrelMap.put("liveId",live.liveId)
            kestrelMap.put("avRoomId",live.roomId)
            kestrelMap.put("chatRoomId",live.chatId)
            kestrelMap.put("isPrivate",live.isPrivate)
            kestrelMap.put("vc",live.vc)
            kestrelMap.put("liveMode",live.liveMode ?:1)
            kestrelMap.put("appId",live.appId ?:"souyue")
            liveArray.add(kestrelMap)
        }
        liveKestrel.sendLiveBlogMsg(Strings.toJson(liveArray))
    }

    /**
     * 获取我的直播提醒
     * @param userId
     * @return
     */
    def getMyLiveNotice(long userId,String appId ,String version){
        def resultMap = [:]
        Date startDate = new Date()
        Date endDate = DateUtil.addHourByDay(startDate,1)
        Map map = [
//            startDate:DateUtil.getFormatDate(startDate,DateUtil.FULL_DATE_PATTERN),
            endDate:DateUtil.getFormatDate(endDate,DateUtil.FULL_DATE_PATTERN),
            userId:userId,
            appId:appId,
            vc: version
        ]
        def userInfo = getUserInfo(userId)
        def result = liveRes.getMyLiveNotice(map)
        if (result){
            resultMap = [
                userId: result.user_id,
                nickName: userInfo?.nickname?:result.nickname,
                serverCurrentTime: startDate.getTime(),
                beginTime: DateUtil.getDate(result.begin_time as String,DateUtil.FULL_DATE_PATTERN).getTime(),
                foreshowId: result.foreshow_id,
                title: result.title,
                foreshowType: result.foreshow_type
            ]
        }

        return resultMap
    }


    /**
     * 获取直播分类信息列表
     * @return
     */
    def getLiveCategroyList(Long userId,String appId){
        def resultList = []
        def testUserList = testUser ?  Arrays.asList(testUser.split(",")): null;
        Map map = [
            isHide:1,   //1 显示，2 隐藏
            isDel:1,     //1 未删除，2 已删除
            appId: appId,
            userId: userId
        ]

        if(!testUserList || !testUserList.contains(map.userId as String)) {//如果是测试用户，不走缓存
            resultList = liveQcloudRedis.getLiveCategoryList(appId)
            if (resultList){
                return resultList
            }else {
                resultList = []
            }
        }

        resultList << [category:'推荐',cateId:0]
        liveRes.findLiveCategroyList(map).each {
            def cateId = it.cat_id
            def type = it.type

            if (type == 2){//共享分类使用引用的共享分类id
                cateId = it.share_cat_id
            }
            resultList << [
                category:it.category_name,
                cateId:cateId
            ]
        }
        if((!testUserList || !testUserList.contains(map.userId as String)) && resultList) {//如果是测试用户，不走缓存
            liveQcloudRedis.setLiveCategoryList(resultList, appId)
        }
        return resultList
    }

    /**
     * 获取直播列表（价值）
     * @param userId
     * @param appId
     * @return
     */
    def getValueLiveList(Map map){
        Set resultSet = new HashSet()//因为互动直播有多条，关联会出现重复数据，通过set去重
        def resultList = []
        def tempResultList = []//缓存列表
        def testUserList = testUser ?  Arrays.asList(testUser.split(",")): null;
        def cateIds = []
        String sortInfo = map.sortInfo
        if (sortInfo=='{}' || sortInfo=='[:]'){
            sortInfo = null
        }
        if (map.cateList){
            map.cateList.each{
                cateIds << it.cateId
            }
        }
        //直播中，不分页
        Map livingMap = [
            userId:map.userId,//userId不参与筛选
            targetUserId:map.targetUserId,
            appId:map.appId,
            categoryId:map.categoryId,
            parentId:map.parentId,
            foreshowStatus:1,
            vc: map.vc,
            from: map.from,
            cateIds: cateIds,
            isAndroidOs:map.isAndroidOs
        ]
        //暂停中，不分页
        Map pausedMap = [
            userId:map.userId,//userId不参与筛选
            targetUserId:map.targetUserId,
            appId:map.appId,
            categoryId:map.categoryId,
            parentId:map.parentId,
            foreshowStatus:6,
            vc: map.vc,
            from: map.from,
            cateIds: cateIds,
            isAndroidOs:map.isAndroidOs
        ]
        //客户端5.6.7之前的版本只显示一小时内开始的预告
        Date beginTime = DateUtil.addHourByDay(new Date(),1)
        if(map.from=='h5' || (map.parentId && VerUtils.toIntVer(map.vc) >= VerUtils.toIntVer("5.6.7"))){
            beginTime = null
        }
        //一小时内开始的预告
        Map foreshowMap = [
            userId:map.userId,//userId不参与筛选
            targetUserId:map.targetUserId,
            appId:map.appId,
            categoryId:map.categoryId,
            parentId:map.parentId,
            foreshowStatus:0,//未开始
            beginTime: beginTime,
            vc: map.vc,
            from: map.from,
            cateIds: cateIds,
            isAndroidOs:map.isAndroidOs
        ]

        //回放，带分页
        Map overMap = [
            userId:map.userId,//userId不参与筛选
            targetUserId:map.targetUserId,
            appId:map.appId,
            categoryId:map.categoryId,
            parentId:map.parentId?:0,
            foreshowStatus:2,
            psize:(map.psize as int) * 3,
            sortInfo:sortInfo,
            vc: map.vc,
            from: map.from,
            cateIds: cateIds,
            isAndroidOs:map.isAndroidOs
        ]
        if(map.isRecommend){
            overMap.isRecommend = 1
        }

        long categoryKey = map.categoryId?:0
        if ((!sortInfo || sortInfo=='{}') && (map.from !='app' ||
            (map.from=='app' && VerUtils.toIntVer(map.vc) >= VerUtils.toIntVer("5.6.7") && map.parentId))){//只在第一页查询直播中数据

            liveRes.findForeshowList(livingMap).each {
                if (!resultSet.contains(it.foreshow_id as long)){
                    if(it.foreshow_id as long)
                        resultSet.add(it.foreshow_id as long)
                    resultList << getReturnForeshow(livingMap,it)
                }
            }

            liveRes.findForeshowList(pausedMap).each {
                if (!resultSet.contains(it.foreshow_id as long)) {
                    if(it.foreshow_id as long)
                        resultSet.add(it.foreshow_id as long)
                    resultList << getReturnForeshow(pausedMap, it)
                }
            }

            //大于等于5.6版本的才查询一小时内即将开始的预告
            if(VerUtils.toIntVer(map.vc) >= VerUtils.toIntVer("5.6")) {
                liveRes.findForeshowList(foreshowMap).each {
                    if (!resultSet.contains(it.foreshow_id as long)) {
                        if(it.foreshow_id as long)
                            resultSet.add(it.foreshow_id as long)
                        resultList << getReturnForeshow(foreshowMap, it)
                    }
                }
            }
            if((!testUserList || !testUserList.contains(map.userId as String)) && !map.nocahche) {//如果是测试用户，不走缓存
                tempResultList = liveQcloudRedis.getLiveValueList(categoryKey,map.appId+"_"+map.parentId+"_"+map.from+"_"+map?.targetUserId,map.vc)
                if (tempResultList) {
                    tempResultList.each {
                        resultList << it
                    }
                    return resultList
                } else {
                    tempResultList = []
                }
            }

        }
        int unpageSize = resultSet.size()//只有回放数据需要根据页大小截取
        liveRes.findForeshowList(overMap).each {
            if (!resultSet.contains(it.foreshow_id as long) && (resultSet.size()-unpageSize) < (map.psize as int)) {
                if(it.foreshow_id as long)
                    resultSet.add(it.foreshow_id as long)
                def foreshow = getReturnForeshow(overMap, it)
                tempResultList << foreshow
                resultList << foreshow
            }
        }

        if (!sortInfo || sortInfo=='{}'){//如果读取第一页，写入缓存
            if((!testUserList || !testUserList.contains(map.userId as String)) && !map.nocahche) {//如果是测试用户，不走缓存
                if (tempResultList && tempResultList.size()>0){
                    liveQcloudRedis.setLiveValueList(categoryKey,map.appId+"_"+map.parentId+"_"+map.from+"_"+map?.targetUserId,tempResultList,map.vc)
                }
            }
        }

        return resultList
    }

    @Override
    def getRecommendLiveList(Object map) {
        //推荐数据分为两部分，一部分是价值直播，一次全部查出来，最多20条，包括直播中，预告，暂停，和推荐的回放
        //另一部分是颜值直播，分页查询，一页20条，包括所有的直播中和推荐的回放
        Set resultSet = new HashSet()//因为互动直播有多条，关联会出现重复数据，通过set去重
        def resultList = []
        def faceLiveList = []
        def tempResultList = []//缓存列表
        def testUserList = testUser ?  Arrays.asList(testUser.split(",")): null;
        String sortInfo = map.sortInfo
        if (sortInfo=='{}' || sortInfo=='[:]'){
            sortInfo = null
        }
        //直播中，不分页
        Map livingMap = [
            userId:map.userId,//userId不参与筛选
            targetUserId:map.targetUserId,
            appId:map.appId,
            categoryId:map.categoryId,
            parentId:map.parentId,
            foreshowStatus:1,
            vc: map.vc,
            from: map.from,
            isAndroidOs:map.isAndroidOs
        ]
        //暂停中，不分页
        Map pausedMap = [
            userId:map.userId,//userId不参与筛选
            targetUserId:map.targetUserId,
            appId:map.appId,
            categoryId:map.categoryId,
            parentId:map.parentId,
            foreshowStatus:6,
            vc: map.vc,
            from: map.from,
            isAndroidOs:map.isAndroidOs
        ]
        //客户端5.6.7之前的版本只显示一小时内开始的预告
        Date beginTime = DateUtil.addHourByDay(new Date(),1)
        if(map.from=='h5' || (map.parentId && VerUtils.toIntVer(map.vc) >= VerUtils.toIntVer("5.6.7"))){
            beginTime = null
        }
        //一小时内开始的预告
        Map foreshowMap = [
            userId:map.userId,//userId不参与筛选
            targetUserId:map.targetUserId,
            appId:map.appId,
            categoryId:map.categoryId,
            parentId:map.parentId,
            foreshowStatus:0,//未开始
            beginTime: beginTime,
            vc: map.vc,
            from: map.from,
            isAndroidOs:map.isAndroidOs
        ]
        Map faceLiveMap = [
            sortInfo:sortInfo,
            displayPrivate: false,
            appId:map.appId,
            isRecommend: 1,
            onlyUgc: "onlyUgc",
            psize: map.psize,
            vc: map.vc,
            currentUserId: map.userId ?: ""
        ]
        long categoryKey = map.categoryId?:0
        if (!sortInfo || sortInfo=='{}'){//只在第一页查询价值直播的数据

            liveRes.findForeshowList(livingMap).each {
                if (!resultSet.contains(it.foreshow_id as long)){
                    if(it.foreshow_id as long)
                        resultSet.add(it.foreshow_id as long)
                    resultList << getReturnForeshow(livingMap,it)
                }
            }

            liveRes.findForeshowList(pausedMap).each {
                if (!resultSet.contains(it.foreshow_id as long)) {
                    if(it.foreshow_id as long)
                        resultSet.add(it.foreshow_id as long)
                    resultList << getReturnForeshow(pausedMap, it)
                }
            }

            //大于等于5.6版本的才查询一小时内即将开始的预告
            if(VerUtils.toIntVer(map.vc) >= VerUtils.toIntVer("5.6")) {
                liveRes.findForeshowList(foreshowMap).each {
                    if (!resultSet.contains(it.foreshow_id as long)) {
                        if(it.foreshow_id as long)
                            resultSet.add(it.foreshow_id as long)
                        resultList << getReturnForeshow(foreshowMap, it)
                    }
                }
            }
            if(resultList.size()<(map.psize as int)){
                //回放，带分页
                Map overMap = [
                    userId:map.userId,//userId不参与筛选
                    targetUserId:map.targetUserId,
                    appId:map.appId,
                    categoryId:map.categoryId,
                    parentId:map.parentId?:0,
                    foreshowStatus:2,
                    psize:(map.psize as int) * 3,
                    sortInfo:sortInfo,
                    vc: map.vc,
                    from: map.from,
                    isRecommend: 1,
                    isAndroidOs:map.isAndroidOs
                ]

                liveRes.findForeshowList(overMap).each {
                    if (!resultSet.contains(it.foreshow_id as long) && (resultSet.size()) < (map.psize as int)){
                        if(it.foreshow_id as long)
                            resultSet.add(it.foreshow_id as long)
                        def foreshow = getReturnForeshow(overMap, it)
                        tempResultList << foreshow
                        resultList << foreshow
                    }

                }
            }
        }
        //查询颜值直播列表：先查询直播中，然后 查询回放
        int isLiving = 0
        if(sortInfo){
            isLiving = (Strings.parseJson(sortInfo).isLiving ?: 0)as int
        }
        if(!sortInfo || (sortInfo && isLiving == 1)){
            faceLiveList = findFaceLiveList(faceLiveMap)
        }
        if((faceLiveList?faceLiveList.size():0) < (map.psize as int)){
            //当直播中不够一页，则查询推荐的回放数据补全
            faceLiveList.addAll(findMyLiveRecordListNew(faceLiveMap))
        }
        if((faceLiveList?faceLiveList.size():0) > (map.psize as int)){
            faceLiveList = faceLiveList.subList(0, map.psize as int)
        }
        //颜值直播的返回数据进行特殊处理
        int count = 1
        List list = []
        List faceList = []
        faceLiveList?.each{
            int viewType = LiveCommon.VIEW_TYPE_FACE_LIVE_NEW
            it.viewType = LiveCommon.VIEW_TYPE_LIVE
            def sort= it.sortInfo
            it.remove("sortInfo")
            list.add(it)
            if(count%2 == 0 || count == faceLiveList.size()){
                faceList << [
                        viewType: viewType,
                        sortInfo: sort,
                        data: list.clone()
                ]
                list.clear()
            }
            count ++
        }
        return [resultList: resultList,faceList: faceList]
    }

    @Override
    def getValueLiveListBySrp(Map map) {
        Set resultSet = new HashSet()//因为互动直播有多条，关联会出现重复数据，通过set去重
        def resultList = []
        def tempResultList = []//缓存列表
        def testUserList = testUser ?  Arrays.asList(testUser.split(",")): null;
        def cateIds = []
        if (map.cateList){
            map.cateList.each{
                cateIds << it.cateId
            }
        }
        //直播中，不分页
        Map livingMap = [
            userId:map.userId,//userId不参与筛选
            appId:map.appId,
            categoryId:map.categoryId,
            foreshowStatus:1,
            srpId:map.srpId,
            vc: map.vc,
            cateIds: cateIds,
            isAndroidOs:map.isAndroidOs
        ]
        //暂停中，不分页
        Map pausedMap = [
            userId:map.userId,//userId不参与筛选
            appId:map.appId,
            categoryId:map.categoryId,
            foreshowStatus:6,
            srpId:map.srpId,
            vc: map.vc,
            cateIds: cateIds,
            isAndroidOs:map.isAndroidOs
        ]
        //回放，带分页
        Map overMap = [
            userId:map.userId,//userId不参与筛选
            appId:map.appId,
            categoryId:map.categoryId,
            foreshowStatus:2,
            parentId:0,
            srpId:map.srpId,
            psize:(map.psize as int) * 3,
            sortInfo:map.sortInfo,
            vc: map.vc,
            cateIds: cateIds,
            isAndroidOs:map.isAndroidOs
        ]

        String sortInfo = map.sortInfo
        long categoryKey = map.categoryId?:0

        if (!sortInfo || sortInfo=='{}'){//只在第一页查询直播中数据

            liveRes.findForeshowList(livingMap).each {
                if (!resultSet.contains(it.foreshow_id as long)) {
                    if(it.foreshow_id as long)
                        resultSet.add(it.foreshow_id as long)
                    resultList << getReturnForeshow(livingMap,it)
                }
            }

            liveRes.findForeshowList(pausedMap).each {
                if (!resultSet.contains(it.foreshow_id as long)) {
                    if(it.foreshow_id as long)
                        resultSet.add(it.foreshow_id as long)
                    resultList << getReturnForeshow(pausedMap,it)
                }
            }

        }
        int unpageSize = resultSet.size()//只有回放数据需要根据页大小截取
        liveRes.findForeshowList(overMap).each {
            if (!resultSet.contains(it.foreshow_id as long) && ((resultSet.size()-unpageSize) < (map.psize as int))) {
                if(it.foreshow_id as long)
                    resultSet.add(it.foreshow_id as long)
                def foreshow = getReturnForeshow(overMap,it)
                tempResultList << foreshow
                resultList << foreshow
            }
        }

//        if (!sortInfo || sortInfo=='{}'){//如果读取第一页，写入缓存
//            if(!testUserList || !testUserList.contains(map.userId as String)) {//如果是测试用户，不走缓存
//                if (tempResultList && tempResultList.size()>0){
//                    liveQcloudRedis.setLiveValueList(categoryKey,tempResultList)
//                }
//            }
//        }

        return resultList
    }
    def getLiveListForSmallProgram(Map map){
        List resultList = []
        Set resultSet = new HashSet()//因为互动直播有多条，关联会出现重复数据，通过set去重
        List liveCategroyList = getLiveCategroyList(map.userId as long,map.appId)
        def cateList = liveCategroyList?.subList(1,liveCategroyList.size())
        def cateIds = []
        if (cateList){
            cateList.each{
                cateIds << it.cateId
            }
        }
        //回放，带分页
        Map overMap = [
            appId:map.appId,
            foreshowStatus:2,
            psize: (map.psize as int) * 3,
            parentId:0,
            sortInfo:map.sortInfo,
            vc: map.vc ?: "5.6",
            userId: map.userId,
            cateIds:cateIds,
            isFree: 1
        ]
        int unpageSize = resultSet.size()//只有回放数据需要根据页大小截取
        liveRes.findForeshowList(overMap).each {
            if (!resultSet.contains(it.foreshow_id as long) && (resultSet.size()-unpageSize) < ((map.psize as int)+1)) {
                resultSet.add(it.foreshow_id as long)
                def foreshow = getReturnForeshow(overMap, it)
                resultList << foreshow
            }
        }
        return resultList
    }
/**
     * 价值直播列表类型标准转换
     * @param map
     * @param it
     * @return
     */
    private def getReturnForeshow(Map map,def it){
        long beginTime =  DateUtil.getTimestamp(it.begin_time ? it.begin_time as String : "")
        int isHost = (map.userId as String).equals(it.user_id as String) ? 1:0
        int invokeType      //跳转类型
        int viewType        //展示类型
        int isOpenRemind
        int watchCount = 0      //观看人数
        int foreshowType = it.foreshow_type?:0 as int
        int liveStatus = it.foreshow_status as int
        String subTitle     //子标题
        int timeSpan = 0       //视频时长
        LiveRecord liveRecord
        long liveId
        int liveNum = it.containsKey("live_num")?(it.live_num?:0):0       //系列期数
        String shortUrl
        def category
        def blogId = 0

        if(isHost == 0){
            isOpenRemind = liveRes.isOpenRemind(map.appId, map.userId as long, it.foreshow_id)
        }else {
            def record = liveRes.findLiveByForeshowId(it.foreshow_id,map.appId)
            if (record && (record?.pgc_type as int) != 1){//非手机推流的都不算是主播
                isHost = 0
            }
        }

        if(foreshowType == 5) {//5直播系列
            invokeType = LiveCommon.INVOKE_TYPE_LIVE_FORESHOW_SERIE
            viewType = LiveCommon.VIEW_TYPE_LIVE_FORESHOW_SERIE
        }else if(foreshowType == 1){//1互动直播
            switch (liveStatus){
                case LiveCommon.FORESHOW_STATUS_2://直播结束（回放）
                    invokeType = LiveCommon.INVOKE_TYPE_LIVE_FORESHOW_RECORD
                    viewType = LiveCommon.VIEW_TYPE_LIVE_FORESHOW_RECORD
                    break;
                case LiveCommon.FORESHOW_STATUS_0://未开始
                    invokeType = LiveCommon.INVOKE_TYPE_LIVE_FORESHOW
                    viewType = LiveCommon.VIEW_TYPE_LIVE_FORESHOW //展示类型与会议直播一致
                    break;
                case LiveCommon.FORESHOW_STATUS_1://更改状态的直播中
                    invokeType = LiveCommon.INVOKE_TYPE_LIVE_FORESHOW
                    viewType = LiveCommon.VIEW_TYPE_PGC_FORESHOW_START_AND_PAUSE //展示类型与会议直播一致
                    break;
                case LiveCommon.FORESHOW_STATUS_6://互动直播无暂停，暂时设为直播中
                    invokeType = LiveCommon.INVOKE_TYPE_LIVE_FORESHOW
                    viewType = LiveCommon.VIEW_TYPE_PGC_FORESHOW_START_AND_PAUSE //展示类型与会议直播一致
                    break;
                case LiveCommon.FORESHOW_STATUS_5://未生成回放地址
                    invokeType = LiveCommon.INVOKE_TYPE_LIVE_FORESHOW_RECORD
                    viewType = LiveCommon.VIEW_TYPE_LIVE_FORESHOW_RECORD
                    break;
            }
        }else if(foreshowType == 2 || foreshowType == 3){//2会议直播，3付费会议直播
            switch (liveStatus){
                case LiveCommon.FORESHOW_STATUS_2://直播结束（回放）
                    invokeType = LiveCommon.INVOKE_TYPE_LIVE_FORESHOW_VALUE_RECORD
                    viewType = LiveCommon.VIEW_TYPE_LIVE_FORESHOW_RECORD
                    break;
                case LiveCommon.FORESHOW_STATUS_0://未开始
                    invokeType = LiveCommon.VIEW_TYPE_PGC_FORESHOW_START_AND_PAUSE
                    viewType = LiveCommon.VIEW_TYPE_LIVE_FORESHOW
                    break;
                case LiveCommon.FORESHOW_STATUS_1://更改状态的直播中
                    invokeType = LiveCommon.INVOKE_TYPE_LIVE_FORESHOW_START_AND_PAUSE
                    viewType = LiveCommon.VIEW_TYPE_PGC_FORESHOW_START_AND_PAUSE
                    break;
                case LiveCommon.FORESHOW_STATUS_6://更改状态的暂停
                    invokeType = LiveCommon.INVOKE_TYPE_LIVE_FORESHOW_START_AND_PAUSE
                    viewType = LiveCommon.VIEW_TYPE_PGC_FORESHOW_START_AND_PAUSE
                    break;
                case LiveCommon.FORESHOW_STATUS_5://未生成回放地址
                    invokeType = LiveCommon.INVOKE_TYPE_LIVE_FORESHOW_VALUE_RECORD
                    viewType = LiveCommon.VIEW_TYPE_LIVE_FORESHOW_RECORD
                    break;
            }
        }else if(foreshowType == 0){//颜值直播
            invokeType = LiveCommon.INVOKE_TYPE_LIVE
            switch (liveStatus){
                case LiveCommon.FORESHOW_STATUS_2://直播结束（回放）
                    invokeType = LiveCommon.INVOKE_TYPE_LIVE_FORESHOW_RECORD
                    viewType = LiveCommon.VIEW_TYPE_LIVE_FORESHOW_RECORD
                    break;
                case LiveCommon.FORESHOW_STATUS_1://更改状态的直播中
                    invokeType = LiveCommon.INVOKE_TYPE_LIVE
                    viewType = LiveCommon.VIEW_TYPE_PGC_FORESHOW_START_AND_PAUSE
                    break;
            }
        }

        if (viewType == LiveCommon.VIEW_TYPE_LIVE || viewType == LiveCommon.VIEW_TYPE_PGC_FORESHOW_START_AND_PAUSE){//互动直播中、会议直播中、会议直播暂停只需要人数
            def liveMode
            def roomId
            def vc
            def recodeAppId
            try {
                liveId = it.live_id?: 0
                liveMode = it.live_mode
                roomId = it.room_id
                vc = it.vc
                recodeAppId = it.recode_app_id
            }catch (Exception){
                liveRecord = getLiveByForeshow(it.foreshow_id as long, map.appId)
                liveId = liveRecord?.liveId ?: 0
                liveMode = liveRecord?.liveMode
                roomId = liveRecord?.roomId
                vc = liveRecord?.vc?:"1.0"
                recodeAppId = liveRecord?.appId
            }
            if(liveId){
                try {
                    watchCount = getLiveWatherCount(liveId)
                }catch (Exception e){
                    watchCount = 0
                }

                shortUrl = shortURLService.getShortUrlLive([liveId: it.foreshow_id ?: 0,liveMode: liveMode?:0,userId:it.user_id as long,roomId: roomId ?: 0, vc: vc,appId: recodeAppId])
            }
        }
        if (viewType == LiveCommon.VIEW_TYPE_LIVE_FORESHOW_RECORD) {//互动直播回放、会议直播回放需要人数、时长
            def liveRecordInfo =  Strings.parseJson(it.live_record_info)
            def videoAddress = liveRecordInfo ? liveRecordInfo.video_address : ""
            boolean flag =  videoAddress ? true: false
            if (flag){
                //获取时间时，如果timeSpan为0，从回放表中获取视频回放信息
                if (liveRecordInfo){

                    timeSpan = liveRecordInfo.time_span?:0 as int
                    watchCount = liveRecordInfo.total_watch_count?:0 as int

                    if (foreshowType!=LiveCommon.FORESHOW_TYPE_1 && it.containsKey("live_id")){//非互动回放不取log表，节省时间
                        watchCount = getLiveWatherTotalCount((it.live_id?:0) as long)
                    }
                    if(timeSpan == 0 || watchCount == 0){
                        flag = false
                    }
                }
            }
            if (!flag){
                int tempTimeSpan = 0
                List liveList = []
                if(it.foreshow_id){
                    liveList = liveRes.findLiveRecordListByForeId([foreshowId:it.foreshow_id,appId:map.appId])
                }else{
                    liveId = (it.live_id ?: 0) as long
                    liveList << liveRes.findLiveRecordByLiveId(it.live_id)
                }
                liveList?.each {row ->
                    Map dataMap = [
                        liveId    : row.live_id,
                        roomId    : row.room_id,
                        foreshowId: row.foreshow_id,
                        appId     : row.app_id
                    ]
                    Map videoAddressMap = getVideoAddressUrlList(row.video_address as String, dataMap)
                    tempTimeSpan += videoAddressMap.timeSpan as int

                    def tempWatchCount = 0
                    try {
                        if(foreshowType == 1 || foreshowType == 0) {//1互动直播
                            tempWatchCount = row.total_watch_count
                        }else {//2会议直播 3付费直播
                            tempWatchCount = getLiveWatherTotalCount((row.live_id?:0 as long))
                        }
                    }catch (Exception e){
                        e.printStackTrace()
                        tempWatchCount = 0
                    }
                    watchCount += tempWatchCount
                }

                if (timeSpan==0){
                    timeSpan = tempTimeSpan
                }
            }

        }
        if (viewType == LiveCommon.VIEW_TYPE_LIVE_FORESHOW_SERIE) {//系列需要副标题、期数
            def lastForeShow = liveRes.findLastForeShow(it.foreshow_id,2,map.vc)
            if (lastForeShow){
                subTitle = "回放："+lastForeShow.title
            }
        }

        if (invokeType == LiveCommon.INVOKE_TYPE_LIVE_FORESHOW_RECORD && !map.from){//IOS 如果返回10002互动回看，不需要foreshowType,微信端需要
            foreshowType = 0
        }

        liveStatus = liveStatus==0 ? (beginTime < System.currentTimeMillis() ? 0 :3 ) : liveStatus //1：直播中 2：直播结束(回放) 3:未开始 6:暂停直播
        if (liveStatus==3){
            invokeType=it.url_tag as int    //跳转类型
            if(!Strings.APP_NAME_SOUYUE.equals(map.appId) && invokeType != 120 && invokeType != 85){
                invokeType = LiveCommon.INVOKE_TYPE_LIVE_FORESHOW_START_AND_PAUSE
            }
            if(invokeType==24) {//24:贴子详情页用于互动直播
                blogId = it.url
//                category = "interest"
            }else if(invokeType == 10){//10:新闻详情页用于互动直播
//                category = "srp"
            }
            if(foreshowType == LiveCommon.FORESHOW_TYPE_1){
                category = "互动直播预告"
            }else if(foreshowType == LiveCommon.FORESHOW_TYPE_2){
                category = "会议直播预告"
            }else if(foreshowType == LiveCommon.FORESHOW_TYPE_3){
                category = "收费会议直播预告"
            }
        }

        return [
            chargeType: it.foreshow_type==3 ? 1 : 2,//1:收费，2:不收费
            invokeType: invokeType,
            viewType: viewType,
            category: category,
            foreshowType: foreshowType,
            foreshowId: it.foreshow_id as long,
            watchCount: watchCount,
            liveThumb: it.img_url,
            title: it.title,
            showTitle: it.show_title_in_list,
            subTitle: subTitle,
            timeSpan: timeSpan,
            url: it.url,
            beginTime: beginTime,
            liveStatus: liveStatus==0 ? (beginTime > System.currentTimeMillis()+3600_000 ? 0 :3 ) : liveStatus, //1：直播中 2：直播结束(回放) 3:未开始 6:暂停直播
            isHost: isHost,
            isOpenRemind: isOpenRemind,
            shortUrl: shortUrl,
            liveNum: liveNum,// 共几期
            anchorInfo: [
                "nickname": it.nickname,
                "userImage": it.user_image,
                "userId": it.user_id
            ],
            sortInfo: [
                "orderId": map.parentId?it.child_sort:it.sort_num,
                "createTime": it.begin_time
            ],
            serverCurrentTime:System.currentTimeMillis(),

            "urlTag":it?.url_tag,//10004:跳会议直播,24:贴子详情页用于互动直播,10:新闻详情页用于互动直播,85:html5用于互动直播,120:H5页面
            "keyword": it.keyword,
            "srpId": it.srp_id,
            "blogId": blogId,
            liveId: liveId
        ]
    }

    /**
     * 对某个组内成员禁言
     * @param userId
     * @param forbidUserId
     * @param roomId
     * @param liveId
     * @param appId
     * @return
     */
    @Override
    def forbidCommentToGroupMember(long userId, long forbidUserId, int roomId, long liveId, String appId) {
        //首先验证操作人userId的身份信息
        int role = livePgcService.getUserRole(liveId, userId)
        boolean flag = false
        if(role > 0 && role != LiveCommon.LIVE_ROLE_3){ //主播，主持人和场控都可以进行踢人和禁言
            flag = true
        }
        Map returnMap = [status: 0, msg:"禁言失败!"]
        //调用禁言接口
        if(flag){
            //验证当前用户是否在马甲列表中，如果是马甲直接返回禁言成功，并且把改马甲从马甲列表中删除
//            def vestUserInfo = liveQcloudRedis.getVestUserInfoFromRedis(forbidUserId)
            def vestUserInfo = vestUserService.getVestUserFromRedis(liveId,forbidUserId)
            if(vestUserInfo){
                Parallel.run([1], {
                    //将该禁言的马甲用户从马甲列表中删除，并更新马甲数缓存
                    liveQcloudRedis.delLiveWather(liveId, forbidUserId)
                    liveQcloudRedis.addLiveWatchCount(liveId,-1)
                    vestUserService.delVestUserByLiveIdAndUserId(liveId,vestUserInfo)
                    liveQcloudRedis.hsetForbidCommentUserInfo(liveId, forbidUserId,Strings.toJson(vestUserInfo))
                }, 1)
                return [status: 1, msg:"禁言成功！"]
            }
            long startTime = System.currentTimeMillis()
            def res = qcloudLiveService.forbidComment([roomId: roomId, forbidUserId: forbidUserId,appId:appId])
            String actionStatus = Strings.parseJson(res).ActionStatus
            log.info("wangtf forbid comment res:{},map:{},请求腾讯云time:{}", res, [roomId: roomId, forbidUserId: forbidUserId,appId:appId],System.currentTimeMillis()-startTime)
            if("OK".equals(actionStatus)){
                returnMap = [status: 1, msg:"禁言成功！"]
                // 将禁言人的信息更新到缓存
                def userInfo = getUserInfo(forbidUserId)
                liveQcloudRedis.hsetForbidCommentUserInfo(liveId, forbidUserId,Strings.toJson(userInfo))
                //给群组成员发送禁言消息
//                Map msgInfo = [userId:forbidUserId, nickname:userInfo.nickname, type:4, liveId: liveId]
//                Map map = [msgInfo: msgInfo, hostUid: userId, roomId: roomId]
//                qcloudLiveService.sendVestDoPrimeMsg(map)
            }
        }else {
            log.info("wangtf fobid comment fail,当前用户没有权限禁言,map:{}",[roomId: roomId, forbidUserId: forbidUserId,appId:appId])
        }
        return returnMap
    }
    /**
     * 直播踢人
     * @param userId
     * @param kickUserId
     * @param roomId
     * @param liveId
     * @param appId
     * @return
     */
    @Override
    def kickGroupMember(long userId, long kickUserId, int roomId, long liveId, String appId) {
        //首先验证操作人userId的身份信息（必须是主播或者场控）
        int role = livePgcService.getUserRole(liveId, userId)
        boolean flag = false
        if(role > 0 && role != LiveCommon.LIVE_ROLE_3){ //主播，主持人和场控都可以进行踢人和禁言
            flag = true
        }
        Map returnMap = [status: 0, msg:"踢人失败!"]
        //调用禁言接口
        if(flag){
            def res = qcloudLiveService.kickGroupMember([roomId: roomId, kickUserId: kickUserId,appId:appId])
            String actionStatus = Strings.parseJson(res).ActionStatus
            log.info("wangtf kick member res:{},map:{}", res, [roomId: roomId, kickUserId: kickUserId])
            if("OK".equals(actionStatus)){
                returnMap = [status: 1, msg:"踢人成功！"]
                // 将禁言人的信息更新到缓存
                def userInfo = getUserInfo(kickUserId)
                liveQcloudRedis.hsetKickGroupMember(liveId, kickUserId,Strings.toJson(userInfo))
                //将该用户从观众列表的缓存中删除
                liveQcloudRedis.delLiveWather(liveId as long, kickUserId)
                liveQcloudRedis.addLiveWatchCount(liveId,-1)
            }
        }else {
            log.info("wangtf kick member fail,当前用户没有权限踢人")
        }
        return returnMap
    }

    @Override
    def updateUserForeshow(long userId, long foreshowId, int operType,String appId) {
        int res = 1
		if (!userId || !foreshowId){
            return res
        }
        if (1 == operType){//1添加关注
            liveRes.addUserForeshow(userId, foreshowId,appId)
            res =1
        }else if (0== operType){//0解除关注
            liveRes.removeUserForeshow(userId, foreshowId,appId)
            res =0
        }
        res
    }

    @Override
    def listUserForeshow(long userId, int psize, String sortInfo,String appId,String vc) {
        if (!userId){
            return []
        }
        def resultList = liveRes.listUserForeshow(userId, LiveCommon.FORESHOW_TYPE_5, psize, sortInfo,appId)
        def parentIdList = []
        resultList?.each{
            parentIdList << it.foreshowId
        }

        List infoList = liveForeshowService.listInfoByParentIds(parentIdList,LiveCommon.FORESHOW_STATUS_2,appId, vc)

        resultList?.each{parent->
            Map info = infoList?.find({it.parentId == parent.foreshowId})
            parent.subTitle = info?.title?:''
            parent.liveNum = info?.count?:0
            if(parent.foreshowType == LiveCommon.FORESHOW_TYPE_5) {//5直播系列
                parent.invokeType = LiveCommon.INVOKE_TYPE_LIVE_FORESHOW_SERIE
                parent.viewType = LiveCommon.VIEW_TYPE_LIVE_FORESHOW_SERIE
            }
        }

        resultList
    }
    def getUserForeshow(long userId,long foreshowId,String appId){
        if (!userId|| !foreshowId){
            return null
        }
        return liveRes.getUserForeshow(userId,foreshowId,appId)
    }

    boolean isOpenRemind(long userId,long foreshowId,String appId){
        return liveRes.isOpenRemind(appId,userId,foreshowId)
    }
    List<Long> listOpenRemind(long userId,String appId){
        return liveRes.listOpenRemind(appId,userId)
    }
    /**
     * 将到开始时间的未开始直播状态改为开始，定时任务执行
     * @return
     */
    def updateLivePgcStatusBegin(){
      def result=liveRes.getNotBeginLiveRecordsByTime()
        result.each{
            liveRes.updateLivePgcStatusByLiveId(it.liveId,LiveCommon.PGC_STATUS_1)
            //liveQcloudRedis.delLiveRecord(it.appId,it.liveId)
        }
    }

    def updateForeshowIdByLiveId(long liveId,long foreshowId,String appId){
        def result = liveRes.updateForeshowIdByLiveId(liveId, foreshowId, appId)
        if(result){
            liveQcloudRedis.delLiveRecord(appId, liveId)
            findLiveByLiveId(liveId)
        }
        return result;
    }

    def formatPlayUrl(LiveRecord liveRecord,String vc,def params){
        if(!liveRecord.viewJson) return "";
     //   Map viewJson =Strings.parseJson(liveRecord.viewJson)
        def playUrl = liveRecord.viewJson.flv
        if(LiveCommon.PGC_PLAY_URL_RATE_STANDARD==(playRate)){
            playUrl =  liveRecord.viewJson.flvStandard
        }else if(LiveCommon.PGC_PLAY_URL_RATE_HIGH==(playRate)){
            playUrl =  liveRecord.viewJson.flvHigh
        }
        if(!playUrl){
            playUrl = liveRecord.viewJson.flv
        }
        playUrl = urlSignedUtil.getSignedUrl(playUrl,0,liveRecord.appId,vc)
        if(!Strings.isAndroidOs(null,params) && playUrl.startsWith("rtmp") && VerUtils.toIntVer(vc) < VerUtils.toIntVer("5.6.9")){//由于ios暂时不支持rtmp，固此时取m3u8地址
            playUrl = this.formatM3u8Url(liveRecord,vc)
        }
        return playUrl
    }

    def formatM3u8Url(LiveRecord liveRecord,String vc){
        if(!liveRecord.viewJson) return "";
      //  Map viewJson =Strings.parseJson(liveRecord.viewJson)
        def m3u8Url =  liveRecord.viewJson.m3u8
        if(LiveCommon.PGC_PLAY_URL_RATE_STANDARD==playRate){
            m3u8Url= liveRecord.viewJson.m3u8Standard
        }else if(LiveCommon.PGC_PLAY_URL_RATE_HIGH==playRate){
            m3u8Url= liveRecord.viewJson.m3u8High
        }
        m3u8Url = urlSignedUtil.getSignedUrl(m3u8Url,0,liveRecord.appId,vc)
        return m3u8Url
    }

    def updateForeshowStatusBegin(){
        def foreshowList=liveRes.findNotBeginForeshowListByTime()
        def msgList=[]
        foreshowList.each{bean->
           def back= liveRes.updateForeshowStatus(bean.foreshowId,LiveCommon.FORESHOW_STATUS_1)
            //如果是会议直播，则给关注用户发消息
            if(bean.foreshowType==LiveCommon.FORESHOW_TYPE_2 || bean.foreshowType==LiveCommon.FORESHOW_TYPE_3){
              msgList<<[category:"meeting",operType:1,userId:bean.userId,srpId: bean.srpId,title: bean.title,foreshowId: bean.foreshowId,appId: bean.appId,imgUrl:bean.imgUrl]
            }
            //直播发送IM消息
            this.sendStartIMMsg(bean.foreshowId)
        }

        //给客户端发直播开始消息
        Parallel.run([1], {
            log.info("LiveServiceImpl.updateForeshowStatusBegin 自动执行pushLiveStartMsg方法，msgList={}",Strings.toJson(msgList))
            this.pushLiveStartMsg(msgList)
        }, TimeUnit.SECONDS.toMillis(5)
        )
    }

    void sendStartIMMsg(long foreshowId){
        Map m = liveQcloudRedis.hgetAllPgcForeshowToLive(foreshowId);
        Set liveIds  = m.keySet()
        liveIds.each {
            LiveRecord live = this.findLiveByLiveId(it as long)
            if(live){
                long timeSpan = System.currentTimeMillis() - DateUtil.getTimestamp(live.beginTime) //总时长
                qcloudLiveService.sendPgcMsg([roomId: live.roomId, fromAccount: live.userId,appId:live.appId,
                                              msgInfo: [pgcStatus: LiveCommon.FORESHOW_STATUS_1, message: "直播开始",timeSapn: timeSpan/1000,liveId:live.liveId]])
            }
        }
    }

    @Override
    def pushLiveStartMsg(List<Map<String, Object>> paramList) {
        if (!paramList){
            return
        }
        def kestrelMapList = []
        paramList.each{
            long foreshowId = it.foreshowId
            String appId = it.appId
            LiveRecord live = getLiveByForeshow(foreshowId,appId)

            if (live){
                //查找avRoomId chatRoomId
                kestrelMapList << [
                       category:it.category,
                       operType : it.operType,
                       userId : it.userId,
                       srpId : it.srpId,
                       title : it.title,
                       liveId : live.liveId,
                       isPrivate : it.isPrivate,
                       vc:"5.5",
                       avRoomId : live.roomId,
                       chatRoomId : live.chatId,
                    foreId:foreshowId,
                    bigImage: it.imgUrl,
                    beginTime: live.beginTime?(DateUtil.getTimestamp(live.beginTime)/1000) as String:"",
                    appId: live.appId ?: "souyue",
                    liveMode: live.liveMode ?: 2
                ]
            }
        }

        log.info("LiveServiceImpl.pushLiveStartMsg 发送liveKestrel消息={}",Strings.toJson(kestrelMapList))
        liveKestrel.sendLiveBlogMsg(Strings.toJson(kestrelMapList))
    }

    @Override
    def deleteRecodeLog(String fileId) {
        def data = liveRes.findLiveRecordLogByFileId(fileId)
        long liveId = 0
        int res = 0
        if(data){
            liveId = (data.live_id ?: 0) as long
        }
        if(liveId){//自己写的工具类，指定删除搜悦的腾讯云文件
            res = delLiveRecord([op_role: 100,liveId: liveId])
        }
        return res
    }

    /**
     * 获取价值直播轮播图
     * @return
     */
    def getRollImgList(Map map){
        def rollImgList = []
        def liveBannerList = []
        String appId = map.appId
        //由于精华包与搜悦共用，固此处将精华包名强转为souyue
        if(appId.equals("com.zhongsou.souyueprime")){
            appId="souyue"
        }
        liveBannerList = liveRes.getRollImgList(appId)

        /*def testUserList = testUser ?  Arrays.asList(testUser.split(",")): null;
        if(!testUserList || !testUserList.contains(map.userId as String)) {//如果是测试用户，不走缓存
            liveBannerList = liveQcloudRedis.getLiveBannerList()
        }
        if (!liveBannerList || liveBannerList.size()<1){
            liveBannerList = liveRes.getRollImgList()
            if(!testUserList || !testUserList.contains(map.userId as String)) {//如果是测试用户，不走缓存
                if(liveBannerList && liveBannerList.size()>0){
                    log.info("--------轮播条数---->>${liveBannerList.size()}")
                    liveQcloudRedis.setLiveBannerList(liveBannerList)
                }
            }
        }*/
        log.info("--------轮播条数---->>${liveBannerList.size()}")
        liveBannerList.each {
            def rollImg = [:]
            def category
            int invokeType=it.url_tag as int    //跳转类型
            if(invokeType==24) {//24:贴子详情页用于互动直播
                category = "interest"
            }else if(invokeType == 10){//10:新闻详情页用于互动直播
                category = "srp"
            }

            rollImg = [
                "invokeType": invokeType, //10004:跳会议直播,24:贴子详情页用于互动直播,10:新闻详情页用于互动直播,85:html5用于互动直播,120:H5页面
                "foreshowId": it.foreshow_id,
                "category":category,
                "liveThumb": it.image,
                "title": it.title,
                "keyword": it.keyword,
                "srpId": it.srp_id,
                "url": it.url,
                "blogId": it.blog_id
            ]

            if(invokeType == 86){//86：预告
                def liveForeshow = liveRes.getForeshowById(it.foreshow_id as long)
                if (liveForeshow){
                    def foreshowStatus = liveForeshow.foreshow_status
                    if ((foreshowStatus as String) && (foreshowStatus as int)==LiveCommon.FORESHOW_STATUS_0){
                        int foreshowInvokeType = liveForeshow.url_tag as int
                        long beginTime =  DateUtil.getTimestamp(liveForeshow.begin_time ? liveForeshow.begin_time as String : "")
                        long blogId = 0
                        int isOpenRemind = 0   //是否开启直播预约，0：未开启，1：开启
                        int isHost = (map.userId as String).equals(liveForeshow.user_id as String) ? 1:0
                        if(isHost == 0){
                            isOpenRemind = liveRes.isOpenRemind(appId, map.userId as long, liveForeshow.foreshow_id)
                        }
                        String shortUrl

                        if (foreshowStatus==1){
                            def liveRecord = getLiveByForeshow(liveForeshow.foreshow_id as long, appId)
                            if (liveRecord)
                                shortUrl = shortURLService.getShortUrlLive([liveId: liveRecord?.foreshowId ?: 0,liveMode: liveRecord?.liveMode?:0,userId:liveForeshow.user_id as long,roomId: liveRecord?.roomId ?: 0, vc: liveRecord?.vc,appId: liveRecord.appId])
                        }

                        if(foreshowInvokeType==20 || foreshowInvokeType==24){
                            blogId=liveForeshow.url as long
                            category = "interest"
                        }else if(foreshowInvokeType == 10){
                            category = "srp"
                        }

                        rollImg = [
                            "invokeType": foreshowInvokeType, //10004:跳会议直播,24:贴子详情页用于互动直播,10:新闻详情页用于互动直播,85:html5用于互动直播,120:H5页面
                            "foreshowId": liveForeshow.foreshow_id,
                            "category":category,//**
                            "liveThumb": liveForeshow.img_url,
                            "title": liveForeshow.title,
                            "keyword": liveForeshow.keyword,
                            "srpId": liveForeshow.srp_id,
                            "url": liveForeshow.url,
                            "blogId": blogId,
                            "beginTime": beginTime,
                            "liveStatus":foreshowStatus,
                            "isHost": isHost,
                            "isOpenRemind": isOpenRemind,
                            "shortUrl": shortUrl,
                            "foreshowType":liveForeshow.foreshow_type //1互动直播 2会议直播，5直播系列
                        ]
                    }else{//直播中，回放，系列
                        rollImg = getReturnForeshow(map,liveForeshow)
                    }
                }
            }

            rollImg.put("title",it.title)
            rollImg.put("liveThumb",it.image)
            rollImg.put("viewType",20000)//轮播样式，安卓固定要求
            rollImgList << rollImg
        }

        return rollImgList
    }

    @Override
    def findUserLive(long userId, long toUserId, String appId) {
        def result = [:]
        def followType = isFollow(userId, toUserId, 1, appId);
        boolean isFollow =  followType > 0 //是否已关注
        def live = liveRes.getUserLiveRecord(appId, toUserId, isFollow)
        if (live){
            LiveRecord liveRecord = toLiveRecord(live);
            int watchCount = vestUserService.getLiveRoomWatchCount(liveRecord.liveId)
            result = [
                "liveId" : liveRecord.liveId,
                "title" : liveRecord.title,
                "foreshowId" : liveRecord.foreshowId,
                liveRoom: [
                    roomId: liveRecord.roomId,
                    chatId: liveRecord.chatId,
                    watchCount: watchCount
                ],
                anchorInfo: [
                    nickname: liveRecord.nickname,
                    userImage: liveRecord.userImage,
                    userId: liveRecord.userId
                ],
                "liveThumb" : liveRecord.liveThump ?: ImageUtils.getLiveListImg(liveRecord.userImage)
            ]
        }
        return result
    }

    @Override
    def valateLiveLimit(String appId){
        //1表示有观看权限，0表示无观看权限
        int res = 1
        try{
            int appWatcherCount = liveQcloudRedis.getAppWatcherCount(appId)?:0
            int appLimit = (qcloudLiveService.getQcloudInfo(appId)?.liveshareValue?:-1) as int
            log.info("liaojing valateLiveLimit appId=>{},appLimit=>{},appWatcherCount=>{}",appId,appLimit,appWatcherCount)
            if(appLimit < 0){//独立账号
                res = 1
            }else if(appLimit == 0){//无共享
                res = 0
            }else if(appWatcherCount >= appLimit){
                res = 0
            }else{
                res = 1
            }
        }catch (Exception e){
            log.info("valateLiveLimit error,appId=>{},Exception=>{}",appId,e.getMessage())
        }
        return res
    }

    @Override
    def addAppLimitInfo(long liveId,int count){
        try{
            //根据appId查询直播中信息
            LiveRecord liveRecord = this.findLiveByLiveId(liveId)
            if(!liveRecord){
                liveRecord = this.findLiveRecordByLiveId(liveId)
            }
            if(!liveRecord){
                log.info("liaojing addAppLimitInfo error,liveRecord is null,liveId=>{}",liveId)
                return
            }
            //设置appId的观众人数
            String appId = liveRecord.appId
            if(count == 0){
                int liveWatcher = liveQcloudRedis.getLiveWatchCount(liveRecord.liveId) as int //当前结束直播的观众人数
                if(liveWatcher){
                    count = -liveWatcher
                }
            }
            if(appId){
                int appWatcherCount = liveQcloudRedis.getAppWatcherCount(appId)?:0
                log.info("liaojing addAppLimitInfo start,liveId=>{},appId=>{},count=>{},appWatcherCount=>{}",liveId,appId,count,appWatcherCount)
                if(count < 0 && count < -appWatcherCount){
                    liveQcloudRedis.delAppWatcherCount(appId)
                }else{
                    liveQcloudRedis.addAppWatcherCount(appId,count)
                }
                appWatcherCount = liveQcloudRedis.getAppWatcherCount(appId)?:0
                log.info("liaojing addAppLimitInfo end,liveId=>{},appId=>{},count=>{},appWatcherCount=>{}",liveId,appId,count,appWatcherCount)
            }else{
                log.info("liaojing addAppLimitInfo error,appId is null,liveId=>{}",liveId)
            }
        }catch (Exception e){
            log.info("addAppLimitInfo error,liveId=>{},Exception=>{}",liveId,e.getMessage())
        }

    }

    @Override
    def getUserPaidList(long userId, String appId,int pageSize,String sortInfo) {
        def liveList = []
        try {
            if (!sortInfo || sortInfo=='{}') {//只在第一页查询缓存
                liveList = liveQcloudRedis.getUserPayList(userId)
            }
            if (!liveList){
                liveList = []
            }else {
                return liveList
            }
            def userInfo = getUserInfo(userId)
            def sort = Strings.parseJson(sortInfo)
            def ids = []//[1000994,1000944,1000943]
            def idsMap = [:]
            def url = "${userPaidListUrl}?userName=${Strings.getUrlEncode(userInfo?.userName,"UTF-8")}&lastId=${sort?.orderId?:0}&pageSize=${pageSize}&appId=${appId}"
            long start = System.currentTimeMillis()
            def result = Http.get(url)
            long end1 = System.currentTimeMillis()
            log.info("liaojing getUserPaidList url:${url},time1:${end1-start},result:${result}")
            def json = Strings.parseJson(result)
            if (json.head.code == 200){
                json.body.dataList.each{
                    def liveId = it.foreshowId as long //php提供的是liveId,名称有问题
                    def liveRecord = findLiveByLiveId(liveId)

                    if (!liveRecord){//先查看直播中是否存在，不存在去回放里找
                        liveRecord = findLiveRecordByLiveId(liveId)
                    }
                    if (liveRecord){
                        ids << liveRecord.foreshowId
                        idsMap.put(liveRecord.foreshowId as long,it.lastId)
                    }
                }
                def map = [
                    appId:appId,
                    userId:userId
                ]
                if (ids){
                    liveRes.getListByIds(ids).each {
                        def obj = getReturnForeshow(map,it)
                        obj.sortInfo.orderId = idsMap.get(obj.foreshowId as long)
                        liveList << obj
                    }

                    // 倒序排列
                    Collections.sort(liveList, new Comparator<Map>() {
                        public int compare(Map o1, Map o2) {
                            long m = o1.sortInfo.orderId as long
                            long n = o2.sortInfo.orderId as long
                            return n.compareTo(m);
                        }
                    });
                }
            }
            long end2 = System.currentTimeMillis()
            log.info("liaojing getUserPaidList url:${url},time2:${end2-end1},result:${result}")
        } catch (Exception e) {
            liveList = []
            e.printStackTrace()
            log.info("getUserPaidList exception:${e.getMessage()}")
        }
        if (!sortInfo || sortInfo=='{}') {//只在第一页设置缓存
            liveQcloudRedis.setUserPayList(userId, liveList)
        }
        return liveList
    }

    @Override
    def getLiveGoodsList(Map map) {
        def userInfo = getUserInfo(map.userId as long)
        String userName = userInfo.userName ?: ""
        def goodList = []
        if(!userName){
            log.info("getLiveGoodsList userName is null,userId=>{},appId=>{}",map.userId,map.appId)
            return goodList
        }
        def config = qcloudLiveService.getQcloudInfo(map.appId as String)
        String goodsListUrl = config.mallUrl
        if(!goodsListUrl){
            log.info("getLiveGoodsList goodsListUrl is null,userId=>{},appId=>{}",map.userId,map.appId)
            return goodList
        }
        goodsListUrl = goodsListUrl+"?username="+URLEncoder.encode(userName,'UTF-8')+"&psize="+(map.psize as int)+"&appName="+map.appId
        if(map.foreshowId){
            goodsListUrl += ("&liveId="+map.forehsowId)
        }else if(map.liveId){
            goodsListUrl += ("&liveId="+map.liveId)

        }
        def value = Http.get(goodsListUrl)
        if(value){
            def result = Strings.parseJson(value)?.result
            result?.each{
                goodList << [
                    "goodId": it.gd_id as int,
                    "goodName": it.gd_name,
                    "goodImg": URLDecoder.decode(it.gd_img as String,"utf-8"),
                    "goodPrice": Strings.getNumFormat(it.gd_price),
                    "mkPrice": Strings.getNumFormat(it.mk_price),        //市场价
                    "commentNum": it.comment_num as int,
                    "shopId": it.shop_id as int,
                    "productType": [
                        "productId": it.product_type?.pt_id as int,
                        "productName": it.product_type?.pt_name
                    ],
                    "shopType": it.shop_type as int,
                    "goodImgSmall": URLDecoder.decode(it.gd_img_small as String,"utf-8"),
                    "goodImgBig": URLDecoder.decode(it.gd_img_big as String,"utf-8"),
                    "goodUrl": URLDecoder.decode(it.gd_url as String,"utf-8")
                ]
            }
        }
        return goodList
    }

    @Override
    def getAppIdsByForeshow(Map params){
        return liveRes.getAppIdsByForeshow(params)
    }

    @Override
    def getAppIdsFromConfig(){
        return liveRes.getAppIdsFromConfig()
    }


    @Override
    def getForeshowNumberForSrpId(String srpId){
        return liveRes.getForeshowNumberForSrpId(srpId);
    }

    @Override
    def searchLiveListByKeyWord(long userId,String keyword, int psize, def sortInfo, String appId,String vc) {
        String lastId = DateUtil.getFormatDteByLong(sortInfo?.beginTime ?: 0)
        int isGroup = sortInfo?.isGroup ?sortInfo.isGroup as int: 0
        log.info("live.search.result.groovy,keyword:{},beginTime:{}",keyword,lastId)
        List liveList = []
        List liveList2 = []
        List groupList = []
        List groupList2 = []
        //共享分类的直播也要查询
        List liveCategroyList = getLiveCategroyList(userId, appId)
        def cateList = liveCategroyList?.subList(1,liveCategroyList.size())
        def cateIds = []
        if (cateList){
            cateList.each{
                cateIds << it.cateId
            }
        }
        Map map = [
            userId: userId,
            keyword: keyword,
            psize: psize+1,
            lastId: lastId,
            appId: appId,
            isGroup: isGroup,
            cateIds: cateIds,
            vc: vc
        ]
        if(isGroup){
            //查询系列
            map.isGroup=1
            groupList = liveRes.searchLiveListByKeyword(map)
        }else {
            //查询普通直播，如果数据不足一页，则查询系列的列表
            map.isGroup=0
            liveList = liveRes.searchLiveListByKeyword(map)
            if(!liveList || liveList?.size()<psize){
                map.isGroup=1
                map.lastId=""
                groupList = liveRes.searchLiveListByKeyword(map)
            }
        }
        liveList?.each{
            def resultMap = getReturnForeshow([appId:appId,userId: userId,vc:vc],it)
            resultMap.viewType = LiveCommon.VIEW_TYPE_SEARCH_FORESHOW_LIST
            int foreshowType = it.foreshow_type as int
            int invokeType= it.url_tag as int    //跳转类型
            if (it.foreshow_status as int==0){
                resultMap.invokeType= invokeType
                if(invokeType==24) {//24:贴子详情页用于互动直播
                    resultMap.blogId = it.url
                    resultMap.category = "interest"
                }else if(invokeType == 10){//10:新闻详情页用于互动直播
                    resultMap.category = "srp"
                }
                // 会议直播预告和付费会议直播预告,在非souyue中除H5以外统一跳转到房间
                if(!Strings.APP_NAME_SOUYUE.equals(appId) && invokeType != 120 && invokeType != 85){
                    resultMap.invokeType = LiveCommon.INVOKE_TYPE_LIVE_FORESHOW_START_AND_PAUSE
                }
            }
            resultMap.sortInfo = [
                "beginTime": it.begin_time,
                "isGroup": 0
            ]
//            //如果是sdk，且是会议直播则跳转类型改为跳直播间
//            if(invokeType < 10000 && !appId.equals(Strings.APP_NAME_SOUYUE) && foreshowType != LiveCommon.FORESHOW_TYPE_1){
//                resultMap.invokeType = LiveCommon.INVOKE_TYPE_LIVE_FORESHOW_START_AND_PAUSE
//            }
            liveList2 << resultMap
        }
        groupList?.each{
            def resultMap = getReturnForeshow([appId:appId,userId: userId,vc:vc],it)
            resultMap.description = it.description ?: ""
            resultMap.sortInfo = [
                "beginTime": it.begin_time,
                "isGroup": 1
            ]
            resultMap.liveThumb = ImageUtils.getSearchLiveListImg(resultMap.liveThumb)
            groupList2 << resultMap
        }

        boolean hasMore = false

        if((liveList2?.size()?: 0)+(groupList2?.size()?: 0)>=psize){
            hasMore = true
            if(liveList2?.size() >psize){
                liveList2 = liveList2.subList(0,psize)
            }
            if((groupList2?.size()?: 0)){
                groupList2= groupList2.subList(0,psize-(liveList2?.size()?: 0))
            }
        }
        return [liveList:liveList2,groupList:groupList2,hasMore:hasMore]
    }

    @Override
    def checkLiveAccessByUserId(long userId, String appId) {
        List appIds = Strings.splitToList(liveAccessAppIds,",")
        if(!appIds.contains(appId)){
            return 1
        }
        def value = Http.post(liveAccessUrl, [userid:userId,appName:appId])
        log.info("wangtf check live access userId:{},appId:{},value:{}",userId,appId,value)
        int code = 0
        if(value){
            code = (Strings.parseJson(value)?.code ?: 0)as int
        }
        return code
    }

    def fansSyncData(def map){
        Parallel.run([1],{
            System.out.println(map)
            String appId = map.appId?:""
            List appIds = Strings.splitToList(liveFansSyncAppIds,",")
            if(!appIds.contains(appId)){
                return
            }
            int operType = "add".equals(map.operType as String)?1:2
            def res = Http.post(liveFansSyncUrl, [userId:map.userId as long,starUid:map.toUserId as long,operType:operType,appId:appId])
            log.info("fansSyncData res:{}",res)
        },1)
    }

    @Override
    def getSnapshotUrl(Map map) {
        String fileId = map.fileId
        String appId = map.appId
        def qcloudInfo = qcloudLiveService.getQcloudInfo(appId)
        if(!qcloudInfo){
            log.info("pullEvent qcloudInfo is null,appId=>{}",appId)
            return null
        }
        String secretId = qcloudInfo.secretId
        String secretKey = qcloudInfo.secretKey
        def data = QcloudLiveCommon.createSnapshotByTimeOffset(secretId,secretKey,fileId)
        log.info("wangtf createSnapshotByTimeOffset,data=>{}",data)
        String taskId = Strings.parseJson(data)?.vodTaskId
        if(taskId){
            //将taskId和liveId的对应关系存储起来，以便更新截图的时候使用
            liveQcloudRedis.hsetSnapshotRedis(taskId, "liveId", map.liveId as String)
            Parallel.run([1],{
                def liveEvent = [
                    taskId:taskId,
                    appId:appId
                ]
                qcloudLiveRes.addLiveEvent(liveEvent)
            },1)
        }
        return taskId
    }

    @Override
    def updateLiveThumpByLiveId(long liveId,def Object data) {
        List picInfo = data.picInfo ?: []
        if(!liveId || !picInfo){
            log.info("直播截图获取异常，liveId:{},picInfo:{}",liveId,picInfo)
            return null
        }
        picInfo.each{
            int status = it.status as int
            if(status == 0){    //截图获取成功
                String liveThump = it.url as String
                log.info("wangtf updateLiveThumpByLiveId liveId:{},liveThump:{}",liveId,liveThump)
                //更新数据库
                return liveRes.updateLiveThumpByLiveId(liveId,liveThump)
            }
        }
    }

    @Override
    def scanForeshowDataForMistake() {
        List foreshowList = liveRes.getAllForeshowList()
        List resultList = []
        foreshowList?.each{
            String liveRecordInfo = it.live_record_info
            try{
                def videoAddress = liveRecordInfo ? Strings.parseJson(liveRecordInfo as String).video_address : ""
                JSON.parse(videoAddress)
            }catch (Exception e){
                log.info("wangtf live_foreshow表直播地址错误的数据，data:{}", it)
                resultList << it
            }
        }
        return resultList
    }

    @Override
    def collectLive(long userId, long foreshowId,int type,String appId) {
        def liveCollection = liveRes.findCollectLiveByuserIdAndForeshowId(userId,foreshowId,appId)
        def result
        if(type == 1){//收藏直播
            if(!liveCollection){
                result = liveRes.collectLive(userId,foreshowId,appId)
            }else {
                result = liveRes.updateCollectLive(userId,foreshowId,0,appId)
            }
        }else if (type == 2){//取消收藏
            if(liveCollection && liveCollection.status as int == 0){
                result = liveRes.updateCollectLive(userId,foreshowId,1,appId)
            }
        }
        if(result){
            return 1
        }
        return 0
    }

    @Override
    def getLiveCollectionListByUserId(Map map, String appId) {
        def result = liveRes.findLiveCollectionListByUserId(map, appId)
        def resultList = []
        result?.each{
            long foreshowId = it.foreshow_id as long
            def liveForeshow = liveRes.findLiveForeshowByForeshowId(foreshowId,appId)
            def returnForehsow = getReturnForeshow(map,liveForeshow)
            returnForehsow.viewType = LiveCommon.VIEW_TYPE_COLLECTION_FORESHOW_LIST
            returnForehsow.sortInfo = [createTime: it.create_time]
            resultList << returnForehsow
        }
        return resultList
    }

    @Override
    boolean isCollectLive(long userId, long foreshowId, String appId) {
        def liveCollection = liveRes.findCollectLiveByuserIdAndForeshowId(userId,foreshowId,appId)
        if(liveCollection && liveCollection.status as int == 0){
            return true
        }
        return false
    }

    @Override
    int updateLiveRecommend(int type, long forehsowId, long liveId,int operType) {
        int status = 0
        int result = 0
        if (type == 1){ //推荐
           status = 1
        }else if (type == 2){   //取消推荐
            status = 0
        }
        if (forehsowId){
            result = liveRes.updateLiveRecommendStatus("live_foreshow",forehsowId,liveId, status,operType)
        }else if (liveId){
            LiveRecord liveRecord = findLiveByLiveId(liveId)
            if(liveRecord){
                result = liveRes.updateLiveRecommendStatus("live_record",forehsowId,liveId, status,operType)
                if(result){ //更新正在直播中的直播列表缓存
                    liveRecord = this.toLiveRecord(liveRes.findLiveByLiveId(liveId))
                    if(liveRecord && liveRecord.liveId){
                        liveQcloudRedis.updateLiveList(liveRecord)
                    }
                }
            }else {
                result = liveRes.updateLiveRecommendStatus("live_record_log",forehsowId,liveId, status,operType)
            }
        }
        return result
    }

    @Override
    def getForehsowInfoByForeshowId(long foreshowId, Map map) {
        def liveForeshow = liveForeshowRes.getLiveForeshowInfo(foreshowId);
        if (liveForeshow){
            return [liveForeshow:getReturnForeshow(map, liveForeshow)]
        }
        return [liveForehsow:[pgcStatus:3,msg:"直播不存在"]]
    }

    def delForeshowQcloud(Long foreshowId,String appId){
        try {
            def liveForeshow = liveRes.findLiveForeshowByForeshowId(foreshowId,appId)
            if(!liveForeshow){
                return
            }
            def liveRecordInfo =  Strings.parseJson(liveForeshow.live_record_info)
            def videoAddress = liveRecordInfo ? liveRecordInfo.video_address : ""
            log.info("liaojing delLiveForeshow qcloud,foreshowId=>{},videoAddress=>{}",foreshowId,videoAddress)
            List list = null
            if(videoAddress){
                list = getVideoAddressUrlList(videoAddress, [:])?.fileId?:[]
            }
            List fileIdList = []
            list.each {
                fileIdList.addAll(Strings.splitToList(it as String))
            }
            log.info("liaojing delLiveForeshow qcloud,foreshowId=>{},fileIdList=>{},size=>{}",foreshowId,fileIdList,fileIdList.size())
            if(!fileIdList){
                return
            }
            //异步删除腾讯云,参数fileId，格式list
            Parallel.run([1], {
                def res = qcloudLiveService.deleteVodFile(fileIdList,appId)
                log.info("liaojing delLiveForeshow qcloud,foreshowId=>{},appId=>{},res=>{},",foreshowId,appId,res)
            }, 1)
        }catch (Exception e){
            e.printStackTrace()
        }
    }

    def getAppName(String appId){
        //调用云平台接口
        String appName = appId
        try {
            long time = System.currentTimeMillis()
            String sign = Strings.md5(appId+time+liveGetAppNameSignkey)
            String res = Http.post(liveGetAppNameUrl,[app_name:appId,sy:1,time:time,sign:sign])
            def json = Strings.parseJson(res)
            //{"head":{"stat":200,"msg":"success"},"body":{"souyue":"搜悦"}}
            if(json.body){
                appName =  json.body."${appId}"
            }
        }catch (Exception e){
            e.printStackTrace()
        }
        return appName
    }
}
