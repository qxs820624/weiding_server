package zs.live.dao.kafka

import kafka.javaapi.producer.Producer
import kafka.producer.KeyedMessage
import kafka.producer.ProducerConfig

/**
 * kafkaUtils
 * Created by Administrator on 2015/9/21.
 */
class KafkaProducer {

    public Producer<String, String> producer = null

    public KafkaProducer(String servers, String zkp = null) {
        Properties props = new Properties();
        //此处配置的是kafka的端口
        props.put("metadata.broker.list", servers);
        //配置value的序列化类
        props.put("serializer.class", "kafka.serializer.StringEncoder");
        //配置key的序列化类
        props.put("key.serializer.class", "kafka.serializer.StringEncoder");
        if (zkp) props.put("zookeeper.connect", zkp);
        //request.required.acks
        //0, which means that the producer never waits for an acknowledgement from the broker (the same behavior as 0.7). This option provides the lowest latency but the weakest durability guarantees (some data will be lost when a server fails).
        //1, which means that the producer gets an acknowledgement after the leader replica has received the data. This option provides better durability as the client waits until the server acknowledges the request as successful (only messages that were written to the now-dead leader but not yet replicated will be lost).
        //-1, which means that the producer gets an acknowledgement after all in-sync replicas have received the data. This option provides the best durability, we guarantee that no messages will be lost as long as at least one in sync replica remains.
        props.put("request.required.acks", "-1");
        this.producer = new Producer<String, String>(new ProducerConfig(props));
    }
    /**
     * 发送kafka
     * @param key
     * @param data
     * @return -1：配置未初始化，0：发送异常，1：发送成功
     */
    int send(String key, String data) {
        if (!producer) return -1
        int re = 1
        try {
            producer.send(new KeyedMessage<String, String>(key, data));
        } catch (e) {
            re = 0
            e.printStackTrace()
        }finally{
            if (producer) producer.close()
        }
        re
    }

}
