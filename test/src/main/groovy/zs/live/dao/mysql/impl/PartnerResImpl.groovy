package zs.live.dao.mysql.impl

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import zs.live.dao.mysql.DataBases
import zs.live.dao.mysql.PartnerRes
import zs.live.entity.Partner

/**
 * Created by liyongguang on 2016/12/30.
 */
@Slf4j
@Repository
class PartnerResImpl implements PartnerRes {
    @Autowired
    DataBases dataBases;

    @Override
    Partner getPartner(String partnerId) {
        if (!partnerId){
            return null
        }
        String sql = "SELECT * FROM live_partner WHERE partner_id = ? AND status = 1"
        def params = [partnerId]
        Partner entity = null
        def res = dataBases.msqlLiveSlave.firstRow(sql,params)
        if (res){
            entity = new Partner([
                partnerId:res.partner_id,
                loginName:res.login_name,
                password:res.password,
                secret: res.secret
            ])
        }
        return entity
    }
}
