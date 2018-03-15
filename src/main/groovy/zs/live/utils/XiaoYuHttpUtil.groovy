package zs.live.utils

import java.util.concurrent.TimeUnit
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import groovy.json.JsonSlurper

class XiaoYuHttpUtil {

	private static final Logger log = LoggerFactory.getLogger(XiaoYuHttpUtil.class);

	public static Map execute(URL url,String Method,String jsonEntity,long timeout,TimeUnit unit){
        log.info("XiaoYuHttpUtil url:{}",url);
		def jsonSlurper = new JsonSlurper();
		HttpURLConnection conn=null;
		Map ret=[:]
		try{
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod(Method);
			conn.addRequestProperty("Accept", "application/json");
			conn.addRequestProperty("Accept-Charset", "UTF-8");
			conn.addRequestProperty("Content-Type", "application/json");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setConnectTimeout((int)unit.toMillis(timeout));
			conn.setReadTimeout((int)unit.toMillis(timeout));
			if(jsonEntity!=null && !jsonEntity.trim().equals("")){
		    conn.getOutputStream().write(jsonEntity.getBytes("utf-8"));
			}
			conn.connect();


			ret.statusCode=conn.getResponseCode();
			ret.reasonPhrase=conn.getResponseMessage();
			InputStream is = conn.getInputStream();
			int formDataLength = conn.getContentLength();

			if(formDataLength>-1){
				InputStream dataStream = conn.getInputStream();
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

				if (totalBytes != formDataLength)
					log.error("an Exception has occurred before read to the end of the request.")


				String respData = new String(body, 0, formDataLength,"UTF-8");
//				log.info("response body string :{}",respData);
				def responseRet=jsonSlurper.parseText(respData);
				if(responseRet instanceof List){
					ret.put("list",responseRet);
				}else{
			     	ret.putAll(responseRet);
				}
			}
		}catch(	Exception ex ){
			log.error("an Exception has occurred ",ex);
		}finally{
			if(conn!=null)
				conn.disconnect();
		}

		return ret;
	}
}
