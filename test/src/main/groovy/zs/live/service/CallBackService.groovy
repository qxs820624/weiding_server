package zs.live.service

/**
 * Created by kang on 2016/11/15.
 * 腾讯云回调接口类
 */
interface CallBackService {

    /**
     * 回调接口
     */
    String callBack(String callCommand);

    /**
     * 回调接口
     */
    int videoMergeCallBack(Map params);

    /**
     * 拉取事件通知
     * @return
     */
    String pullEvent(String appId)

    /**
     * 确认事件通知
     * @param msgHandle 事件句柄
     * @return
     */
    String confirmEvent(List msgHandle,String appId)
}
