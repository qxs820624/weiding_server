package com.qcloud.Common;

import com.qcloud.Utilities.MD5;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2016/12/6 0006.
 */
public class QcloudDown {
    public static final Logger timerLog = LoggerFactory.getLogger("timerLog");
    public static final Logger videoGatherLog = LoggerFactory.getLogger("videoGatherLog");
    private static String lo="http://200006652.vod.myqcloud.com/200006652_a6fe5eb55a6141839b91090704b9f60c.f0.mp4";
    private static String downloadDir;


    public static final RequestConfig DEFAULT_REQUEST_CONFIG
        = RequestConfig.custom()
        .setSocketTimeout(200000)//数据传输处理时间
        .setConnectTimeout(2000)
        .setConnectionRequestTimeout(10 * 1000)
        .build();
    public static CloseableHttpClient createClient() {
        return HttpClients.custom()
            .setDefaultRequestConfig(DEFAULT_REQUEST_CONFIG)
            .build();
    }

    /**
     * 下载文件
     * @param urls
     * @param path
     * @return
     */
    public static List<String> download(String[] urls,String path){
        downloadDir = path;
        List<String> list = new ArrayList<>();
        try {
            for (String url : urls) {
                String localPath = generateLocalPath(url);
                downloadByUrl(url,localPath);
                list.add(localPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
            videoGatherLog.info(e.getMessage());
        }

        return list;
    }

    /**
     * 根据url下载文件
     * @param url
     * @throws Exception
     */
    private static void downloadByUrl(String url,String localPath) throws Exception {
        File file = new File(localPath);
        long fileLength = 0;
        long newFileLength = 0;
        if (file.exists()){
            fileLength = file.length();
        }
        CloseableHttpClient client = createClient();
        HttpGet get= new HttpGet(url);
        CloseableHttpResponse response = null;
        InputStream inputStream = null;
        BufferedOutputStream outputStream = null;
        try{

            response = client.execute(get);
            HttpEntity entity = response.getEntity();

            newFileLength = entity.getContentLength();
            if (fileLength < newFileLength){//判断是否文件下载完整
                file.delete();
                inputStream = entity.getContent();
                outputStream = new BufferedOutputStream(new FileOutputStream(localPath));
                final byte[] tmp = new byte[4096];
                int l;
                while((l = inputStream.read(tmp)) != -1) {
                    outputStream.write(tmp, 0, l);
                    outputStream.flush();
                }
            }else {
                timerLog.info("mergeVideo downloadByUrl 文件已存在 下载地址>>> " + url + " 保存地址>>> "+localPath);
            }

            if (file.length() < newFileLength){
                throw new Exception("downloadByUrl 文件下载不完整，文件大小："+newFileLength+" 下载大小："+file.length());
            }
        }finally {
            if (response != null) {response.close();}
            if (inputStream != null) {inputStream.close();}
            if (outputStream != null) {outputStream.close();}
            get.abort();
            client.close();
        }
    }

    /**
     * 生成本地地址
     * @param url
     * @return
     */
    public static String generateLocalPath(String url){
        videoGatherLog.info("原始下载地址 >> " + url);
        if (url.contains("?")){
            url = url.substring(0,url.lastIndexOf("?"));
            videoGatherLog.info("截取下载地址 >> " + url);
        }
        String lastName = url.substring(url.lastIndexOf("."));
        String newPath = downloadDir + MD5.stringToMD5(url) + ".f0" + lastName;
        return newPath;
    }

}
