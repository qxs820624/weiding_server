package com.qcloud.Common;

import com.qcloud.Module.Vod;
import com.qcloud.QcloudApiModuleCenter;
import com.qcloud.Utilities.Json.JSONObject;
import com.qcloud.Utilities.SHA1;

import java.io.File;
import java.util.TreeMap;

public class VodDemo {
    public static String updload(String file){
        TreeMap<String, Object> config = new TreeMap<String, Object>();
        String fileId = null;
        config.put("SecretId", QcloudConfig.SECRET_ID);
        config.put("SecretKey", QcloudConfig.SECRET_KEY);
        config.put("RequestMethod", "POST");
        config.put("DefaultRegion", "gz");
        QcloudApiModuleCenter module = new QcloudApiModuleCenter(new Vod(), config);
        try{
            String fileName = file;
            long fileSize = new File(fileName).length();
            String fileSHA1 = SHA1.fileNameToSHA(fileName);

            int fixDataSize = 1024*1024*5;  //每次上传字节数，可自定义[512*1024 ~ 5*1024*1024]
            int firstDataSize = 1024*512;    //最小片字节数（默认不变）
            int tmpDataSize = firstDataSize;
            long remainderSize = fileSize;
            int tmpOffset = 0;
            int code, flag;

            String result = null;

            while (remainderSize>0) {
                TreeMap<String, Object> params = new TreeMap<String, Object>();
                params.put("fileSha", fileSHA1);
                params.put("fileType", "mp4");
                params.put("fileName", "jimmyTest");
                params.put("fileSize", fileSize);
                params.put("dataSize", tmpDataSize);
                params.put("offset", tmpOffset);
                params.put("file", fileName);

                result = module.call("MultipartUploadVodFile", params);
                System.out.println("vodDemo result: "+result);
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
                    System.out.println("vodDemo fileId: "+fileId);
                    break;
                } else {
                    tmpOffset = Integer.parseInt(json_result.getString("offset"));
                }
                remainderSize = fileSize - tmpOffset;
                if (fixDataSize < remainderSize) {
                    tmpDataSize = fixDataSize;
                } else {
                    tmpDataSize = (int) remainderSize;
                }
            }
            System.out.println("vodDemo end...");
            return fileId;
        }
        catch (Exception e) {

            e.printStackTrace();
            System.out.println("vodDemo error...");
            return fileId;
        }

    }

    /*public static void main(String[] args) {
        VodDemo.updload("F:/FFResult/1-2.mp4");
    }*/
}
