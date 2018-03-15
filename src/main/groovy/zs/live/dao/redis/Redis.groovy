package zs.live.dao.redis

import groovy.util.logging.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

@Slf4j("liveRedisLog")
public class Redis {

    //public static final Logger liveRedisLog = LoggerFactory.getLogger("liveRedisLog")
    private static ConcurrentHashMap<String, JedisPool> pools = new ConcurrentHashMap<String, JedisPool>();
    private static JedisPoolConfig poolConfig = config();
    private static JedisPoolConfig poolConfigCluster = configCluster();

   // private static   GenericObjectPoolConfig configRedis3 = initPoolConfiguration();
    //private static ConcurrentHashMap<String, JedisCluster> poolsRedis3Map = new ConcurrentHashMap<String, JedisCluster>();
    /**
     * 主从模式
     * @param ipPort
     * @param closure
     * @return
     */
    public static <V> V with(String ipPort, Closure<V> closure) {
        JedisPool pool = pool(ipPort)
        Jedis jedis = null

        V val = null
        try {
            (1..2).each {
                if (!jedis) {
                    if (it > 1) sleep(200)
                    jedis = pool.getResource()
                }
            }
            val = closure?.call(jedis)
        } catch (ignored) {
            liveRedisLog.error("redis $ipPort", ignored)
            if (jedis) pool.returnBrokenResource(jedis)
            jedis = null
        } finally {
            if (jedis) pool.returnResource(jedis)
        }
        val
    }
    /**
     * 集群模式---针对redis2.0
     * @param ipPorts
     * @param closure
     * @return
     */
    public static <V> V withCluster(String ipPorts, Closure<V> closure) {
        List ipPortList=ipPorts.split(",")
        String ipPort=""
        JedisPool pool = null
        Jedis jedis = null
        V val = null
        try {
            (1..5).each {//如果链接一个IP失败，则尝试用其他IP链接
                if (!jedis) {
                    if (it > 1) sleep(50)
                    //随机获取1个，实现负载均衡
                    Random random=new Random()
                    def index= random.nextInt(ipPortList.size())
                    ipPort= ipPortList.get(index)
                    try{
                        pool = poolCluster(ipPort)
                        jedis = pool.getResource()
                    }catch (Exception e){

                    }
                }
            }
            val = closure?.call(jedis)
        } catch (ignored) {
            liveRedisLog.error("redis $ipPort", ignored)
            if (jedis) pool.returnBrokenResource(jedis)
            jedis = null
        } finally {
            if (jedis) pool.returnResource(jedis)
        }
        val
    }
   /* *//**
     * 集群模式---针对redis3.0
     * @param ipPorts
     * @param closure
     * @return
     *//*
    public static <V> V withClusterToRedis3(String ipPorts, Closure<V> closure) {
        JedisCluster jc= poolClusterToRedis3(ipPorts);
        V val = null
        try {
            val = closure?.call(jc)
        } catch (ignored) {
            if(jc) jc.close()
        }
        val
    }*/




    private static JedisPoolConfig config() {
        JedisPoolConfig cfg = new JedisPoolConfig();
        cfg.setMaxTotal(3000);
        cfg.setMinIdle(100);
        cfg.setMaxIdle(1500);
        cfg.setMaxWaitMillis(3000);
        cfg.setTestOnBorrow(true);
        cfg.setTestOnReturn(true);
        return cfg;
    }

    private static JedisPool pool(String ipPort) {
        JedisPool pool = pools.get(ipPort)
        try{
            if (pool == null) {
                String host = null;
                int port = 6379;
                String[] hp = ipPort.split("[:]+");
                if (hp.length > 0) {
                    host = hp[0];
                    if (hp.length > 1 && Pattern.matches("[0-9]+", hp[1]))
                        port = Integer.parseInt(hp[1]);
                }
                pool = new JedisPool(poolConfig, host, port);
                pools.put(ipPort, pool)
            }
        }catch (Exception e){
            liveRedisLog.error("redis pool",e);
        }

        return pool
    }
    /**
     * 集群配置
     * @return
     */

    private static JedisPoolConfig configCluster() {
        JedisPoolConfig cfg = new JedisPoolConfig();
        cfg.setMaxTotal(3000);
        cfg.setMinIdle(100);
        cfg.setMaxIdle(1500);
        cfg.setMaxWaitMillis(3000);
        return cfg;
    }

    private static JedisPool poolCluster(String ipPort) {
        JedisPool pool = pools.get(ipPort)
        try{
            if (pool == null) {
                String host = null;
                int port = 6379;
                String[] hp = ipPort.split("[:]+");
                if (hp.length > 0) {
                    host = hp[0];
                    if (hp.length > 1 && Pattern.matches("[0-9]+", hp[1]))
                        port = Integer.parseInt(hp[1]);
                }
                pool = new JedisPool(poolConfigCluster, host, port);
                pools.put(ipPort, pool)
            }
        }catch (Exception e){
            liveRedisLog.error("redis poolCluster",e);
        }

        return pool
    }
   /* private static JedisCluster poolClusterToRedis3(String ipPorts) {
        JedisCluster jedisCluster=poolsRedis3Map.get(ipPorts)
        if(jedisCluster==null){
            List ipPortList=ipPorts.split(",")
            Set<HostAndPort> jedisClusterNodes = new HashSet<HostAndPort>();
            ipPortList.each{
                def  ipPortArray=it.split(":")
                jedisClusterNodes.add(new HostAndPort(ipPortArray[0], ipPortArray[1] as int))
            }
            jedisCluster= new JedisCluster(jedisClusterNodes,configRedis3);
            poolsRedis3Map.put(ipPorts,jedisCluster)
        }
        return jedisCluster
    }
    private static GenericObjectPoolConfig initPoolConfiguration() {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setLifo(true);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(false);
        config.setBlockWhenExhausted(true);
        // config.setMinIdle(Math.max(1, (int) (poolSize / 10))); // keep 10% hot always
        config.setMinIdle(1);
        config.setMaxTotal(3000); // the number of Jedis connections to create
        config.setTestWhileIdle(false);
        config.setSoftMinEvictableIdleTimeMillis(3000L);
        config.setNumTestsPerEvictionRun(5);
        config.setTimeBetweenEvictionRunsMillis(5000L);
        config.setJmxEnabled(true);
        return config;
    }*/

}

