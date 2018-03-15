import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.common.LiveCommon
import zs.live.dao.mysql.LiveRes
import zs.live.entity.LiveRecord
import zs.live.service.GiftService
import zs.live.service.LiveService
import zs.live.utils.Http
import zs.live.utils.HttpQcould
import zs.live.utils.Parallel
import zs.live.utils.Strings

Logger log = LoggerFactory.getLogger(this.getClass())

ApiUtils.process {
    binding.head  = [hasMore: false]
    String appId = Strings.getAppId(params)
    params.appId = appId
    int errorStatus = 0; //等于1表示发生了错误
    String resultStr = ""; //php返回json格式的字符串
    List<String> items = ["gm_c", "opId", "openId", "userName", "clientOprateOrder", "giftName",
                      "giftPrice", "giftNum", "liveId", "liveTitle", "userId", "inviter", "appId"]
    if(params.isSale){
        items.add("isSale")
    }
    if(params.liveType){
        items.add("liveType")
    }

    LiveRes liveRes=getBean(LiveRes)
    LiveService liveService=getBean(LiveService)
    GiftService giftService=getBean(GiftService)

    String phpURI = giftService.giftPayorderUrl
    Map<String, String> phpParams = new LinkedHashMap<>();
    items.each {
        if ("gm_c".equals(it))
            phpParams.put(it, URLEncoder.encode(params.get(it) as String, "UTF-8"));
        else
            phpParams.put(it, params.get(it) as String);
    }
    String phpFullUrl=phpURI
    int count=0;
    for (String key : phpParams.keySet()) {
        if(count==0){
            phpFullUrl = phpFullUrl + "?" + key +"="+ phpParams.get(key);
        }else{
            phpFullUrl = phpFullUrl + "&" + key +"="+ phpParams.get(key);
        }
        count = 1;
    }
    System.out.println(phpParams.get("gm_c"))
    resultStr = Http.post(phpURI, phpParams, 10000);
    log.info(" payorder phpURI :{},resultStr:{}", phpURI, resultStr);

//    body = [req: JsonOutput.toJson(phpParams), phpURI: phpURI,result:"",phpFullUrl:phpFullUrl];
    binding.head.status = 500
    JsonSlurper jsonSlurper=new JsonSlurper();
    if (!errorStatus && resultStr) {
        def resultObj = jsonSlurper.parseText(resultStr)
        if (resultObj) {
            binding.head.status =resultObj.head?.code ?:500
            binding.head.msg=resultObj.head?.msg ?: ""

            //付费成功，关注主播,以便可以收到直播开始时的推送通知
            if(resultObj.head?.code ==200){

                Parallel.run([1],{
                    def liveRecordRet = liveRes.findLiveByLiveId(phpParams.liveId as long)
                    LiveRecord  liveRecord = liveService.toLiveRecord(liveRecordRet)
                    liveService.addFollow(phpParams.userId as long,liveRecord?.userId,1,liveRecord?.appId)
                },1)
            }else if(resultObj.head?.code ==700){
              throw new ApiException(ApiException.STATUS_GET_BODY_MSG,resultObj.head?.msg ?: "")
            }
        }
    }
    return
}
