package zs.live.service

import zs.live.entity.LiveRecord

/**
 * Created by Administrator on 2016/7/25.
 */
interface GiftService {

    /**
     * 获取礼物列表
     * @param pno
     * @param pageSize
     * @param appId
     * @return
     */
    def getGiftList(int pno, int pageSize, String appId)

    /**
     * 礼物扣费
     * @param map
     * @return
     */
    def giftPayment(Map map)

    /**
     * 根据用户id获取魅力值(不需要用户中心加密)
     * @param userId
     * @param opId
     * @param openId
     * @return
     */
    def getUserCharmCount(def userInfo,String appId)


    /**
     * 根据用户id获取搜悦币、魅力值
     * @param userId
     * @param opId
     * @param openId
     * @param appId
     * @return
     */
    def getUserSybCount(Long userId,String opId,String openId,String appId)

    /**
     * 将马甲打赏礼物的记录入库，客户端展示打赏记录使用
     * @param giftMap
     * @return
     */
    def addVestPayGiftOrder(Map giftMap)

}
