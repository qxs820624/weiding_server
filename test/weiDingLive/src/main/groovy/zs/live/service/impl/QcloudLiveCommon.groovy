package zs.live.service.impl

import com.alibaba.fastjson.JSON
import com.google.gson.Gson
import com.qcloud.Module.Live
import com.qcloud.Module.Vod
import com.qcloud.QcloudApiModuleCenter
import com.qcloud.Utilities.Json.JSONArray
import com.qcloud.Utilities.Json.JSONObject
import com.qcloud.Utilities.SHA1
import com.qcloud.VodCall
import groovy.util.logging.Slf4j
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import zs.live.common.QcloudCommon
import zs.live.utils.DateUtil
import zs.live.utils.Http
import zs.live.utils.HttpQcould
import zs.live.utils.Strings

import java.text.SimpleDateFormat

/**
 * Created by Administrator on 2016/10/12.
 */
@Slf4j
@Component
class QcloudLiveCommon {
    public static final Logger timerLog = LoggerFactory.getLogger("timerLog");

    private final static int QCLOUD_RETRY_COUNT = 3
    /**
     * 直播推流接口
     * @param liveId
     * @param roomId
     * @param sdkappId
     * @param userId
     * @param signId
     * @return
     * @throws Exception
     */
    public static String tuiliuBegin(long liveId,int roomId,long sdkappId,long userId,String signId,String liveEnv) throws Exception{

        def tuiliuString;
        //增加推流重试3次机制
        for(int i=0;i< QCLOUD_RETRY_COUNT; i++){

            Map reqHeadMap = new HashMap();
            reqHeadMap.put("uint32_sub_cmd", 6);
            reqHeadMap.put("uint32_seq", 123);
            reqHeadMap.put("uint32_auth_key", roomId);
            reqHeadMap.put("uint32_sdk_appid", sdkappId);
            reqHeadMap.put("rpt_to_Account", [userId.toString()]);
            reqHeadMap.put("bytes_cookie_buff", "5655");


            Map req0x6Map = new HashMap();
            req0x6Map.put("uint32_oper", 1);
            req0x6Map.put("uint32_live_code", 1);
            req0x6Map.put("uint32_sdk_type", 1);
            req0x6Map.put("str_channel_name", roomId+"_"+liveId);
            if("test".equals(liveEnv)){
                req0x6Map.put("str_channel_describe", "测试");
            }else if("pre".equals(liveEnv)){
                req0x6Map.put("str_channel_describe", "预上线");
            }else{
                req0x6Map.put("str_channel_describe", "线上");
            }

            req0x6Map.put("uint32_push_data_type", 0);
            req0x6Map.put("uint32_record_type", 0);  //不需要录制，因为该录制没有转码，需要单独调用互动直播的录制接口进行录制工作，
            Map reqbodyMap = new HashMap();
            reqbodyMap.put("req_0x6", req0x6Map);

            Map param = new LinkedHashMap<>();
            param.put("reqhead", reqHeadMap);
            param.put("reqbody", reqbodyMap);

            String json = new Gson().toJson(param);
            String url = "https://yun.tim.qq.com/v3/openim/videorelay";
            Map m= new HashMap();
            m.put("usersig", signId);
            m.put("Identifier", userId);
            m.put("sdkappId", sdkappId);
            m.put("contenttype", "json");
            m.put("apn", "1");
            tuiliuString = HttpQcould.post(m, url,json);
            log.info("liveId = {},推流第{}次，返回参数{}",liveId,i+1,tuiliuString)
            def tuiliu = Strings.parseJson(tuiliuString)
            String actionStatus = tuiliu?.ActionStatus
            String err = tuiliu?.rspbody?.rsp_0x6?.str_errorinfo
            int errCode = tuiliu?.rspbody?.rsp_0x6?.uint32_result? tuiliu?.rspbody?.rsp_0x6?.uint32_result as int:0
            if(StringUtils.isBlank(actionStatus) && (StringUtils.isBlank(err) || errCode == QcloudCommon.QCLOUD_NOROOM_CODE)){
                break
            }
            Thread.sleep(2*1000) //如果发生推流错误（可能是腾讯那边网络有问题） 休眠2秒 再发第二次
        }
        return tuiliuString
    }

    /**
     * 开始录制接口
     * @param liveId
     * @param roomId
     * @param sdkappId
     * @param userId
     * @param signId
     * @return
     */
    public static String luzhiBegin(long liveId,int roomId,long sdkappId,long userId,String signId){
        Map reqHeadMap = new HashMap();
        reqHeadMap.put("uint32_sub_cmd", 5);
        reqHeadMap.put("uint32_seq", 123);
        reqHeadMap.put("uint32_auth_key", roomId);
        reqHeadMap.put("uint32_sdk_appid", sdkappId);
        reqHeadMap.put("rpt_to_Account", [userId.toString()]);
        reqHeadMap.put("bytes_cookie_buff", "5655");


        Map req0x5Map = new HashMap();
        req0x5Map.put("uint32_oper", 1);
        req0x5Map.put("string_file_name", roomId+"_"+liveId);
        req0x5Map.put("uint32_IsTransCode", 1);
        req0x5Map.put("uint32_sdk_type", 1);
        req0x5Map.put("uint32_record_data_type", 0);
        Map reqbodyMap = new HashMap();
        reqbodyMap.put("req_0x5", req0x5Map);

        //Map param = new HashMap();
        Map param = new LinkedHashMap<>();
        param.put("reqhead", reqHeadMap);
        param.put("reqbody", reqbodyMap);

        String json = new Gson().toJson(param);
        System.out.println("json:"+json);

        String url = "https://yun.tim.qq.com/v3/openim/videorelay";
        Map<String,String> m= new HashMap();
        m.put("Identifier", userId);
        m.put("sdkappId", sdkappId);
        m.put("contenttype", "json");
        m.put("usersig", signId);
        String luzhiString = HttpQcould.post(m, url,json);
        log.info("liveId:"+liveId+",录制开始返回值："+luzhiString)
    }

