import com.alibaba.fastjson.JSONObject
import zs.live.utils.CipherUtil
import zs.live.utils.Strings

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * url解析Json
 */
def str
def path=""
def shortPath=""
long a=System.currentTimeMillis()
String lv_c=request.getParameter("lv_c")?:""
def lv_c_us=request.getParameter("lv_c_us")
def lv_c_random=request.getParameter("lv_c_random")
def isFrom=request.getParameter("isFrom")
def type=request.getParameter("type")?:"request"
def env=request.getParameter("env")?:"0"
if (lv_c){
    lv_c=lv_c.trim()
//    if (lv_c.indexOf("%") > -1) {
//        lv_c = URLDecoder.decode(lv_c as String, "utf-8")
//    }
    def map=[:];
    if(lv_c.contains("lv_c=")){
        int index = lv_c.indexOf(".groovy")
        map.path = lv_c.substring(0,index+".groovy".length())
        map.content=Strings.getParamFormURL(lv_c,"lv_c")
        map.lv_c_us=Strings.getParamFormURL(lv_c,"lv_c_us")
        map.lv_c_random=Strings.getParamFormURL(lv_c,"lv_c_random")
    }else{
        //不含有lv_c,说明参数即是需要解密的字符串，至多后面跟着空格加 HTTP/1.1
        map.content=lv_c.trim()
        map.lv_c_us = lv_c_us.trim()
        map.lv_c_random = lv_c_random.trim()
    }
    if(map.content){
        map.content=(map.content as String).trim().replaceAll(" ","")
        if(map.lv_c_us && map.lv_c_random){
            isFrom=isFrom?:"app";
            String aesKey = "";
            if("1".equals(env)){
                env = "online"
            }else{
                env = "test"
            }
            if("return".equals(type)){
                aesKey = CipherUtil.getAesKey(env as String,isFrom,"encrypt",map.lv_c_us,map.lv_c_random)
            }else{
                aesKey = CipherUtil.getAesKey(env  as String,isFrom,"decrypt",map.lv_c_us,map.lv_c_random)
            }
            str= CipherUtil.decryptAESNew(map.content,aesKey)
        }else{
            str= CipherUtil.decryptAES(map.content)
        }
        map.str=str;

        long b=System.currentTimeMillis()
        str = "耗时："+(b-a)+"毫秒， 解密后："+str
    }
    if("return".equals(type)){
        str = str
    }else if(map.str){
        map.result="";
        map.shortResult="";
        JSONObject jsonObject = JSONObject.parseObject(map.str) ;
       for(Map.Entry<String,Object> entry:jsonObject.entrySet()){
           map.result+=entry.getKey()+"="+entry.getValue()+"&"
           if(!["pv","opSource","ct","imei","state","netType","channel","lat","appName","long","sy_t"].contains(entry.getKey())){
               map.shortResult+=entry.getKey()+"="+entry.getValue()+"&"
           }
       }

        if((map.result as String).endsWith("&")){
            map.result=(map.result as String).substring(0,(map.result as String).length()-1)
        }
        if((map.shortResult as String).endsWith("&")){
            map.shortResult=(map.shortResult as String).substring(0,(map.shortResult as String).length()-1)
        }
        if(map.path){
            path+=map.path+"?"+map.result
            shortPath+=map.path+"?"+map.shortResult
        }else{
            path+=map.result;
            shortPath+=map.shortResult
        }

        if(map.vc){
            path+="&"+map.vc;
            shortPath+="&"+map.vc
        }
    }
}

def html = """
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
</head>
<body>
<form action="/live/tools/decrypt.groovy" method="post">
<table>
    <tr>
        <td>lv_c:</td>
        <td>
            <input type="text" name="lv_c" style="width: 800px; height: 30px;">
        </td>
    </tr>
    <tr>
        <td>lv_c_us:</td>
        <td>
            <input type="text" name="lv_c_us" style="width: 800px; height: 30px;">
        </td>
    </tr>
    <tr>
        <td>lv_c_random:</td>
        <td>
            <input type="text" name="lv_c_random" style="width: 800px; height: 30px;">
        </td>
    </tr>
    <tr>
        <td>isFrom:</td>
        <td>
            <input type="text" name="isFrom" style="width: 300px; height: 30px;">h5或者app,默认是app
        </td>
    </tr>
    <tr>
        <td>何种解密:</td>
        <td>
            <input type="text" name="type" style="width: 300px; height: 30px;">request(请求数据解密)或者return(返回数据解密),默认是request
        </td>
    </tr>
    <tr>
        <td>环境:</td>
        <td>
            <input type="text" name="env" style="width: 300px; height: 30px;">0:测试，1：线上，默认是测试
        </td>
    </tr>
    <tr>
        <td colspan="2" align="center" style="width: 300px; height: 30px;">
        <input type="submit" value="提交">
        </td>
    </tr>
    <tr>
        <td>备注:</td>
        <td>
            lv_c字段即支持老版解密，同时也支持新版数据源url解密，新版示例如下:<br/>
            <br/>
            lv_c=E8B5262682D191F5647C27DC4758917FE6xH%2BlDMwYcUWC9bvWIVfhWudiVdvmPB%2BvgdmezD7ohJxJAwlYVaOfT6qCryIVzclhgxiDc=&lv_c_us=828f720439cefaeb&lv_c_random=777a65b9476e4ed39959d1c4d1335e4a
        </td>
    </tr>
</table>
</br>
${str? str+" <hr/>":""}<br/>
${path?path+" <hr/>":""}<br/>
${shortPath?shortPath+" <hr/>":""}<br/>
</form>
</body>
</html>
"""
out << html
