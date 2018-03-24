import groovyx.net.http.ContentType
import groovyx.net.http.RESTClient

@Grab('org.codehaus.groovy.modules.http-builder:http-builder')
class Consul {
    static put(String consulServer, String folder, Map params) {
        def restClient = new RESTClient(
            "http://${consulServer ?: '47.92.80.247:8500'}/v1/kv/",            ContentType.URLENC)
        params?.each { k, v ->
            println "$folder/$k = $v"
            restClient.put(
                path: "${folder ?: ''}/$k".replaceAll('//+', '/').replaceAll('^/', ''),
                body: String.valueOf(v))
        }
    }
}


Consul.put(null, 'configuration/weiDing', [
    'live.env':'test',
    'test.address':'lvtest.souyue.mobi',
    'pre.address':'lvpre.souyue.mobi',
    'online.address':'lv.souyue.mobi',

    'live.redis.cluster':'47.92.80.247:6379',

    'db.mysql.live.url':'jdbc:mysql://47.92.80.247:3675/weiding?characterEncoding=UTF-8&autoReconnect=true',
    'db.mysql.live.user':'weiding',
    'db.mysql.live.password':'YSU%$S7js6%A8dks',

    'db.mysql.live.slave.url':'jdbc:mysql://47.92.80.247:3675/weiding?characterEncoding=UTF-8&autoReconnect=true',
    'db.mysql.live.slave.user':'weiding',
    'db.mysql.live.slave.password':'YSU%$S7js6%A8dks',

    'live.mongo.servers':'47.92.80.247:27017',
    'live.mongo.db':'live',
    'live.mongo.connectionsPerHost':'40',
    'live.mongo.connectionMultiplier':'100',


    'live.import.mongo.servers':'47.92.80.247:27017',
    'live.import.mongo.db':'live',
    'live.import.mongo.connectionsPerHost':'40',
    'live.import.mongo.connectionMultiplier':'100',

    'user.info.souyue.url':'http://47.104.10.160:8080/tolive/userInfo',
    'live.heart.task.time':60,

    'gift.list.url':'https://sybtest.zhongsou.com/api/giftlist',
    'gift.payment.url':'https://sybtest.zhongsou.com/api/payusergift',
    'user.sybinfo.url':'https://sybtest.zhongsou.com/api/getuserinfo',
    'user.charminfo.url':'https://sybtest.zhongsou.com/api/getUserCharm',
    'usercenter.encrypt.url':'http://usercentertest.zhongsou.com/api/user/encrypt',
    'gift.payorder.url':'https://sybtest.zhongsou.com/payliveapi/payorder',

    'short.url.master':'202.108.1.230:6379',
    'short-url.slaves':'202.108.1.231:6379,103.29.134.146:6379,103.29.134.147:6379',
    'live.share.url':'http://mtest.zhongsou.com/Widgetshare/index',//互動直播對應的預告分享url
    'live.pgc.share.url':'http://mtest.zhongsou.com/meetingshare/index',//普通的會議直播
    'live.pay.share.url':'http://mtest.zhongsou.com/paylivesy/index',//付費直播
    'live.group.share.url':'http://mtest.zhongsou.com/paylivewx/album',//直播系列分享地址

    'live.kestrel.servers':'103.29.134.58:22133',
    'live.kestrel.name.blog':'live_blog',
    'live.defalut.srpid':'f479751542203f3e5f4d300ddacd1127',
    'live.defalut.keyword':'heyl公开圈',
    'url.count.limit.memcached.servers':'47.92.80.247:11212',
    'live.test.user.list':'105,214,45874,42519,41594,42492',
    'max.vest.count':'2000',

    'db.mysql.old.live.slave.url':'jdbc:mysql://103.29.134.138:3306/syadmin?characterEncoding=UTF-8&autoReconnect=true',
    'db.mysql.old.live.slave.user':'sycmsdb',
    'db.mysql.old.live.slave.password':'SY#CM $s2015',

    'db.mysql.souyue.live.slave.url':'jdbc:mysql://103.7.220.201:3306/souyue01?characterEncoding=UTF-8&autoReconnect=true',
    'db.mysql.souyue.live.slave.user':'sy_user',
    'db.mysql.souyue.live.slave.password':'sy#20141104$zs01',

    'live.video.uploadMode':'push',//push：推送至云 pull：云端拉取
    'live.video.ffmpeg':'/data01/srpnews/ffmpeg/bin/ffmpeg',
    'live.video.download':'/data01/srpnews/ffmpeg/temp/',
    'live.video.upload':'/data01/video/',
    'live.video.upload.url':'http://lvtest.souyue.mobi/',
    'live.video.callback.url':'http://newcmstest.zhongsou.com/Api/ToVideoService',
    'live.info.play.rate' : 0, //0 550 900

    'live.data.kafka.start':true,
    'live.data.kafka.servers':'61.135.210.239:9092',
    'live.data.kafka.topic':'live_record',

    'live.kestrel.video.cms.gather':'video_download_cms',
    'live.kestrel.video.gather':'video_download',
    'live.video.gather.callback.url':'http://newcmstest.zhongsou.com/DealVideo/VideoReFun',
    'live.cms.key':'VIDEOTESTKEY1@#$78',
    'live.cms.server':'http://newcmstest.zhongsou.com',

    'vest.payGift.url': 'https://sybtest.zhongsou.com/api/vestPayGift',
    'live.user.pay.list.url':'https://sybtest.zhongsou.com/payliveapi/getUserPayLiveInfo',

    'live.getuserpay.url':'https://sybtest.zhongsou.com/payliveapi/getuserpay',
    'live.pay.abstract.url':'http://mtest.zhongsou.com/payliveapp/H5Abstract',
    'live.pay.invite.url':'http://mtest.zhongsou.com/payliveapp/H5Invitation_Ranking',
    'live.pay.record.url':'https://sybtest.zhongsou.com/payliveapi/recordApi',
    'live.statistic.nginx.url':'http://lvtest.souyue.mobi/live/live.nginx.log.groovy',

    'live.message.url':'http://103.29.134.224/d3api2/user/sendMobileMsg.groovy',
    'live.get.appname.url':'http://ydytest.zhongsou.com/WebApi/getname',
    'live.get.appname.signkey':'zs_!@#$%^&*&^%$#@webservice',

    'live.foreshow.pay.status':'https://sybtest.zhongsou.com/payliveapi/getSeriesPayStatus',

    'live.try.look.time':'600000',
    'live.souyue.api.url':"http://103.29.134.224",

    'live.access.check.url': "http://gctvtest.zhongsou.com/Mobile/checkLiveAccess",
    'live.access.check.appIds':"com.gctv,com.zsgctv",
    'live.fans.sync.url': "http://ydytest.zhongsou.com/mcp/gctv/live.sdk.follow.groovy",
    'live.fans.sync.appIds':"com.gctv,com.zsgctv,com.gedoushijie,com.zsgedoushijie,com.gedoushijie2017",

    'live.flux.callback.php':"https://sybtest.zhongsou.com/payliveapi/dealflowcost",
    'qcloud.play.sign.key':"6c9743dea2b00fef3660bb9c67112c13",
    'pili.sign.key':"a8ba0988d2713240ddc1864c337a48c5",

    'brief.url':"http://mtest.zhongsou.com/payliveapp/H5Abstract",
])



