import groovyx.net.http.ContentType
import groovyx.net.http.RESTClient

@Grab('org.codehaus.groovy.modules.http-builder:http-builder')
class Consul {
    static put(String consulServer, String folder, Map params) {
        def restClient = new RESTClient(
            "http://${consulServer ?: 'localhost:8500'}/v1/kv/",
            ContentType.URLENC)
        params?.each { k, v ->
            println "$folder/$k = $v"
            restClient.put(
                path: "${folder ?: ''}/$k".replaceAll('//+', '/').replaceAll('^/', ''),
                body: String.valueOf(v))
        }
    }
}


Consul.put(null, 'configuration/apps', [
    'live.env':'online',
    'test.address':'lvtest.souyue.mobi',
    'pre.address':'lvpre.souyue.mobi',
    'online.address':'lv.souyue.mobi',

    'live.redis.cluster':'202.108.1.140:7379,202.108.1.142:7379,202.108.1.144:7379,202.108.1.145:7379',

    'db.mysql.live.url':'jdbc:mysql://103.7.220.201:3306/live?characterEncoding=UTF-8&autoReconnect=true',
    'db.mysql.live.user':'live',
    'db.mysql.live.password':'LiVe20161@3$Rw',

    'db.mysql.live.slave.url':'jdbc:mysql://103.7.220.146:3306/live?characterEncoding=UTF-8&autoReconnect=true',
    'db.mysql.live.slave.user':'live',
    'db.mysql.live.slave.password':'LiVe20161@3$Rw',

    'live.mongo.servers':'localhost:30002',
    'live.mongo.db':'live',
    'live.mongo.connectionsPerHost':'40',
    'live.mongo.connectionMultiplier':'100',


    'live.import.mongo.servers':'103.7.220.78:30000',
    'live.import.mongo.db':'live',
    'live.import.mongo.connectionsPerHost':'40',
    'live.import.mongo.connectionMultiplier':'100',

    'user.info.souyue.url':'http://api2.souyue.mobi/d3api2/pc/user/user.redis.info.groovy',
    'live.heart.task.time':60,

    'gift.list.url':'https://syb.zhongsou.com/api/giftlist',
    'gift.payment.url':'https://syb.zhongsou.com/api/payusergift',
    'user.sybinfo.url':'https://syb.zhongsou.com/api/getuserinfo',
    'user.charminfo.url':'https://syb.zhongsou.com/api/getUserCharm',
    'usercenter.encrypt.url':'http://usercenter.zhongsou.com/api/user/encrypt',
    'gift.payorder.url':'https://syb.zhongsou.com/payliveapi/payorder',

    'short.url.master':'202.108.1.140:6380',
    'short-url.slaves':'202.108.1.142:6380,202.108.1.144:6380,202.108.1.145:6380',
    'live.share.url':'http://m.zhongsou.com/Widgetshare/index',//互動直播對應的預告分享url
    'live.pgc.share.url':'http://m.zhongsou.com/meetingshare/index',//普通的會議直播
    'live.pay.share.url':'http://m.zhongsou.com/paylivesy/index',//付費直播
    'live.group.share.url':'http://m.zhongsou.com/paylivewx/album',//直播系列分享地址

    'live.kestrel.servers':'103.7.220.155:22134',
    'live.kestrel.name.blog':'live_blog',
    'live.defalut.srpid':'d213c22005a9a5dadce7210a5354a7db',
    'live.defalut.keyword':'直播圈',
    'url.count.limit.memcached.servers':'103.7.220.156:11211',
    'live.test.user.list':'82069,1723778,727597,1707529,1707527,1455042,1715811,308061,705876,40554,25749,1667820,1667603,672924,706143,1954,121635,3369295,343281,3323268,4100430,4046981,1916604,2882582,3571533,1687253,1474314,40554,1603187,1712291,1765036,82135,31585,825434,3326719,673329',
    'max.vest.count':'2000',

    'db.mysql.old.live.slave.url':'jdbc:mysql://103.29.134.138:3306/syadmin?characterEncoding=UTF-8&autoReconnect=true',
    'db.mysql.old.live.slave.user':'sycmsdb',
    'db.mysql.old.live.slave.password':'SY#CM$s2015',

    'live.video.uploadMode':'push',//push：推送至云 pull：云端拉取
    'live.video.ffmpeg':'/home/kaifa/ffmpeg/bin/ffmpeg',
    'live.video.download':'/data02/video/download/',
    'live.video.upload':'/data02/video/upload/',
    'live.video.upload.url':'http://lvzb.zhongsou.com/',
    'live.video.callback.url':'http://lvzb.zhongsou.com/',
    'live.info.play.rate' : 0, //0 550 900

    'live.data.kafka.start':true,
    'live.data.kafka.servers':'103.29.134.193:20157,203.29.134.194:20157,103.29.134.195:20157',
    'live.data.kafka.topic':'live_record',
    'live.kestrel.video.cms.gather':'video_download_cms',
    'live.kestrel.video.gather':'video_download',
    'live.video.gather.callback.url':'http://sycms.zhongsou.com/DealVideo/VideoReFun',
    'live.cms.key':'VIDEOTESTKEY1@#$78',
    'live.cms.server':'http://sycms.zhongsou.com',

    'vest.payGift.url': 'https://syb.zhongsou.com/api/vestPayGift',
    'live.user.pay.list.url':'https://syb.zhongsou.com/payliveapi/getUserPayLiveInfo',

    'live.getuserpay.url':'https://syb.zhongsou.com/payliveapi/getuserpay',
    'live.pay.abstract.url':'http://m.zhongsou.com/payliveapp/H5Abstract',
    'live.pay.invite.url':'http://m.zhongsou.com/payliveapp/H5Invitation_Ranking',
    'live.pay.record.url':'https://syb.zhongsou.com/payliveapi/recordApi',
    'live.statistic.nginx.url':'http://lv.souyue.mobi/live/live.nginx.log.groovy',

    'live.message.url':'http://api2.souyue.mobi/d3api2/user/sendMobileMsg.groovy',
    'live.get.appname.url':'http://ydy.zhongsou.com/WebApi/getname',
    'live.get.appname.signkey':'zs_!@#$%^&*&^%$#@webservice',

    'live.foreshow.pay.status':'https://syb.zhongsou.com/payliveapi/getSeriesPayStatus',

    'live.try.look.time':'600000',
    'live.souyue.api.url':"http://api2.souyue.mobi",

    'live.access.check.url': "http://gctv.zhongsou.com/Mobile/checkLiveAccess",
    'live.access.check.appIds':"com.gctv,com.zsgctv",
    'live.fans.sync.url': "http://ydy.zhongsou.com/mcp/gctv/live.sdk.follow.groovy",
    'live.fans.sync.appIds':"com.gctv,com.zsgctv,com.gedoushijie,com.zsgedoushijie,com.gedoushijie2017",

    'live.flux.callback.php':"https://syb.zhongsou.com/payliveapi/dealflowcost",
    'qcloud.play.sign.key':"d48e4e43862530f94c498aeda9ae8c9f",
    'pili.sign.key':"a8ba0988d2713240ddc1864c337a48c5",

    'brief.url':"http://m.zhongsou.com/payliveapp/H5Abstract",
])


