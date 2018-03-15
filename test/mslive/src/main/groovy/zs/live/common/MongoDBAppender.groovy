package zs.live.common

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.IThrowableProxy
import ch.qos.logback.classic.spi.StackTraceElementProxy
import ch.qos.logback.core.UnsynchronizedAppenderBase
import com.mongodb.MongoClient
import com.mongodb.MongoException
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.slf4j.Marker
import zs.live.utils.Strings

/**
 * Created by Administrator on 2017/1/12.
 */
class MongoDBAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
    private String host;
    private int port;
    private String dbName;
    private String collectionName;
    private MongoCollection mongoCollection = null;
    private String ip ;
    public MongoDBAppender() {
    }

    @Override
    public void start() {
        try {
            super.start();
            connect();
            ip=InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException | MongoException e) {
            addError("Can't connect to mongo: host=" + host + ", port=" + port, e);
        }
    }

    private void connect() throws UnknownHostException,MongoException {
        MongoClient client = new MongoClient(host, port);
        mongoCollection = client.getDatabase(dbName).getCollection(collectionName);
    }

    @Override
    protected void append(ILoggingEvent evt) {
        if (evt == null) return; //just in case
        Document log = getBasicLog(evt);
        try {
            mongoCollection.insertOne(log)
        } catch (Exception e) {
            addError("Could not insert log to mongo: " + evt, e);
        }
    }

    private Document getBasicLog(ILoggingEvent evt) {
        Document doc = new Document();
        doc.put("createTime", new Date(evt.getTimeStamp()));
        doc.append("ip",ip);
        doc.append("level", evt.getLevel().toString());
        doc.append("logger", evt.getLoggerName());
        doc.append("thread", evt.getThreadName());
        Marker m = evt.getMarker();
        if (m != null) {
            doc.put("marker", m.getName());
        }


        String message = evt.getFormattedMessage();
        doc.append("message", message);

        IThrowableProxy proxy = evt.getThrowableProxy();
        if (proxy) {
            StackTraceElementProxy[] tackTraceElementProxies = proxy.getStackTraceElementProxyArray();
            doc.put("location",tackTraceElementProxies?tackTraceElementProxies[0].getSTEAsString():null);

            def stackTraceList = tackTraceElementProxies?.findAll {
                String steAsString = it.getSTEAsString();
                return steAsString && steAsString.startsWith('at zs.live')
            }
            doc.put("stackTrace", proxy.getClassName() + ":" + message + "\n" + stackTraceList);
            doc.put("fullStackTrace", proxy.getClassName() + ": " + message + "\n" + toStackTraceString(tackTraceElementProxies));
        }

        return doc;
    }

    String toStackTraceString(StackTraceElementProxy[] stackTraceElementProxies){
        if (!stackTraceElementProxies)
            return ""

        return Strings.toJson(stackTraceElementProxies*.getSTEAsString())
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }


    String getDbName() {
        return dbName
    }

    void setDbName(String dbName) {
        this.dbName = dbName
    }

    String getCollectionName() {
        return collectionName
    }

    void setCollectionName(String collectionName) {
        this.collectionName = collectionName
    }

}