    /**
     * 结束推流
     * @param liveId
     * @param roomId
     * @param sdkappId
     * @param userId
     * @param signId
     * @return
     */
    public static String tuiliuEnd(long liveId,int roomId,long sdkappId,long userId,String signId){
        Map reqHeadMap = new HashMap();
        reqHeadMap.put("uint32_sub_cmd", 6);
        reqHeadMap.put("uint32_seq", 123);
        reqHeadMap.put("uint32_auth_key", roomId);
        reqHeadMap.put("uint32_sdk_appid", sdkappId);
        reqHeadMap.put("rpt_to_Account", [userId.toString()]);
        reqHeadMap.put("bytes_cookie_buff", "5655");


        Map req0x6Map = new HashMap();
        req0x6Map.put("uint32_oper", 2);
        req0x6Map.put("uint32_live_code", 1);
        req0x6Map.put("uint32_sdk_type", 1);
        req0x6Map.put("str_channel_name", roomId+"_"+liveId);
        req0x6Map.put("str_channel_describe", "sdk test");
        req0x6Map.put("uint32_push_data_type", 0);
        Map reqbodyMap = new HashMap();
        reqbodyMap.put("req_0x6", req0x6Map);

        Map param = new LinkedHashMap<>();
        param.put("reqhead", reqHeadMap);
        param.put("reqbody", reqbodyMap);

        String json = new Gson().toJson(param);
        System.out.println("json:"+json);
        String url = "https://yun.tim.qq.com/v3/openim/videorelay";
        Map m= new HashMap();
        m.put("usersig", signId);
        m.put("Identifier", userId);
        m.put("sdkappId", sdkappId);
        m.put("contenttype", "json");
        m.put("apn", "1");
        String tuiliuEndString = HttpQcould.post(m, url,json);

        return tuiliuEndString
    }

    public static String luzhiEnd(long liveId,int roomId,long sdkappId,long userId,String signId){
        Map reqHeadMap = new HashMap();
        reqHeadMap.put("uint32_sub_cmd", 5);
        reqHeadMap.put("uint32_seq", 123);
        reqHeadMap.put("uint32_auth_key", roomId);
        reqHeadMap.put("uint32_sdk_appid", sdkappId);
        reqHeadMap.put("rpt_to_Account", [userId.toString()]);
        reqHeadMap.put("bytes_cookie_buff", "5655");


        Map req0x5Map = new HashMap();
        req0x5Map.put("uint32_oper", 2);
        req0x5Map.put("string_file_name", roomId+"_"+liveId);
        req0x5Map.put("uint32_sdk_type", 1);
        req0x5Map.put("uint32_record_data_type", 0);
        Map reqbodyMap = new HashMap();
        reqbodyMap.put("req_0x5", req0x5Map);

        //Map param = new HashMap();
        Map param = new LinkedHashMap<>();
        param.put("reqhead", reqHeadMap);
        param.put("reqbody", reqbodyMap);

        String json = new Gson().toJson(param);

        String url = "https://yun.tim.qq.com/v3/openim/videorelay";
        Map<String,String> m= new HashMap();
        m.put("Identifier", userId);
        m.put("sdkappId", sdkappId);
        m.put("contenttype", "json");
        m.put("usersig", signId);
        String luzhiEndString = HttpQcould.post(m, url,json);
        return luzhiEndString
    }

    public static String createIMGroup(int roomId,String signId,long userId,long sdkappId,String liveAdminIdentifier){
        log.info("enter createIMGroup roomId===>{}",roomId)
        String postString = "";
        for(int i=0;i< QCLOUD_RETRY_COUNT; i++) {
            try {
                String url = "https://console.tim.qq.com/v4/group_open_http_svc/create_group";
                Map params = new HashMap();
                //params.put("Owner_Account", userId.toString());
                params.put("Type", "AVChatRoom");
                params.put("GroupId", roomId.toString());
                params.put("Name", roomId.toString());
                String json = new Gson().toJson(params);
                Map m = new HashMap();
                m.put("usersig", signId);
                m.put("Identifier", liveAdminIdentifier);
                m.put("sdkappId", sdkappId);
                m.put("contenttype", "json");
                m.put("apn", "1");
                postString = HttpQcould.execute(m, url, json,"POST");
                if (postString) {
                    def postObj = Strings.parseJson(postString)
                    if (postObj.ActionStatus && "OK".equals(postObj.ActionStatus)) {
                        break;
                    }
                    log.info("retry createIMGroup roomId===>{},createIMGroup postString===>:{}",roomId, postString)
                }
            }catch (Exception e){
                e.printStackTrace()
            }
        }
        log.info("successfully createIMGroup roomId===>{},createIMGroup postString===>:{}",roomId, postString)
        return postString
    }

