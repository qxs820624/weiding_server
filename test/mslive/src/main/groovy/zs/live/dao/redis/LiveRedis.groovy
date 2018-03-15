package zs.live.dao.redis

import groovy.transform.Synchronized
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import redis.clients.jedis.Jedis
import redis.clients.jedis.Pipeline
import zs.live.utils.Strings

/**
 * 该redis主要存放直播相关缓存
 */
@Repository
@Slf4j
class LiveRedis {
    @Value('${live.redis.cluster}')
    String liveCluster

    void set(String key,String cont,int seconds = 0){
        Redis.withCluster(liveCluster){Jedis jedis ->
            jedis.set(key,cont)
            if (seconds > 0) jedis.expire(key,seconds)
        }
    }
    def get = { String key ->
        def back = ""
        Redis.withCluster(liveCluster) { Jedis jedis ->
            back = jedis.get(key)
        }
        return back
    }
    List<String> mget(List key){
        List<String> back = []
        if(!key){
            return back
        }
        Redis.withCluster(liveCluster){Jedis jedis ->
            back = jedis.mget(key.toArray(new String[key.size()]))
        }
        return back
    }

    def del(String key){
        def back=""
        Redis.withCluster(liveCluster){Jedis jedis ->
            back=jedis.del(key)
        }
        return back
    }

    void hset(String key,String field,String cont,int seconds = 0){
        Redis.withCluster(liveCluster){Jedis jedis ->
            jedis.hset(key,field,cont)
            if (seconds > 0) jedis.expire(key,seconds)
        }
    }

    def  hget(String key,String field){
        def back=""
        Redis.withCluster(liveCluster){Jedis jedis ->
            back= jedis.hget(key,field)
        }
        return  back
    }

    def  hgetAll(String key){
        def back=[:]
        Redis.withCluster(liveCluster){Jedis jedis ->
            back= jedis.hgetAll(key)
        }
        return  back
    }

    void hdel(String key,String field){
        Redis.withCluster(liveCluster){ Jedis jedis ->
            jedis.hdel(key,field)
        }
    }

    long  sadd(String key, String ... members){
        long back = 0
        Redis.withCluster(liveCluster){Jedis jedis ->
            back= jedis.sadd(key, members)
        }
        return  back
    }

    long  zadd(String key,double score,String member){
        long back = 0
        Redis.withCluster(liveCluster){Jedis jedis ->
            back= jedis.zadd(key, score, member)
        }
        return  back
    }
    /**
     * 通过管道模式一次性添加多个值
     * @param key
     * @param score
     * @param list
     * @return
     */
    void  zaddWithPipeline(String key,List list){
        Redis.withCluster(liveCluster){Jedis jedis ->
           Pipeline pipeline=jedis.pipelined()
            list.each{
                double score=it.userId as double
                jedis.zadd(key, score, Strings.toJson(it))
            }
           pipeline.syncAndReturnAll()
        }
    }

    void  lpush(String key,String... strings){
        Redis.withCluster(liveCluster){Jedis jedis ->
            jedis.lpush(key,strings)
        }
    }
    long  llen(String key){
        Redis.withCluster(liveCluster){Jedis jedis ->
            jedis.llen(key)
        }
    }
    void  lrange(String key,int start,int end){
        Redis.withCluster(liveCluster){Jedis jedis ->
            jedis.lrange(key,start,end)
        }
    }
    long  zrem(String key,String member){
        long back = 0
        Redis.withCluster(liveCluster){Jedis jedis ->
            back = jedis.zrem(key,member)
        }
        return  back
    }

    long zcard(String key ){
        long back = 0
        Redis.withCluster(liveCluster){Jedis jedis ->
            back = jedis.zcard(key)
        }
        return back
    }

    Double zscore(String key,String member){
        Double back
        Redis.withCluster(liveCluster){Jedis jedis ->
            back = jedis.zscore(key,member)
        }
        return back
    }

    /**
     * 倒序（从大到小）获取指定区间内的数据
     * @param key
     * @param start
     * @param end
     * @return
     */
    LinkedHashSet<String> zrevrange(String key, long start, long end){
        Set<String> back
        Redis.withCluster(liveCluster){Jedis jedis ->
            back = jedis.zrevrange(key,start,end)
        }
        return back
    }

    /**
     * 正序（从小到大）获取指定区间内的数据
     * @param key
     * @param start
     * @param end
     * @return
     */
    LinkedHashSet<String> zrange(String key, long start, long end){
        Set<String> back
        Redis.withCluster(liveCluster){Jedis jedis ->
            back = jedis.zrange(key,start,end)
        }
        return back
    }

    /**
     * 根据score值，倒序（从大到小）获取指定区间内的数据
     * @param key
     * @param start
     * @param end
     * @return
     */
    LinkedHashSet<String> zrevrangeByScore(String key, String score, Integer psize){
        Set<String> back
        Redis.withCluster(liveCluster){Jedis jedis ->
            back = jedis.zrevrangeByScore(key,"("+score,"-inf",0,psize)
        }
        return back
    }
    /**
     * 获取score值介于score1和score2之间的数据
     * @param key
     * @param score1
     * @param score2
     * @return
     */
    LinkedHashSet<String> zrangeByScore(String key, String score1, String score2){
        Set<String> back
        Redis.withCluster(liveCluster){Jedis jedis ->
            back = jedis.zrangeByScore(key, score1, score2)
        }
        return back
    }


    /**
     * 设置自增长
     * @param key
     * @param num
     * @return
     */
    @Synchronized
    def  setIncy(String key,long num = 1L,int seconds = 0){
        def back=null
        Redis.withCluster(liveCluster){ Jedis jedis ->
            back= jedis.incrBy(key,num as long)
            if (seconds > 0) jedis.expire(key,seconds)
        }
        return back
    }

    def rpush(String key, String value){
        def back
        Redis.withCluster(liveCluster){Jedis jedis ->
            back = jedis.rpush(key,value)
        }
        return back
    }
    def rpop(String key){
        def back
        Redis.withCluster(liveCluster){Jedis jedis ->
            back = jedis.rpop(key)
        }
        return back
    }
    /**
     * 删除指定区域的数据
     * @param key
     * @param start
     * @param end
     * @return
     */
    long zremrangeByRank(String key,long start,long end){
        def back
        Redis.withCluster(liveCluster){Jedis jedis ->
            back= jedis.zremrangeByRank(key,start,end)
        }
        return back
    }
}
