package zs.live.entity
/**
 * User: wangtf
 * Date: 16-9-3
 */
class LiveRecord {

    long liveId
    long foreshowId
    String title
    String srpId
    String keyword
    String m3u8Url
    String appId
    String appModel
    String channelId
    String taskId
    String liveThump //缩略图
    String liveBg    //封面URL
    long userId
    String userImage  //主播头像
    String nickname
    int roomId
    int chatId
    long admireCount   //点赞数
    long watchCount
    long totalWatchCount // PV，总得用户进入房间的次数
    long vestCount //马甲数量
    long timeSpan
    String createTime  //统一使用字符串的日期类型，精确到毫秒
    String updateTime  //统一使用字符串的日期类型，精确到毫秒
    String pushTime    //统一使用字符串的日期类型，精确到毫秒
	int liveStatus      //1为直播中，2为历史，3为删除
    String videoAddress //回看地址json数组
    int isPrivate   //1:私密,0:普通
    int newliveSortNum
    int backliveSortNum
    int liveType
    String vc
    String fileId
    String playUrl
    String compereUserId //主持人
    String fieldControlId //场控
    String beginTime //会议直播开始时间
    String rtmpUrl //直播推流地址
    int pgcStatus = 0 //会议直播的直播状态 0.未开始,1.直播中, 6: 暂停直播
    int pgcType //1.手机 2.小鱼 3.搜悦推流
    String brief //直播简介
    int liveMode = 1 //1:互动直播 2：会议直播
    String xiaoYuLiveId;      //小鱼的liveId
    String xiaoYuId   ;  //小鱼号
    String xiaoYuConfNo ; //小鱼会议号
    String briefHtml ;  //主播提示语
    int liveFromSdk = 0;   // 1.由直播sdk发起的互动直播，0表示未改版前手机端发起的直播
    String statisticsInfo ;//直播统计信息，json，目前只有直播流量sum_flux

    Map   viewJson=[:] //存放各种格式，清晰度的url
    int liveRecommend = 0;    //是否推荐，0：没有推荐，1：推荐到直播首页
    String recommendTime    //推荐时间
    int isShowLiveHomepage = 0  //'是否在直播首页展示,0:展示，1:不展示';
}
