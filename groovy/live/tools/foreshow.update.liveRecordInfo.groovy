import com.alibaba.fastjson.JSON
import zs.live.ApiUtils
import zs.live.dao.mysql.LiveRes
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.service.VideoMergeService
import zs.live.utils.Strings

import static java.net.URLEncoder.*

String foreshowIdStr=request.getParameter("foreshowId")?.trim()
String durationStr=request.getParameter("duration")?.trim()
String fileIdStr=request.getParameter("fileId")?.trim()
String address1Str=request.getParameter("address1")?.trim()
String address2Str=request.getParameter("address2")?.trim()
String address3Str=request.getParameter("address3")?.trim()
String address4Str=request.getParameter("address4")?.trim()
String address5Str=request.getParameter("address5")?.trim()
def retrunLiveRecordInfo = ""
try{
    boolean flag = true
    long foreshowId = (foreshowIdStr ?:0) as long
    int duration = (durationStr ?:0) as int
    if(!foreshowId){
        retrunLiveRecordInfo = "foreshowId不能为空"
        flag = false
    }else if(duration <=0){
        retrunLiveRecordInfo = "视频时长不能为空"
        flag = false
    }else if(!fileIdStr || fileIdStr.length() != 19){
        System.out.println("fileIdStr:"+fileIdStr+",length:"+fileIdStr.length())
        retrunLiveRecordInfo = "请填写正确的fileId"
        flag = false
    }else if(!address1Str && !address2Str && !address3Str && !address4Str && !address5Str){
        retrunLiveRecordInfo = "回放地址不能为空"
        flag = false
    }
    if(flag){
        LiveRes liveRes = ApiUtils.getBean(context,LiveRes)
        QcloudLiveService qcloudLiveService = ApiUtils.getBean(context,QcloudLiveService)

        def liveForeshow = liveRes.findLiveForeshowByForeshowId(foreshowId, "")
        if(!liveForeshow){
            retrunLiveRecordInfo = "预告不存在"
            flag = false
        }
        if(flag){
            def liveRecordInfo = liveForeshow?.live_record_info
            System.out.println(liveRecordInfo)
            def videoAddress = liveRecordInfo ? Strings.parseJson(liveRecordInfo as String).video_address : ""
            if(videoAddress){  //判断之前是否合并过文件需要删除之前的合并文件
                def fileIds = JSON.parse(videoAddress)?.fileId
                List fileIdList = []
                for(String fielId:fileIds){
                    fileIdList.addAll(Strings.splitToList(fielId))
                }
                if(fileIdList && fileIdList.size() >= 2){
                    String res = qcloudLiveService.deleteVodFile(fileIdList,liveForeshow.app_id)
                    retrunLiveRecordInfo = "删除原合并文件成功，fileIdList:${fileIdList}"
                    if(!res.contains("Success")){
                        retrunLiveRecordInfo = "删除原合并文件失败，fileIdList:${fileIdList}"
                        ApiUtils.log.info("foreshow.update.liveRecordInfo deleteVodFile error，foreshowId=>{},fileIdList=>{},res=>{}",foreshowId,fileIdList,res)
                    }
                }else{
                    ApiUtils.log.info("foreshow.update.liveRecordInfo fileIdList is one，fileIdList=>{}",fileIdList)
                }
            }else{
                ApiUtils.log.info("foreshow.update.liveRecordInfo videoAddress is null，foreshowId=>{}",foreshowId)
            }
            //修改foreshow表中的live_record_info内容
            StringBuffer live_record_info = new StringBuffer()
            live_record_info.append("{\"time_span\":${duration},\"video_address\": \"[{\\\"duration\\\":${duration},\\\"playSet\\\":[")
            if(address1Str){
                live_record_info.append("{\\\"url\\\":\\\"${address1Str}\\\"}")
            }
            if(address2Str){
                live_record_info.append(",{\\\"url\\\":\\\"${address2Str}\\\"}")
            }
            if(address3Str){
                live_record_info.append(",{\\\"url\\\":\\\"${address3Str}\\\"}")
            }
            if(address4Str){
                live_record_info.append(",{\\\"url\\\":\\\"${address4Str}\\\"}")
            }
            if(address5Str){
                live_record_info.append(",{\\\"url\\\":\\\"${address5Str}\\\"}")
            }
            live_record_info.append("],\\\"fileId\\\":\\\"${fileIdStr}\\\"}]\"}")
            int foreshow_status = liveForeshow.foreshow_status as int
            int res = liveRes.updateLiveForeshowLiveRecordInfo(live_record_info.toString(),foreshow_status,foreshowId,liveForeshow.app_id)
            if(res == 1){
                retrunLiveRecordInfo =retrunLiveRecordInfo+"，修改成功"
                foreshowIdStr="";durationStr="";fileIdStr="" ;
                address1Str="";address2Str="";address3Str="";address4Str="";address5Str=""
            }else{
                retrunLiveRecordInfo =retrunLiveRecordInfo+",修改失败"
            }
        }
    }
}catch (Exception e){
    retrunLiveRecordInfo = e.getMessage()
    e.printStackTrace()
}
def html = """
    <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
    <html xmlns="http://www.w3.org/1999/xhtml">
    <head></head>
    <body>
        <form action="/live/tools/foreshow.update.liveRecordInfo.groovy" method="post">
        </br>
        <font size="3" color="red">foreshowId(预告id):</font>
        <input type="text" name= "foreshowId" value ="${foreshowIdStr ?:""}"/>&nbsp;&nbsp;
        视频时长(秒):
        <input type="text" name= "duration" value ="${durationStr ?:""}"/>
        fileId(视频唯一标识):
        <input type="text" name= "fileId" value ="${fileIdStr ?:""}" size="40"/>&nbsp;&nbsp;
        </br>
        回放地址1:
        <input type="text" name= "address1" value ="${address1Str ?:""}" size="150"/></br>
        回放地址2:
        <input type="text" name= "address2" value ="${address2Str ?:""}" size="150"/></br>
        回放地址3:
        <input type="text" name= "address3" value ="${address3Str ?:""}" size="150"/></br>
        回放地址4:
        <input type="text" name= "address4" value ="${address4Str ?:""}" size="150"/></br>
        回放地址5:
        <input type="text" name= "address5" value ="${address5Str ?:""}" size="150"/></br>
        </br>
        <font size="6" weight="bold">foreshowId(预告id)一定不能填错，否则会弄坏其他正常的视频</font>
        </br>
        <input type="submit" value="提交" />
        </br>
        </br>
        执行结果:</br>
        <textarea rows="10" cols="150">${retrunLiveRecordInfo ?:""}</textarea>
"""
html+= """</form>
    </body>
    </html>
    """
out << html

