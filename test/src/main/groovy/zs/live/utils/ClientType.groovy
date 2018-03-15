package zs.live.utils

import javax.servlet.http.HttpServletRequest

class ClientType {
    static String get(String ua) {
        String type = null
        if (ua) {
            [
                    'Apple-',
                    'iPhone ',
                    'iPad ',
                    'iPod touch',
            ].each { String s ->
                if (ua.contains(s))
                    type = 'apple'
            }
            if (!type) {
                if (ua.toLowerCase().contains('android'))
                    type = 'android'
            }
        }
        type
    }
    //是否为苹果操作系统
    static boolean isAppledOs(HttpServletRequest request) {
        String agent = request.getHeader("User-Agent");
        agent=String.valueOf(agent).toLowerCase()
        return (agent.contains("iphone")||agent.contains("ipod")||agent.contains("ipad")) ? true : false;
    }

    static boolean isAndroidOs(HttpServletRequest request) {
        String agent = request.getHeader("User-Agent");
        return (String.valueOf(agent).toLowerCase().contains("android")) ? true : false;
    }

    //生成clientId
    static String createClientId(String validImei,String appId){
        return  Md5Util.md5(appId+validImei);
    }
    //如果imei,mac,imsi都无效，则用uuid生成一个
    static String getValidImei(String imei,String mac,String simsn){
        String validImei=""
        if(checkImei(imei)){
            validImei=imei
        }else if(checkMac(mac)){
            validImei=mac
        }else if(checkSimsn(simsn)){
            validImei=simsn
        }
        return  validImei?:getImeiUUID()
    }
    static  String checkImei(String imei){
        HashSet set=new HashSet()
        set.add("000000000000000")
        set.add("111111111111111")
        set.add("222222222222222")
        set.add("333333333333333")
        set.add("444444444444444")
        set.add("555555555555555")
        set.add("666666666666666")
        set.add("777777777777777")
        set.add("888888888888888")
        set.add("999999999999999")
        return  (imei&&!set.contains(imei))?imei:""
    }
    static  String checkMac(String mac){
        HashSet set=new HashSet()
        set.add("MAC:00:00:00:00:00:00")
        set.add("MAC:11:11:11:11:11:11")
        set.add("MAC:33:33:33:33:33:33")
        set.add("MAC:44:44:44:44:44:44")
        set.add("MAC:55:55:55:55:55:55")
        set.add("MAC:66:66:66:66:66:66")
        set.add("MAC:77:77:77:77:77:77")
        set.add("MAC:88:88:88:88:88:88")
        set.add("MAC:99:99:99:99:99:99")
        return (mac&&!set.contains(mac))?mac:""
    }
    static String checkSimsn(String simsn){
        HashSet set=new HashSet()
        set.add("SIMSN:012345678901234")
        return (simsn&&!set.contains(simsn))?simsn:""
    }

    static String getImeiUUID(){
        return  "UUID:"+UUID.randomUUID().toString()
    }
    /**
     * 是否为安卓模拟机
     * @param params
     * @return
     */
    static boolean isAndroidVirtualMachine(Map params){
        boolean  result=false
        if(params.deviceInfo){
          //  def deviceInfo= Strings.parseJson(params.deviceInfo)
            //if("unknown".equals(deviceInfo.build_serial)||!deviceInfo.build_serial){
            //    result= true
           // }
        }
        //System.out.println("===="+[imei:params.imei,vc:params.vc,isAndroidVirtualMachine:result,deviceInfo:params.deviceInfo].toString())

        return result
    }
}
