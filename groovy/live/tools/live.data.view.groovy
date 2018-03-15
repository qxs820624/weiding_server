import groovy.sql.GroovyRowResult
import zs.live.ApiUtils
import zs.live.dao.mysql.LiveRes
import zs.live.service.LiveService

/**
 * url解析Json
 */
LiveRes liveRes = ApiUtils.getBean(context,LiveRes)
LiveService liveService = ApiUtils.getBean(context,LiveService)
String sql=request.getParameter("sql")
System.out.println(sql+"=======")
List<GroovyRowResult> dataList = []
if(sql){
    sql = sql.trim()
    if((sql.startsWith("select") || sql.startsWith("SELECT") )&& (sql.contains("limit") || sql.contains("LIMIT"))){
        dataList = liveRes.findDataListBySql(sql)
    }
}


def html = """
    <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
    <html xmlns="http://www.w3.org/1999/xhtml">
    <head>
    <style type="text/css">
        .xwtable {width: 100%;border-collapse: collapse;border: 1px solid #ccc;}
        .xwtable thead td {font-size: 12px;color: #ccc;text-align: center;border: 1px solid #ccc; font-weight:bold;}
        .xwtable tbody tr {background: #fff;font-size: 15px;color: #666666;}
        .xwtable tbody tr.alt-row {background: #f2f7fc;}
        .xwtable td{line-height:20px;text-align: left;padding:4px 10px 3px 10px;height: 18px;border: 1px solid #ccc;}
    </style>
    </head>
    <body>
    <form action="/live/tools/live.data.view.groovy" method="post">
    sql:</br>
    <textarea rows="10" cols="200" name= "sql">${sql ?:""}</textarea>
    </br>
    <input type="submit" value="提交" onclick="display_alert()"/> 
    </br>
    举例:select * from live_foreshow  order by foreshow_id desc  limit 0,10<br>
        select * from live_record  order by live_id desc  limit 0,10<br>
        select * from live_record_log  order by live_id desc  limit 0,10<br>
"""
if(dataList){
    html+=""" <table class="xwtable">"""
    for(int i = 0; i<dataList.size();i++){
        Set keys = dataList.get(i).keySet()
        if(i == 0){
            html+="""<thead><tr>"""
            keys.each{
                html+="""<th>${it}</th>"""
            }
            html+="""</tr></thead> <tbody><tr>"""

        }
        for(String key: keys){
            String value = dataList.get(i).get(key)
            html+="""<td>${value}</td>"""
        }
        html+=""" </tr>"""
    }
    html+=""" </tbody></table>"""
}
html+= """</form>
    </body>
    </html>
    """
out << html