    public static String importIMAccount(int roomId,String signId,long userId,long sdkappId,String liveAdminIdentifier){
        log.info("enter importIMAccount roomId=>{},userId=>{}",roomId,userId)
        String postString = "";
        for(int i=0;i< QCLOUD_RETRY_COUNT; i++) {
            try {
                String url = "https://console.tim.qq.com/v4/im_open_login_svc/account_import";
                Map params = new HashMap();
                params.put("Identifier", userId.toString());
                params.put("Nick", userId.toString());
                params.put("FaceUrl", "");
                String json = new Gson().toJson(params);
                Map m = new HashMap();
                m.put("usersig", signId);
                m.put("Identifier", liveAdminIdentifier);
                m.put("sdkappId", sdkappId);
                m.put("contenttype", "json");
                m.put("apn", "1");
                postString = HttpQcould.execute(m, url, json,"POST");
                if (postString) {
                    def postObj = Strings.parseJson(postString)
                    if (postObj.ActionStatus && "OK".equals(postObj.ActionStatus)) {
                        break;
                    }
                    log.info("retry importIMAccount roomId=>{},userId=>{},importIMAccount postString===>:{}",roomId,userId,postString)
                }
            }catch (Exception e){
                e.printStackTrace()
            }
        }
        log.info("successfully importIMAccount roomId=>{},userId=>{},importIMAccount postString===>:{}",roomId,userId,postString)
        return postString
    }

    public static String destroyIMGroup(int roomId,String signId,String userId,long sdkappId){
        String postString
        try{
            String url = "https://console.tim.qq.com/v4/group_open_http_svc/destroy_group";
            Map params = new HashMap();
            params.put("GroupId", roomId.toString());
            String json = new Gson().toJson(params);
            Map m= new HashMap();
            m.put("usersig", signId);
            m.put("Identifier", userId);
            m.put("sdkappId", sdkappId);
            m.put("contenttype", "json");
            postString = HttpQcould.post(m, url,json);
        }catch (Exception e){
            e.printStackTrace()
            log.info("destroyIMGroup 强制解散房间失败:",e.getMessage())
        }
        log.info("roomId:"+roomId+"强制解散房间返回:"+ postString)
        return postString
    }
    public static String sendIMMsg(int roomId, String signId,long sdkappId,String liveAdminIdentifier,long fromAccount,int userAction,def actionParam){
        long startTime = System.currentTimeMillis()
        String url = "https://console.tim.qq.com/v4/group_open_http_svc/send_group_msg"
        Random random = new Random()
        int rand = random.nextInt(65535)
        Map urlParamMap = [
            usersig: signId,
            sdkappid: sdkappId,
            identifier: liveAdminIdentifier,
            contenttype: "json",
            random: rand
        ]
        Map params = new HashMap();
        def data = [
            userAction: userAction,
            actionParam: actionParam
        ]
        List msgBody = [
            [
                MsgType: "TIMCustomElem", // 文本
                MsgContent: [
                    Data: JSON.toJSONString(data),
                    Desc: "这是一条消息数据",
                    Ext: "",
                    Sound: ""
                ]
            ]
        ]
        params = [
            GroupId: roomId.toString(),
            From_Account: fromAccount.toString(), //指定消息发送者（选填）
            Random: rand, // 随机数字，五分钟数字相同认为是重复消息
            MsgBody: msgBody
        ]
        String json = new Gson().toJson(params);
        String postString = HttpQcould.execute(urlParamMap, url,json,"POST");
        //log.info("wangtf 请求腾讯云发送IM消息所需时间：{}",System.currentTimeMillis()-startTime)
        return postString
    }
    public static String getGroupMemberInfo(int roomId,int offset,String signId,long sdkappId,String liveAdminIdentifier){
            String url = "https://console.tim.qq.com/v4/group_open_http_svc/get_group_member_info"
            Random random = new Random()
            int rand = random.nextInt(65535)
            Map urlParamMap = [
                usersig: signId,
                sdkappid: sdkappId,
                identifier: liveAdminIdentifier,
                contenttype: "json",
                random: rand
            ]
            Map params = new HashMap();
            params = [
                GroupId: roomId.toString(),
                Limit: 100,   // 最多获取多少个成员的资料
                Offset: offset   // 从第多少个成员开始获取
            ]
            String json = new Gson().toJson(params);
            String postString = HttpQcould.execute(urlParamMap, url,json,"POST");
            return postString
    }

