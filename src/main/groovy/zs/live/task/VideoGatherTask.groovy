package zs.live.task

import com.alibaba.fastjson.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import zs.live.dao.kestrel.VideoGatherKestrel
import zs.live.service.VideoGatherService
import zs.live.utils.Strings

/**
 * CMS自动抓取的视频采集与上传
 *
 * 消息格式：
 * {
 "title": "[搞笑]东土大唐F4巡回演出场面火爆",
 "source": "优酷网",
 "news_link": "http://v.youku.com/v_show/id_XMjY0MjU5ODIzNg==.html",
 "time": 1489652041,
 "data": {
 "playlink": "http://v.ifeng.com/video_45531.shtml",
 "status": 0,
 "video_list": [
 {
 "video_address": "http://ips.ifeng.com/video19.ifeng.com/video09/2016/07/22/15364-280-068-1511.mp4"
 }
 ],
 "video_num": 1,
 "video_type": "mp4"
 }
 }
 */
class VideoGatherTask implements Runnable{
    public static final Logger videoGatherLog = LoggerFactory.getLogger("videoGatherLog");

    VideoGatherService videoGatherService = (VideoGatherService)context.getBean(VideoGatherService.class)
    VideoGatherKestrel videoGatherKestrel = (VideoGatherKestrel)context.getBean(VideoGatherKestrel.class)

    ApplicationContext context

    VideoGatherTask(ApplicationContext context){
        this.context = context
    }

    @Override
    void run() {
        def random = new Random()
        while (true) {
            def msgStr
            try {
                msgStr = videoGatherKestrel.getGatherMsg()
                if (msgStr) {
                    videoGatherLog.info("GatherVideo 视频采集消息 >> ${msgStr}")
                    JSONObject msgJson = JSONObject.parse(msgStr)
                    def params = [
                        title:msgJson?.title,
                        newsLink:msgJson?.news_link,
                        source:msgJson?.source,
                        pubtime:msgJson?.pubtime,
                        appId:Strings.getAppId([:])
                    ]
                    List files = []
                    msgJson.data.video_list.each{
                        def url
                        if ((it.video_address as String).startsWith("http://") || (it.video_address as String).startsWith("https://")){
                            url = it.video_address
                        }else {
                            url = "http://${it.video_address}"
                        }
                        files << url
                    }
                    List<String> uris = videoGatherService.downloadFiles(files)
                    uris.each {
                        try {
                            videoGatherService.multipartUpload(it,params)
                        } catch (Exception e) {
                            e.printStackTrace()
                            videoGatherLog.info("GatherVideo 采集视频上传失败 >> ${it} 错误信息 >> ${e.message}")
                        }
                    }

                    int num = random.nextInt(240) + 60
                    Thread.sleep(num * 1000)
                } else {
                    videoGatherLog.info("GatherVideo 无视频采集消息，休眠 1 秒")
                    Thread.sleep(1000)
                }
            } catch (Exception e) {
                e.printStackTrace()
                videoGatherLog.info("GatherVideo 采集视频上传失败 >> ${e.message}")
                Thread.sleep(60*1000)
            }
        }
    }
}
