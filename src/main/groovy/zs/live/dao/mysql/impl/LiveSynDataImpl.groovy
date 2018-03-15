package zs.live.dao.mysql.impl

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import zs.live.dao.mysql.DataBases
import zs.live.dao.mysql.LiveSynData

/**
 * Created by kpc on 2016/11/22.
 */
@Slf4j
@Repository
class LiveSynDataImpl implements LiveSynData{

    @Autowired
    DataBases dataBases;

    @Override
    void synFans() {
        def list = dataBases.msqlSouyueLiveSlave.rows(" select * from sy_user_fans ")
        list.each { l ->
            try{
                log.info("====>userId:"+l.user_id+"fans_user_id:"+l.fans_user_id+", fans_type: "+l.fans_type +", app_name:" +l.app_name)
                def result = dataBases.msqlLiveSlave.firstRow("SELECT * from live_user_fans where user_id=? and fans_user_id=? and fans_type=?",
                    [l.user_id,l.fans_user_id,l.fans_type])
                if(!result){
                    log.info("********=>userId:"+l.user_id+"fans_user_id:"+l.fans_user_id+", fans_type: "+l.fans_type +", app_name:" +l.app_name)
                    dataBases.msqlLive.executeInsert("insert into live_user_fans(user_id,fans_user_id,fans_type,follow_type,app_id)  values(?,?,?,?,?)",
                        [l.user_id, l.fans_user_id, l.fans_type,l.follow_type,l.app_name]);
                }else{
                    int fllowType = l.follow_type? l.follow_type as int : 0
                    if(fllowType==2){
                        log.info("fllowType等于2 删除新表 插入旧表记录")
                        dataBases.msqlLive.executeUpdate("update live_user_fans set follow_type = 2 where user_id=? and fans_user_id=? and fans_type=?",
                            [l.user_id,l.fans_user_id,l.fans_type]
                        )
                    }
                }
                Thread.sleep(500);//停2秒 以免对数据库造成压力
            }catch (Exception e){
                e.printStackTrace()
            }

        }
    }
}