    /**
     * 将某个成员禁言
     * @param roomId
     * @param signId
     * @param sdkappId
     * @param liveAdminIdentifier
     * @param hostUid
     * @param forbidUserId
     * @return
     */
    public static String forbidComment(int roomId, String signId,long sdkappId,String liveAdminIdentifier,long forbidUserId){
        String url = "https://console.tim.qq.com/v4/group_open_http_svc/forbid_send_msg"
        Random random = new Random()
        int rand = random.nextInt(65535)
        Map urlParamMap = [
            usersig: signId,
            sdkappid: sdkappId,
            identifier: liveAdminIdentifier,
            contenttype: "json",
            random: rand
        ]
        Map params = new HashMap();
        params = [
            GroupId: roomId.toString(),
            Members_Account: [  // 最多支持500个
                forbidUserId as String
            ],
            ShutUpTime: Integer.MAX_VALUE  // 禁言时间，单位为秒
        ]
        String json = new Gson().toJson(params);
        String postString = HttpQcould.execute(urlParamMap, url,json,"POST");
        return postString
    }
    /**
     *
     * @param roomId
     * @param signId
     * @param sdkappId
     * @param liveAdminIdentifier
     * @param kickUserId
     * @return
     */
    public static String kickGroupMember(int roomId, String signId,long sdkappId,String liveAdminIdentifier,long kickUserId){
        String url = "https://console.tim.qq.com/v4/group_open_http_svc/delete_group_member"
        Random random = new Random()
        int rand = random.nextInt(65535)
        Map urlParamMap = [
            usersig: signId,
            sdkappid: sdkappId,
            identifier: liveAdminIdentifier,
            contenttype: "json",
            random: rand
        ]
        Map params = new HashMap();
        params = [
            GroupId: roomId.toString(),
            MemberToDel_Account: [  // 最多支持500个
                kickUserId as String
            ]
        ]
        String json = new Gson().toJson(params);
        String postString = HttpQcould.execute(urlParamMap, url,json,"POST");
        return postString
    }
    /**
     * 根据前缀获取回看列表
     * @param args
     */
    public static String getVodPlayListInfoByFileId(String secretId,String secretKey,String fileId) {

        String backStr;
        if(!fileId){
            return backStr;
        }
        for(int i=0;i< QCLOUD_RETRY_COUNT; i++){
            /* 如果是循环调用下面举例的接口，需要从此处开始你的循环语句。切记！ */
            TreeMap<String, Object> config = new TreeMap<String, Object>();
            config.put("SecretId", secretId);
            config.put("SecretKey", secretKey);
            /* 请求方法类型 POST、GET */
            config.put("RequestMethod", "GET");
            /* 区域参数，可选: gz:广州; sh:上海; hk:香港; ca:北美;等。 */
            config.put("DefaultRegion", "gz");
            QcloudApiModuleCenter module = new QcloudApiModuleCenter(new Vod(), config);
            TreeMap<String, Object> params = new TreeMap<String, Object>();
            params.put("vid", fileId);
            try {
                String result = module.call("DescribeRecordPlayInfo", params);
                log.error("调用腾讯云，回看列表（用fileID）返回数据,fileId=>{},result=>{}",fileId,result)
                def palyInfo = Strings.parseJson(result)

                int code = palyInfo?.code as int
                if(code == 0){
                    backStr = Strings.toJson(palyInfo?.fileSet)
                    break;
                }
            } catch (Exception e) {
                log.error("error..." + e.getMessage());
            }
        }
        return backStr

    }

    /**
     * 根据前缀获取回看列表
     * @param args
     */
    public static String getVodPlayListInfoByFileIdNew(String secretId,String secretKey,String fileId) {

        String backStr;
        if(!fileId){
            return backStr;
        }
        for(int i=0;i< QCLOUD_RETRY_COUNT; i++){
            /* 如果是循环调用下面举例的接口，需要从此处开始你的循环语句。切记！ */
            TreeMap<String, Object> config = new TreeMap<String, Object>();
            config.put("SecretId", secretId);
            config.put("SecretKey", secretKey);
            /* 请求方法类型 POST、GET */
            config.put("RequestMethod", "GET");
            /* 区域参数，可选: gz:广州; sh:上海; hk:香港; ca:北美;等。 */
            config.put("DefaultRegion", "gz");
            QcloudApiModuleCenter module = new QcloudApiModuleCenter(new Vod(), config);
            TreeMap<String, Object> params = new TreeMap<String, Object>();
            params.put("fileId", fileId);
            try {
                String result = module.call("GetVideoInfo", params);
                log.error("调用腾讯云，回看列表（用fileID）返回数据,fileId=>{},result=>{}",fileId,result)
                def palyInfo = Strings.parseJson(result)

                int code = palyInfo?.code as int
                if(code == 0){
                    backStr = Strings.toJson(palyInfo?.fileSet)
                    break;
                }
            } catch (Exception e) {
                log.error("error..." + e.getMessage());
            }
        }
        return backStr

    }

