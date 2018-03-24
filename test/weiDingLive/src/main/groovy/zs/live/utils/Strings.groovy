package zs.live.utils

import com.alibaba.fastjson.JSON

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult
import org.jsoup.Jsoup
import org.jsoup.examples.HtmlToPlainText
import org.springframework.util.StringUtils
import sun.misc.BASE64Decoder

import javax.servlet.http.HttpServletRequest
import java.lang.reflect.Array
import java.security.MessageDigest
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.regex.Matcher
import java.util.regex.Pattern

@Slf4j
public class Strings {

    public static final String APP_NAME_SOUYUE = "weiding"//搜悦，搜悦简版，搜悦精华版的appname
    public static final String APP_NAME_FIGHT = "com.zhongsou.fightvison"//格斗世界的appname
    public static final String APP_NAME_GIRLU = "com.zhongsou.souyueu" //女U

    private static Gson gson = new GsonBuilder()
        .setVersion(1.0).setPrettyPrinting()
        .setDateFormat("yyyy-MM-dd HH:mm:ss").create();

    public static String toJson(Object obj) {
        if (!obj) return null
        return JSON.toJSONString(obj);
    }
/**
 * 返回去掉格式的json数据
 * @param obj
 * @return
 */
    public static String toJsonCompress(Object obj) {
        if (!obj) return null
        return toJson(obj).trim().replaceAll("\t", "").replaceAll("\r", "").replaceAll("\n", "")
    }

    public static Object parseJson(String json) {
        if (!json) {
            return null;
        }
        try {
            return new JsonSlurper().parseText(json);
        } catch (Exception e) {
            log.error("parse Json: " + json, e);
        }
        return null;
    }
    //参数decode一下
    public static Object parseJsonDecode(String json) {
        if (!json) {
            return null;
        }
        try {
            String decodeStr = URLDecoder.decode(json, "utf-8")
            return new JsonSlurper().parseText(decodeStr);
        } catch (Exception e) {
            log.error("parse Json: " + json, e);
        }
        return null;
    }

    public static Object parseJson(String json, Class clazz) {
        if (!json) {
            return null;
        }
        try {
            return JSON.parseObject(json, clazz)
        } catch (Exception e) {
            log.error("parseJson: " + json, e);
        }
        return null;
    }

    public static List parseArray(String json, Class clazz) {
        if (!json) {
            return null;
        }
        try {
            return JSON.parseArray(json, clazz)
        } catch (Exception e) {
            log.error("parseArray: " + json, e);
        }
        return null;
    }

    public static GPathResult parseXML(String xml) {
        if (xml) {
            try {
                def parser = new XmlSlurper()
                // parser.setFeature("http://xml.org/sax/features/external-general-entities", false)
                // parser.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                parser.setFeature("http://xml.org/sax/features/namespaces", false)
                parser.setFeature("http://xml.org/sax/features/validation", false);
                return parser.parseText(xml);
            } catch (Exception e) {
                log.error("parse xml: " + xml, e);
            }
        }
        return null;
    }

    public static text(htmlOrText) {
        if (htmlOrText)
            try {
                def doc = Jsoup.parse(String.valueOf(htmlOrText)?.replace('\n', '<br/>'))
                return new HtmlToPlainText().getPlainText(doc)?.replaceAll('[\n]+', '\n')
            } catch (ignored) {
            }

        return htmlOrText
    }


    static String toListString(Collection c) {
        StringBuffer sb = new StringBuffer();
        boolean isFirst = true;
        for (Object obj : c) {
            if (isFirst) {
                sb.append(obj.toString());
                isFirst = false;
            } else {
                sb.append(",");
                sb.append(obj.toString());
            }
        }
        return sb.toString()
    }

    static String toListString(Collection c,String separator) {
        StringBuffer sb = new StringBuffer();
        boolean isFirst = true;
        for (Object obj : c) {
            if (isFirst) {
                sb.append(obj.toString());
                isFirst = false;
            } else {
                sb.append(separator);
                sb.append(obj.toString());
            }
        }
        return sb.toString()
    }

    static String md5(String str) {
        return md5(str, null)
    }

    static String getMd5(String str, String code) {
        return md5(str, code)
    }

    static String md5(String str, String charset) {
        MessageDigest digest = MessageDigest.getInstance("MD5")
        if (charset) {
            digest.update(str.getBytes(charset))
        } else {
            digest.update(str.getBytes())
        }

        StringBuffer buf = new StringBuffer("");
        byte[] b = digest.digest();
        int i
        for (int offset = 0; offset < b.length; offset++) {
            i = b[offset];
            if (i < 0)
                i += 256;
            if (i < 16)
                buf.append("0");
            buf.append(Integer.toHexString(i));
        }
        return buf.toString();
    }

