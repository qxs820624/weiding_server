import zs.live.ApiUtils
import zs.live.common.LiveCommon
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.dao.redis.LiveRedis


LiveQcloudRedis liveQcloudRedis = ApiUtils.getBean(context, LiveQcloudRedis)
LiveRedis liveRedis = ApiUtils.getBean(context,LiveRedis)
LiveCommon liveCommon = ApiUtils.getBean(context,LiveCommon)
String env = liveCommon.liveEnv

def html = """
<!doctype html>
<html>
<head>
  <meta charset="utf-8">
  <meta http-equiv="X-UA-Compatible" content="IE=edge">
  <meta name="description" content="">
  <meta name="keywords" content="">
  <meta name="viewport"
        content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
  <title>搜悦上线APPSTORE信息管理</title>

  <!-- 禁止搜索引擎收录 -->
  <META NAME="ROBOTS" CONTENT="NOINDEX, NOFOLLOW">

  <!-- Set render engine for 360 browser -->
  <meta name="renderer" content="webkit">

  <!-- No Baidu Siteapp-->
  <meta http-equiv="Cache-Control" content="no-siteapp"/>


  <!-- Add to homescreen for Chrome on Android -->
  <meta name="mobile-web-app-capable" content="yes">

  <!-- Add to homescreen for Safari on iOS -->
  <meta name="apple-mobile-web-app-capable" content="yes">
  <meta name="apple-mobile-web-app-status-bar-style" content="black">

  <!-- Tile icon for Win8 (144x144 + tile color) -->
  <meta name="msapplication-TileColor" content="#0e90d2">
  <script src="http://libs.baidu.com/jquery/2.0.0/jquery.min.js"></script>
  <script src="http://cdn.amazeui.org/amazeui/2.1.0-beta1/js/amazeui.min.js"></script>
  <link href="http://cdn.amazeui.org/amazeui/2.1.0-beta1/css/amazeui.min.css" rel="stylesheet" type="text/css">
</head>
<body style="height:200%">
<div id="a" data-am-widget="titlebar" class="am-titlebar am-titlebar-default">
<h2 class="am-titlebar-title">搜悦上线APPSTORE信息管理</h2>
</div>
<form>
<div>
输入操作的APPName(必填项):
<input type="text" name="appName"/>
输入操作的版本号(必填项):
<input type="text" name="version"/>
</div>
<div>
<input type="hidden" name="sy_check" value="admin_system_person"/>
</div>
<button type="submit" class="am-btn am-btn-default" name="query_appstore_status" value="query_appstore_status">点击设置APPSTORE版本信息</button>
</form>
</body>
"""


def sy_check = request.getParameter("sy_check")? request.getParameter("sy_check") as String:""
println  sy_check
if(sy_check==null || "".equals(sy_check) || !"admin_system_person".equals(sy_check)){
    return "没有权限访问"
}


def query = request.getParameter("query_appstore_status")?request.getParameter("query_appstore_status") as String:""
def version = request.getParameter("version")?request.getParameter("version") as String:""
String appName = request.getParameter("appName")?request.getParameter("appName") as String:""
String back = request.getParameter("back")?request.getParameter("back") as String:""


