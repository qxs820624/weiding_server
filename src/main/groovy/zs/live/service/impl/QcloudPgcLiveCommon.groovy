package zs.live.service.impl

import com.qcloud.Module.Live
import com.qcloud.Module.Vod
import com.qcloud.QcloudApiModuleCenter
import groovy.util.logging.Slf4j

/**
 * Created by Administrator on 2016/12/13.
 */
@Slf4j
class QcloudPgcLiveCommon {

    /**
     * 批量停止直播频道
     */
    public static String createLVBChannel(String secretId,String secretKey,long liveId,int roomId,String liveEnv){
        String back="";
        /* 如果是循环调用下面举例的接口，需要从此处开始你的循环语句。切记！ */
        TreeMap<String, Object> config = new TreeMap<String, Object>();
        config.put("SecretId", secretId);
        config.put("SecretKey", secretKey);
        /* 请求方法类型 POST、GET */
        config.put("RequestMethod", "GET");
        /* 区域参数，可选: gz:广州; sh:上海; hk:香港; ca:北美;等。 */
        config.put("DefaultRegion", "gz");
        QcloudApiModuleCenter module = new QcloudApiModuleCenter(new Live(), config);
        TreeMap<String, Object> params = new TreeMap<String, Object>();
        if("test".equals(liveEnv)){
            params.put("channelDescribe", "测试");
            params.put("channelName", "meeting_test_"+liveId+"_"+roomId);
        }else if("pre".equals(liveEnv)){
            params.put("channelDescribe", "预上线");
            params.put("channelName", "meeting_pre_"+liveId+"_"+roomId);
        }else{
            params.put("channelDescribe", "线上");
            params.put("channelName", "meeting_online_"+liveId+"_"+roomId);
        }
		params.put("outputSourceType", 3);
		params.put("sourceList.1.name", "video-1999");
		params.put("sourceList.1.type", 1);
        params.put("outputRate.0", 0);
        params.put("outputRate.1", 10);
        params.put("outputRate.2", 20);

        try {
            back = module.call("CreateLVBChannel", params);
        } catch (Exception e) {
            log.info("CreateLVBChannel：" + e.getMessage());
        }
        log.info("liveId==>{}，创建频道返回：{}",liveId,back)
        return back

    }

    /**
     * 批量停止直播频道
     */
    public static String getVodRecordFiles(String secretId,String secretKey,long liveId,String liveEnv,String channelId,String beginTime){
        String back="";
        /* 如果是循环调用下面举例的接口，需要从此处开始你的循环语句。切记！ */
        TreeMap<String, Object> config = new TreeMap<String, Object>();
        config.put("SecretId", secretId);
        config.put("SecretKey", secretKey);
        /* 请求方法类型 POST、GET */
        config.put("RequestMethod", "GET");
        /* 区域参数，可选: gz:广州; sh:上海; hk:香港; ca:北美;等。 */
        config.put("DefaultRegion", "gz");
        QcloudApiModuleCenter module = new QcloudApiModuleCenter(new Live(), config);
        TreeMap<String, Object> params = new TreeMap<String, Object>();
        params.put("channelId", channelId);
        params.put("startTime", beginTime);

        try {
            back = module.call("GetVodRecordFiles", params);
        } catch (Exception e) {
            log.info("GetVodRecordFiles：" + e.getMessage());
        }
        log.info("liveId==>{}，会议直播获取回看返回：{}",liveId,back)
        return back

    }
}