    static List<String> splitToList(String listStr) {
        def List<String> list = []
        def len = listStr.length()
        def start = 0
        def end = listStr.indexOf(',')
        while (true) {
            if (end > -1) {
                def str = listStr.substring(start, end)
                list << str?.trim()
                start = end + 1
                end = listStr.indexOf(',', start)
            } else {
                end = len
                def str = listStr.substring(start, end)
                list << str?.trim()
                break
            }
        }
        return list
    }

    static List<String> splitToList(String listStr,String separator) {
        def List<String> list = []
        def len = listStr.length()
        def start = 0
        def end = listStr.indexOf(separator)
        while (true) {
            if (end > -1) {
                def str = listStr.substring(start, end)
                list << str?.trim()
                start = end + 1
                end = listStr.indexOf(separator, start)
            } else {
                end = len
                def str = listStr.substring(start, end)
                list << str?.trim()
                break
            }
        }
        return list
    }

    static List<Long> splitToLongList(String listStr) {
        List<Long> longList = []
        def list = splitToList(listStr)
        list.each { longList << it.toLong() }
        return longList
    }

    static String getUrlParamVal(String paramName, String url) {
        url = url.replaceAll("%26", "&");
        def pattern = Pattern.compile('(^|\\?|&)' + paramName + '=(\\d*|\\w*)(&|$)');
        def matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return "";
    }

    public static String getBASE64(String s) {
        if (s == null) return "";
        return (new sun.misc.BASE64Encoder()).encode(s.getBytes());
    }
    // 将 BASE64 编码的字符串 s 进行解码
    public static String getFromBASE64(String s) {
        if (s == null) return "";
        def decoder = new BASE64Decoder();
        try {
            byte[] b = decoder.decodeBuffer(s);
            return new String(b);
        } catch (Exception e) {
            return "";
        }
    }

    public static String getUrlEncode(String str, String charset) {
        if (str) {
            return URLEncoder.encode(str, charset);
        }
        return "";
    }
    //得到UTF-8格式的字符串长度
    public static int getStrUTF8len(String str) {
        int len = 0;
        for (int i = 0; i < str.length(); i++) {
            def c = str.charAt(i);
            //单字节加1
            if ((c >= 0x0001 && c <= 0x007e) || (0xff60 <= c && c <= 0xff9f)) {
                len++;
            } else {
                len += 3;
            }
        }
        return len;
    }

    public static boolean isMobile(String mobile) {
        def regexp = "^1[3|4|5|7|8][0-9]\\d{8}\$"
        def pattern = Pattern.compile(regexp);
        def matcher = pattern.matcher(mobile);
        return matcher.find()
    }

    public static boolean isDigit(String mobile) {
        def regexp = "^\\d*\$"
        def pattern = Pattern.compile(regexp);
        def matcher = pattern.matcher(mobile);
        return matcher.find()
    }

    public static boolean isChinese(char c) {
        if ((c >= 0x0001 && c <= 0x007e) || (0xff60 <= c && c <= 0xff9f)) {
            return false;
        }
        return true;
    }

    public static String subStr(String str, int maxLen) {
        int emNum = 0;
        if (getStrUTF8len(str) > maxLen) {
            int endIndex = 0;
            String tmp = "";
            for (int i = 0; i < str.length(); i++) {
                if (endIndex < maxLen) {
                    tmp += String.valueOf(str.charAt(i));
                    if (isChinese(str.charAt(i))) {
                        endIndex = endIndex + 3;
                    } else {
                        endIndex++;
                    }
                } else {
                    break;
                }
            }
            return tmp + "...";
        }
        return str;
    }

    //过滤掉HTML标签
    def static String filterHtmlTag(String content) {
        String regEx_html = "<[^>]+>";
        def pattern = Pattern.compile(regEx_html);
        def matcher = pattern.matcher(content);
        def back = "";
        if (matcher.find()) {
            back = matcher.replaceAll("")
        } else {
            back = content
        }
        back = back.replaceAll(" ", "")
            .replaceAll("\\n", "")
            .replaceAll("\\r", "")
            .replaceAll("\\n\\r", "")
            .replaceAll("&nbsp;", "")
            .replaceAll("&gt;", "")
            .replaceAll("&emsp;", "")
        return back;
    }

    /**
     * 标题去html处理
     * @param content
     * @return
     */
    def static String titleHtmlTag(String title) {
        String regEx_html = "<[^>]+>";
        def pattern = Pattern.compile(regEx_html);
        def matcher = pattern.matcher(title);
        def back = "";
        if (matcher.find()) {
            back = matcher.replaceAll("")
        } else {
            back = title
        }
        back = back.replaceAll("\\n", "")
            .replaceAll("\\r", "")
            .replaceAll("\\n\\r", "")
            .replaceAll("&nbsp;", "")
            .replaceAll("&gt;", "")
            .replaceAll("&emsp;", "").trim()
        return back;
    }

