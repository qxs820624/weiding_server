package zs.live

import com.alibaba.fastjson.JSON
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.codec.digest.DigestUtils
import org.codehaus.groovy.runtime.MethodClosure
import org.codehaus.groovy.runtime.StackTraceUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import zs.live.utils.CipherUtil
import zs.live.utils.Strings
import zs.live.utils.UrlCountLimitMemcached
import zs.live.utils.VerUtils
import zs.live.utils.ZURL

import javax.servlet.ServletContext
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.concurrent.TimeUnit

@Slf4j
class ApiUtils {
    public static final Logger requestLog = LoggerFactory.getLogger("requestLog")

    public static final Logger apiExceptionLog = LoggerFactory.getLogger("apiExceptionLog")

    public static final String ENCRY="1" //加密

    public static final String NO_ENCRY="2" //不加密


    @CompileStatic
    static <T> T getBean(ServletContext context, String name) {
        ApplicationContext ctx = APP.instance.context
        Object o = ctx.getBean(name)
        return o == null ? null : (o as T)
    }

    @CompileStatic
    static <T> T getBean(String name) {
        ApplicationContext ctx = APP.instance.context
        Object o = ctx.getBean(name)
        return o == null ? null : (o as T)
    }

    @CompileStatic
    static <T> T getBean(ServletContext context, Class<T> clazz) {
        ApplicationContext ctx = APP.instance.context
        return ctx.getBean(clazz)
    }

    @CompileStatic
    static void process(Closure normal) {
        process(normal, null)
    }
    @CompileStatic
    static void processNoEncry(Closure normal) {
        processNoEncry(normal, null)
    }
    /**
     * @param normal 处理代码
     * @param options 杂项参数 <ul>
     *     <li>clientCacheMinute  告诉客户端可缓存的时间，默认 0，立即过期</li>
     *     <li>timeout  等待缓存更新读取等操作的时间，过期返回 null</li></ul>
     */
    static void process(Closure code, Map options) {
        Binding binding = code.binding
        processImpl(code, binding, options,ENCRY);
    }
    static void processNoEncry(Closure code, Map options) {
        Binding binding = code.binding
        processImpl(code, binding, options,NO_ENCRY);
    }



