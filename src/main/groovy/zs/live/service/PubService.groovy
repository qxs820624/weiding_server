package zs.live.service
/**
 * 公共的服务，用于存放不好区分的服务接口
 * Created by ls on 2015/6/18.
 */
interface PubService {

    /**
     * 根据版本判断是否要进行升级提醒
     * @param appName
     * @param channel
     * @param vc
     * @param isAndroid 操作系统 android ,apple
     * @return
     */
    def upgradeVc(String appName,String channel,String vc,boolean isAndroid)
}
