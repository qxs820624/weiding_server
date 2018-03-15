package zs.live.cfg

import groovy.sql.Sql
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import zs.live.dao.mongo.LiveMongodb
import zs.live.utils.DBUtils


@Configuration
class CfgDatabase {

    @Bean
    Sql msqlLive(@Value('${db.mysql.live.url}') String url,
                     @Value('${db.mysql.live.user}') String user,
                     @Value('${db.mysql.live.password}') String pass) {
        DBUtils.newSql(url, user, pass)
    }

    @Bean
    Sql msqlLiveSlave(@Value('${db.mysql.live.url}') String url,
                 @Value('${db.mysql.live.user}') String user,
                 @Value('${db.mysql.live.password}') String pass) {
        DBUtils.newSql(url, user, pass)
    }
    @Bean
    Sql msqloldLiveSlave(@Value('${db.mysql.old.live.slave.url}') String url,
                      @Value('${db.mysql.old.live.slave.user}') String user,
                      @Value('${db.mysql.old.live.slave.password}') String pass) {
        DBUtils.newSql(url, user, pass)
    }
    @Bean
    Sql msqlSouyueLiveSlave(@Value('${db.mysql.souyue.live.slave.url}') String url,
                         @Value('${db.mysql.souyue.live.slave.user}') String user,
                         @Value('${db.mysql.souyue.live.slave.password}') String pass) {
        DBUtils.newSql(url, user, pass)
    }

//    @Bean
//    LiveMongodb hemsMongodb(
//        @Value('${live.mongo.servers}') String servers,
//        @Value('${live.mongo.db}') String dbName) {
//        new LiveMongodb(servers, dbName)
//    }



}