    /**
     *
     * @param code
     * @param binding
     * @param options
     * @param encryFlag 1 加密 2 非加密
     */
    @SuppressWarnings("GroovyUnusedAssignment")
    @CompileStatic
    private static void processImpl(Closure code, Binding binding, Map options,String encryFlag) {
        def ret = null
        int retCode = 200, clientCacheMinute = 0

        ServletContext context = null
        HttpServletRequest request = null
        Map params = null
        HttpServletResponse response = null
        PrintWriter out = null
        def reqBody = null
        try {
            context = binding.getVariable('context') as ServletContext
            request = binding.getVariable('request') as HttpServletRequest
            response = binding.getVariable('response') as HttpServletResponse
            out = binding.getVariable('out') as PrintWriter
            params = binding.getVariable('params') as Map

            // 读取请求内容
            BufferedReader br = new BufferedReader(new InputStreamReader(request.inputStream));
            String line = null;
            StringBuilder sb = new StringBuilder();
            while((line = br.readLine())!=null){
                sb.append(line);
            }

            // 将资料解码
            String reqBodyStr =  sb.toString();
            reqBody = Strings.parseJson(reqBodyStr)
            //---------解密---------------------
            def urlCountLimtCache = ApiUtils.getBean(context, UrlCountLimitMemcached.class) as UrlCountLimitMemcached
            def vc = getVc(params)
            params.vc = vc //处理非加密模式下，传2个VC的情况
            params.env = urlCountLimtCache.env
            if(ENCRY.equals(encryFlag)){
                //解密，测试环境支持不加密，其他环境必须使用加密
                if (urlCountLimtCache.env.equals("test") || urlCountLimtCache.env.equals("pre")) {
                    if (params.keySet().contains("lv_c")) {//如果加密参数存在，则执行解密
                        params = decryptParams(params, binding)
                        //url有效性判断
                        urlCountLimt(request,urlCountLimtCache)
                    }
                }else{
                    params = decryptParams(params, binding)
                    //url有效性判断
                    urlCountLimt(request,urlCountLimtCache)
                }
            }else{
                if (params.keySet().contains("lv_c")) {//如果加密参数存在，则执行解密
                    params = decryptParams(params, binding)
                    //url有效性判断
                    urlCountLimt(request,urlCountLimtCache)
                }
            }

            binding.setVariable('error', new MethodClosure(this, 'error'))
            initBeanMethod(binding, context)

            String url=request.getRequestURL().toString()
            requestLog.info(">>>handling request from client, url={},params={}",url,params)

            ret = code?.call()
            if (ret instanceof Map && ret.containsKey('code'))
                retCode = ret.remove('code') as int
            Object obj = options ? options.clientCacheMinute : null
            if (obj instanceof Integer) {
                clientCacheMinute = obj as int
            } else if (obj instanceof Closure) {
                Closure c = obj as Closure
                clientCacheMinute = c.call() as int
            }
        } catch (ApiException e) {
            retCode = (e as ApiException).getStatus()
            ret = (e as ApiException).getMessage()
            log.error(String.valueOf(ret), StackTraceUtils.deepSanitize(e))
        } catch(Throwable e){

            retCode = 500
            ret = e.message ?: '未知错误'

            retCode = ApiException.STATUS_UNKNOWN_ERROR
//                ret = e.message ?: '未知错误'
            ret = '网络异常，请稍候重试'
            Map m = Errors.get((e.cause ?: e).message)
            if (m) {
                if (m.code) retCode = m.code as int
                if (m.message) ret = m.message
            }

            apiExceptionLog.error(String.valueOf(ret), StackTraceUtils.deepSanitize(e))
        }
        respondJson(response,
            out,
            retCode,
            binding,
            ret,
            clientCacheMinute)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    @CompileStatic
    static void error(String name) { throw new Exception(name) }

    @CompileStatic
    static respondJson(HttpServletResponse response, PrintWriter out,
                       int status, Binding binding, Object data,
                       int expireInMinutes) {
        def head = binding.hasVariable('head') ? binding.getVariable('head') : null
        int cacheMinutes = 0
        if (status == 200) cacheMinutes = Math.max(expireInMinutes, 0)
        if (cacheMinutes == 0) {
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
            response.setHeader("Pragma", "no-cache"); // HTTP 1.0
        }
        response.setCharacterEncoding('UTF-8')
        response.setContentType("application/json")
        long now = System.currentTimeMillis()
        response.addHeader('EndTime', String.valueOf(now))
        response.addDateHeader('GenTime', now)
        response.addDateHeader('Expires', now + TimeUnit.MINUTES.toMillis(cacheMinutes))

        def oHead = [:]
        oHead.status = status
        oHead.hasMore = false
        if (head instanceof Map)
            oHead.putAll(head)
        else
            oHead.data = head
        def params = binding.getVariable('params') as Map
        String lv_c_us = (params.lv_c_us?:"") as String
        String lv_c_random =Strings.shortUUID().toLowerCase()
        String isFrom = (params.isFrom?:"") as String
        String env = params.env?:"test"
        String responseJson = ""
        if(lv_c_us && lv_c_random){//返回数据加密
            String aesKey = CipherUtil.getAesKey(env,isFrom,"encrypt",lv_c_us,lv_c_random)
            String enData
            if(data){
                enData = CipherUtil.encryptAES(JSON.toJSONString([head: oHead, body: data]),aesKey)
            }else{
                enData = CipherUtil.encryptAES(JSON.toJSONString([head: oHead, body: {}]),aesKey)
            }
            responseJson = JSON.toJSONString(["lv_c":enData,"lv_c_us":lv_c_us,"lv_c_random":lv_c_random])
        }else{
            responseJson = JSON.toJSONString([head: oHead, body: data])
        }
        requestLog.info(">>>response to the request, data json is:{}",responseJson)
        out << responseJson
    }

    //访问的url，1分钟内可以重复
    static void urlCountLimt(HttpServletRequest request,UrlCountLimitMemcached urlCountLimtCache) {
        String url = request.getRequestURL().toString() + request.getParameter("lv_c")
        String urlMd5 = generateMD5(url)
        long createTime = urlCountLimtCache.getUrlCreateDate(urlMd5)
        if (createTime == 0) {
            urlCountLimtCache.setUrlCreateDate(urlMd5, 30)
        } else { //转化为分钟
            int intervalTime = (System.currentTimeMillis() - createTime) / 60000 as int
            if (!urlCountLimtCache.env.equals("test") && !urlCountLimtCache.env.equals("pre")) {//测试预上线环境不过期
                if (intervalTime > 1) {
                    throw new ApiException(ApiException.STATUS_DUPLICATE_DATA, "链接不能频繁访问")
                }
            }
        }
    }

    static def generateMD5(String s) {
       ZURL.generateMD5(s)
    }


    static Map decryptParams(Map params,Binding binding){
        return decryptParamsAndroid(params,binding)
    }

    static Map decryptParamsAndroid(Map params,Binding binding){
        String msg = "解密失败"
        String msgParams = params
        try {
            def decryptStr
            String isFrom = params.isFrom?:""
            String lv_c = params.lv_c?:""
            String lv_c_us = params.lv_c_us?:""
            String lv_c_random = params.lv_c_random?:""
            String env = params.env?:"test"
            if(lv_c_us && lv_c_random){//新版解密
                String aesKey = CipherUtil.getAesKey(env,isFrom,"decrypt",lv_c_us,lv_c_random)
                decryptStr = CipherUtil.decryptAESNew(lv_c,aesKey)
            }else{//旧版解密
                decryptStr=CipherUtil.decryptAES(lv_c)
            }
            if(decryptStr){
                params=Strings.parseJson(decryptStr) as Map
                params.put("isEncrypt",true)
                if(lv_c_us && lv_c_random){//新版加密校验
                    params.put("lv_c_us",lv_c_us)
                    params.put("lv_c_random",lv_c_random)
                    params.put("isFrom",isFrom)
                    params.put("env",env)
                    String userId = params.uid?:""
                    String userIdSha1 = DigestUtils.sha1Hex(userId).substring(0, 16);
                    if(!lv_c_us.equals(userIdSha1)){
                        log.error("解密失败,userId校验失败,msgParams:{}",msgParams)
                        msg = "解密校验失败"
                        throw new ApiException(ApiException.STATUS_WRONG_PARAMS, msg)
                    }
                }
                binding.setVariable("params",params)
            } else {
                log.error("解密失败,decryptStr is null,msgParams:{}",msgParams)
                msg = "解密失败,decryptStr is null"
                throw new   ApiException(ApiException.STATUS_WRONG_PARAMS, msg)
            }
        } catch (Exception e){
            log.error("${msg}:{}",msgParams)
            throw new   ApiException(ApiException.STATUS_WRONG_PARAMS, msg)
        }
        return params
    }

    static String getVc(def params) {
        def vc = "4.0"
        if (params.vc) {
            if (params.vc instanceof String) {
                vc = params.vc ?: "4.0"
            } else {
                params.vc.each { //兼容vc传了2遍的问题
                    vc = it
                }
            }
        }
        return vc
    }

    /**
     * 初始化bean方法
     * @param binding
     */
    private static void initBeanMethod(Binding binding, ServletContext context) {
        def code = { def nameOrClazz ->
            switch (nameOrClazz.getClass()) {
                case String:
                    return getBean(context, nameOrClazz as String)
                case Class:
                    return getBean(context, nameOrClazz as Class)
            }
        }
        binding.setVariable('bean', code)
        binding.setVariable('getBean', code)
    }
}

class Errors {
    static Map get(String name) { errors.get(name) }

    static Map<String, Map> errors = { List lst ->
        def em = new HashMap()
        lst.each { Map m -> if (m?.name) em.put(m.name, m) }
        em
    }.call([
        [name: 'TOKEN', code: ApiException.STATUS_INVALID_TOKEN, message: '用户信息丢失，请重新登录'],
        [name: '', code: 500, message: '未知错误'],
    ])
}

