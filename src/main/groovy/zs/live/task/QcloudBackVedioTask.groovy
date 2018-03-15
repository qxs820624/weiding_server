package zs.live.task

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import zs.live.APP
import zs.live.common.LiveCommon
import zs.live.dao.mysql.QcloudLiveRes
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.entity.LiveRecord
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.service.impl.QcloudLiveCommon
import zs.live.utils.Strings

/**
 * Created by kpc on 2016/10/25.
 * 定时任务（30秒）
 * 定时扫描直播记录表中当天的没有回看记录的直播
 * 条件
 * 1.当天 （频率不高所以直接查询库）
 * 2.有channelId（发起成功）
 * 3.直播时长控制
 */
@Slf4j
class QcloudBackVedioTask implements Runnable{

    QcloudLiveRes qcloudLiveRes = (QcloudLiveRes) context.getBean(QcloudLiveRes.class);
    QcloudLiveService qcloudLiveService = (QcloudLiveService) context.getBean(QcloudLiveService.class);
    LiveService liveService = (LiveService) context.getBean(LiveService.class);
    LiveCommon liveCommon = (LiveCommon) context.getBean(LiveCommon.class)

    ApplicationContext context
    QcloudBackVedioTask(ApplicationContext context){
        this.context = context
    }

    @Override
    void run() {
        while (true) {
            try{
                log.info("取回看记录循环开始！！！")
                //查询当天的直播，
                List<LiveRecord> liveRecordList = qcloudLiveRes.getNoBackVedioLiveRecordListToday()
                log.info("查询出回看列表数据,liveRecordList.size=>{}",(liveRecordList?liveRecordList.size():0))
                if(liveRecordList){
                      for (def it : liveRecordList){
                        if(liveCommon.liveEnv.equals("pre")){
                            if(!(it.title ? it.title as String: "").startsWith("zstest") ){
                                continue;
                            }
                        }
                          log.info("线上循环开始,liveId"+it.liveId)
                          def qcloudInfo = qcloudLiveService.getQcloudInfo(it.appId)
                          String secretId = qcloudInfo.secretId
                          String secretKey = qcloudInfo.secretKey
                          long timeSub = 0L
                          def playSet
                          if(it.videoAddress){
                              def videoInfo = liveService.getVideoAddressUrlList(it.videoAddress,[:])
                              long oldTimeSpan = videoInfo.timeSpan as long
                              timeSub = it.timeSpan - oldTimeSpan

                              playSet = videoInfo.url
                          }
                          if(!it.videoAddress || timeSub > 60*10 || !playSet){   //kpc 增加逻辑  回看地址和统计的回看时长差距10分钟以上的 重新取回看
                              log.info("取回看记录开始： liveId={},roomId={},fileId={},channelId={}",it.liveId,it.roomId,it.fileId,it.channelId)
                              //kpc 修改 16.12.05 fileId 取多段
                              List lastList = new ArrayList();
                              it.fileId?.split(",").each {
                                  String adrr = QcloudLiveCommon.getVodPlayListInfoByFileId(secretId,secretKey, it)
                                  List vo = Strings.parseJson(adrr,List)
                                  if(vo && vo.size() > 0){
                                      lastList.addAll(vo)
                                  }
                              }
                              //增加逻辑 kpc 16.11.24  先取fileId
                              String videoAddress = Strings.toJson(lastList)
                              log.info("取回看记录开始 取到回看记录通过fileId： liveId={},roomId={},fileId={},channelId={},videoAddress={}",it.liveId,it.roomId,it.fileId,it.channelId,videoAddress)
//                              if(videoAddress){
//                                  log.info("liveId={},channelId={},按fileId 取回放返回：{}" ,it.liveId ,it.channelId,  videoAddress)
//                              }
                              //按fileID取不到
                              if(!videoAddress || lastList.size() < 1){
                                  //按前缀取 首先按名称前缀取 roomId_liveId 取MP4格式
                                  String prefix = it.roomId+"_"+it.liveId
                                  videoAddress = QcloudLiveCommon.getVodPlayListInfo(secretId, secretKey, it.liveId,it.roomId,prefix)
//                                  if(videoAddress){
                                      log.info("liveId={},channelId={},prefix=>{},按前缀 roomId+liveId 取回放返回，：{}" ,it.liveId ,it.channelId,prefix,videoAddress)
//                                  }

                                  //如果没取到 则按liveroomId_liveId 前缀取  取出是 flv格式
                                  if(!videoAddress){
                                      prefix = "live"+it.roomId+"_"+it.liveId
                                      videoAddress = QcloudLiveCommon.getVodPlayListInfo(secretId, secretKey, it.liveId,it.roomId,prefix)
//                                      if(videoAddress){
                                          log.info("liveId={},channelId={},prefix=>{},按前缀 live+roomId+liveId 取回放返回：{}" ,it.liveId , it.channelId,prefix,videoAddress)
//                                      }
                                      //如果还为空则按前缀  live+channel_id取
                                      if(!videoAddress && it.channelId){
                                          prefix = "live"+it.channelId
                                          videoAddress = QcloudLiveCommon.getVodPlayListInfo(secretId, secretKey, it.liveId,it.roomId,prefix)
//                                          if(videoAddress){
                                              log.info("liveId={},channelId={},prefix=>{}, 按前缀 live+channelId 取回放返回：{}" ,it.liveId,it.channelId,prefix,videoAddress)
//                                          }
                                      }
                                  }
                              }

                              if(videoAddress){
                                  //修改liverecord_log表中vedio_adress字段的值
                                  qcloudLiveRes.updateLiveRecordVideoAddress(videoAddress, it.liveId,null)
                                  if(it.foreshowId){
                                      log.info("调用合并预告,liveId=>{},foreshowId:{}",it.liveId,it.foreshowId)
                                      liveService.updateForeshowMergeInfo(Long.valueOf(it.foreshowId),it.appId)
                                  }
                              }
                          }else{
                              log.info("取回看记录失败： liveId={},roomId={},fileId={},channelId={}",it.liveId,it.roomId,it.fileId,it.channelId)
                          }
                          //休眠1秒，防止腾讯云报4400（访问超过限制的错误）
                          Thread.sleep(1 * 1000)
                    }
                }
            }catch (Exception e){
                log.info("QcloudBackVedioTask Exception:",e.getMessage())
                e.printStackTrace()
            }finally{
                Thread.sleep(30 * 1000)
            }
        }
    }

}
