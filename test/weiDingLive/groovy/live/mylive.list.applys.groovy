import zs.live.ApiUtils
import zs.live.common.LiveCommon
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.dao.redis.LiveRedis
import zs.live.service.QcloudLiveService
import zs.live.utils.Strings
import zs.live.utils.VerUtils

import static zs.live.ApiUtils.process

/**
 * 场合： 搜悦4.0 "发现"页面中罗列的应用功能列表
 *
 *    //--************************-重要提示： 如果用外部浏览器打开连接，则加outBrowser:true ***********************
 */
process({
    URL base = new URL(request.getRequestURL().toString())
    LiveCommon liveCommon = getBean(LiveCommon)
    def list = []
    list << [
        invokeType: liveCommon.INVOKE_TYPE_LIVE_FANS_SDK,
        image   : "http://souyue-xqq.b0.upaiyun.com/dunzd/1705/1519/02/149844223973051494846138.jpg",
        outBrowser  :false,
        title  :"粉丝",
        url  : new URL(base, "/live/fans.list.groovy?isEncryption=1&hasPubParam=1")
    ]
    list << [
        invokeType:liveCommon.INVOKE_TYPE_LIVE_FOLLOW_SDK,
        image   :"http://souyue-xqq.b0.upaiyun.com/dunzd/1705/1519/02/150340200339481494846138.jpg",
        outBrowser  :false,
        title  :"关注",
        url  :new URL(base, "/live/fans.list.groovy?isEncryption=1&hasPubParam=1")
    ]
    list << [
        invokeType: liveCommon.INVOKE_TYPE_LIVE_MY_PAYLIVE_SDK,
        image     : "http://souyue-xqq.b0.upaiyun.com/dunzd/1705/1519/02/149891074370521494846144.jpg",
        outBrowser: false,
        title     : "我购买的付费直播",
        url       : new URL(base, "live/pgc/user.foreshow.list.groovy?isEncryption=1&hasPubParam=1")
    ]
    return [mylist:list]
}, [clientCacheMinute: 30])
