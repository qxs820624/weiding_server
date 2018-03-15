package com.qcloud.Common;

import com.qcloud.Module.Vod;
import com.qcloud.QcloudApiModuleCenter;

import java.util.TreeMap;

/**
 * Created by Administrator on 2016/12/5.
 */
public class PullVd {
    /**
     * 组装传给腾讯云的参数
     * @param url
     * @param fileName
     * @return
     */
    public static TreeMap<String,Object> pullParam(String url,String fileName,String callback){
        TreeMap<String, Object> params = new TreeMap<String, Object>();
        params.put("pullset.1.url", url);//需要拉取的视频URL，n为一个整数，第一个视频 n填1， 第二个视频 n填2， 依次递增；下同
        params.put("pullset.1.fileName", fileName);//视频文件的名称
        params.put("pullset.1.notifyUrl", callback);//腾讯云通过回调该URL地址通知；调用方该视频已经拉取完毕。
        params.put("pullset.1.isTranscode", 1);//是否转码，0：否，1：是，默认为0；
        params.put("pullset.1.isScreenshot", 1);//是否截图，0：否，1：是，默认为0
        params.put("pullset.1.priority", 1);//优先级0:中 1：高 2：低
        params.put("pullset.1.isReport", 1);//回调开关，是否需要回包给开发商，0：否，1：是，默认为0
        return params;
    }

    /**
     * 发送拉取视频请求
     * @param params
     * @throws Exception
     */
    public static  String  pull(TreeMap<String,Object> params) throws Exception {

        TreeMap<String, Object> config = new TreeMap<String, Object>();
        config.put("SecretId", QcloudConfig.SECRET_ID);
        config.put("SecretKey", QcloudConfig.SECRET_KEY);
        config.put("RequestMethod", "POST");
        config.put("DefaultRegion", "gz");
        QcloudApiModuleCenter module = new QcloudApiModuleCenter(new Vod(), config);

        return  module.call("MultiPullVodFile", params);//每个参数对应不同的操作
    }

    public static String pullFile(String url,String fileName,String callback) throws Exception {
        TreeMap<String,Object> params= pullParam(url,fileName,callback);
        String result = pull(params);
        return result;
    }

    public static String getVideoPlayUrl(String fileId) throws Exception {
        TreeMap<String, Object> params = new TreeMap<String, Object>();
        params.put("fileId", fileId);

        TreeMap<String, Object> config = new TreeMap<String, Object>();
        config.put("SecretId", QcloudConfig.SECRET_ID);
        config.put("SecretKey", QcloudConfig.SECRET_KEY);
        config.put("RequestMethod", "POST");
        config.put("DefaultRegion", "gz");
        QcloudApiModuleCenter module = new QcloudApiModuleCenter(new Vod(), config);

        return  module.call("DescribeVodPlayUrls", params);//每个参数对应不同的操作
    }

    public static String getVideoInfo(String fileId) throws Exception {
        TreeMap<String, Object> params = new TreeMap<String, Object>();
        params.put("fileIds.1", fileId);

        TreeMap<String, Object> config = new TreeMap<String, Object>();
        config.put("SecretId", QcloudConfig.SECRET_ID);
        config.put("SecretKey", QcloudConfig.SECRET_KEY);
        config.put("RequestMethod", "POST");
        config.put("DefaultRegion", "gz");
        QcloudApiModuleCenter module = new QcloudApiModuleCenter(new Vod(), config);

        return  module.call("DescribeVodInfo", params);//每个参数对应不同的操作
    }

    /**
     * 视频文件转码
     * @param fileId
     * @param callback
     * @return
     * @throws Exception
     */
    public static String convertVodFile(String fileId,String callback) throws Exception {
        TreeMap<String, Object> params = new TreeMap<String, Object>();
        params.put("fileId", fileId);
        params.put("isScreenshot", 1);//是否截图，0不需要，1需要
        params.put("isWatermark", 0);//是否添加水印，0不需要，1需要
        params.put("notifyUrl", callback);//转码结果回调地址

        TreeMap<String, Object> config = new TreeMap<String, Object>();
        config.put("SecretId", QcloudConfig.SECRET_ID);
        config.put("SecretKey", QcloudConfig.SECRET_KEY);
        config.put("RequestMethod", "POST");
        config.put("DefaultRegion", "gz");
        QcloudApiModuleCenter module = new QcloudApiModuleCenter(new Vod(), config);

        return  module.call("ConvertVodFile", params);//每个参数对应不同的操作
    }

    /*public static void main(String a[]) throws Exception {
//        String url ="http://lvzb.zhongsou.com/222.mp4";
//        String name="拉取测试222";
//        String callback="http://www.baidu.com?foreshowId=123";
        String result = PullVd.convertVodFile("9031868222884110502","http://lvpre.souyue.mobi/live/video.merge.callback.groovy?foreshowId=100963");

//        TreeMap<String,Object> params= pullParam(url,name,callback);
//        String result = pull(params);

//        100469 14651978969511300265 qiyue.f0.mp4
//        String result = PullVd.convertVodFile("14651978969511300265","http://lvpre.souyue.mobi/live/video.merge.callback.groovy?foreshowId=100469");
//        String result = PullVd.getVideoPlayUrl("14651978969511300265");
//        String result = PullVd.getVideoInfo("14651978969511300265");

//        100471 14651978969511300434 souyue.f0.mp4
//        String result = PullVd.convertVodFile("14651978969511300434","http://lvpre.souyue.mobi/live/video.merge.callback.groovy?foreshowId=100471");
//        String result = PullVd.getVideoPlayUrl("14651978969511300434");
//        String result = PullVd.getVideoInfo("14651978969511300434");

//        100472 14651978969511300469 yunyue.f0.mp4
//        String result = PullVd.convertVodFile("14651978969511300469","http://lvpre.souyue.mobi/live/video.merge.callback.groovy?foreshowId=100472");
//        String result = PullVd.getVideoPlayUrl("14651978969511300469");
//        String result = PullVd.getVideoInfo("14651978969511300469");

//        100470 14651978969511300644 luowenyong.f0.mp4
//        String result = PullVd.convertVodFile("14651978969511300644","http://lvpre.souyue.mobi/live/video.merge.callback.groovy?foreshowId=100470");
//        String result = PullVd.getVideoPlayUrl("14651978969511300644");
//        String result = PullVd.getVideoInfo("14651978969511300644");

//        100468 14651978969511300620 chenpei.f0.mp4
//        String result = PullVd.convertVodFile("14651978969511300620","http://lvpre.souyue.mobi/live/video.merge.callback.groovy?foreshowId=100468");
//        String result = PullVd.getVideoPlayUrl("14651978969511300620");
//        String result = PullVd.getVideoInfo("14651978969511300620");

        //100469 14651978969511301533 qiyue7.f0.mp4 //14651978969511301555
//        String result = PullVd.convertVodFile("14651978969511301533","http://lvpre.souyue.mobi/live/video.merge.callback.groovy?foreshowId=100469");
//        String result = PullVd.getVideoPlayUrl("14651978969511301533");
//        String result = PullVd.getVideoInfo("14651978969511301533");
        System.out.println("result=="+result);
    }*/
}
