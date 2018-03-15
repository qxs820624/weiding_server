package zs.live.service.impl

import com.mongodb.BasicDBObject
import com.mongodb.DBCursor
import com.mongodb.DBObject
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import zs.live.dao.mongo.LiveImportMongodb
import zs.live.dao.mongo.LiveMongodb
import zs.live.dao.mysql.DataBases
import zs.live.entity.LiveRecord
import zs.live.service.ImportBackVideoService
import zs.live.service.LiveService
import zs.live.utils.DateUtil
import zs.live.utils.Strings

/**
 * Created by Administrator on 2016/11/14.
 */
@Slf4j
@Service
class ImportBackVideoServiceImpl implements ImportBackVideoService{


    @Autowired
    DataBases dataBases
    @Autowired
    LiveService liveService

    @Autowired
    LiveImportMongodb liveImportMongodb

    @Override
    void importBackVideoService(long liveId) {
       // getfromMongo();

//      //  def list = dataBases.msqloldLiveSlave.rows(" SELECT * from t_live_record_log where status = 2 and id = 10256")
//        def list = dataBases.msqloldLiveSlave.rows(" select * from t_live_record_log where video_address != 'not have video' and video_address !='' and status = 2")
//
//        list.each { l ->
//            System.out.println("----"+l.title);
//            System.out.println("----"+l.id);
//            try{
//                int vest_count = (l.watch_count? l.watch_count as int :0) - (l.maxOnlineNum? l.maxOnlineNum as int :0)
//                def backList = dataBases.msqlLive.executeInsert(
//                    """
//               INSERT live_record_log(live_id,title,srp_id,keyword,app_id,app_model,live_thump,live_bg,user_id,
//                                        user_image,nickname,room_id,
//                               chat_id,create_time,is_private,time_span,live_status,m3u8_url,channel_id,task_id,
//                               push_time,update_time,total_watch_count,vest_count,watch_count,foreshow_id,live_type,admire_count,
//                               video_address)
//                    Values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
//                    ON DUPLICATE KEY UPDATE update_time = ?
//                """,
//                    [   l.id,l.title,l.srpId,l.keyword,l.appName,l.app_model,l.host_avatar,l.host_avatar,l.host_uid,
//                        l.host_avatar,l.host_username,l.av_room_id,
//                        l.chat_room_id,l.create_time,l.isPrivate?:0,l.time_span,1,l.m3u8Url,l.channel_id,l.taskId,
//                        l.modify_time,l.modify_time,l.watch_count,vest_count,l.watch_count,0,0,l.admire_count,l.video_address,
//                        new Date() //updateTime
//                    ]
//                )
//                log.info("insert live_record_log backList: "+ backList)
//            }catch (Exception e){
//                e.printStackTrace()
//            }
//
//        }


        String collection = "liveRecord";
        DBObject query = new BasicDBObject()
        query.append("status",2).append("videoAddress", new BasicDBObject('$ne', "")).append("liveId",new BasicDBObject('$gt',liveId))
        //def a = liveMongodb.findOne(collection,query)
        DBCursor dbCursor = liveImportMongodb.find(collection,query)
        while (dbCursor.hasNext()){
            DBObject l = dbCursor.next()
            log.info("========="+l.liveId)
            log.info("========="+new Date(l.modifyTime))
            log.info("========="+new Date(l.createTime))
            try{
                int watchCount = l.watchCount? l.watchCount as int :0;
                int maxOnlineNum = l.maxOnlineNum? l.maxOnlineNum as int :0;
                int vestCount = watchCount - maxOnlineNum
                if(vestCount<0){
                    vestCount = 0
                    watchCount = maxOnlineNum
                }
                def backList = dataBases.msqlLive.executeInsert(
                    """
               INSERT live_record_log(live_id,title,srp_id,keyword,app_id,app_model,live_thump,live_bg,user_id,
                                        user_image,nickname,room_id,
                               chat_id,create_time,is_private,time_span,live_status,m3u8_url,channel_id,task_id,
                               push_time,update_time,total_watch_count,vest_count,watch_count,foreshow_id,live_type,admire_count,
                               video_address)
                    Values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    ON DUPLICATE KEY UPDATE update_time = ?
                """,
                    [   l.liveId,l.title,l.srpId,l.keyword,l.appName,l.appModel,l.hostAvatar,l.hostAvatar,l.hostUid,
                        l.hostAvatar,l.hostUsername,l.avRoomId,
                        l.avRoomId,new Date(l.createTime),l.isPrivate?:0,l.timeSpan,1,l.m3u8Url,l.channelId,l.taskId,
                        new Date(l.modifyTime),new Date(l.modifyTime),watchCount,vestCount,watchCount,0,0,l.admireCount,l.videoAddress,
                        new Date() //updateTime
                    ]
                )
                log.info("insert live_record_log backList: "+ backList)
            }catch (Exception e){
                e.printStackTrace()
            }

        }
    }


    @Override
    void deleteImport() {
        int backInt =  dataBases.msqlLive.executeUpdate("delete from live_record_log where live_id <100000 ")
        log.info("backInt====>"+backInt)
    }

    @Override
    def insertForeshowLiveData(List liveIds, String imageUrl) {
        if(liveIds){
            def liveRecordInfo = ""
            def title = ""
            long userId = 0
            def nickname = ""
            def userImage = ""
            def createTime = ""
            def updateTime = ""
            long foreshowId = 0
            boolean flag = false
            liveIds.each {
                long liveId = it as long
                LiveRecord liveRecord = liveService.findLiveRecordByLiveId(liveId)
                if (liveRecord && liveRecord.videoAddress && liveRecord.liveStatus == 1) {
                    //必须是符合回看的数据
                    flag = true
                    int timeSpan = liveRecord.timeSpan
                    int watchCountTotal = liveRecord.totalWatchCount
                    int watchCount = liveRecord.watchCount
                    int vestCount = liveRecord.vestCount
                    def data = [watch_count: watchCount, total_watch_count: watchCountTotal, vest_count: vestCount, time_span: timeSpan]
                    liveRecordInfo = Strings.toJson(data)
                    title = title?: liveRecord.title
                    userId = userId ?: liveRecord.userId
                    userImage = userImage ?: liveRecord.userImage
                    nickname = nickname ?: liveRecord.nickname
                    createTime = createTime ?: liveRecord.createTime
                    updateTime = updateTime ?: liveRecord.updateTime
                }
            }
            def back = null
            if(flag){
                try{
                    back = dataBases.msqlLive.executeInsert(
                        """
                INSERT live_foreshow(title,img_url,user_id,nickname,user_image,begin_time,
                    create_time,update_time,foreshow_status,live_record_info)
                    Values(?,?,?,?,?,?,?,?,?,?)
                """,
                        [ title,imageUrl,userId,nickname,userImage,createTime,createTime, updateTime,2,liveRecordInfo ]
                    )
                    log.info("insert live_foreshow back: "+ back)
                    if(back){
                        foreshowId = back.get(0).get(0)
                    }
                }catch (Exception e){
                    e.printStackTrace()
                }
                if(foreshowId){
                    for(int i=0;i<liveIds.size();i++){
                        long id = liveIds.get(i) as long
                        try{
                            back = dataBases.msqlLive.executeUpdate("""
                            UPDATE live_record_log set foreshow_id = ${foreshowId}, live_type=1 WHERE live_id =${id}
                        """)
                            log.info("update live_record_log back: {},liveId:{}", back, id)
                        }catch (Exception e){
                            e.printStackTrace()
                        }
                    }

                }
            }
        }
    }
}
