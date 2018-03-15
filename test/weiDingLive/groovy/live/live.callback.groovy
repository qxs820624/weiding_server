
import zs.live.ApiUtils
import zs.live.service.CallBackService
import zs.live.service.QcloudLiveService



BufferedReader br = new BufferedReader(new InputStreamReader(request.inputStream));
String line = null;
StringBuilder sb = new StringBuilder();
while((line = br.readLine())!=null){
    sb.append(line);
}
// 将资料解码
String reqBody = sb.toString();
//reqBody = """
//{"CallbackCommand":"Group.CallbackAfterNewMemberJoin","GroupId":"1000688","JoinType":"Apply","NewMemberList":[{"Member_Account":"3230937"}],"Operator_Account":"3230937","Type":"AVChatRoom"}
//"""
CallBackService callBackService = ApiUtils.getBean(context,CallBackService.class)
callBackService.callBack(reqBody);

