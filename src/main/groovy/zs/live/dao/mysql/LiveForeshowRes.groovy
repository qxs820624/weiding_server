package zs.live.dao.mysql

import zs.live.entity.LiveForeshow

/**
 * Created by Administrator on 2016/12/14.
 */
interface LiveForeshowRes {
    /**
     * 返回单个LiveForeshow
     * @param foreshowId
     * @return
     */

    public LiveForeshow get(long foreshowId)
    /**
     * 根据预告id获取预告详情
     * @param foreshowId
     * @return
     */
    public def getLiveForeshowInfo(long foreshowId)
    /**
     * 新增
     * @param LiveForeshow
     * @return
     */

    public int insert(LiveForeshow liveForeshow)


    /**
     * 修改
     * @param LiveForeshow
     * @return
     */

    public int update(LiveForeshow liveForeshow)

    /**
     * 关联系列和预告?
     * @param foreshowId
     * @param subForeshowIdList
     * @return
     */
    int fillLiveForeshow(long foreshowId, long cateId, List<Long> subForeshowIdList, String appId)
    /**
     * 查询出系列下的预告列表，按照开始时间排序
     * @param foreshowId
     * @param appId
     * @return
     */
    def findForeshowListByParentId(long foreshowId, String appId)
    /**
     * 根据parentId取出直播预告
     * @param long userId,long parent,int psize, String sortInfo, int status ,String appId,String vc
     * @return
     */
    List listForeShowByParentId(Map params);

    /**
     * 根据parentId取出直播ID
     * @param foreshowId
     * @param status
     * @return
     */
    List<Long> listLiveIdByParentId(long foreshowId, int status)

    /**
     * 根据预告id查询直播ID
     * @param foreshowId
     * @param status
     * @return
     */
    List<Long> listLiveIdById(long foreshowId, int status)

    /**
     * 修改parentId
     * @param foreshowId
     * @param status
     * @return
     */
    def updateParentId(long foreshowId, String appId);

    /**
     * 将预告的parentId和cateId的状态置为0
     * @param foreshowId
     * @param appId
     */
    def updateParentIdByForeshowId(long foreshowId,String appId)

    /**
     * 根据预告的foreshowId修改cateId
     * @param foreshowId
     * @param cateId
     */
    def setForeshowCateId( long foreshowId, long cateId )
    /**
     * 修改暂停的状态
     * @param foreshowId
     * @param status
     * @return
     */
    def updateStatus(long foreshowId,int status)

    /**
     * 查询系列的信息，最近的回放，共几场等
     * @param parentIdList
     * @param status
     * @return
     */
    def listInfoByParentIds(List<Long> parentIdList,int status,String appId,String vc)


    /**
     * 更新某个记录的update_time
     */
    def updateUpdateTime(long foreshowId,Date updateTime)

    /**
     * 插入暂停日志
     * @param map
     * @return
     */
    def insertPauseLog(Map map)

    /**
     * 获取暂停日志
     * @param foreshowId
     * @param liveId
     * @return
     */
    def findPauseLog(long foreshowId,long liveId)

    /**
     * 重置已结束的预告排序
     * @return
     */
    def resetForeshowOrder()
}
