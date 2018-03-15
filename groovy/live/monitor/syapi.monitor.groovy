import zs.live.Check
import zs.live.utils.Http
import zs.live.utils.Strings

/**
 * 直播接口监控
 */
response.setContentType('text/plain')
def errors = Check.get('sy_web_services').check([
    "syapi.monitor":
        {
            def liveList=Http.get("http://lv.souyue.mobi/live/pc/live.config.groovy")
            def liveListJson= Strings.parseJson(liveList)
            def liveListFlag=(liveListJson.body.max)?1:"直播马甲设置页面错误"
            if (liveListFlag!=1){
                throw new Exception(liveListFlag)
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
