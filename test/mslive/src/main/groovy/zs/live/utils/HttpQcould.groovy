package zs.live.utils

import groovy.util.logging.Slf4j
import javafx.beans.binding.StringBinding
import org.apache.commons.httpclient.params.HttpClientParams
import org.apache.http.params.CoreConnectionPNames
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zs.live.exception.ExceptionCode

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;

/**
 * 腾讯云http 请求类
 */
@Slf4j()
public class HttpQcould {
	public static final int HTTP_RETRIES = 3;
    public static final int HTTP_TIME_OUT = 5000 //秒
	private static MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
    public static final Logger httpQcloudLog = LoggerFactory.getLogger("httpQcloudLog")

    static{
        //System.setProperty("http.KeepAlive.remainingData", "wp"); //default 512k bytes
        System.setProperty("sun.net.http.errorstream.enableBuffering", "true"); //default false
        System.setProperty("http.maxConnections", "20"); //default 5
        System.setProperty("sun.net.http.errorstream.timeout", "300"); //default: 300 millisecond
        System.setProperty("sun.net.http.errorstream.bufferSize", "4096"); //default: 4096 bytes

    }

    /**
     *
     * @param urlParamMap   因为腾讯云直接请求post参数调用不同 所以暂时先采用这种方式 拼接url 对外方便使用还是采用post
     * @param urlprefix url前缀
     * @param postJson  post需要的json (子命令)
     * @return
     * @throws Exception
     */
    public static String post(Map<String,String> urlParamMap, String urlprefix,String postJson) throws Exception {
        //拼接url参数
        int count=0;
        for (String key : urlParamMap.keySet()) {
            if(count==0){
                urlprefix = urlprefix + "?" + key +"="+ urlParamMap.get(key);
            }else{
                urlprefix = urlprefix + "&" + key +"="+ urlParamMap.get(key);
            }
            count = count +1;
        }
        PostMethod method
        InputStream is
        BufferedReader br
        String response = ""
        for (int i = 0; i < HTTP_RETRIES; i++) {
            try {
                method = new PostMethod(urlprefix);
                HttpMethodParams param = method.getParams();
                param.setParameter(HttpMethodParams.RETRY_HANDLER,
                    new DefaultHttpMethodRetryHandler(HTTP_RETRIES, false));
                param.setParameter("Content-Type", "text/xml");
                //设置连接管理超时
                param.setLongParameter(HttpClientParams.CONNECTION_MANAGER_TIMEOUT,HTTP_TIME_OUT as long)
                param.setSoTimeout(HTTP_TIME_OUT)
                param.setContentCharset("UTF-8");
                RequestEntity requestEntity = new StringRequestEntity(postJson,"text/xml","UTF-8");
                method.setRequestEntity(requestEntity);
                HttpClient client = new HttpClient(connectionManager);
                client.getHttpConnectionManager().getParams().setConnectionTimeout(HTTP_TIME_OUT);
                client.getHttpConnectionManager().getParams().setSoTimeout(HTTP_TIME_OUT);
                client.executeMethod(method);
                is = method.getResponseBodyAsStream();
                br = new BufferedReader(new InputStreamReader(is));
                StringBuffer stringBuffer = new StringBuffer();
                while((response = br.readLine()) != null){
                    stringBuffer .append(response);
                }
                response = stringBuffer.toString();
                if (response.length() > 0) {
                    break;
                }else{
                    log.error("============>请求失败,重试");
                    httpQcloudLog.error(urlprefix, new SocketTimeoutException());
                }
            } finally {
                br?.close()
                method?.releaseConnection();
            }
        }
        if (!response || response.length() <= 0){
            httpQcloudLog.error("============>post请求重试3次后失败：{}",urlprefix)
        }
        return response;
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
            if(conn.getResponseCode()==HttpURLConnection.HTTP_OK){
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
            }else {
                dataStream=conn.getErrorStream()
            }

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
}
