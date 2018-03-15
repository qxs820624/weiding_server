package zs.live.utils

import com.google.code.yanf4j.core.impl.StandardSocketOption
import groovy.util.logging.Slf4j
import net.rubyeye.xmemcached.MemcachedClient
import net.rubyeye.xmemcached.MemcachedClientBuilder
import net.rubyeye.xmemcached.XMemcachedClientBuilder
import net.rubyeye.xmemcached.transcoders.CompressionMode
import net.rubyeye.xmemcached.utils.AddrUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * 根据url查询限制次数
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 14-11-17
 * Time: 下午6:38
 * To change this template use File | Settings | File Templates.
 */
@Slf4j
@Component
class UrlCountLimitMemcached {
    @Value('${url.count.limit.memcached.servers}')
    String servers
    @Value('${live.env}')
    String env // 是否是测试环境

    /*@Lazy List<MemCachedClient> clients = { String serversStr ->
        serversStr?.split('[,]+')?.collect { String ipPort ->
            MemCachedClient memCache = new MemCachedClient(ipPort);
            memCache.setPrimitiveAsString(true);
            memCache.setCompressEnable(true);
            memCache.setCompressThreshold(4096);

            SockIOPool pool = SockIOPool.getInstance(ipPort);
            pool.setServers([ipPort] as String[]);
            pool.setInitConn(3);
            pool.setMinConn(3);
            pool.setMaxConn(250);
            pool.setMaxIdle(1000 * 60 * 60 * 6L);
            pool.setMaintSleep(30);
            pool.setNagle(false);
            pool.setSocketTO(3000);
            pool.setSocketConnectTO(1000);
            pool.initialize();
            memCache
        }
    }.call(servers)*/

    @Lazy
    List<MemcachedClient> clients = { String serversStr ->
        serversStr?.split('[,]+')?.collect { String server ->
            MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil.getAddresses(server));
            //这个必须加：连接池大小.在高负载环境下，nio的单连接也会遇到瓶颈，此时你可以通过设置连接池来让更多的连接分担memcached的请求负载，从而提高系统的吞吐量。
            //推荐在0-30之间为好，太大则浪费系统资源，太小无法达到分担负载的目的
            builder.setConnectionPoolSize(30);
            //果你的数据较小，如在1K以下，默认的配置选项已经足够。如果你的数据较大，我会推荐你调整网络层的TCP选项，如设置socket的接收和发送缓冲区更大，启用Nagle算法等等.
            builder.setSocketOption(StandardSocketOption.SO_RCVBUF, 50 * 1024);
            builder.setSocketOption(StandardSocketOption.SO_SNDBUF, 50 * 1024);
            //启用nagle算法，提高吞吐量，默认关闭
            builder.setSocketOption(StandardSocketOption.TCP_NODELAY, true);
            builder.setSocketOption(StandardSocketOption.SO_REUSEADDR, true);
            //设置读线程数 .强烈建议将读线程数设置大于0，接近或者等于memcached节点数（具体参数看你的测试结果）
            builder.getConfiguration().setReadThreadCount(1);
            //设置是否允许读写并发处理
            builder.getConfiguration().setHandleReadWriteConcurrently(true);

            MemcachedClient mc = builder.build()
            mc.setConnectTimeout(3000)
            mc = builder.build();
            //这个必须加
            mc.setOptimizeGet(false);
            mc.getTranscoder().setCompressionMode(CompressionMode.ZIP);
            // 2k > 则压缩
            mc.getTranscoder().setCompressionThreshold(2048);
            mc.setFailureMode(true);
            mc
        }
    }.call(servers)

    long getUrlCreateDate(String md5) {
        long cnt = 0
        try{
            (0..clients.size() - 1).each {
                if (cnt==0){
                    def tmp= clients[it]?.get(md5)
                    cnt = tmp?tmp as long :0
                }
            }
        }catch (Exception e){
            cnt=0
        }
        return cnt
    }
    /**
     * 将url所对应的md5值作为KEY
     * @param md5
     * @param expireDay
     */
    //@HystrixCommand(fallbackMethod = 'setUrlCreateDateFallback')
    void setUrlCreateDate(String md5,int expireDay) {
        try{
            (0..clients.size() - 1).each {  //30天有效，即该url30天内都不允许重复访问
                clients[it]?.set(md5,24*3600*expireDay,System.currentTimeMillis())//
            }
        }catch (Exception e){
            e.printStackTrace()
        }
    }

}
