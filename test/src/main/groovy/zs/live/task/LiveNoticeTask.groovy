package zs.live.task

import ch.qos.logback.core.net.SyslogOutputStream
import groovy.util.logging.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import zs.live.common.LiveCommon
import zs.live.dao.mysql.LiveRes
import zs.live.service.LiveService
import zs.live.service.VideoMergeService
import zs.live.utils.DateUtil
import zs.live.utils.Http
import zs.live.utils.Strings

/**
 * Created by liaojing on 2017/11/29.
 * 当有新的直播时，发信息通知运营人员，主要已经发送过信息的数据就不要重复发送
 */
@Slf4j
class LiveNoticeTask implements Runnable{
    public static final Logger timerLog = LoggerFactory.getLogger("timerLog")

    LiveRes liveRes = (LiveRes) context.getBean(LiveRes.class);
    LiveService liveService = (LiveService) context.getBean(LiveService.class);
    LiveCommon liveCommon = (LiveCommon) context.getBean(LiveCommon.class)

    ApplicationContext context

    LiveNoticeTask(ApplicationContext context){
        this.context = context
    }
    List mobieList = []
    List noticeList = []  //存储已发送过的liveId
    long count = 0

    @Override
    void run() {
        String messageUrl = liveService.messageUrl
        while (true){
            try{
                List liveList = liveRes.findNoticeLive()
                int minute = DateUtil.getMinute(new Date())
                if(count ==0 || minute==10 || minute==40){//为减少数据库的查询，每半个时间查询一次数据库
                    timerLog.info("LiveNoticeTask mobieList from mysql,count={},minute={}",count,minute)
                    mobieList = liveRes.findNoticeMobie()
                    count = 1
                }
                if(!liveList ){
                    continue
                }
                if(!mobieList){
                    continue
                }
                String message = ""
                int num = 0
                String liveModeStr = ""
                for (def live : liveList){
                    ++num
                    int liveMode = (live.live_mode?:0) as int
                    int liveType = (live.live_type?:0) as int
                    int noticeListValue = (live.foreshow_id?:0) as int
                    if(liveType == 0 ){
                        noticeListValue = live.live_id
                        liveModeStr = "颜值直播"
                        long time = System.currentTimeMillis()-DateUtil.getTimestamp(String.valueOf(live.create_time))
                        timerLog.info("LiveNoticeTask noticeListValue=>{},time=>{}",noticeListValue,time)
                        if(time < 60*1000){//直播没有超过1分钟的颜值直播不发送信息
                            continue
                        }
                    }else if(liveMode == 1){
                        liveModeStr = "互动直播"
                    }else if(liveMode == 2){
                        liveModeStr = "会议直播"
                    }else if(liveMode == 3){
                        liveModeStr = "付费直播"
                    }
                    String title = live.title?:"无标题"
                    //zstest的数据不发送短信
                    if(title.startsWith("zstest")){
                        continue
                    }
                    //校验是否已经发送过短信
                    if(!live || noticeList.contains(noticeListValue)){
                        continue
                    }else{
                        noticeList.add(noticeListValue)
                    }
                    String appName = liveService.getAppName(live.app_id)
                    message +="${num}、【${appName}】的用户 ${live.nickname} 发起 ${liveModeStr} ，标题为“${title}”;"
                }
                if(liveCommon.liveEnv.equals("online") && message){//测试环境不发短信
                    timerLog.info("LiveNoticeTask message=>{},mobieList=>{}",message,Strings.toListString(mobieList*.mobile))
                    try {
                        //flag=1的时候，发送次数为100
                        def res = Http.post(messageUrl,[phones:Strings.toListString(mobieList*.mobile),cont:message,flag:1])
                        timerLog.info("LiveNoticeTask message=>{},messageResult=>{}",message,res)
                    }catch (Exception e1){
                        e1.printStackTrace()
                    }
                }else{
                    timerLog.info("LiveNoticeTask is perfect")
                }
                //将已发送信息的数据保持在一定范围内，防止数据溢出
                if(noticeList.size() > 500){
                    noticeList = noticeList.subList(300,noticeList.size())
                }
                ++count
            }catch (Exception e){
                e.printStackTrace()
            }finally{
                Thread.sleep(60*1000)
            }
        }
    }

}
