package zs.live.task

import com.alibaba.fastjson.JSON
import com.qcloud.Utilities.MD5
import groovy.util.logging.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import zs.live.common.LiveCommon
import zs.live.dao.mysql.LiveRes
import zs.live.dao.mysql.QcloudLiveRes
import zs.live.entity.LiveRecord
import zs.live.service.QcloudLiveService
import zs.live.service.impl.QcloudLiveCommon
import zs.live.utils.DateUtil
import zs.live.utils.Http
import zs.live.utils.Parallel
import zs.live.utils.Strings

/**
 * Created by kpc on 2017/07/14.
 * 获取直播播放流量，定时任务（10分钟）
 * 定时扫描直播记录表中当天的会议直播（live_mode=2,3）
 * 根据create_time调用流量查询接口
 */
@Slf4j
class VideoFluxTask implements Runnable{

    LiveRes liveRes = (LiveRes) context.getBean(LiveRes.class);
    QcloudLiveRes qcloudLiveRes = (QcloudLiveRes) context.getBean(QcloudLiveRes.class);
    QcloudLiveService qcloudLiveService = (QcloudLiveService) context.getBean(QcloudLiveService.class)
    LiveCommon liveCommon = (LiveCommon) context.getBean(LiveCommon.class)
    ApplicationContext context

    VideoFluxTask(ApplicationContext context){
        this.context = context
    }

    @Override
    void run() {
        while (true) {
            try{
                //查询最近两天没有统计信息的会议直播
                List<LiveRecord> liveRecordList = qcloudLiveRes.getPgcNoBackVedioLiveRecordList()
                if(liveRecordList){
                    log.info("查询最近两天没有统计信息的会议直播：{}",liveRecordList.size());
                    for (LiveRecord live : liveRecordList) {
                        String title = live.title ? live.title as String: ""
                        if(liveCommon.liveEnv.equals("pre")){
                            if(!(title.startsWith("zstest"))){
                                continue;
                            }
                        }
                        if(liveCommon.liveEnv.equals("online")){
                            if(title.startsWith("zstest")){
                                continue;
                            }
                        }
                        //获取channelId
                        String m3u8Url = live.m3u8Url
                        if(!m3u8Url){
                            log.info("VideoFluxTask m3u8Url is null,liveId=>{}:",live.liveId)
                            continue;
                        }
                        int index_start = m3u8Url.indexOf("3717_")
                        int index_end= m3u8Url.lastIndexOf(".")
                        String channelId = m3u8Url.substring(index_start,index_end)

                        def config = qcloudLiveService.getQcloudInfo(live.appId)
                        String qcloudAppid = config.qcloudAppid
                        String apiKey = config.apiKey
                        long startTime = DateUtil.getTimestamp(live.updateTime)/1000-86400
                        long endTime = DateUtil.getTimestamp(live.updateTime)/1000
                        String flux = QcloudLiveCommon.qcloudFlux(channelId,qcloudAppid,apiKey,startTime,endTime)
                        if(flux){
                            //更新live_record_log表中统计字段的值
                            String statisticsInfo = JSON.toJSONString([sum_flux:flux])
                            liveRes.updateStatisticsInfoByLiveId(live.liveId,statisticsInfo)
                            //异步通知php
//                            Parallel.run([1], {//由于分账功能暂时不上线，固先屏蔽该功能
//                                String res = Http.post(liveCommon.fluxCallbackPhp,[liveId:live.liveId])
//                                log.info("VideoFluxTask php,liveId=>{},res=>{}",live.liveId,res)
//                            }, 1)
                        }
                        log.info("VideoFluxTask liveId=>{},sum_flux=>{},startTime=>{},endTime=>{}",live.liveId,flux,startTime,endTime)
                        //休眠1秒，防止腾讯云报4400（访问超过限制的错误）
                        Thread.sleep(1 * 1000)
                    }
                }
            }catch (Exception e){
                log.info("VideoFluxTask Exception:",e.getMessage())
                e.printStackTrace()
            }finally{
                Thread.sleep(1 * 60 * 1000)
            }
        }
    }

}
