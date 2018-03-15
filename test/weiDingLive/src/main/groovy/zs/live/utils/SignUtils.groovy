package zs.live.utils

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Created by liyongguang on 2016/12/29.
 */
class SignUtils {
    // 编码方式
    private static final String CONTENT_CHARSET = "UTF-8";
    // HMAC算法
    private static final String HMAC_ALGORITHM = "HmacSHA1";

//    public static void main(String[] args){
//        TreeMap params = new TreeMap();
//        params.put("cdd","3")
//        params.put("ad","1")
//        params.put("ab","")
//
//        String secret = "hello world"
//        String paramsStr = buildParamStr(params)
//        println ("参与签名的字符串:" + paramsStr)
//
//        String sig = null;
//        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
//        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(CONTENT_CHARSET), mac.getAlgorithm());
//
//        mac.init(secretKey);
//        byte[] hash = mac.doFinal(paramsStr.getBytes(CONTENT_CHARSET));
//
//        sig = new String(Base64.encoder.encode(hash),CONTENT_CHARSET);
//
//        println ("sign = "+ sig)
//
//        boolean result = checkSign(params,sig,secret)
//        println(result)
//
//    }

    /**
     * 验证签名
     * @param requestParams 参数
     * @param sign 签名
     * @param secret 签名密钥
     *
     * @return
     */
    public static boolean checkSign(Map<String, Object> requestParams, String sign, String secret)  {
        if (!requestParams || !sign || !secret){
            return false
        }

        String signStr = buildParamStr(requestParams)

        String sig = null;
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(CONTENT_CHARSET), mac.getAlgorithm());

        mac.init(secretKey);
        byte[] hash = mac.doFinal(signStr.getBytes(CONTENT_CHARSET));

        // base64
        //sig = new String(new BASE64Encoder().encode(hash).getBytes());
        //sig = new String(Base64.encodeBase64(hash));
        sig = new String(Base64.encoder.encode(hash),CONTENT_CHARSET);

        return sign == sig
    }

    /**
     * 构造参与签名的字符串
     * @param requestParams
     * @return
     */
    protected static String buildParamStr(Map<String, Object> requestParams) {
        Map<String, Object> sortedMap = sortMapByKey(requestParams)
        StringBuilder retStr = new StringBuilder();
        for(String key: sortedMap.keySet()) {
            //排除上传文件的参数
            Object value = requestParams.get(key);
            if(value && value.toString().substring(0, 1).equals("@") ){
                continue;
            }
            if (retStr.length()>0) {
                retStr.append('&');
            }
            retStr.append(Strings.getUrlEncode(key,CONTENT_CHARSET))
                .append('=')
            if (value){
                retStr.append(Strings.getUrlEncode(value.toString(),CONTENT_CHARSET));
            }
        }
        return retStr.toString();
    }

    /**
     * 排序：Key的ASCII码升序(字典序)
     * @param oriMap
     * @return
     */
    private static Map<String, Object> sortMapByKey(Map<String, Object> oriMap) {
        Map<String, Object> sortedMap = new TreeMap<String, Object>(new Comparator<String>() {
            public int compare(String key1, String key2) {
                key1.compareTo(key2)
            }});

        if (oriMap == null || oriMap.isEmpty()) {
            return sortedMap;
        }

        sortedMap.putAll(oriMap)
        return sortedMap;
    }

}