    /**
     * 根据名称前缀拿录制信息
     */
    public static String  getVodPlayListInfo(String secretId,String secretKey,long liveId,int roomId,String prefix){
        String backStr;
        for(int i=0;i< QCLOUD_RETRY_COUNT; i++){
            /* 如果是循环调用下面举例的接口，需要从此处开始你的循环语句。切记！ */
            TreeMap<String, Object> config = new TreeMap<String, Object>();
            config.put("SecretId", secretId);
            config.put("SecretKey", secretKey);
            /* 请求方法类型 POST、GET */
            config.put("RequestMethod", "GET");
            /* 区域参数，可选: gz:广州; sh:上海; hk:香港; ca:北美;等。 */
            config.put("DefaultRegion", "gz");
            QcloudApiModuleCenter module = new QcloudApiModuleCenter(new Vod(), config);
            TreeMap<String, Object> params = new TreeMap<String, Object>();
            params.put("fileName", prefix);
            try {
                String result = module.call("DescribeVodPlayInfo", params);
                log.info("liveId=>{},roomId=>{},直播取回放返回结果==》{}",liveId,roomId,result)
             //   log.info("liveId:"+liveId+"调用腾讯云，回看列表(用前缀)返回数据："+result)
                def palyInfo = Strings.parseJson(result)
                int code = palyInfo?.code as int
                if(code == 0){
                    backStr = Strings.toJson(palyInfo?.fileSet)
                    break;
                }
            } catch (Exception e) {
                log.error("error..." ,e);
            }
        }

        return backStr
    }

    /**
     * 删除腾讯云视频
     */
    public static def deleteVodFile(String secretId,String secretKey,List<String> fileIds){
        def res = ""
        fileIds?.each {
            /* 如果是循环调用下面举例的接口，需要从此处开始你的循环语句。切记！ */
            TreeMap<String, Object> config = new TreeMap<String, Object>();
            config.put("SecretId", secretId);
            config.put("SecretKey", secretKey);
            /* 请求方法类型 POST、GET */
            config.put("RequestMethod", "GET");
            /* 区域参数，可选: gz:广州; sh:上海; hk:香港; ca:北美;等。 */
            config.put("DefaultRegion", "gz");
            QcloudApiModuleCenter module = new QcloudApiModuleCenter(new Vod(), config);
            TreeMap<String, Object> params = new TreeMap<String, Object>();
            params.put("fileId", it);
            params.put("priority", 0);
            try {
                res = module.call("DeleteVodFile", params);
                log.info("wangtf delete video file, filedid=>{}, res=>{}", it, res)
            } catch (Exception e) {
                log.error("删除回看文件报错：" + e.getMessage());
            }
        }
        return res
    }

    /**
     * 批量停止直播频道
     */
    public static String stopLVBChannel(String secretId,String secretKey,String channelId){
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
        params.put("channelIds.1", channelId);
        try {
            back = module.call("StopLVBChannel", params);
        } catch (Exception e) {
            log.error("StopLVBChannel：" + e.getMessage());
        }
        return back

    }
    /**
     * 批量启用直播频道
     */
    public static String startLVBChannel(String secretId,String secretKey,String channelId){
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
        params.put("channelIds.1", channelId);
        try {
            back = module.call("StartLVBChannel", params);
        } catch (Exception e) {
            log.error("StartLVBChannel：" + e.getMessage());
        }
        return back
    }

    /**
     * 批量停止直播频道
     */
    public static String deleteLVBChannel(String secretId,String secretKey,String channelId){
        String back= "";
        TreeMap<String, Object> config = new TreeMap<String, Object>();
        config.put("SecretId", secretId);
        config.put("SecretKey", secretKey);
        /* 请求方法类型 POST、GET */
        config.put("RequestMethod", "GET");
        /* 区域参数，可选: gz:广州; sh:上海; hk:香港; ca:北美;等。 */
        config.put("DefaultRegion", "gz");
        QcloudApiModuleCenter module = new QcloudApiModuleCenter(new Live(), config);
        TreeMap<String, Object> params = new TreeMap<String, Object>();
        params.put("channelIds.1", channelId);
        try {
           back = module.call("DeleteLVBChannel", params);
        } catch (Exception e) {
            log.error("deleteLVBChannel：" + e.getMessage());
        }
        return back
    }

