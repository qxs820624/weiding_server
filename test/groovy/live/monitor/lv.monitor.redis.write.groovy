import zs.live.Check
import zs.live.utils.Http
import zs.live.utils.Strings

/**
 * 直播接口监控
 */
response.setContentType('text/plain')
def errors = Check.get('lv_redis_services').check([
    "lvapi.monitor":
        {
            def liveData1=Http.get("http://lv.souyue.mobi/live/pc/monitor.redis.write.groovy?key=twem_monitor_key_mhw_40")
            def liveData2=Http.get("http://lv.souyue.mobi/live/pc/monitor.redis.write.groovy?key=twem_monitor_key_mhw_0")
            def liveData3=Http.get("http://lv.souyue.mobi/live/pc/monitor.redis.write.groovy?key=twem_monitor_key_mhw_100")
            def liveData4=Http.get("http://lv.souyue.mobi/live/pc/monitor.redis.write.groovy?key=twem_monitor_key_mhw_10")
            def liveDataJson1= Strings.parseJson(liveData1)
            def liveDataJson2= Strings.parseJson(liveData2)
            def liveDataJson3= Strings.parseJson(liveData3)
            def liveDataJson4= Strings.parseJson(liveData4)
            def liveListFlag1=("ok".equals(liveDataJson1.body.msg))?1:"直播redis集群写异常,key:twem_monitor_key_mhw_40"
            def liveListFlag2=("ok".equals(liveDataJson2.body.msg))?1:"直播redis集群写异常,key:twem_monitor_key_mhw_0"
            def liveListFlag3=("ok".equals(liveDataJson3.body.msg))?1:"直播redis集群写异常,key:twem_monitor_key_mhw_100"
            def liveListFlag4=("ok".equals(liveDataJson4.body.msg))?1:"直播redis集群写异常,key:twem_monitor_key_mhw_10"
            if (liveListFlag1!=1){
                throw new Exception(liveListFlag1)
            }
            if (liveListFlag2!=1){
                throw new Exception(liveListFlag1)
            }
            if (liveListFlag3!=1){
                throw new Exception(liveListFlag1)
            }
            if (liveListFlag4!=1){
                throw new Exception(liveListFlag1)
            }
        },
],
    -1).findAll { if (params.all) println it; it.hasError }

if (!errors) {
    println 1
} else {
    out << """
error
======================
${errors.join('\n\n')}
"""
}
