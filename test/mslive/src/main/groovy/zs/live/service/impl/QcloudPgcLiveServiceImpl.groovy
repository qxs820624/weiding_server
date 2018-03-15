package zs.live.service.impl

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import zs.live.ApiException
import zs.live.common.QcloudCommon
import zs.live.dao.mysql.QcloudLiveRes
import zs.live.entity.LiveRecord
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.service.QcloudPgcLiveService
import zs.live.utils.DateUtil
import zs.live.utils.QcloudBizidUtil
import zs.live.utils.Strings
import zs.live.utils.VerUtils

/**
 * Created by Administrator on 2016/12/13.
 */
@Service
@Slf4j
class QcloudPgcLiveServiceImpl implements QcloudPgcLiveService{

    @Value('${live.env}')
    String liveEnv
    @Value('${live.defalut.srpid}')
    String liveDefalutSrpId
    @Value('${live.defalut.keyword}')
    String liveDefalutKeyword

    @Autowired
    QcloudLiveRes qcloudLiveRes;
    @Autowired
    LiveService liveService
    @Autowired
    QcloudLiveService qcloudLiveService

    @Override
    Map fillPgcQcloudParams(Map params,long liveId,int roomId) {
 //       if(VerUtils.toIntVer("1.5") <VerUtils.toIntVer(params.vc)){
            def config = qcloudLiveService.getQcloudInfo(Strings.getAppId(params))
            log.info("qcloudInfo====>"+Strings.toJson(config))
            int bizid = config.bizid as int
            String key = config.pushKey
            //不同前缀取值
            String stream_id=getStreamId(bizid,liveId,roomId)
            long beginTime = params.beginTime as long
            long txTime = beginTime/1000 + 100*60*60*24 //开始时间的 一百天以后
            String lastString = QcloudBizidUtil.getSafeUrl(key, stream_id, txTime)
           // System.out.println(getSafeUrl(key, stream_id, txTime));
            String rtmpUrl = """rtmp://"""+bizid+"""."""+QcloudCommon.LIVE_PUSH_URL + stream_id+"""?bizid="""+bizid+"""&"""+lastString;
            String m3u8Url = """http://"""+bizid+"""."""+QcloudCommon.LIVE_PLAY_URL + stream_id+""".m3u8"""
            String playUrl = """http://"""+bizid+"""."""+QcloudCommon.LIVE_PLAY_URL + stream_id+""".flv"""

            params.put("rtmpUrl",rtmpUrl)
            params.put("m3u8Url",m3u8Url)
            params.put("playUrl",playUrl)
            log.info("rtmpUrl========>"+rtmpUrl)
            log.info("m3u8Url========>"+m3u8Url)
            log.info("playUrl========>"+playUrl)
//        }else{
//            String appId = Strings.getAppId(params)
//            def qcloudInfo = qcloudLiveService.getQcloudInfo(appId)
//            String secretId = qcloudInfo.secretId
//            String secretKey = qcloudInfo.secretKey
//            //请求腾讯云创建频道
//            String createString = QcloudPgcLiveCommon.createLVBChannel(secretId,secretKey,liveId,roomId,liveEnv)
//            def createObj = Strings.parseJson(createString)
//            if(!createObj || createObj?.code != 0 ){
//                String  msg = "腾讯云创建频道失败，请稍后重试";
//                throw new ApiException(700, msg);
//            }
//            String rtmpUrl = createObj?.channelInfo?.upstream_address+"&record=mp4"   //推流地址
//            String channelId = createObj?.channel_id
//            params.channelId = channelId
////        if(playUrl){
////            playUrl = playUrl.get(0)
////        }
////        if(m3u8Url){
////            m3u8Url = m3u8Url.get(0)
////        }
//            def array = createObj?.channelInfo?.downstream_address
//            params.playUrl = array?.get(0)?.flv_downstream_address
//            params.m3u8Url = array?.get(0)?.hls_downstream_address
//            params.flvStandard = array?.get(1)?.flv_downstream_address  //标清
//            params.m3u8Standard =  array?.get(1)?.hls_downstream_address //标清
//            params.flvHigh = array?.get(2)?.flv_downstream_address      //高清
//            params.m3u8High = array?.get(2)?.hls_downstream_address       //高清
//            //组织直播数剧
//            params.put("rtmpUrl",rtmpUrl)
//        }

        return params
    }