    /**
     * 视频上传
     * @param fileName 文件全路径
     * @return
     */
    @Deprecated
    public static String multipartUploadVodFile(String secretId,String secretKey,String fileName){
        String lastName = fileName.substring(fileName.lastIndexOf(".")+1);
        String fileTitle = fileName.substring(fileName.lastIndexOf("/")+1,fileName.lastIndexOf("."));

        TreeMap<String, Object> config = new TreeMap<String, Object>();
        String fileId = null;
        config.put("SecretId", secretId);
        config.put("SecretKey", secretKey);
        config.put("RequestMethod", "POST");
        config.put("DefaultRegion", "gz");
        QcloudApiModuleCenter module = new QcloudApiModuleCenter(new Vod(), config);
        try{
            long fileSize = new File(fileName).length();
            String fileSHA1 = SHA1.fileNameToSHA(fileName);

            long fixDataSize = 1024*1024*5;  //每次上传字节数，可自定义[512*1024 ~ 5*1024*1024]
            long firstDataSize = 1024*512;    //最小片字节数（默认不变）
            long tmpDataSize = firstDataSize;
            long remainderSize = fileSize;
            long tmpOffset = 0;
            int code, flag;

            String result = null;
            timerLog.info("multipartUpload 上传文件：${fileName}");
            while (remainderSize>0) {
                TreeMap<String, Object> params = new TreeMap<String, Object>();
                params.put("fileSha", fileSHA1);
                params.put("fileType", lastName);
                params.put("fileName", fileTitle);
                params.put("fileSize", fileSize);
                params.put("dataSize", tmpDataSize);
                params.put("offset", tmpOffset);
                params.put("file", fileName);
                timerLog.info("multipartUpload 上传参数："+params);
                result = module.call("MultipartUploadVodFile", params);
                timerLog.info("multipartUpload 返回结果："+result);
                JSONObject json_result = new JSONObject(result);
                code = json_result.getInt("code");
                if (code == -3002) {               //服务器异常返回，需要重试上传(offset=0, dataSize=512K)
                    tmpDataSize = firstDataSize;
                    tmpOffset = 0;
                    continue;
                } else if (code != 0) {
                    return null;
                }
                flag = json_result.getInt("flag");
                if (flag == 1) {
                    fileId = json_result.getString("fileId");
                    timerLog.info("multipartUpload 返回fileId："+fileId);
                    break;
                } else {
                    tmpOffset = json_result.getLong("offset")
//                    tmpOffset = Integer.parseInt(json_result.getString("offset"));
                }
                remainderSize = fileSize - tmpOffset;
                if (fixDataSize < remainderSize) {
                    tmpDataSize = fixDataSize;
                } else {
                    tmpDataSize = remainderSize;
                }
            }
            timerLog.info("multipartUpload 上传结束 fileId=${fileId}");
            return fileId;
        } catch (Exception e) {
            timerLog.info("multipartUpload 异常："+e.getMessage());
            e.printStackTrace();
            return fileId;
        }

    }

    /**
     * 分段上传
     * @param secretId
     * @param secretKey
     * @param fileName
     * @param title
     * @return
     */
    public static String multipartUpload(String secretId,String secretKey,String fileName,String title=null){
        def result = null
        String fileType = fileName.substring(fileName.lastIndexOf(".")+1);
        String fileTitle = title?:fileName.substring(fileName.lastIndexOf("/")+1,fileName.lastIndexOf("."));

        VodCall vodCall = new VodCall();
        vodCall.Init(secretId, secretKey, VodCall.USAGE_UPLOAD, 12);
        vodCall.SetFileInfo(fileName, fileTitle, fileType, 12);
        int ret = vodCall.Upload();
        if (ret == 0){
            result = vodCall.m_strFileId
        }
        return result
    }

    /**
     * 开启关闭推流（直播码模式）
     * @param appId
     * @param channelId 直播码
     * @param status    0:关闭； 1:开启
     * @param key
     * @param t UNIX时间戳(十进制)
     * @return
     */
    public static String liveChannelSetStatus(String appId,String channelId,int status,String key,long t){
        def sign = Strings.md5(key + t)
        String url = "http://fcgi.video.qcloud.com/common_access?cmd=${appId}&interface=Live_Channel_SetStatus&Param.s.channel_id=${channelId}&Param.n.status=${status}&t=${t}&sign=${sign}"
        //def result = Http.get(url)
        def result = HttpQcould.post(new HashMap<String, String>(),url,"")
        log.info("liveChannelSetStatus url:${url} result:${result}")
        return result
    }

    /**
     * 查询录制文件（直播码模式）
     * @param appId
     * @param channelId 直播码
     * @param key
     * @param t UNIX时间戳(十进制)
     * @return
     */
    public static String liveTapeGetFilelist(String qcloudAppid,String channelId,String key,long t){
        def result = []
        def url
        try {
            def sign = Strings.md5(key + t)
            url = "http://fcgi.video.qcloud.com/common_access?cmd=${qcloudAppid}&interface=Live_Tape_GetFilelist&Param.s.channel_id=${channelId}&t=${t}&sign=${sign}"
            //def response = Http.get(url)
            def response = HttpQcould.post(new HashMap<String, String>(),url,"")
            log.info("liveTapeGetFilelist channelId==>{},response========>{}",channelId,response);
            //Strings.parseJson()
            JSONObject jsonResult = new JSONObject(response)
            JSONObject jsonOutput = jsonResult.output
            JSONArray file_list = jsonOutput.file_list
            int all_count = jsonOutput.all_count
            for (i in 0..< all_count){
                def it = file_list.get(i)
                def duration = ((DateUtil.getDate(it.end_time,"yyyy-MM-dd HH:mm:ss").getTime() - DateUtil.getDate(it.start_time,"yyyy-MM-dd HH:mm:ss").getTime()))/1000
                result << [
                    duration: duration,
                    fileId: it.file_id,
                    fileName:null,
                    image_url:null,
                    playSet:[[
                        definition:null,
                        url:it.record_file_url,
                        vbitrate:null,
                        vheight:null,
                        vwidth:null
                    ]],
                    status:null,
                    vid:it.vid,
                ]
            }
        } catch (Exception e) {
            e.printStackTrace()
            log.info("liveTapeGetFilelist exception ${e.getMessage()}")
        }
        log.info("liveTapeGetFilelist url:${url} result:${result}")
        return JSON.toJSONString(result)
    }

