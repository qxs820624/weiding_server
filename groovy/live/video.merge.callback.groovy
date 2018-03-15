import zs.live.ApiUtils
import zs.live.service.CallBackService
import zs.live.utils.Strings

/**
 * 场景：直播回看地址合并的回调接口
 访问地址：http://域名/live/video.merge.callback.groovy
 参数：
 {

 }
 返回值：
 {
     "head": {
     "status": 200,
     "hasMore": false
 },
 "body": {
    "status": 0
 }
 }
 视频合并回调的流程：
    1、当有回调的时候，判断当前配置的视频合并方式是传至云(push)，还是拉取(pull)
    2、当为push时，先从缓存中根据foreshowId获取fileId，然后根据foreshowId从腾讯云获取视频的地址和时长，更新数据库
    3、当为pull时，根据返回的参数判断时上传成功的回调还是转码成功的回调，
        如果是上传成功的回调不更新，当为转码成功的回调则更新数据库
    4、如果判断上传失败，则会有三次的重试机会，重新上传
    5、从腾讯云获取视频地址的时候，如果取到的值为空，则会休息5秒，重新获取。
 * */
ApiUtils.processNoEncry{
    ApiUtils.log.info("video.merge.callback.groovy params=======>{}",params)
    CallBackService callBackService = getBean(CallBackService.class)
    int status = callBackService.videoMergeCallBack(params as Map)
    ApiUtils.log.info("video.merge.callback.groovy return status:{}=======>",status)
    return [code:status,message:""]
}

//System.out.println("video merge callback start.....................")
//BufferedReader br = new BufferedReader(new InputStreamReader(request.inputStream));
//String line = null;
//StringBuilder sb = new StringBuilder();
//while((line = br.readLine())!=null){
//    sb.append(line);
//}
//// 将资料解码
//def params = Strings.parseJson(sb.toString());
//System.out.println("video merge callback params:"+params)
//CallBackService callBackService = ApiUtils.getBean(context,CallBackService.class)
//callBackService.videoMergeCallBack(params as Map)
