package zs.live.service

import zs.live.entity.LiveRecord

/**
 * Created by Administrator on 2016/12/14.
 */
interface LivePgcService {
    /**
     * 创建pgc直播
     * @return
     */
    def create(Map param);
    /**
     * 修改直播
     * @param param
     * @return
     */
    def modify(Map param);
    /**
     * 删除直播
     * @param param
     * @return
     */
    def delete(Map param);

    Map stop(LiveRecord liveRecord,String msg)
    /**
     * 获取用户的角色信息，主持人1、场控2、普通观众3
     * @param liveId
     * @param UserId
     * @return
     */
    def getUserRole(Long liveId,Long userId)

    /**
     * 修改场控信息
     * @param params
     * @return
     */
    def updateFieldControl(Map params)

    /**
     * 更新回看地址
     */
    def updateBackVideoAddress(LiveRecord live)
    /**
     * 创建第三方直播
     * @param params
     * @return
     */
    Map createThirdParty(Map params);
    /**
     * 修改第三方直播
     * @param params
     * @return
     */
    Map modifyThirdParty(Map params);

    /**
     * 获取roomId并创建腾讯云房间
     * @param userId
     * @param appId
     * @return
     */
    int getRoomId(long userId,String appId);
}