    /**
     * 查询录制文件（直播码模式）
     * @param appId
     * @param channelId 直播码
     * @param key
     * @param t UNIX时间戳(十进制)
     * @return
     */
    public static String liveTapeGetFilelistByTime(String qcloudAppid,String channelId,String key,long t,String beginTime,String endTime){

        def result = []
        def url
        try {
            def sign = Strings.md5(key + t)
            url = "http://fcgi.video.qcloud.com/common_access?cmd=${qcloudAppid}&interface=Live_Tape_GetFilelist&Param.s.channel_id=${channelId}&t=${t}&sign=${sign}&Param.s.start_time=${Strings.getUrlEncode(beginTime,"UTF-8")}&Param.s.end_time=${Strings.getUrlEncode(getAfterMinutesTime(endTime,2),"UTF-8")}"
            //def response = Http.get(url)
//            Map<String,String> urlMap = new HashMap<String,String>()
//            urlMap.put("cmd",qcloudAppid)
//            urlMap.put("interface","Live_Tape_GetFilelist")
//            urlMap.put("Param.s.channel_id",channelId)
//            urlMap.put("t",t)
//            urlMap.put("sign",sign)
//            urlMap.put("Param.s.start_time",Strings.getUrlEncode(beginTime,"UTF-8"))
//            urlMap.put("Param.s.end_time",Strings.getUrlEncode(endTime,"UTF-8"))
//            String urlprefix = "http://fcgi.video.qcloud.com/common_access"
            String postJson=""
            //def response = HttpQcould.post(urlMap,urlprefix,postJson)
            def response = HttpQcould.post(new HashMap<String, String>(),url,postJson)
            //def response = HttpQcould.executeHttp(urlMap,urlprefix,postJson,"GET")
            //def response = HttpQcould.executeHttp(new HashMap<String, String>(),url,postJson,"GET")
            //System.out.println("++++++++++"+response);
            log.info("liveTapeGetFilelist channelId==>{},response========>{}",channelId,response);
            //Strings.parseJson()
            JSONObject jsonResult = new JSONObject(response)
            JSONObject jsonOutput = jsonResult.output
            JSONArray file_list = jsonOutput.file_list
            int all_count = jsonOutput.all_count
            for (i in 0..< all_count){
                def it = file_list.get(i)
                def duration = ((DateUtil.getDate(it.end_time,"yyyy-MM-dd HH:mm:ss").getTime() - DateUtil.getDate(it.start_time,"yyyy-MM-dd HH:mm:ss").getTime()))/1000
                result << [
                    duration: duration,
                    fileId: it.file_id,
                    fileName:null,
                    image_url:null,
                    playSet:[[
                                 definition:null,
                                 url:it.record_file_url,
                                 vbitrate:null,
                                 vheight:null,
                                 vwidth:null
                             ]],
                    status:null,
                    vid:it.vid,
                ]
            }
        } catch (Exception e) {
            e.printStackTrace()
            log.info("liveTapeGetFilelist exception ${e.getMessage()}")
        }
        log.info("liveTapeGetFilelist url:${url} result:${result}")
        return JSON.toJSONString(result)
    }



    public static String getAfterMinutesTime(String time, int i){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date d = sdf.parse(time)
        Date afterDate = new Date(d.getTime() + 300000);
        System.out.println(sdf.format(afterDate ));
        return sdf.format(afterDate );
    }


    /**
     * 视频拼接
     * @param secretId
     * @param secretKey
     * @param fileIds   文件ID list
     * @param name
     * @param dstType   拼接类型list mp4 m3u8
     * @return
     */
    public static String concatVideo(String secretId,String secretKey,def fileIds,String name,def dstType){
        String back
        try {
            def config = getConfig(secretId,secretKey,"POST");
            QcloudApiModuleCenter module = new QcloudApiModuleCenter(new Vod(), config);
            def params = new TreeMap()
            params.put("name", name);
            fileIds.eachWithIndex{it, i ->
                params.put("srcFileList."+i+".fileId", it);
            }
            dstType.eachWithIndex{it, i ->
                params.put("dstType."+i, it);
            }
            println "concatVideo params >>> " +params
            back = module.call("ConcatVideo", params);
        } catch (Exception e) {
            log.error("ConcatVideo：" + e.getMessage());
        }
        return back
    }

    /**
     * 按时间点截图
     * @param secretId
     * @param secretKey
     * @param fileId
     * @return
     */
    public static String createSnapshotByTimeOffset(String secretId,String secretKey,String fileId){
        String back
        try {
            def config = getConfig(secretId,secretKey,"POST");
            QcloudApiModuleCenter module = new QcloudApiModuleCenter(new Vod(), config);
            def params = new TreeMap()
            params.put("fileId", fileId);
            params.put("definition", 10);
            params.put("timeOffset.0", 60000); //以毫秒为单位
            println "concatVideo params >>> " +params
            back = module.call("CreateSnapshotByTimeOffset", params);
        } catch (Exception e) {
            log.error("CreateSnapshotByTimeOffset：" + e.getMessage());
        }
        return back
    }
    /**
     * 拉取事件通知
     * @param secretId
     * @param secretKey
     * @return
     */
    public static String pullEvent(String secretId,String secretKey){
        String back
        try {
            def config = getConfig(secretId,secretKey,"GET");
            QcloudApiModuleCenter module = new QcloudApiModuleCenter(new Vod(), config);
            def params = new TreeMap()
            params.put("a", "a");//空值会报错，随便写了个参数
            back = module.call("PullEvent", params);
        } catch (Exception e) {
            log.error("PullEvent：" + e.getMessage());
        }
        return back
    }

