package zs.live.utils

import org.codehaus.groovy.runtime.StackTraceUtils

import java.security.MessageDigest
import java.util.regex.Pattern

/**
 * 解析中搜的搜索 URL
 */
class ZURL {
    static String parse(String url) { parse(url, null) }

    static String parse(String url, String encoding) {
        if (!url) return null

        String sw = extractSearchWord(url, 'w') ?: url
        if (encoding) return URLDecoder.decode(sw, encoding)

        String kw = tryDecodeGBK(sw)
        if (sw && !kw)
            try {
                boolean ok = false
                ((sw?.findAll('%')?.size() % 3) ? ['GBK'] : ['UTF-8', 'GBK',]).each {
                    if (!ok) {
                        def decoded = URLDecoder.decode(sw, it)
                        def encoded = URLEncoder.encode(decoded, it).replace('+', '%20')
                        if (sw.equals(encoded)) {
                            kw = decoded
                            ok = true
                        }
                    }
                }
            } catch (e) { StackTraceUtils.printSanitizedStackTrace(e) }
        kw
    }

    static String extractSearchWord(String url, String paramName) {
        String kw = null
        if (url?.length() > 0)
            try {
                def q = new URL(url).query
                q?.split('&+')?.each { String kv ->
                    if (kv) {
                        int pos = kv.indexOf('=')
                        String k = kv.substring(0, pos)
                        if (paramName.equalsIgnoreCase(k))
                            kw = kv.substring(Math.min(pos + 1, kv.length()))
                    }
                }
            } catch (e) { StackTraceUtils.printSanitizedStackTrace(e) }
        kw?.replace('+', ' ')
    }

    static String tryDecodeGBK(String str) {
        if (!str) return null
        try {
            boolean hasPercent = false
            StringBuilder sb = new StringBuilder()
            int pos = 0, len = str.length()
            byte[] bs = new byte[2]
            while (pos < len) {
                char c = str.charAt(pos)
                if (percent == c && pos + 5 < len) {
                    // 名称      第一字节             第二字节
                    // GB2312   0xB0-0xF7(176-247)  0xA0-0xFE(160-254)
                    // GBK      0x81-0xFE(129-254)  0x40-0xFE(64-254)
                    // Big5     0x81-0xFE(129-255)  0x40-0x7E(64-126) 0xA1－0xFE(161-254)
                    int b1 = Integer.parseInt(str[pos + 1..pos + 2], 16)
                    int b2 = Integer.parseInt(str[pos + 4..pos + 5], 16)
                    if (129 <= b1 && b1 <= 254 && 64 <= b2 && b2 <= 254) {
                        // GBK
                        bs[0] = b1 as byte
                        bs[1] = b2 as byte
                        sb.append(new String(bs, 'GBK'))
                        pos += 5
                    } else {
                        // 非 GBK 汉字的编码
                        hasPercent = true
                        sb.append(c)
                    }
                } else {
                    if (c == percent) hasPercent = true
                    sb.append(c)
                }
                pos++
            }
            if (hasPercent) {
                return null
            } else {
                return sb.toString()
            }
        } catch (ignored) {}
        return null
    }

    private static char percent = '%'

    /**
     * 图片的url，如果是 pic[0-9].zhongsou.com 的图
     * id 5xx -> 4xx
     * http://pic4.zhongsou.com/img?id=4801ccf0d47b2d441ea
     * http://pic4.zhongsou.com/img/4801ccf0d47b2d441ea
     * @param url
     * @return
     */
    static String transPic(url) {
        if (!url) return url
        String s = String.valueOf(url)
// 不用转换图片地址了
//        if (s ==~ /.*pic[0-9].zhongsou.com.*/) {
//            return s.replaceAll('=5', '=4').replaceAll('/5', '/4')
//        } else
        return replaceImageWithUpyun(s)
    }

    /**
     * 去掉统计增加的参数.如果带上短链的key很快就会消耗完了（一条公用新闻对每个人都要建个KEY）。
     *
     */
    static ps = ['token','userid','version']

    static String trans(url) {
        if (!url) return url
        String s = String.valueOf(url)
        ps.each {
            s = s.replaceAll("[?]${it}=[^&]*[&]?", "?")
                    .replaceAll('[?]$', "")
                    .replaceAll("[&]${it}=[^&]*", "")
        }
        s
    }

    /**
     * 判断是否过滤掉参数中的token
     * @param url
     * @return
     */
    static boolean isFilterToken(String url){
        return  (url.contains("mall/index.aspx")||url.contains("mobile/aclist"))?false:true
    }

    /**
     * 去掉表单微件中的参数
     *
     */
    static ps_deal = ['md5']

    static String dealUrl(String url){
        if (url && url.indexOf('Mdetail/getformdetail') != -1){
            ps_deal.each {
                url = url.replaceAll("[?]${it}=[^&]*[&]?", "?")
                        .replaceAll('[?]$', "")
                        .replaceAll("[&]${it}=[^&]*", "")
            }
        }
        url
    }

    public static def generateMD5(String s) {
        s=ZURL.trans(s)  //去掉token和uid这样的参数及值，他们是和用户相关
        MessageDigest digest = MessageDigest.getInstance("MD5")
        digest.update(s.bytes);
        new BigInteger(1, digest.digest()).toString(16).padLeft(32, '0')
    }

    /**
     * 将又拍云上的小图url转换成大图
     * http://souyue-image.b0.upaiyun.com/user/0001/98291047.jpg!android
     *
     * @param url
     * @return
     */
    public static String getUpyunOrigImage(String url) {
        String ret = url
        if (ret)
            ['!android', '!ios'].each {
                if (ret.endsWith(it))
                    ret = ret.substring(0, ret.length() - it.length())
            }
        return ret
    }

    /**
     * 原本在客户端处理的图片转换，改到服务器端
     *
     * http://pic.*.zhongsou.com/5205e570f01003bda4c
     * to
     * http://souyue-image.b0.upaiyun.com/newspic/list/5e/57/4205e570f01003bda4c_android.jpg!android
     * @param urlSource
     * @return
     */
    public static String replaceImageWithUpyun(String urlSource) {
        return zsPic.matcher(urlSource).replaceAll(Upyun)
    }
    static Pattern zsPic = Pattern.compile("http://pic.+zhongsou\\.com.+(w{3}(\\w{2})(\\w{2})\\w{12}).*") // 原图格式
    static String Upyun = 'http://souyue-image.b0.upaiyun.com/newspic/list/$2/$3/$1_android.jpg!android'  // 又拍云格式
}
