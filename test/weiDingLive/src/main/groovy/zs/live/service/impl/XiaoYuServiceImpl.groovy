package zs.live.service.impl

import com.alibaba.fastjson.JSON
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Service
import zs.live.service.XiaoYuService
import zs.live.utils.XiaoYuHttpUtil
import zs.live.utils.XiaoYuSignatureUtil

import java.util.concurrent.TimeUnit


@Service("xiaoYuService")
@Slf4j
class XiaoYuServiceImpl implements XiaoYuService{
	private static final String  extID="922e3910592c9821a9beda479ba2df52c60fbebf";
	private static final String token="51773b22fcc4008a40b7944ceca5ec7c57fcffd86feb8165c224b97fa9eac48f";
	private static final XiaoYuSignatureUtil signatureSample = new XiaoYuSignatureUtil();

    @Override
	public Map reserveLive(String xiaoYuNumber,Map<String, Object> data){



		String urlStr = "https://www.ainemo.com/api/rest/external/v1/liveVideo2/enterprise/"+extID+
				"/xiaoyunumber/"+xiaoYuNumber+"/live?enterpriseId="+extID;

		String jsonEntity = JSON.toJSONString(data);
		String signature = signatureSample.computeSignature(jsonEntity, "POST", token, urlStr);
		urlStr += "&signature=" + signature;

		return	XiaoYuHttpUtil.execute(new URL(urlStr),"POST", jsonEntity, 10,TimeUnit.SECONDS);
	}

    @Override
	public Map updateLive(String xiaoYuNumber,String xiaoYuLiveId,Map<String, Object> data){

		String urlStr = "https://www.ainemo.com/api/rest/external/v1/liveVideo2/enterprise/"+extID+
				"/xiaoyunumber/"+xiaoYuNumber+"/live/"+xiaoYuLiveId+"?enterpriseId="+extID;

		String jsonEntity =    JSON.toJSONString(data);
		String signature = signatureSample.computeSignature(jsonEntity, "PUT", token, urlStr);
		urlStr += "&signature=" + signature;

		return	XiaoYuHttpUtil.execute(new URL(urlStr),"PUT",jsonEntity, 10,TimeUnit.SECONDS);
	}

    @Override
	public Map removeLive(String xiaoYuNumber,String xiaoYuLiveId){
		String urlStr = "https://www.ainemo.com/api/rest/external/v1/liveVideo2/enterprise/"+extID+
				"/xiaoyunumber/"+xiaoYuNumber+"/live/"+xiaoYuLiveId+"?enterpriseId="+extID;
        log.info("remove xiaoYu meeting reserve urlStr:{}",urlStr);
		String signature = signatureSample.computeSignature("", "DELETE", token, urlStr);
		urlStr += "&signature=" + signature;

		return	XiaoYuHttpUtil.execute(new URL(urlStr),"DELETE","", 10,TimeUnit.SECONDS);
	}

    @Override
	public Map getLiveInfo(String xiaoYuNumber,String xiaoYuLiveId){
		String urlStr = "https://www.ainemo.com/api/rest/external/v1/liveVideo2/enterprise/"+extID+
				"/xiaoyunumber/"+xiaoYuNumber+"/live/"+xiaoYuLiveId+"?enterpriseId="+extID;

		String signature = signatureSample.computeSignature("", "GET", token, urlStr);
		urlStr += "&signature=" + signature;

		return	XiaoYuHttpUtil.execute(new URL(urlStr),"GET","", 10,TimeUnit.SECONDS);
	}
    @Override
	public Map getVideos(String xiaoYuNumber,String xiaoYuLiveId){
		String urlStr = "https://www.ainemo.com/api/rest/external/v1/liveVideo2/enterprise/"+extID+
				"/xiaoyunumber/"+xiaoYuNumber+"/live/"+xiaoYuLiveId+"/videos?enterpriseId="+extID;

		String signature = signatureSample.computeSignature("", "GET", token, urlStr);
		urlStr += "&signature=" + signature;

		return	XiaoYuHttpUtil.execute(new URL(urlStr),"GET","", 10,TimeUnit.SECONDS);
	}

    @Override
    public Map getVideosWithDuration(String xiaoYuNumber,String xiaoYuLiveId){
        String urlStr = "https://www.ainemo.com/api/rest/external/v1/liveVideo2/enterprise/"+extID+
            "/xiaoyunumber/"+xiaoYuNumber+"/live/"+xiaoYuLiveId+"/videoswithduration?enterpriseId="+extID;

        String signature = signatureSample.computeSignature("", "GET", token, urlStr);
        urlStr += "&signature=" + signature;

        return	XiaoYuHttpUtil.execute(new URL(urlStr),"GET","", 10,TimeUnit.SECONDS);
    }



