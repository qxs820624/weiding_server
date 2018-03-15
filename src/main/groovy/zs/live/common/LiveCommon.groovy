package zs.live.common

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import zs.live.utils.Http
import zs.live.utils.Strings

/**
 * Created by Administrator on 2016/10/24.
 */
@Component
class LiveCommon {
    @Value('${live.env}')
    String liveEnv
    @Value('${live.flux.callback.php}')
    String fluxCallbackPhp
    //直播中跳转类型
    final static int INVOKE_TYPE_LIVE = 10000
    //直播预告跳转类型（互动直播）
    final static int INVOKE_TYPE_LIVE_FORESHOW = 10001
    //官方回看列表跳转类型
    final static int INVOKE_TYPE_LIVE_FORESHOW_RECORD = 10002
    //直播预告系列跳转类型
    final static int INVOKE_TYPE_LIVE_FORESHOW_SERIE = 10003
    //价值直播中和暂停跳转类型
    final static int INVOKE_TYPE_LIVE_FORESHOW_START_AND_PAUSE = 10004
    //会议直播回看
    final static int INVOKE_TYPE_LIVE_FORESHOW_VALUE_RECORD = 10005
    //从sdk发起的互动直播
    final static int INVOKE_TYPE_LIVE_FROM_SDK = 10006

    //粉丝
    final static int INVOKE_TYPE_LIVE_FANS_SDK = 10007
    //关注
    final static int INVOKE_TYPE_LIVE_FOLLOW_SDK = 10008
    //我的搜悦币
    final static int INVOKE_TYPE_LIVE_SYB_SDK = 10009
    //我的直播收益，我的直播商品，我的直播商品，跳转web页
    final static int INVOKE_TYPE_LIVE_WEB_SDK = 10010
    //我的直播回放
    final static int INVOKE_TYPE_LIVE_BACKVIDEO_SDK = 10011
    //我关注的直播系列
    final static int INVOKE_TYPE_LIVE_ATTENTION_SERIES_SDK = 10012
    //我购买的付费直播
    final static int INVOKE_TYPE_LIVE_MY_PAYLIVE_SDK = 10013
    //空白行
    final static int INVOKE_TYPE_LIVE_BLANK_SDK = 10014
    //我的直播收藏
    final static int INVOKE_TYPE_LIVE_MY_COLLECTION = 10015

    //直播中展示类型(带用户信息)
    final static int VIEW_TYPE_LIVE = 10000
    //预告列表展示类型
    final static int VIEW_TYPE_LIVE_FORESHOW = 10001
    //官方回看列表展示类型
    final static int VIEW_TYPE_LIVE_FORESHOW_RECORD = 10002
    //预告系列展示类型
    final static int VIEW_TYPE_LIVE_FORESHOW_SERIE = 10003
    //价值直播中和暂停展示类型
    final static int VIEW_TYPE_PGC_FORESHOW_START_AND_PAUSE = 10004
    //搜索结果列表页的直播列表展示类型
    final static int VIEW_TYPE_SEARCH_FORESHOW_LIST = 10005
    //直播收藏列表展示类型
    final static int VIEW_TYPE_COLLECTION_FORESHOW_LIST = 10006
    //互动直播一行展示两个直播的展示样式
    final static int VIEW_TYPE_FACE_LIVE_NEW = 10007

    //预告状态，0：未开始，1：直播中，2：预告结束(回看生成)，3：删除，4：回放删除5:暂时没有回放地址 6:直播暂停
    final static int FORESHOW_STATUS_0 = 0
    final static int FORESHOW_STATUS_1 = 1
    final static int FORESHOW_STATUS_2= 2
    final static int FORESHOW_STATUS_3 = 3
    final static int FORESHOW_STATUS_4 = 4
    final static int FORESHOW_STATUS_5 = 5
    final static int FORESHOW_STATUS_6 = 6
    //直播日志表状态，1符合回看,2不符合回看,3用户删除，4彻底删除即腾讯云删除
    final static int LIVERECORD_STATUS_1 = 1
    final static int LIVERECORD_STATUS_2= 2
    final static int LIVERECORD_STATUS_3= 3
    final static int LIVERECORD_STATUS_4= 4
    //直播用户角色 1主持人，2场控，3普通观众,4主播
    final static int LIVE_ROLE_1 = 1
    final static int LIVE_ROLE_2= 2
    final static int LIVE_ROLE_3= 3
    final static int LIVE_ROLE_4= 4
    //多个关键词的分隔符
    final static String KEYWORD_SEPARATOR= "#@&#"

    final static int FORESHOW_TYPE_1=1//互动直播
    final static int FORESHOW_TYPE_2=2 //会议直播
    final static int FORESHOW_TYPE_3=3;//付费会议直播
    final static int FORESHOW_TYPE_5=5 //直播系列
    //会议直播的直播状态
    final static int PGC_STATUS_0=0 //未开始
    final static int PGC_STATUS_1=1 //直播中
    final static int PGC_STATUS_6=6 //暂停直播

    final static int PGC_PLAY_URL_RATE_ORIGINAL = 0 //原始码率
    final static int PGC_PLAY_URL_RATE_STANDARD = 550 //标清码率
    final static int PGC_PLAY_URL_RATE_HIGH = 900 //高清码率

    //视频合并上传方式_推送至云
    final static String LIVE_UPLOADMODE_PUSH = "push"
    //视频合并上传方式_云端拉取
    final static String LIVE_UPLOADMODE_PULL = "pull"

    //直播统计协议1：创建直播，2：腾讯云回调，3：直播结束,4：预约(新增，取消),5：预告（新增、修改）
    final static int STATISTIC_JSON_TYPE_1 = 1
    final static int STATISTIC_JSON_TYPE_2 = 2
    final static int STATISTIC_JSON_TYPE_3 = 3
    final static int STATISTIC_JSON_TYPE_4 = 4
    final static int STATISTIC_JSON_TYPE_5 = 5


    //直播类型1:互动直播 2：会议直播 3付费直播
    final static int LIVE_MODE_1=1 //互动直播
    final static int LIVE_MODE_2=2 //会议直播
    final static int LIVE_MODE_3=3 //付费直播
}

