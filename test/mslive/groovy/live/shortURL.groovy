import zs.live.ApiUtils
import zs.live.service.ShortURLService

ShortURLService service = ApiUtils.getBean(context, ShortURLService)
String input, result = null

switch (params.method) {
    case 'set':
        input = (isFilterToken(params.url)) ? ZURL.trans(params.url) : params.url
        String longURL = (new URL(input.replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll(' ', '&nbsp;').replaceAll('\r', '&r;').replaceAll('\n', '&n;').replaceAll('\t', '&t;').replaceAll('`', '%60')).toURI().toASCIIString())

        if (input) result = service.set(longURL)
        if (result) {
            out << Strings.toJson([
                    head: [status: 200],
                    //增加环境判断，处理测试环境短链接问题 by ls 20150806
                    body: service.test ? "http://${service.testAddress}/live/shortURL.groovy?id=$result".toString() : "http://tt.zhongsou.com/u/$result".toString()
            ])
        } else {
            out << Strings.toJson([head: [status: 500], body: ""])
        }
        break
    case 'get':
    default:
        input = params.id
        if (input) result = service.get(input)
        if (result) result = result.replaceAll('&lt;', '<').replaceAll('&gt;', '>').replaceAll('&nbsp;', ' ').replaceAll('&r;', '\r').replaceAll('&n;', '\n').replaceAll('&t;', '\t')
        StringBuilder sb = new StringBuilder()
        params.each { k, v ->
            if (!['method', 'id'].contains(k)) {
                sb.append('&').append(k).append('=');
                sb.append(getUTF8(v as String));
            }

        }
        if (result && sb.length() > 0) {
            if (result.contains('?'))
                result = result + sb.toString()
            else
                result = result + '?' + sb.deleteCharAt(0).toString()
        }
        response.sendRedirect(result ?: 'http://www.souyue.mobi/')
}

boolean isFilterToken(String url) {
    return (url.contains("mall/index.aspx") || url.contains("mobile/aclist") || url.contains("mobile/pdetail") || url.contains("ubox/detail.aspx")) ? false : true
}

String getUTF8(String str) {
    String result = str;
    if (str) {
        try {
            result = new String(str.getBytes("ISO-8859-1"), "UTF-8");
            if (isMessyCode(result)) {
                result = str;
            }
            result = URLDecoder.decode(result, "utf-8");
            result = URLEncoder.encode(result, "utf-8");
        } catch (UnsupportedEncodingException e) {
            System.out.println("getUTF8 Exception" + str);
        }
    }
    return result;
}

boolean isMessyCode(String str) {
    for (int i = 0; i < str.length(); i++) {
        char c = str.charAt(i);
        if ((int) c == 0xfffd || c == 0x3f) {
            return true;
        }
    }
    return false;
}
