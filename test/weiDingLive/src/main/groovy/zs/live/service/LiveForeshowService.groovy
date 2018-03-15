package zs.live.service

import zs.live.entity.LiveForeshow
import zs.live.entity.LiveRecord

/**
 * Created by Administrator on 2016/12/14.
 */
interface LiveForeshowService {
    /**
     * 返回单个LiveForeshow
     * @param foreshowId
     * @return
     */

    LiveForeshow get(long foreshowId)
    /**
     * 添加或修改
     * @param paramMap
     * @return
     */
    int saveOrUpdate(Map paramMap)
    /**
     * 删除
     * @param foreshowId
     * @return
     */
    int remove(long foreshowId, String appId)
    /**
     * 移除栏目下的所有预告的数据
     * @param foreshowId
     * @return
     */
    int removeGroupDataList(long foreshowId, String appId)

    /**
     * 根据预告的foreshowId修改cateId
     * @param foreshowId
     * @param cateId
     * @return
     */
    int setForeshowCateId( long foreshowId, long cateId )

    def saveOrUpdateGroup(Map paramMap)

    /**
     * 关联专栏和预告
     * @param foreshowId
     * @param subForeshowIdList
     * @return
     */
    int fillLiveForeshow(long foreshowId, List<Long> subForeshowIdList,String appId)


    /**
     * Pgc
     * @param foreshowId
     * @return
     */
    def pausePgc(long foreshowId, String appId,LiveRecord liveRecord,  LiveForeshow liveForeshow, String message,String vc)

    /**
     * Pgc开始预告
     * @param foreshowId
     * @return
     */
    def beginPgc(long foreshowId, String appId,LiveRecord liveRecord, LiveForeshow liveForeshow, String message,String vc)

    /**
     * 更新live_forshow表中系列的开始时间为系列下预告列表中开始时间最大的时间
     * @param liveForeshow
     * @param foreshowId
     * @param appId
     * @return
     */
    def updateForeshowGroupBeginTime (LiveForeshow liveForeshow, long foreshowId, String appId)

    /**
     * 更新live_forshow表中系列的开始时间为系列下预告列表中开始时间最大的时间
     * @param foreshowId
     * @param appId
     * @return
     */
    def updateForeshowGroupBeginTime (long foreshowId, String appId)

    /**
     * 取出一个直播系列下的的预告并
     * @param long parentId, int psize, String sortInfo,long userId, String appId, int status ,String vc
     * @return
     */
    List  listForeShowByParentId(Map params)
    /**
     * 查看一个直播系列下的观看人数
     * @param foreshowId
     * @return
     */
    int getLiveWatcherCountByParentId(long foreshowId, int status)

    /**
     * 直播结束
     */
    def stopForeshow(long foreshowId,String appId,LiveRecord liveRecord,LiveForeshow liveForeshow)

    /**
     * 查询系列的信息
     * @param parentIdList
     * @param status
     * @return
     */
    List  listInfoByParentIds(List<Long> parentIdList ,int status,String appId,String vc)

    /**
     * 更新某个记录的update_time
     */
    def updateUpdateTime(long foreshowId,Date updateTime)

    /**
     * 获取暂停日志
     * @param foreshowId
     * @param liveId
     * @return
     */
    def findPauseLog(long foreshowId, long liveId)

    /**
     * 修改预告状态
     * @param foreshowId
     * @param foreshow_status
     * @return
     */
    def updateStatus(long foreshowId,int foreshow_status)

    /**
     * 修改已经开始的预告
     * @param paramMap
     * @return
     */
    def updateForeshowStarted(Map paramMap)

    /**
     * 回写会议直播观看人数
     * @return
     */
    def writebackForeshowWatchCount()

    /**
     * 获取系列支付状态
     * params.userId
     * params.foreshowIds 1009051,1009052
     * @return
     */
    def checkForeshowPayment(Map params)
}
