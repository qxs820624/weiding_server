package zs.live.service.impl

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import redis.clients.jedis.Jedis
import zs.live.dao.redis.Redis
import zs.live.service.ShortURLService
import zs.live.utils.Strings


@Service("shortURLService")
@Slf4j
class ShortURLServiceImpl implements ShortURLService{

    @Value('${live.env}')
    String liveEnv //当前环境，测试：test，预上线：pre，线上：online
    @Value('${test.address}')
    String testAddress
    @Value('${pre.address}')
    String preAddress
    @Value('${short.url.master}')
    String master
    @Value('${short-url.slaves}')
    String slaves
    @Value('${live.share.url}')
    String liveShareUrl
    @Value('${live.pgc.share.url}')
    String livePgcShareUrl
    @Value('${live.pay.share.url}')
    String livePayShareUrl
    @Value('${live.group.share.url}')
    String liveGroupShareUrl

    @Lazy serversArray = slaves?.split('[,]+')
    @Lazy Random next = new Random()

    static String s2l = "short2long", l2s = "long2short"
    static String chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz-.'

//    @HystrixCommand
    @Override
    String get(String shortURL) {
        String svr = null, url = null
        if (serversArray?.length > 0)
            svr = serversArray[next.nextInt(serversArray.length)]
        [svr, master].each { String s ->
            if (!url && s) url = Redis.with(s) { Jedis jedis ->
                jedis.hget(s2l, shortURL)
            }
        }
        return url
    }

//    @HystrixCommand
    @Override
    String set(String longURL) {
        String shortURL = null
        Redis.with(master) { Jedis jedis ->
            shortURL = jedis.hget(l2s, longURL)
            if (!shortURL) {
                long n = jedis.hincrBy(l2s, 'index', 1)
                shortURL = long2str(n)
                jedis.hset(s2l, shortURL, longURL)
                jedis.hset(l2s, longURL, shortURL)
            }
            shortURL
        }
    }

    @Override
    def getShortUrl(String longUrl){
        String shortUrl = "http://tt.zhongsou.com/u/${set(longUrl)}"
        if("test".equals(liveEnv)){
            shortUrl = "http://${testAddress}/live/shortURL.groovy?id=${set(longUrl)}"
        }else if("pre".equals(liveEnv)){
            shortUrl = "http://${preAddress}/live/shortURL.groovy?id=${set(longUrl)}"
        }
        return shortUrl
    }

    @Override
    def getShortUrlLive(Map map){
        String vc = map.vc ?: "5.4"
        String appId = Strings.getAppId(map)
        String longUrl = liveShareUrl+"?blogType=3&liveId="+map.liveId+"&liveMode="+map.liveMode+"&hostUserId="+map.userId+"&roomId="+map.roomId+"&vc="+vc+"&appId="+appId
        longUrl = appendSchemeToLongUrl(longUrl, appId)
        return this.getShortUrl(longUrl)
    }

    @Override
    def getShortUrlForeshow(Map map){
        String vc = map.vc ?: "5.4"
        String appId = Strings.getAppId(map)
        String longUrl = liveShareUrl+"?blogType=3&foreshowId="+map.foreshowId+"&liveMode="+map.liveMode+"&hostUserId="+map.userId+"&vc="+vc+"&appId="+appId
        longUrl = appendSchemeToLongUrl(longUrl, appId)
        return this.getShortUrl(longUrl)
    }
    @Override
    def getShortUrlPgcForeshow(Map map){
        String vc = map.vc ?: "5.4"
        String appId = Strings.getAppId(map)
        String longUrl = livePgcShareUrl+"?blogType=3&foreshowId="+map.foreshowId+"&liveId="+map.liveId+"&userId="+map.userId+"&vc="+vc+"&appId="+appId
        longUrl = appendSchemeToLongUrl(longUrl, appId)
        return this.getShortUrl(longUrl)
    }
    @Override
    def getShortUrlLivePay(Map map){
        String vc = map.vc ?: "5.4"
        String appId = Strings.getAppId(map)
        String longUrl = livePayShareUrl+"?blogType=3&foreshowId="+map.foreshowId+"&liveId="+map.liveId+"&userId="+map.userId+"&vc="+vc+"&appId="+appId
        longUrl = appendSchemeToLongUrl(longUrl, appId)
        return this.getShortUrl(longUrl)
    }

    @Override
    def getShortUrlLiveGroup(Map map) {
        String appId = Strings.getAppId(map)
        String longUrl = liveGroupShareUrl+"?serieId="+map.foreshowId+"&userId="+map.userId+"&appId="+appId
        longUrl = appendSchemeToLongUrl(longUrl, appId)
        return this.getShortUrl(longUrl)
    }

    private String appendSchemeToLongUrl(String longurl, String appId){
//        wx360a9785675a8653  搜悦				souyue
//        wxf9ec8596a607339f    搜悦精华			com.zhongsou.souyueprime
        if(appId.equals(Strings.APP_NAME_SOUYUE)){
            longurl += "&scheme=wx360a9785675a8653"
        }else if(appId.equals("com.zhongsou.souyueprime")){
            longurl += "&scheme=wxf9ec8596a607339f"
        }
        return longurl
    }

    static String long2str(long n) {
        StringBuilder sb = new StringBuilder()
        while (n > 0) {
            int i = n & 0x3F
            sb.append(chars[i])
            n >>= 6
        }
        sb.toString()
    }
}