    /**
     * 确认事件通知
     * @param secretId
     * @param secretKey
     * @param msgHandle 事件句柄list
     * @return
     */
    public static String confirmEvent(String secretId,String secretKey,def msgHandle){
        String back
        try {
            def config = getConfig(secretId,secretKey,"POST");
            QcloudApiModuleCenter module = new QcloudApiModuleCenter(new Vod(), config);
            def params = new TreeMap()
            msgHandle.eachWithIndex{it, i ->
                params.put("msgHandle."+i, it);
            }
            back = module.call("ConfirmEvent", params);
        } catch (Exception e) {
            log.error("ConfirmEvent：" + e.getMessage());
        }
        return back
    }

    /**
     * 公共参数
     * @param secretId
     * @param secretKey
     * @return
     */
    public static TreeMap<String, Object> getConfig(String secretId,String secretKey,String method){
        /* 如果是循环调用下面举例的接口，需要从此处开始你的循环语句。切记！ */
        TreeMap<String, Object> config = new TreeMap<String, Object>();
        config.put("SecretId", secretId);
        config.put("SecretKey", secretKey);
        /* 请求方法类型 POST、GET */
        config.put("RequestMethod", method);
        /* 区域参数，可选: gz:广州; sh:上海; hk:香港; ca:北美;等。 */
        config.put("DefaultRegion", "gz");
        return config
    }

    /**
     * 视频文件转码
     * @param fileId
     * @param callback
     * @return
     * @throws Exception
     */
    public static String convertVodFile(String secretId,String secretKey,String fileId,String callback) throws Exception {
        String back
        try {
            def config = getConfig(secretId,secretKey,"POST");
            QcloudApiModuleCenter module = new QcloudApiModuleCenter(new Vod(), config);
            def params = new TreeMap()
            params.put("fileId", fileId);
            params.put("isScreenshot", 1);//是否截图，0不需要，1需要
            params.put("isWatermark", 0);//是否添加水印，0不需要，1需要
            params.put("notifyUrl", callback);//转码结果回调地址
            back = module.call("ConvertVodFile", params);
        } catch (Exception e) {
            log.error("ConvertVodFile：" + e.getMessage());
        }
        return back
    }

    public static String getVideoPlayUrl(String secretId,String secretKey,String fileId) throws Exception {
        String back
        try {
            def config = getConfig(secretId,secretKey,"POST");
            QcloudApiModuleCenter module = new QcloudApiModuleCenter(new Vod(), config);
            def params = new TreeMap()
            params.put("fileId", fileId);

            back = module.call("DescribeVodPlayUrls", params);//每个参数对应不同的操作
        } catch (Exception e) {
            log.error("DescribeVodPlayUrls：" + e.getMessage());
        }
        return back
    }

    public static String getVideoInfo(String secretId,String secretKey,String fileId) throws Exception {
        String back
        try {
            def config = getConfig(secretId,secretKey,"POST");
            QcloudApiModuleCenter module = new QcloudApiModuleCenter(new Vod(), config);
            def params = new TreeMap()
            params.put("fileIds.1", fileId);

            back = module.call("DescribeVodInfo", params);//每个参数对应不同的操作
        } catch (Exception e) {
            log.error("DescribeVodInfo：" + e.getMessage());
        }
        return back
    }

    /**
     * 查询直播播放流量
     * @param liveId
     * @param roomId
     * @param sdkappId
     * @param userId
     * @param signId
     * @return
     */
    public static String qcloudFlux(String channelId,String qcloudAppid,String apiKey,Long startTime,Long endTime){
        long t = DateUtil.addDateByMinute(new Date(),1).getTime()/1000  //1分钟失效
        def sign = Strings.md5(apiKey + t)
        def map =[
            "cmd":qcloudAppid,
//        "interface":"Get_LiveStat",
//        "interface":"Get_LivePushStat",
//        "interface":"Get_LivePlayStat",
//        "interface":"Get_LivePushStatHistory",
           "interface":"Get_LivePlayStatHistory",
           "Param.n.start_time":startTime,//10位时间戳
           "Param.n.end_time":endTime,//10位时间戳
           "Param.s.stream_id":channelId,
           "t":t.toString(),
           "sign":sign,
        ]
        String url ="http://statcgi.video.qcloud.com/common_access"
        def res = HttpQcould.post(map,url,"")
        log.info("qcloudFlux res=>{}",res)
        def json = JSON.parse(res)
        String flux = json.output?.sum_info?.sum_flux?:""
        log.info("qcloudFlux flux=>{}",flux)
        return flux
    }
}