    @Override
	public Map getXiaoYuNumberInfo(String xiaoYuNumber){
		String urlStr = "https://www.ainemo.com/api/rest/external/v1/deviceInfo/"+
		xiaoYuNumber+"?enterpriseId="+extID;

		String signature = signatureSample.computeSignature("", "GET", token, urlStr);
		urlStr += "&signature=" + signature;

		return	XiaoYuHttpUtil.execute(new URL(urlStr),"GET","", 10,TimeUnit.SECONDS);
	}

    @Override
	public Map getAllXiaoYuNumberInfo(){
		String urlStr = "https://www.ainemo.com/api/rest/external/v1/deviceInfo?enterpriseId="+extID;

		String signature = signatureSample.computeSignature("", "GET", token, urlStr);
		urlStr += "&signature=" + signature;

		return	XiaoYuHttpUtil.execute(new URL(urlStr),"GET","", 10,TimeUnit.SECONDS);
	}

    @Override
    public Map getLiveInfoForOld(String xiaoYuNumber){
        String urlStr = "https://www.ainemo.com/api/rest/external/v1/liveVideo/"+xiaoYuNumber+"?enterpriseId="+extID;

        String signature = signatureSample.computeSignature("", "GET", token, urlStr);
        urlStr += "&signature=" + signature;

        return	XiaoYuHttpUtil.execute(new URL(urlStr),"GET","", 10,TimeUnit.SECONDS);
    }

    @Override
    public Map removeLiveForOld(String xiaoYuNumber){
        String urlStr = "https://www.ainemo.com/api/rest/external/v1/liveVideo/"+xiaoYuNumber+"?enterpriseId="+extID;

        String signature = signatureSample.computeSignature("", "DELETE", token, urlStr);
        urlStr += "&signature=" + signature;

        return	XiaoYuHttpUtil.execute(new URL(urlStr),"DELETE","", 10,TimeUnit.SECONDS);
    }

    @Override
    public Map updateLiveForOld(String xiaoYuNumber,Map<String, Object> data){

        String urlStr = "https://www.ainemo.com/api/rest/external/v1/liveVideo/"+xiaoYuNumber+"?enterpriseId="+extID;

        String jsonEntity =    JSON.toJSONString(data);
        String signature = signatureSample.computeSignature(jsonEntity, "PUT", token, urlStr);
        urlStr += "&signature=" + signature;

        return	XiaoYuHttpUtil.execute(new URL(urlStr),"PUT",jsonEntity, 10,TimeUnit.SECONDS);

    }


	/*public static void main(String[] args) throws Exception {
		XiaoYuServiceImpl xiaoYuService=new XiaoYuServiceImpl();
		Map<String, Object> data = new HashMap<>();
		String title="2099康总";
		data.put("title",title);
		//confNo为会议室id，在一个会议室中可以有多个会议直播
		//	data.put("confNo","918515379270");
		data.put("confNo","918515379270");
		//直播开始和终止时间，似乎没用
		data.put("startTime", System.currentTimeMillis() + 30*60*1000);  //半个小 时
		data.put("endTime", System.currentTimeMillis() + 60 * 60 * 1000*3);  //3个小时后结束
		data.put("detail",title+"的详情是这样的，我先说，恩。你来");
		data.put("autoRecording", true);
		data.put("autoPublishRecording", true);

		data.put("location",title+"的location是在China,恩,确实如此");
		def ret=null;
//  ret=xiaoYuService.reserveLive(String.valueOf(725159),data);

 		println ret;

		data.put("title",title+"update");
		//confNo为会议室id，在一个会议室中可以有多个会议直播
		data.put("confNo","918515379270");

		//直播开始和终止时间，似乎没用
		data.put("startTime", System.currentTimeMillis() + 60000);  //半个小 时
		data.put("endTime", System.currentTimeMillis() + 60 * 60 * 1000*3);  //3个小时后结束
		data.put("detail",title+"的详情是这样的，我先说，恩。你来"+"update");
		data.put("autoRecording", true);
		data.put("autoPublishRecording", true);

		data.put("location",title+"的location是在China,恩,确实如此"+"update");

		//	println	xiaoYuService.updateLive(String.valueOf(725159),ret.liveId,data);

		//	println xiaoYuService.removeLive(String.valueOf(725159),ret.liveId);

	  def getOne= xiaoYuService.getLiveInfo(String.valueOf(725159),"ff80808158d62e4f0158f7a1f1b511e9");
			println "getOne.status :"+getOne.status+",viewUrl:"+getOne.viewUrl;

		println xiaoYuService.getVideos(String.valueOf(725159),"ff80808158d62e4f0158f7a1f1b511e9");
		println xiaoYuService.getXiaoYuNumberInfo(String.valueOf(725159));
		println xiaoYuService.getAllXiaoYuNumberInfo();
	}*/
}
