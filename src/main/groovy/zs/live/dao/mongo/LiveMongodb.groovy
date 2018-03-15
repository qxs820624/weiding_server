package zs.live.dao.mongo

import com.mongodb.*
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository

@Repository
@Slf4j
@CompileStatic
class LiveMongodb {

    @Value('${live.mongo.servers}')
    String servers // host:port,host:port,host:port,...
    @Value('${live.mongo.db}')
    String dbName
    @Value('${live.mongo.connectionsPerHost}')
    int  connectionsPerHost
    @Value('${live.mongo.connectionMultiplier}')
    int  allowedToBlockForConnectionMultiplier
    private static final String LiveMsgCollection = "live_msg_collection";

    private static final String EXCEPTION="exc"

    @Lazy Mongo mongo = { String servers ->
        List<ServerAddress> addrs = []
        servers?.split('[,]+')?.each { String hostPort ->
            String[] arr = hostPort?.split(':')
            if (arr?.length > 0) {
                String host = arr[0]
                int port = 27017
                if (arr.length > 1 && arr[1] ==~ /[0-9]+/)
                    port = arr[1] as int
                addrs.add(new ServerAddress(host, port))
            }
        }
        Mongo mongo=   new Mongo(addrs)
        MongoOptions opt = mongo.getMongoOptions()
        opt.connectionsPerHost=connectionsPerHost;
        opt.threadsAllowedToBlockForConnectionMultiplier = allowedToBlockForConnectionMultiplier;
        return  mongo
    }.call(servers)

    @Lazy DB mongoDB = mongo.getDB(dbName)

    @CompileStatic
    public void insertLiveMsg(DBObject msgO){
        DBCollection coll = mongoDB.getCollection(LiveMsgCollection)
        coll.insert(msgO,WriteConcern.NORMAL)
    }

    @CompileStatic
    public List findLiveMsgList(DBObject query, DBObject sort, int psize){
        DBCursor cursor = null;
        List ret = []
        try {
            DBCollection coll = mongoDB.getCollection(LiveMsgCollection)
            cursor = coll.find(query).hint("idx_live_msg_collection").sort(sort).limit(psize)
            ret = cursor?.toArray()
        }catch (Exception e){
            e.printStackTrace()
        }finally {
            if (cursor != null) try { cursor.close() } catch (ignored) {}
        }
        return ret
    }
    @CompileStatic
    public boolean deleteLiveComment(DBObject query){
        boolean ret = false;
        try {
            ret = del(LiveMsgCollection,query)
        }catch (Exception e){
            e.printStackTrace()
        }finally {
        }
        return ret
    }
    @CompileStatic
    public long findLiveUserCount(DBObject query){
        long count = 0;
        List ret = []
        try {
            DBCollection coll = mongoDB.getCollection(LiveMsgCollection)
            count = coll.count(query)
        }catch (Exception e){
            e.printStackTrace()
        }finally {
        }
        return count
    }
    @CompileStatic
    DBObject findOne(String collName, DBObject query) {
        DBObject obj = null
        try {
            DBCollection coll = mongoDB.getCollection(collName)
            obj = coll.findOne(query)
        }catch (Exception e){
            e.printStackTrace()
        }finally {
        }
        return obj
    }

    @CompileStatic
    DBCursor find(String collName, DBObject query) {
        DBCursor obj = null
        try {
            DBCollection coll = mongoDB.getCollection(collName)
            obj = coll.find(query)
        }catch (Exception e){
            e.printStackTrace()
        }finally {
        }
        return obj
    }

    @CompileStatic
    public boolean addLiveRecord(String collName, DBObject object){
        DBCursor cursor = null;
        boolean ret = false
        try {
            DBCollection coll = mongoDB.getCollection(collName)
            def back = coll.save(object,WriteConcern.SAFE)
            ret = (back) ? true : false;
        }catch (Exception e){
            e.printStackTrace()
        }finally {
            if (cursor != null) try { cursor.close() } catch (ignored) {}
        }
        return ret
    }

    @CompileStatic
    public def update(String collName, DBObject query, DBObject value){
        def ret = false
        try {
            DBCollection coll = mongoDB.getCollection(collName)
            DBObject v = new BasicDBObject('$set', value)
            def back = coll.findAndModify(query, v)
            ret = (back) ? true : false;
        }catch (Exception e){
            e.printStackTrace()
        }finally {
        }
        return ret
    }
    @CompileStatic
    public def del(String collName, DBObject query) {
        def ret = false
        try {
            DBCollection coll = mongoDB.getCollection(collName)
            def back = coll.remove(query, WriteConcern.SAFE)
            ret = ((back?.getN() as int)>0) ? true : false;
        }catch (Exception e){
            e.printStackTrace()
        }finally {
        }
        return ret
    }
}