if(query!=null && !"".equals(query) && "query_appstore_status".equals(query)){
    String souyueVersion=""
    if(env.equals("pre")){
        liveRedis.hset(liveQcloudRedis.SY_APPSTORE_SOUYUE_VERSION_PRE+appName,liveQcloudRedis.SY_APPSTORE_SOUYUE_FIELD_PRE,version)
         souyueVersion = liveRedis.hget(liveQcloudRedis.SY_APPSTORE_SOUYUE_VERSION_PRE+appName,liveQcloudRedis.SY_APPSTORE_SOUYUE_FIELD_PRE)
    }else{
         liveRedis.hset(liveQcloudRedis.SY_APPSTORE_SOUYUE_VERSION+appName,liveQcloudRedis.SY_APPSTORE_SOUYUE_FIELD,version)
         souyueVersion = liveRedis.hget(liveQcloudRedis.SY_APPSTORE_SOUYUE_VERSION+appName,liveQcloudRedis.SY_APPSTORE_SOUYUE_FIELD)
    }

  html = """
  <!doctype html>
  <html>
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="description" content="">
    <meta name="keywords" content="">
    <meta name="viewport"
          content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
    <title>搜悦版本升级版本信息管理</title>

    <!-- 禁止搜索引擎收录 -->
    <META NAME="ROBOTS" CONTENT="NOINDEX, NOFOLLOW">

    <!-- Set render engine for 360 browser -->
    <meta name="renderer" content="webkit">

    <!-- No Baidu Siteapp-->
    <meta http-equiv="Cache-Control" content="no-siteapp"/>


    <!-- Add to homescreen for Chrome on Android -->
    <meta name="mobile-web-app-capable" content="yes">

    <!-- Add to homescreen for Safari on iOS -->
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="black">

    <!-- Tile icon for Win8 (144x144 + tile color) -->
    <meta name="msapplication-TileColor" content="#0e90d2">
    <script src="http://libs.baidu.com/jquery/2.0.0/jquery.min.js"></script>
    <script src="http://cdn.amazeui.org/amazeui/2.1.0-beta1/js/amazeui.min.js"></script>
    <link href="http://cdn.amazeui.org/amazeui/2.1.0-beta1/css/amazeui.min.css" rel="stylesheet" type="text/css">
  </head>
  <body style="height:200%">
  <div id="a" data-am-widget="titlebar" class="am-titlebar am-titlebar-default">
  <h2 class="am-titlebar-title">搜悦版本升级版本信息管理</h2>
  </div>
  <form>
  <div>
  <input type="hidden" name="sy_check" value="admin_system_person"/>
  操作的appName为:<input type="text" name="appName" value="${appName}" readonly="true"/>
  操作的版本为:<input type="text" name="version" value="${version}" readonly="true"/>
  <br/>
  (输入新的appName或者新的版本后按回车即可修改appName,版本的信息)
  <h1/>
  </div>
  <table>
    <tr>
         <td align="center">
         当前版本号
         </td>

         <td align="center">
         页面名称
         </td>

         <td align="center">
         是否屏蔽代码
         </td>

     </tr>

     <tr>

         <td>
            <div class="am-form-group">
              <input type="text" class="am-form-field am-radius" name="version" value="${souyueVersion}" readonly="readonly"/>
            </div>
         </td>

         <td>
            <div class="am-form-group">
              <input type="text" class="am-form-field am-radius" value="widget.list.groovy" readonly="readonly"/>
            </div>
         </td>

         <td>
            <div class="am-form-group">

              <input type="text" class="am-form-field am-radius" value="是" readonly="readonly"/>
            </div>

         </td>

     </tr>

     <tr>

          <td>
            <div class="am-form-group">
              <input type="text" class="am-form-field am-radius" name="version" value="${souyueVersion}" readonly="readonly"/>
            </div>
         </td>

         <td>
            <div class="am-form-group">
              <input type="text" class="am-form-field am-radius" value="search.result.groovy" readonly="readonly"/>
            </div>
         </td>

          <td>
            <div class="am-form-group">

              <input type="text" class="am-form-field am-radius" value="是" readonly="readonly"/>
            </div>

         </td>

     </tr>


     <tr>

          <td>
            <div class="am-form-group">
              <input type="text" class="am-form-field am-radius" name="version" value="${souyueVersion}" readonly="readonly"/>
            </div>
         </td>

         <td>
            <div class="am-form-group">
              <input type="text" class="am-form-field am-radius" value="sy.pay.groovy" readonly="readonly"/>
            </div>
         </td>



         <td>
            <div class="am-form-group">

              <input type="text" class="am-form-field am-radius" value="是" readonly="readonly"/>
            </div>

         </td>

     </tr>

     <tr>

          <td>
            <div class="am-form-group">
              <input type="text" class="am-form-field am-radius" name="version" value="${souyueVersion}" readonly="readonly"/>
            </div>
         </td>

         <td>
            <div class="am-form-group">
              <input type="text" class="am-form-field am-radius" value="sy.applys.groovy" readonly="readonly"/>
            </div>
         </td>



         <td>

            <div class="am-form-group">
              <input type="text" class="am-form-field am-radius" value="是" readonly="readonly"/>
            </div>
         </td>

     </tr>

    <tr>
        <td></td>
        <td></td>
        <td>
            <form>
               <input type="hidden" name="sy_check" value="admin_system_person"/>
               <button type="submit" name="back" value="back">返回</button>
            </form>
        </td>
    </tr>
    </table>
  </form>
  <br/>
"""
}


if(back!=null && !"".equals(back) && "back".equals(back)){
    html = """
<!doctype html>
<html>
<head>
  <meta charset="utf-8">
  <meta http-equiv="X-UA-Compatible" content="IE=edge">
  <meta name="description" content="">
  <meta name="keywords" content="">
  <meta name="viewport"
        content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
  <title>搜悦上线APPSTORE信息管理</title>

  <!-- 禁止搜索引擎收录 -->
  <META NAME="ROBOTS" CONTENT="NOINDEX, NOFOLLOW">

  <!-- Set render engine for 360 browser -->
  <meta name="renderer" content="webkit">

  <!-- No Baidu Siteapp-->
  <meta http-equiv="Cache-Control" content="no-siteapp"/>


  <!-- Add to homescreen for Chrome on Android -->
  <meta name="mobile-web-app-capable" content="yes">

  <!-- Add to homescreen for Safari on iOS -->
  <meta name="apple-mobile-web-app-capable" content="yes">
  <meta name="apple-mobile-web-app-status-bar-style" content="black">

  <!-- Tile icon for Win8 (144x144 + tile color) -->
  <meta name="msapplication-TileColor" content="#0e90d2">
  <script src="http://libs.baidu.com/jquery/2.0.0/jquery.min.js"></script>
  <script src="http://cdn.amazeui.org/amazeui/2.1.0-beta1/js/amazeui.min.js"></script>
  <link href="http://cdn.amazeui.org/amazeui/2.1.0-beta1/css/amazeui.min.css" rel="stylesheet" type="text/css">
</head>
<body style="height:200%">
<div id="a" data-am-widget="titlebar" class="am-titlebar am-titlebar-default">
<h2 class="am-titlebar-title">搜悦上线APPSTORE信息管理</h2>
</div>
<form>
<div>
输入操作的APPName(必填项):
<input type="text" name="appName"/>
输入操作的版本号(必填项):
<input type="text" name="version"/>
</div>
<div>
<input type="hidden" name="sy_check" value="admin_system_person"/>
</div>
<button type="submit" class="am-btn am-btn-default" name="query_appstore_status" value="query_appstore_status">点击设置APPSTORE版本信息</button>
</form>
</body>
"""
}
out << html
