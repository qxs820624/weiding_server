package zs.live.service

/**
 * Created by liaojing on 2016/10/22
 * 获取分享短连接
 */
interface ShortURLService {

    /**
     * 根据短链id从redis获取url
     * @param shortURL
     * @return
     */
    String get(String shortURL)

    /**
     * 分享落地页长连接入redis
     * @param longURL
     * @return
     */
    String set(String longURL)

    /**
     * 根据长连接获取分享短链
     * @param longUrl
     * @return
     */
    def getShortUrl(String longUrl)

    /**
     * 获取直播的分享短链
     * @param map
     * @return
     */
    def getShortUrlLive(Map map)

    /**
     * 互動直播對應的预告的分享短链
     * @param map
     * @return
     */
    def getShortUrlForeshow(Map map)
    /**
     * 普通的會議直播的分享短鏈
     * @param map
     * @return
     */
    def getShortUrlPgcForeshow(Map map)
    /**
     * 获取付费直播的分享短链
     * @param map
     * @return
     */
    def getShortUrlLivePay(Map map)

    /**
     * 获取直播系列分享地址
     * @param map
     * @return
     */
    def getShortUrlLiveGroup(Map map)
}
