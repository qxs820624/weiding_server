package zs.live.dao.kafka

import groovy.util.logging.Slf4j
import kafka.consumer.Consumer
import kafka.consumer.ConsumerConfig
import kafka.consumer.KafkaStream
import kafka.javaapi.consumer.ConsumerConnector
import kafka.serializer.StringDecoder
import kafka.utils.VerifiableProperties
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import zs.live.utils.Parallel

import java.util.concurrent.TimeUnit

/**
 * kafka消息队列发送
 *
 * Created by ls on 2015/11/30.
 */
@Repository
@Slf4j
class KafkaRes {
    //统计系统的kafka
    @Value('${live.data.kafka.servers}')
    String servers
    @Value('${live.data.kafka.topic}')
    String liveDataTopic
    //是否开启直播统计功能
    @Value('${live.data.kafka.start}')
    Boolean liveStatisticStart

    /**
     * 发送kafka至后台统计，直播相关（C:王龙）
     * @param data
     * @return
     */
    int sendLiveData(String data){
        send(data,servers,liveDataTopic)
    }

    int send(String data, String server,String topic){
        try{
            Parallel.run([1], {
//                Logger reqLog = LoggerFactory.getLogger("dynStatLog");
                int reStr = (new KafkaProducer(server).send(topic, data))
//                reqLog.info("---kafka,severs:${server},topic:${topic},src:"+data+",result:${reStr+""}")
            }, TimeUnit.SECONDS.toMillis(5))
        }catch (e){e.printStackTrace()}
        1
    }


}
