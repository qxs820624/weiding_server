package zs.live.dao.kestrel

import com.google.code.yanf4j.core.impl.StandardSocketOption
import groovy.util.logging.Slf4j
import net.rubyeye.xmemcached.MemcachedClient
import net.rubyeye.xmemcached.MemcachedClientBuilder
import net.rubyeye.xmemcached.XMemcachedClientBuilder
import net.rubyeye.xmemcached.transcoders.CompressionMode
import net.rubyeye.xmemcached.utils.AddrUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository

/**
 * 锁定圈子工具类
 */
@Slf4j
@Repository
class Kestrel {



    /**
     * 获取数据源的过程
     */
    @Value('${live.kestrel.servers}')
    String servers;

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


    def sendMsg(String key,String msg) {
        //保存到缓存中的过程
        (0..clients.size() - 1).each {
            MemcachedClient mcc = clients[it];
            boolean a = mcc.set(key, 0, msg)
            log.info("liveKestrel 返回："+a);
        }
    }

    String getMsg(String key) {
        String value = null;
        try{
            (0..clients.size() - 1).each {
                if (!value) value = clients[it]?.get(key)
            }
        }catch (Exception e){
            value=null;
        }
        value
    }
}
