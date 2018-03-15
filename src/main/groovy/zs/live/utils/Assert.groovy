package zs.live.utils

import org.apache.commons.lang.StringUtils
import zs.live.ApiException

import javax.servlet.http.HttpServletRequest
import java.util.regex.Matcher
import java.util.regex.Pattern

class Assert {
    static void isNotBlank(HttpServletRequest request, String key) {
        if (StringUtils.isBlank(request.getParameter(key))) {
             throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "参数'${key}'不能为空")
        }
    }

    static void matchPattern(HttpServletRequest request, String key, String patternStr) {
        Pattern pattern = Pattern.compile(patternStr)
        Matcher matcher = pattern.matcher(request.getParameter(key))
        if (!matcher.matches()) {
            throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "参数'${key}'不能匹配模式'${patternStr}'")
        }
    }
    static void matchPatternParam(Map map, String key, String patternStr) {
        Pattern pattern = Pattern.compile(patternStr)
        Matcher matcher = pattern.matcher(map.get(key))
        if (!matcher.matches()) {
            throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "参数'${key}'不能匹配模式'${patternStr}'")
        }
    }

    static void eitherOrJudge(HttpServletRequest request, String key1,String key2){
        String value1=request.getParameter(key1);
        String value2=request.getParameter(key2);
        if(StringUtils.isBlank(value1)&&StringUtils.isBlank(value2)){
            throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "参数'${key1}'和参数'${key2}'不能同时为空")
        }
    }
    static void eitherOrJudgeParam(Map params, String key1,String key2){
        String value1=params?.get(key1);
        String value2=params?.get(key2);
        if(StringUtils.isBlank(value1)&&StringUtils.isBlank(value2)){
            throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "参数'${key1}'和参数'${key2}'不能同时为空")
        }
    }
    static void eitherOrJudgeParam(Map params, String key1,String key2,String key3){
        String value1=params?.get(key1);
        String value2=params?.get(key2);
        String value3=params?.get(key3);
        if(StringUtils.isBlank(value1)&&StringUtils.isBlank(value2)&&StringUtils.isBlank(value3)){
            throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "参数'${key1}'和参数'${key2}'和参数'${key3}'不能同时为空")
        }
    }
    static void eitherOrJudgeParam(Map params, String key1,String key2,String key3,String key4){
        String value1=params?.get(key1);
        String value2=params?.get(key2);
        String value3=params?.get(key3);
        String value4=params?.get(key4);
        if(StringUtils.isBlank(value1)&&StringUtils.isBlank(value2)&&StringUtils.isBlank(value3)&&StringUtils.isBlank(value4)){
            throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "参数'${key1}'和参数'${key2}'和参数'${key3}'和参数'${key4}'不能同时为空")
        }
    }
    static void isNotBlankParam(Map params, String key) {
        String value=params?.get(key);
        if (StringUtils.isBlank(value)) {
            throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "参数'${key}'不能为空")
        }
    }
}