    String updateBackVideoAddressByQcloud(LiveRecord live){
        log.info("环境变量liveEnv=={}",liveEnv)
        def qcloudInfo = qcloudLiveService.getQcloudInfo(live.appId)
        String secretId = qcloudInfo.secretId
        String secretKey = qcloudInfo.secretKey
        //回看需要取
        String prefix;
        //不同前缀取值
        if("test".equals(liveEnv)){
            prefix = "meeting_test_"+live.liveId+"_"+live.roomId
        }else if("pre".equals(liveEnv)){
            prefix = "meeting_pre_"+live.liveId+"_"+live.roomId
        }else{
            prefix = "meeting_online_"+live.liveId+"_"+live.roomId
        }
        String videoAddress = QcloudLiveCommon.getVodPlayListInfo(secretId,secretKey,live.liveId,live.roomId,prefix)
        log.info("通过前缀prefix =={},videoAddress=={}",prefix,videoAddress)
        if(!videoAddress){
            //不同前缀取值
            if("test".equals(liveEnv)){
                prefix = "livemeeting_test_"+live.liveId+"_"+live.roomId
            }else if("pre".equals(liveEnv)){
                prefix = "livemeeting_pre_"+live.liveId+"_"+live.roomId
            }else{
                prefix = "livemeeting_online_"+live.liveId+"_"+live.roomId
            }
            videoAddress = QcloudLiveCommon.getVodPlayListInfo(secretId,secretKey,live.liveId,live.roomId,prefix)
            log.info("通过前缀prefix =={},videoAddress=={}",prefix,videoAddress)
        }
        return videoAddress
    }

    @Override
    void stopQcloudChannel(LiveRecord live,String msg) {
        def qcloudInfo = qcloudLiveService.getQcloudInfo(live.appId)
        String secretId = qcloudInfo.secretId
        String secretKey = qcloudInfo.secretKey

        String stopMes = QcloudLiveCommon.stopLVBChannel(secretId,secretKey,live.channelId)
        log.info("流程{},liveId={},foreshowId={},roomId={},结束频道返回值={}",msg,live.liveId,live.foreshowId,live.roomId,stopMes);
        String delMes = QcloudLiveCommon.deleteLVBChannel(secretId,secretKey,live.channelId)
        log.info("流程{},liveId={},foreshowId={},roomId={}，删除频道返回值={}",msg,live.liveId,live.foreshowId,live.roomId,delMes);

    }

    @Override
    void startQcloudChannel(LiveRecord live) {
        def qcloudInfo = qcloudLiveService.getQcloudInfo(live.appId)
        String secretId = qcloudInfo.secretId
        String secretKey = qcloudInfo.secretKey

        QcloudLiveCommon.startLVBChannel(secretId,secretKey,live.channelId)

    }

    @Override
    void pauseQcloudChannel(LiveRecord live){
        def qcloudInfo = qcloudLiveService.getQcloudInfo(live.appId)
        String secretId = qcloudInfo.secretId
        String secretKey = qcloudInfo.secretKey

        QcloudLiveCommon.stopLVBChannel(secretId,secretKey,live.channelId)
    }

    @Override
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

    @Override
    def startLiveChannel(LiveRecord live){
        def qcloudInfo = qcloudLiveService.getQcloudInfo(live.appId)
        String appId = qcloudInfo.qcloudAppid
        String channelId = getStreamId(qcloudInfo.bizid as int,live.liveId,live.roomId)   //直播码
        int status = 1         //0:关闭； 1:开启
        String key = qcloudInfo.apiKey
        long t = DateUtil.addDateByMinute(new Date(),1).getTime()/1000  //1分钟失效
        QcloudLiveCommon.liveChannelSetStatus(appId,channelId,status,key,t)
    }

    @Override
    def stopLiveChannel(LiveRecord live){
        def qcloudInfo = qcloudLiveService.getQcloudInfo(live.appId)
        String appId = qcloudInfo.qcloudAppid
        String channelId = getStreamId(qcloudInfo.bizid as int,live.liveId,live.roomId)   //直播码
        int status = 0         //0:关闭； 1:开启
        String key = qcloudInfo.apiKey
        long t = DateUtil.addDateByMinute(new Date(),1).getTime()/1000  //1分钟失效
        QcloudLiveCommon.liveChannelSetStatus(appId,channelId,status,key,t)
    }
}
