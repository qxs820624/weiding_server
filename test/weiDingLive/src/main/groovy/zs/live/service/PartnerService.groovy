package zs.live.service

import zs.live.entity.Partner

/**
 * Created by liyongguang on 2016/12/30.
 */
interface PartnerService {
    /**
     * 根据id查询第三方用户
     * @param partnerId
     * @return
     */
    Partner getPartner(String partnerId)
}
