package zs.live.service

/**小鱼服务相关服务
 * http://open.ainemo.com/xiaoyu-sdk/sdk/wikis/Livevideov2
 */
interface XiaoYuService {

    /**
     * 从第三方后台预约直播
     * @param xiaoYuNumber 小鱼号码
     * @param data
     * @return
     */
    public Map reserveLive(String xiaoYuNumber, Map<String, Object> data);

    /**
     * 从第三方后台更新一条现有未开始的直播。
     * @param xiaoYuNumber
     * @param xiaoYuLiveId
     * @param data
     * @return
     */
    public Map updateLive(String xiaoYuNumber, String xiaoYuLiveId, Map<String, Object> data);

    /**
     * 从第三方后台同步删除一条存在的直播。会删除和这个直播相关的所有信息，包括但不限于回放列表。
     * @param xiaoYuNumber
     * @param xiaoYuLiveId
     * @return
     */
    public Map removeLive(String xiaoYuNumber, String xiaoYuLiveId);

    /**
     * 获取某个小鱼上的直播。
     * 状态有"WAIT","LIVING","PAUSE","END"
     * @param xiaoYuNumber
     * @param xiaoYuLiveId
     * @return
     */
    public Map getLiveInfo(String xiaoYuNumber, String xiaoYuLiveId);

    /**
     * 获取某个直播的视频列表
     * @param xiaoYuNumber
     * @param xiaoYuLiveId
     * @return
     */
    public Map getVideos(String xiaoYuNumber, String xiaoYuLiveId);

    /**
     * 获取某个直播的视频列表,带分段时间及开始时间等
     * @param xiaoYuNumber
     * @param xiaoYuLiveId
     * @return
     */
    public Map getVideosWithDuration(String xiaoYuNumber,String xiaoYuLiveId);

    /**
     * 获取某个小鱼的状态
     * idle	空闲
     * incall	呼叫中
     * offline	不在线，小鱼断网或者关机5分钟以上会从idle/incall状态切换为offline
     * @param xiaoYuNumber
     * @return
     */
    public Map getXiaoYuNumberInfo(String xiaoYuNumber);

    /**
     * 获取某个企业管理平台中所有小鱼的状态
     * @return
     */
    public Map getAllXiaoYuNumberInfo();

    /**
     * 旧接口 只能获取一条正在直播的会议
     * @param xiaoYuNumber
     * @return
     */
    public Map getLiveInfoForOld(String xiaoYuNumber);

    /**
     *  旧接口 只能删除一条正在直播的会议
     * @param xiaoYuNumber
     * @return
     */
    public Map removeLiveForOld(String xiaoYuNumber);

    /**
     *  旧接口 只能修改一条正在直播的会议
     * @param xiaoYuNumber
     * @return
     */
    public Map updateLiveForOld(String xiaoYuNumber,Map<String, Object> data);

}
