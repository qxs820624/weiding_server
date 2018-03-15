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
    String appId = Strings.getAppId(params)
    URL base = new URL(request.getRequestURL().toString())
    LiveCommon liveCommon = getBean(LiveCommon)
    QcloudLiveService qcloudLiveService = getBean(QcloudLiveService)
    LiveQcloudRedis liveQcloudRedis = getBean(LiveQcloudRedis)
    LiveRedis liveRedis = getBean(LiveRedis)
    String env = liveCommon.liveEnv
    String vc = params.vc
    String version = ""
    List iosForbidAppId = ["com.zhangshangzhou","com.zhongsou.kekef","com.haiyang.zs","com.zhongjianzhihui"]
    //获取版本
    if(env.equals("pre")){
        version = liveRedis.hget(liveQcloudRedis.SY_APPSTORE_SOUYUE_VERSION_PRE+(appId?:"souyue"),liveQcloudRedis.SY_APPSTORE_SOUYUE_FIELD_PRE)
    }else{
        version = liveRedis.hget(liveQcloudRedis.SY_APPSTORE_SOUYUE_VERSION+(appId?:"souyue"),liveQcloudRedis.SY_APPSTORE_SOUYUE_FIELD)
    }
    if("".equals(version) || version==null){
        version = "9.9.9"
    }
    def config = qcloudLiveService.getQcloudInfo(appId)
    String url = config.mallUrl
    String mallKey;
    if(url){
        int begin = url.indexOf("/ptmall/")
        int end = url.indexOf("/api/")
        mallKey =  url.substring(begin+8,end)
    }

    def list = []
    if(Strings.isAndroidOs(request,params)||(!Strings.isAndroidOs(request,params)&&VerUtils.toIntVer(vc) < VerUtils.toIntVer(version))){
        list << [
            title  :"",  //分隔行
        ]
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
            title  :"",
        ];
        String mySybUrl;
        if("test".equals(env)){
            mySybUrl = "https://sybtest.zhongsou.com/payalipay/recharge?isEncryption=1&hasPubParam=1"
        }else if("pre".equals(env)){
            mySybUrl = "https://sybpre.zhongsou.com/payalipay/recharge?isEncryption=1&hasPubParam=1"
        }else if("online".equals(env)){
            mySybUrl = "https://syb.zhongsou.com/payalipay/recharge?isEncryption=1&hasPubParam=1"
        }

        if(!iosForbidAppId.contains(appId)){//由于ios上架，临时屏蔽
            list << [
                invokeType:liveCommon.INVOKE_TYPE_LIVE_SYB_SDK,
                image   :Strings.APP_NAME_SOUYUE.equals(appId)?"http://souyue-xqq.b0.upaiyun.com/dunzd/1705/1519/02/150120231261011494846145.jpg":"http://souyue-xqq.b0.upaiyun.com/dunzd/1705/1617/07/149594732970871494925664.jpg",
                outBrowser  :false,
                title  : Strings.APP_NAME_SOUYUE.equals(appId)?"我的搜悦币":"我的钻石",
                content:"充值",
                url    :  mySybUrl
            ]
        }

        String myLivePaymentUrl;
        if("test".equals(env)){
            myLivePaymentUrl = "https://sybtest.zhongsou.com/sybweb/mysyb?isEncryption=1&hasPubParam=1"
        }else if("pre".equals(env)){
            myLivePaymentUrl = "https://sybpre.zhongsou.com/sybweb/mysyb?isEncryption=1&hasPubParam=1"
        }else if("online".equals(env)){
            myLivePaymentUrl = "https://syb.zhongsou.com/sybweb/mysyb?isEncryption=1&hasPubParam=1"
        }
        if(!iosForbidAppId.contains(appId)) {//由于ios上架，临时屏蔽
            list << [
                invokeType: liveCommon.INVOKE_TYPE_LIVE_WEB_SDK,
                image     : "http://souyue-xqq.b0.upaiyun.com/dunzd/1705/1519/02/149960434601791494846142.jpg",
                outBrowser: false,
                title     : "我的直播收益",
                content   : "提现",
                url       : myLivePaymentUrl
            ]
        }
        list << [
            invokeType:liveCommon.INVOKE_TYPE_LIVE_BACKVIDEO_SDK,
            image   :"http://souyue-xqq.b0.upaiyun.com/dunzd/1705/1519/02/150448978279741494846142.jpg",
            outBrowser  :false,
            title  :"我的直播回放",
            url  :new URL(base, "live/live.record.my.list.groovy?isEncryption=1&hasPubParam=1")
        ]
        if(VerUtils.toIntVer(vc) >= VerUtils.toIntVer("5.6.7")){
            list << [
                invokeType:liveCommon.INVOKE_TYPE_LIVE_MY_COLLECTION,
                image   :"",
                outBrowser  :false,
                title  :"我的直播收藏",
                url  :new URL(base, "live/pgc/live.collection.list.groovy?isEncryption=1&hasPubParam=1")
            ]
        }
        list << [
            title  :"",
        ]
        list << [
            invokeType:liveCommon.INVOKE_TYPE_LIVE_ATTENTION_SERIES_SDK,
            image   :"http://souyue-xqq.b0.upaiyun.com/dunzd/1705/1519/02/1496433918381494846143.jpg",
            outBrowser  :false,
            title  :"我关注的直播系列",
            url  :new URL(base, "live/pgc/user.foreshow.list.groovy?isEncryption=1&hasPubParam=1")
        ]
        if(!iosForbidAppId.contains(appId)) {//由于ios上架，临时屏蔽
            list << [
                invokeType: liveCommon.INVOKE_TYPE_LIVE_MY_PAYLIVE_SDK,
                image     : "http://souyue-xqq.b0.upaiyun.com/dunzd/1705/1519/02/149891074370521494846144.jpg",
                outBrowser: false,
                title     : "我购买的付费直播",
                url       : new URL(base, "live/pgc/user.foreshow.list.groovy?isEncryption=1&hasPubParam=1")
            ]
        }
        if(mallKey && !iosForbidAppId.contains(appId) ){ //由于ios上架，临时屏蔽
            list << [
                title  :"",
            ]
            String mylivegoodsListUrl;
            if("test".equals(env)){
                mylivegoodsListUrl = "http://ssj.zhongsou.com/distribution/myshop/index?mallkey=${mallKey}&isEncryption=1&hasPubParam=1"
            }else if("pre".equals(env)){
                mylivegoodsListUrl = "http://mall.zhongsou.com/distribution/myshop/index?mallkey=${mallKey}&isEncryption=1&hasPubParam=1"
            }else if("online".equals(env)){
                mylivegoodsListUrl = "http://mall.zhongsou.com/distribution/myshop/index?mallkey=${mallKey}&isEncryption=1&hasPubParam=1"
            }
            list << [
                invokeType:liveCommon.INVOKE_TYPE_LIVE_WEB_SDK,
                image   :"http://souyue-xqq.b0.upaiyun.com/dunzd/1705/1519/02/150001772741551494846144.jpg",
                outBrowser  :false,
                title  :"我的直播商品",
                url  :mylivegoodsListUrl
            ]
            String myDistributionOrderUrl;
            if("test".equals(env)){
                myDistributionOrderUrl = "http://ssj.zhongsou.com/distribution/withdrawal/orderManage?mallkey=${mallKey}&isEncryption=1&hasPubParam=1"
            }else if("pre".equals(env)){
                myDistributionOrderUrl = "http://mall.zhongsou.com/distribution/withdrawal/orderManage?mallkey=${mallKey}&isEncryption=1&hasPubParam=1"
            }else if("online".equals(env)){
                myDistributionOrderUrl = "http://mall.zhongsou.com/distribution/withdrawal/orderManage?mallkey=${mallKey}&isEncryption=1&hasPubParam=1"
            }
            list << [
                invokeType:liveCommon.INVOKE_TYPE_LIVE_WEB_SDK,
                image   :"http://souyue-xqq.b0.upaiyun.com/dunzd/1705/1519/02/150202082871751494846140.jpg",
                outBrowser  :false,
                title   :"我分销的订单",
                url     :myDistributionOrderUrl
            ]

        }
    }else{
        list << [
            title  :"",  //分隔行
        ]
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
            title  :"",
        ];
        String mySybUrl;
        if("test".equals(env)){
            mySybUrl = "https://sybtest.zhongsou.com/payalipay/recharge?isEncryption=1&hasPubParam=1"
        }else if("pre".equals(env)){
            mySybUrl = "https://sybpre.zhongsou.com/payalipay/recharge?isEncryption=1&hasPubParam=1"
        }else if("online".equals(env)){
            mySybUrl = "https://syb.zhongsou.com/payalipay/recharge?isEncryption=1&hasPubParam=1"
        }
        if(!iosForbidAppId.contains(appId)) {//由于ios上架，临时屏蔽
            list << [
                invokeType: liveCommon.INVOKE_TYPE_LIVE_SYB_SDK,
                image     : Strings.APP_NAME_SOUYUE.equals(appId) ? "http://souyue-xqq.b0.upaiyun.com/dunzd/1705/1519/02/150120231261011494846145.jpg" : "http://souyue-xqq.b0.upaiyun.com/dunzd/1705/1617/07/149594732970871494925664.jpg",
                outBrowser: false,
                title     : Strings.APP_NAME_SOUYUE.equals(appId) ? "我的搜悦币" : "我的钻石",
                content   : "充值",
                url       : mySybUrl
            ]
        }

        String myLivePaymentUrl;
        if("test".equals(env)){
            myLivePaymentUrl = "https://sybtest.zhongsou.com/sybweb/mysyb?isEncryption=1&hasPubParam=1"
        }else if("pre".equals(env)){
            myLivePaymentUrl = "https://sybpre.zhongsou.com/sybweb/mysyb?isEncryption=1&hasPubParam=1"
        }else if("online".equals(env)){
            myLivePaymentUrl = "https://syb.zhongsou.com/sybweb/mysyb?isEncryption=1&hasPubParam=1"
        }
        if(!iosForbidAppId.contains(appId)) {//由于ios上架，临时屏蔽
            list << [
                invokeType: liveCommon.INVOKE_TYPE_LIVE_WEB_SDK,
                image     : "http://souyue-xqq.b0.upaiyun.com/dunzd/1705/1519/02/149960434601791494846142.jpg",
                outBrowser: false,
                title     : "我的直播收益",
                content   : "提现",
                url       : myLivePaymentUrl
            ]
        }
        list << [
            invokeType:liveCommon.INVOKE_TYPE_LIVE_BACKVIDEO_SDK,
            image   :"http://souyue-xqq.b0.upaiyun.com/dunzd/1705/1519/02/150448978279741494846142.jpg",
            outBrowser  :false,
            title  :"我的直播回放",
            url  :new URL(base, "live/live.record.my.list.groovy?isEncryption=1&hasPubParam=1")
        ]
        if(VerUtils.toIntVer(vc) >= VerUtils.toIntVer("5.6.7")){
            list << [
                invokeType:liveCommon.INVOKE_TYPE_LIVE_MY_COLLECTION,
                image   :"",
                outBrowser  :false,
                title  :"我的直播收藏",
                url  :new URL(base, "live/pgc/live.collection.list.groovy?isEncryption=1&hasPubParam=1")
            ]
        }
        list << [
            title  :"",
        ]
        list << [
            invokeType:liveCommon.INVOKE_TYPE_LIVE_ATTENTION_SERIES_SDK,
            image   :"http://souyue-xqq.b0.upaiyun.com/dunzd/1705/1519/02/1496433918381494846143.jpg",
            outBrowser  :false,
            title  :"我关注的直播系列",
            url  :new URL(base, "live/pgc/user.foreshow.list.groovy?isEncryption=1&hasPubParam=1")
        ]
        if(!iosForbidAppId.contains(appId)) {//由于ios上架，临时屏蔽
            list << [
                invokeType:liveCommon.INVOKE_TYPE_LIVE_MY_PAYLIVE_SDK,
                image   :"http://souyue-xqq.b0.upaiyun.com/dunzd/1705/1519/02/149891074370521494846144.jpg",
                outBrowser  :false,
                title  :"我购买的付费直播",
                url  :new URL(base, "live/pgc/user.foreshow.list.groovy?isEncryption=1&hasPubParam=1")
            ]
        }
        if(mallKey && !iosForbidAppId.contains(appId)){
            list << [
                title  :"",
            ]
            String mylivegoodsListUrl;
            if("test".equals(env)){
                mylivegoodsListUrl = "http://ssj.zhongsou.com/distribution/myshop/index?mallkey=${mallKey}&isEncryption=1&hasPubParam=1"
            }else if("pre".equals(env)){
                mylivegoodsListUrl = "http://mall.zhongsou.com/distribution/myshop/index?mallkey=${mallKey}&isEncryption=1&hasPubParam=1"
            }else if("online".equals(env)){
                mylivegoodsListUrl = "http://mall.zhongsou.com/distribution/myshop/index?mallkey=${mallKey}&isEncryption=1&hasPubParam=1"
            }
            list << [
                invokeType:liveCommon.INVOKE_TYPE_LIVE_WEB_SDK,
                image   :"http://souyue-xqq.b0.upaiyun.com/dunzd/1705/1519/02/150001772741551494846144.jpg",
                outBrowser  :false,
                title  :"我的直播商品",
                url  :mylivegoodsListUrl
            ]
            String myDistributionOrderUrl;
            if("test".equals(env)){
                myDistributionOrderUrl = "http://ssj.zhongsou.com/distribution/withdrawal/orderManage?mallkey=${mallKey}&isEncryption=1&hasPubParam=1"
            }else if("pre".equals(env)){
                myDistributionOrderUrl = "http://mall.zhongsou.com/distribution/withdrawal/orderManage?mallkey=${mallKey}&isEncryption=1&hasPubParam=1"
            }else if("online".equals(env)){
                myDistributionOrderUrl = "http://mall.zhongsou.com/distribution/withdrawal/orderManage?mallkey=${mallKey}&isEncryption=1&hasPubParam=1"
            }
            list << [
                invokeType:liveCommon.INVOKE_TYPE_LIVE_WEB_SDK,
                image   :"http://souyue-xqq.b0.upaiyun.com/dunzd/1705/1519/02/150202082871751494846140.jpg",
                outBrowser  :false,
                title   :"我分销的订单",
                url     :myDistributionOrderUrl
            ]

        }
    }



    return ["mylist":list];
}, [clientCacheMinute: 30])

//boolean isAndroidOs() {
//    String agent = request.getHeader("User-Agent");
//    return (String.valueOf(agent).toLowerCase().contains("android")) ? true : false;
//}
//
//boolean isNewsFlag(String token, boolean isTest) {
//    def url = (isTest) ? "https://hdgl.test.zae.zhongsou.com/api/actIsNew?isEncryption=1" : "https://hd.zae.zhongsou.com/api/actIsNew?isEncryption=1"
//    def back = Http.post(url, [token: token])
//    back = back ? Strings.parseJson(back) : null
//    return back && back.state == 1
//}
//
//boolean isNewsFlagRedis(Map reMap, String fieid) {
//    return reMap && reMap.get(fieid) ? reMap.get(fieid) as int == 1 : false
//}
//Map getNewsFLagRedis(String token){
//    PubRedis pubRedis = bean(PubRedis)
//    TokenRes tokenRes = bean(TokenRes)
//    long userId = tokenRes.getUserIdByToken(token) ?: 0
//    return pubRedis.isNewsFlagRedis(userId)
//}
//
//boolean isHideChannel(ch) {
//    return false
////    ['主题社区sns053',
////     '主题社区sns055',].contains(ch)
//}
