package zs.live.utils

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.params.HttpMethodParams
import org.codehaus.groovy.runtime.StackTraceUtils

import javax.net.ssl.HttpsURLConnection
import javax.servlet.http.HttpServletRequest

/**
 * 普通http请求类
 */

@Slf4j
@CompileStatic
class Http {
    public static final int HTTP_TIME_OUT = 5000 //秒

    static String get(String url) {
        return get(url,5000)
    }

    static byte[] BOM = [0xef as byte, 0xbb as byte, 0xbf as byte]

    static String get(URL url) {
        byte[] bytes = url.bytes
        if (bytes?.length > 3 && bytes[0] == BOM[0] && bytes[1] == BOM[1] && bytes[2] == BOM[2]){
            return new String(bytes, 3, bytes.length - 3, 'UTF-8')
        }else{
            return new String(bytes, 'UTF-8')
        }
    }

    static String get(String url, int timeout) {
        if (!url) return null

        HttpClient htpc = new HttpClient();
        GetMethod getMethod = new GetMethod(url.trim());
        getMethod.getParams().setSoTimeout(timeout)
        getMethod.getParams().setParameter(
                HttpMethodParams.RETRY_HANDLER,
                new DefaultHttpMethodRetryHandler());

        byte[] bytes = null
        try {
            int statusCode = htpc.executeMethod(getMethod);
            if (statusCode < HttpStatus.SC_BAD_REQUEST) {
                bytes = getMethod.getResponseBody();
            }
            if (bytes && bytes?.length > 3 && bytes[0] == BOM[0] && bytes[1] == BOM[1] && bytes[2] == BOM[2]){
                return new String(bytes, 3, bytes.length - 3, 'UTF-8')
            }else if(bytes){
                return new String(bytes, 'UTF-8')
            }else{
                return null
            }
        } catch (Exception e) {
            log.error("http get to {},异常信息:",url,e)
        } finally {
            getMethod.releaseConnection();
        }
    }

    static String post(String url, Map<String, String> params) { post(url, params, 5000) }

    static String post(String url, Map<String, String> params, int timeout) {
        String ret = null, encoding = 'UTF-8'
        HttpURLConnection conn = null
        BufferedReader reader = null
        for (int i = 0; i < 3; i++) {
            try {
                String param = params?.collect({ k, v -> "${k}=${v}" })?.join('&')
                byte[] bytes = param?.getBytes(encoding)
                URL u = new URL(url)
                conn = u.openConnection() as HttpURLConnection
                conn.setRequestMethod('POST')
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("Content-Length", String.valueOf(bytes?.length ?: 0));
                conn.setRequestProperty("Content-Language", "en-US");
                conn.setConnectTimeout(timeout)
                conn.setReadTimeout(timeout)
                conn.setUseCaches(false);
                conn.setDoInput(true);
                conn.setDoOutput(true);

                //Send request
                conn.getOutputStream().write(bytes)
                conn.getOutputStream().flush()

                //Get Response
                InputStream is = conn.getInputStream();

                reader = new BufferedReader(new InputStreamReader(is, encoding));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                ret = response.toString()
                break;
            } catch (e) {
                log.error("http post to ${url}, params: ${params} 异常信息:{}", StackTraceUtils.sanitize(e), e.getMessage())
                continue;
            } finally {
                reader?.close()
                conn?.disconnect()
            }
        }

        return ret
    }

    static String postJSON(String url, String param) {
        int timeout=5000
        String ret = null, encoding = 'UTF-8'
        HttpURLConnection conn = null
        BufferedReader reader = null
        for (int i = 0; i < 3; i++) {
            try {
                byte[] bytes = param?.getBytes(encoding)
                URL u = new URL(url)
                conn = u.openConnection() as HttpURLConnection
                conn.setRequestMethod('POST')
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("Content-Length", String.valueOf(bytes?.length ?: 0));
                conn.setRequestProperty("Content-Language", "en-US");
                conn.setConnectTimeout(timeout)
                conn.setReadTimeout(timeout)
                conn.setUseCaches(false);
                conn.setDoInput(true);
                conn.setDoOutput(true);

                //Send request
                conn.getOutputStream().write(bytes)
                conn.getOutputStream().flush()

                //Get Response
                InputStream is = conn.getInputStream();

                reader = new BufferedReader(new InputStreamReader(is, encoding));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                ret = response.toString()
                break;
            } catch (e) {
                log.error("http post to ${url}, params: ${param} 异常信息:{}", StackTraceUtils.sanitize(e), e.getMessage())
                continue;
            } finally {
                reader?.close()
                conn?.disconnect()
            }
        }

        return ret
    }


    public static String execute(Map<String,String> urlParamMap, String urlprefix,String postJson,String Method) throws Exception {
        HttpsURLConnection conn = null;
        InputStream   dataStream =null;
        try {

            int count=0;
            for (String key : urlParamMap.keySet()) {
                if(count==0){
                    urlprefix = urlprefix + "?" + key +"="+ urlParamMap.get(key);
                }else{
                    urlprefix = urlprefix + "&" + key +"="+ urlParamMap.get(key);
                }
                count = 1;
            }

            URL url = new URL(urlprefix);
            conn = (HttpsURLConnection)url.openConnection();
            conn.setRequestMethod(Method);
            conn.addRequestProperty("Accept", "application/json");
            conn.addRequestProperty("Accept-Charset", "UTF-8");
            conn.addRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setConnectTimeout(HTTP_TIME_OUT);
            conn.setReadTimeout(HTTP_TIME_OUT);
            if(postJson!=null && !postJson.trim().equals("")){
                conn.getOutputStream().write(postJson.getBytes("utf-8"));
            }
            conn.connect();
            dataStream = conn.getInputStream();
            int formDataLength = conn.getContentLength();
            if(formDataLength>-1){
                byte[] body = new byte[formDataLength];
                int totalBytes = 0;
                while (totalBytes < formDataLength) {
                    int bytes = dataStream.read(body, totalBytes, formDataLength-totalBytes);
                    if (bytes == -1) {
                        break;
                    } else {
                        totalBytes += bytes;
                    }
                }

                if (totalBytes != formDataLength ){
                    log.error("an Exception has occurred before read to the end of the request.")
                    return "";
                }
                String respData = new String(body, 0, totalBytes,"UTF-8");
                // log.info("execute in HttpQcould:{}",respData);
                return respData;
            }
            dataStream.close();
        } catch (IOException e) {
            try {
                int  respCode = ((HttpsURLConnection) conn).getResponseCode();
                InputStream es = ((HttpsURLConnection) conn).getErrorStream();
                int ret = 0;
                byte[] buf = new byte[1024];
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                while ((ret = es.read(buf)) > 0) {
                    bos.write(buf,0,ret);
                }
                es.close();
                String msg=bos.toString("UTF-8");
                log.error("IOException while in  execute of HttpQcould.groovy respCode :{},msg :{}",msg,respCode,e);
            } catch (IOException ex) {
                log.error("IOException while in handle IOException execute of HttpQcould.groovy ",ex);
            }
            e.printStackTrace()
        }finally{
            if(dataStream!=null) dataStream.close();
        }
        return "";

    }

    public static String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.length() >= 30) {
            ip = ip.substring(0, 29);
        }
        if (ip == null || "".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }
}
