package zs.live.service.impl

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import zs.live.dao.mysql.PartnerRes
import zs.live.entity.Partner
import zs.live.service.PartnerService

/**
 * Created by liyongguang on 2016/12/30.
 */
@Slf4j
@Service
class PartnerServiceImpl implements PartnerService {
    @Autowired
    PartnerRes partnerRes

    @Override
    Partner getPartner(String partnerId) {
        partnerRes.getPartner(partnerId)
    }
}
