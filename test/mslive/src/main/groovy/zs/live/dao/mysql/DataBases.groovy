package zs.live.dao.mysql

import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

import java.sql.SQLException

@Repository
@Slf4j
class DataBases {

    @Autowired
    Sql msqlLive

    @Autowired
    Sql msqlLiveSlave

    @Autowired
    Sql msqloldLiveSlave

    @Autowired
    Sql msqlSouyueLiveSlave

}