    static boolean isRichText(String content) {
        String regEx_html = "<[^>]+>";
        def pattern = Pattern.compile(regEx_html);
        def matcher = pattern.matcher(content);
        return matcher.find()
    }

    public static boolean isEmail(String email) {
        def regexp = "^(\\w)+(\\.\\w+)*@(\\w)+((\\.\\w+)+)\$"
        def pattern = Pattern.compile(regexp);
        def matcher = pattern.matcher(email);
        return matcher.find()
    }

    public static boolean verifyMobile(String mobile) {
        def regexp = "^1[3|4|5|7|8][0-9]\\d{8}\$"
        def pattern = Pattern.compile(regexp);
        def matcher = pattern.matcher(mobile);
        return matcher.find()
    }
    //是否为安卓系统
    public static boolean isAndroidOs(HttpServletRequest request,def params) {
//        String agent = request.getHeader("User-Agent");
//        return (String.valueOf(agent).toLowerCase().contains("android")) ? true : false;
        //无法判断客户端类型的统一按ios算，因为需要保证ios机器直播能正常播放，ios客户端不支持rtmp（5.6.7以下版本）
        String appModel = (params.appModel ?:"iphone").toLowerCase()
        log.info("liaojing isAndroidOs,appModel=>{}",appModel)
        return ((appModel.contains("ipod")||appModel.contains("ipad")||appModel.contains("iphone"))) ? false : true;
    }
    // 是否大于vc2
    public static boolean isGreaterEqualVc2(String vc1, String vc2) {
        return VerUtils.toIntVer(vc1) >= VerUtils.toIntVer(vc2)
    }

    public static String gbkToUtf8(String str) {
        //System.out.println("------------:"+str)
        //System.out.println("======:"+getUTF8StringFromGBKString(str))
        return getUTF8StringFromGBKString(str)
    }

