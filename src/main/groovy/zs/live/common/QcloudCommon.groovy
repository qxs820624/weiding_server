package zs.live.common

/**
 * Created by Administrator on 2016/10/20.
 */
class QcloudCommon {
    public static final String CALLBACK_AFTERNEWMEMBER_JOIN = "Group.CallbackAfterNewMemberJoin";
    public static final String CALLBACK_AFTERMEMBER_EXIT = "Group.CallbackAfterMemberExit";
    public static final String CALLBACK_AFTERSENDMSG ="Group.CallbackAfterSendMsg";
    public static final String CALLBACK_BEFORE_SENDMSG ="Group.CallbackBeforeSendMsg";
    public static final String CALLBACK_AFTERGROUP_DESTROYED ="Group.CallbackAfterGroupDestroyed";

    public static final String TuiLiu_Exception_Key = "tuiliu_exception"
    public static final String desIm_Exception_Key = "luzhi_exception"

    public static final int QCLOUD_SUCCESS = 1
    public static final int QCLOUD_HAVE_FORESHOW = 2 //有预告
    public static final int LIVE_NOT_EXIST = 11
    public static final int QCLOUD_NORESEAON_ERROR = 601 //601 （服务器端推流发生未知错误）（获取签名开始...重新走start流程）
    public static final int QCLOUD_GETSIGN_ERROR  = 602  // 602调用腾讯云接口 报signId错误 （强制获取签名开始）
    public static final int QCLOUD_NOUSER_ERROR = 603 //603 用户登录问题，重新登录,（腾讯接口返回no user!）
    public static final int QCLOUD_NOROOM_ERROR = 604 //604 房间问题，退出房间重新创建，(包括 no room!和 销毁房间失败）
    public static final int QCLOUD_STOPTUILIU_ERROR = 605 //605 服务器端结束推流失败  （需要客户端结束推流）
    public static final int QCLOUD_STOPBYBEIGN_SLEEP_ERROR = 606 //服务器端开始结束后停留2秒
    public static final int LIVE_NOT_EXIST_SERVER_STOP = 101 //服务器端结束，客户端为接到通知
    public static final int LIVE_NOT_EXIST_APP_STOP = 102 //create成功，但是start过程中，客户端发起了结束直播，导致live_record表中没有记录
    public static final int USER_NOT_ACCESS_CREATE_LIVE = 607   //用户没有全此案创建直播


    public static final int QCLOUD_NOUSER_CODE = 40000411
    public static final int QCLOUD_NOROOM_CODE = 40000410
    public static final int QCLOUD_IM_NO_ROOM_CODE = 10010

    public static final int AVIMCMD_Comment = -1;          // 发送普通消息回调使用，回看展示评论解析使用
    public static final int AVIMCMD_DESTORY = -2;          // 解散房间
    public static final int AVIMCMD_EnterLive = 1;          // 用户加入直播, Group消息  1
    public static final int AVIMCMD_ExitLive = 2;         // 用户退出直播, Group消息  2
    public static final int AVIMCMD_Praise = 3;           // 点赞消息, Demo中使用Group消息  3
    public static final int AVIMCMD_Host_Leave = 4;         // 主播离开, Group消息  4
    public static final int AVIMCMD_Host_Back = 5;         // 主播回来, Demo中使用Group消息  5
    public static final int AVIMCMD_ENTERLIVE_FILL_DATA = 6;  // 马甲 假数据 用户加入群 6
    public static final int AVIMCMD_SET_SILENCE = 7;  // 禁言消息  7
    public static final int AVIMCMD_FOLLOW = 8;           // 关注    8
    public static final int AVIMCMD_Praise_first = 9;         // 观众第一次点心时发送该消息  9
    public static final int AVIMCMD_SYSTEM_NOTIFY = 10;         // 官方提示消息   10
    public static final int AVIMCMD_ZHONGSOUBI = 11;         // 中搜币打赏消息   11
    public static final int AVIMCMD_GIFT = 12;         // 礼物消息   12
    public static final int AVIMCMD_SYNCHRONIZING_INFORMATION = 13;         // 同步信息13  :  watchCount,timeSpan,charmCount
    public static final int AVIMCMD_SEND_MESSAGE = 14       //马甲及H5用户发送普通消息 userId,nickname,message
    public static final int AVIMCMD_ROOM_EXIT = 15  //直播异常结束 下次开启直播时发消息 强制游客解散房间  roomId,liveId
    public static final int AVIMCMD_USER_OPEAR = 16;      //  马甲操作，显示消息 userId,nickname,liveId,type(1关注,2点赞第一次，3点赞,4踢人，5禁言)
    public static final int AVIMCMD_SHOW_INFO = 17;       // 显示消息  message color ，type （1 评论列中显示， 2 评论之上显示）
    public static final int AVIMCMD_WITH_USER_ROLE = 18;       // 文本消息带有角色 message, role
    public static final int AVIMCMD_PGC_STATUS = 19;       // 会议直播显示消息  message color ，

    public static final String LIVE_PUSH_URL = "livepush.myqcloud.com/live/";
    public static final String LIVE_PLAY_URL = "liveplay.myqcloud.com/live/";
}
