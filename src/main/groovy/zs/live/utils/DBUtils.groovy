package zs.live.utils

import groovy.sql.Sql
import org.apache.tomcat.jdbc.pool.DataSource
import org.apache.tomcat.jdbc.pool.PoolProperties

class DBUtils {
    static Sql newSql(String url, String user, String pass) {
        new Sql(newDatasource(url, user, pass))
    }

    static javax.sql.DataSource newDatasource(
            String url,
            String user, String pass) {
        newDatasource(url, null, user, pass)
    }

    static javax.sql.DataSource newDatasource(
            String url, String driver,
            String user, String pass) {
        // 提前 load class，曾经出现过：对不同类型的数据库创建不同的连接池，都卡在load driver class的地方.
        String cn = driver ?: guessDriver(url)
        Class.forName(cn)

        PoolProperties properties = defaultProperties()
        properties.setUrl(url)
        properties.setDriverClassName(cn)
        properties.setUsername(user)
        properties.setPassword(pass)
        new DataSource(properties);
    }


    static PoolProperties defaultProperties() {
        PoolProperties p = new PoolProperties()
        p.maxActive = 500
        p.initialSize = 5
        p.minIdle = 5

        p.testWhileIdle = true
        p.testOnBorrow = true
        p.testOnReturn = false

        p.validationInterval = 30000
        p.validationQuery = 'select 1'

        p.maxWait = 30000

        p.logAbandoned = true
        p.removeAbandoned = true
        p.removeAbandonedTimeout = 180

       // p.jdbcInterceptors = 'ConnectionState;StatementFinalizer'

        p
    }

    static String guessDriver(String url) {
        if (url) {
            if (url.startsWith('jdbc:mysql:'))
                return 'com.mysql.jdbc.Driver'
            else if (url.startsWith('jdbc:sqlserver:'))
                return 'com.microsoft.sqlserver.jdbc.SQLServerDriver'
        }
        return null
    }
}