    public static String getUTF8StringFromGBKString(String gbkStr) {
        try {
            return new String(gbk2utf8(gbkStr), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new InternalError();
        }
    }

    public static byte[] gbk2utf8(String chenese) {
        char[] c = chenese.toCharArray();
        byte[] fullByte = new byte[3 * c.length];
        for (int i = 0; i < c.length; i++) {
            int m = (int) c[i];
            String word = Integer.toBinaryString(m);

            StringBuffer sb = new StringBuffer();
            int len = 16 - word.length();
            for (int j = 0; j < len; j++) {
                sb.append("0");
            }
            sb.append(word);
            sb.insert(0, "1110");
            sb.insert(8, "10");
            sb.insert(16, "10");

            String s1 = sb.substring(0, 8);
            String s2 = sb.substring(8, 16);
            String s3 = sb.substring(16);

            byte b0 = Integer.valueOf(s1, 2).byteValue();
            byte b1 = Integer.valueOf(s2, 2).byteValue();
            byte b2 = Integer.valueOf(s3, 2).byteValue();
            byte[] bf = new byte[3];
            bf[0] = b0;
            fullByte[i * 3] = bf[0];
            bf[1] = b1;
            fullByte[i * 3 + 1] = bf[1];
            bf[2] = b2;
            fullByte[i * 3 + 2] = bf[2];

        }
        return fullByte;
    }

    /**
     * 判断是否为数据
     * @param value
     * @return
     */
    public static boolean isDigital(String value) {
        Pattern pattern = Pattern.compile("[0-9]+");
        Matcher matcher = pattern.matcher((CharSequence) value);
        boolean result = matcher.matches();
        return result;
    }

    /***
     * 加密手机串
     * @param value
     * @return
     */
    public static String encryptMobileNO(String value) {
        Pattern p = Pattern.compile("^((13[0-9])|(15[^4,\\D])|(18[0-9]))\\d{8}\$");
        Matcher m = p.matcher(value);
        if (m.matches()) {
            String ret = value.substring(0, value.length() - (value.substring(3)).length()) + "****" + value.substring(7);
            return ret;
        }
        return value;
    }

    /**
     * 判断是否为模拟器
     * @return
     */
    public static boolean isVirtualDevice(String modeType) {
        boolean flag = false;
        if (modeType && (modeType.indexOf("lan") > -1 || modeType.indexOf("sdk") > -1 || modeType.indexOf("droid4x-win") > -1 || modeType.indexOf("t-mobile") > -1)) {
            flag = true;
        }
        return flag;
    }

    /**
     * 获取日期中的天
     * @return
     */
    public static String getDayOfDate(Date date) {
        if (!date) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    /**
     * 获取日期中的天
     * @return
     */
    public static String getDayOfTime(Date date) {
        if (!date) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }


    /**
     * 获取list是否为空
     * @return
     * true 空
     * false 不为空
     */
    public static boolean isListNul(List list) {
        if (list && list.size() > 0) {
            return false;
        } else {
            return true;
        }
    }

    public static boolean isListNotNul(List list) {
        if (list && list.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 获取appId
     * 如appName不存在，则获取appname，如都不存在则返回souyue
     * 如appname中包含souyue，则返回souyue，否则返回原appname
     * @param params
     * @return
     */
    public static String getAppId(def params) {
        String reStr = "weiding"
        if (params instanceof Map) {
            reStr = params.pfAppName
            if (!reStr) {
                reStr = params.appId ?: params.appName ?: params.appname ?:APP_NAME_SOUYUE
            }
        }
        return reStr
    }

    /**
     * 该方法是用来处理订阅的redis的key值
     * 方法比较固定，不是通用的方法
     * @param prefix
     * @param suffix
     * @return
     */
    public static String getRedisKey(String prefix, String suffix) {
        StringBuilder sb = new StringBuilder(128).append(prefix);
        if (suffix && !APP_NAME_SOUYUE.equals(suffix)) {
            sb.append("_").append(suffix)
        }
        return sb.toString()
    }

    /**
     * 去掉json数据中的空格及回车符
     * @param jsonData
     */
    static String getNoJsonData(String jsonData) {
        return jsonData ? jsonData.replaceAll("\t", "").replaceAll("\n", "") : ""
    }
    /**
     * 获得url中的参数
     * @param url
     * @param name
     * @return
     */
    static String getQueryString(String url, String name) {
        String value = "";
        url = url.substring(url.indexOf("?"))
        Pattern p = Pattern.compile("(^|\\?|&)" + name + "=\\d*");
        Matcher m = p.matcher(url);
        if (m.find()) {
            value = m.group(0)
        }
        String[] varr = value.split("=");
        if (varr.length > 1) {
            value = varr[1]
        }
        return value;
    }
    /**
     * 时间戳处理,时间戳处理成13位
     * User: ls
     */
    static long getCTime(long ctime) {
        if (ctime > 0) {
            String time = (ctime + "")
            if (time.length() < 13) time = time + "0000000000000"
            return time.substring(0, 13).toLong()
        }
        0
    }

    /**
     * 生成一个没有'-'的UUID
     */
    static String shortUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "")
    }

    /**
     * 处理正文图片方法
     * @param content
     * @return
     */
    static String repalceContentImg(String content) {
        if (StringUtils.hasText(content)) {
            Pattern p = Pattern.compile("<\\s*img(?![^<>]*?source[^<>]*?>).*?>", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(content);
            def a
            def b
            while (m.find()) {
                a = m.group();
                b = a.replaceAll("(<(?i)img[^>]*?)\\s+src(\\s*=\\s*\\S+)", " \$1 src=\"images/none.png\" source\$2");
                content = content.replace(a, b)
            }
        }
        content
    }

    /**
     * 取url中的参数
     * @param url
     * @param param
     * @return
     */
    static String getParamFormURL(String url, String param) {
        String returnParam = "";
        Pattern p = Pattern.compile(param + "=(.+?)(&|\$)");
        Matcher m = p.matcher(url);
        if (m.find()) {
            returnParam = m.group(1)
        }
        returnParam
    }

    static String getNumFormat(def num) {
        try {
            double res = num as double;
            return String.format("%.2f",res);
        }catch (Exception e){
            log.info("getNumFormat error=>{}",e.printStackTrace())
            return "0.00"
        }
    }

    static String getMultiplyNum(String str1,String str2) {
        String res = str1
        try {
            BigDecimal s1= new BigDecimal(str1)
            BigDecimal s2= new BigDecimal(str2)
            res = s1.multiply(s2)
            int index = res.indexOf(".")
            if(index >=0){
                res = res.substring(0,index)
            }
        }catch (Exception e){
            log.info("getMultiplyNum error=>{}",e.printStackTrace())
        }
        return res
    }

    static String getDivideNum(String str1,String str2) {
        String res = str1
        try {
            BigDecimal s1= new BigDecimal(str1)
            BigDecimal s2= new BigDecimal(str2)
            BigDecimal divide = s1.divide(s2)
            DecimalFormat df2  = new DecimalFormat("#0.00");
            res = df2.format(divide)
        }catch (Exception e){
            log.info("getDivideNum error=>{}",e.printStackTrace())
        }
        return res
    }
}
