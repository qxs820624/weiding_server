package zs.live.utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.SignatureException;
import java.util.*;

public class XiaoYuSignatureUtil {
    private static final Logger log = LoggerFactory.getLogger(XiaoYuSignatureUtil.class);

    private static final String requestUriPrefix = "https://www.ainemo.com/api/rest/external/v1/";

    protected String computeStringToSign(String requestPath, Map<String, String> reqParams, String reqJsonEntity, String reqMethod) throws Exception {
        //1. request method
        StringBuffer strToSign = new StringBuffer(reqMethod);
        strToSign.append("\n");
        //2. request path
        strToSign.append(requestPath.substring(requestUriPrefix.length()));
        strToSign.append("\n");
        //3. sorted request param and value
        List<String> params = new ArrayList<>(reqParams.keySet());
        Collections.sort(params);
        for (String param : params) {
            strToSign.append(param);
            strToSign.append("=");
            try {
                strToSign.append(URLEncoder.encode(reqParams.get(param), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
            }
            strToSign.append("&");
        }
        strToSign.deleteCharAt(strToSign.length() - 1);
        strToSign.append("\n");
        //4. request entity
        byte[] reqEntity = reqJsonEntity.getBytes("utf-8");
        if (reqEntity.length == 0) {
            byte[] entity = DigestUtils.sha256("");
            strToSign.append(Base64.encodeBase64String(entity));
        } else {
            byte[] data = null;
            if (reqEntity.length <= 100) {
                data = reqEntity;
            } else {
                data = Arrays.copyOf(reqEntity, 100);
            }
            byte[] entity = DigestUtils.sha256(data);
            strToSign.append(Base64.encodeBase64String(entity));
        }

        String ret = strToSign.toString();
//	        log.info("strToSign :{}",ret);
        return ret;
    }

    private String calculateHMAC(String data, String key) throws SignatureException {
        try {
            SecretKeySpec e = new SecretKeySpec(key.getBytes("UTF8"), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(e);
            byte[] rawHmac = mac.doFinal(data.getBytes("UTF8"));
            String result = Base64.encodeBase64String(rawHmac);
            return result;
        } catch (Exception var6) {
            throw new SignatureException("Failed to generate HMAC : " + var6.getMessage());
        }
    }

    public String computeSignature(String jsonEntity, String method, String token, String reqPath) {
        try {
            Map<String, String> reqParams = new HashMap<>();
            int idx = reqPath.indexOf("?");
            String[] params = reqPath.substring(idx + 1).split("&");
            for (String param : params) {
                String[] pair = param.split("=");
                reqParams.put(pair[0], pair[1]);
            }
            reqPath = reqPath.substring(0, idx);
            String strToSign = computeStringToSign(reqPath, reqParams, jsonEntity, method);
            String mySignature = calculateHMAC(strToSign, token);
            mySignature = mySignature.replace(" ", "+");
            return URLEncoder.encode(mySignature, "utf-8");
        } catch (Exception e) {
            return null;
        }
    }

   /* public static void main(String[] args) throws Exception {
        String url = "https://www.ainemo.com/api/rest/external/v1/conferenceControl/nemo/584051/invitation?enterpriseId=a94a8fe5ccb19ba61c4c0873d391e987982fbbd3";
        String token = "e86d742dab336561c3bc01f12c8fef7a2322b2cf5db4323decd7d452ddb927f8";
        String jsonEntity = "{\"meetingRoomNumber\":\"913812341243\"}";
        log.info(new XiaoYuSignatureUtil().computeSignature(jsonEntity, "PUT", token, url));

    }*/
}
