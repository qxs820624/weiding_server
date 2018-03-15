package zs.live.dao.kestrel

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import zs.live.utils.Parallel

/**
 * Created by Administrator on 2016/10/18.
 */
@Repository
class LiveKestrel {

    @Autowired
    Kestrel kestrel

    @Value('${live.kestrel.name.blog}')
    String LIVE_UPDATE_MBLOG_KEY

    /**
     * 发送kestrel 创建直播贴
     * @param msg
     */
    void sendLiveBlogMsg(String msg){
        Parallel.run([1], {
            kestrel.sendMsg(LIVE_UPDATE_MBLOG_KEY, msg)
        },10)
    }
    /**
     * 取kestrel 取创建直播贴json 主要用来测试
     * @return
     */
    String getLiveBlogMsg(){
        return kestrel.getMsg(LIVE_UPDATE_MBLOG_KEY)
    }
}
