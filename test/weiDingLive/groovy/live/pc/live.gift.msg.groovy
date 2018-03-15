import zs.live.ApiUtils
import zs.live.service.QcloudLiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 *     传参：
 params：
 {
     roomId    //房间号
     userInfo:{ //打赏礼物的用户的信息
         "nickname": "hanhan3",
         "userImage": "http://user-pimg.b0.upaiyun.com/selfcreate/default/default_29.jpg!sy",
         "userId": 3200265
     }
     giftInfo:{  //礼物信息
         "giftId": 123,
         "name": "",
         "imageUrl":"",
         "gifUrl": "",
         "price": 100,
         "giftType": 1    //展示类型1：静态图，2：gif
     }
     giftCount    //礼物个数
 }
 */
ApiUtils.processNoEncry{
    String roomId = params.roomId ?: ""
    String appId=Strings.getAppId(params)
    def giftInfo = Strings.parseJson(params.giftInfo ?: "")
    int giftCount = (params.giftCount ?: 0) as int
    def userInfo = Strings.parseJson(params.userInfo ?: "")
    Map map = [
            roomId: roomId,
            giftInfo: giftInfo,
            giftCount: giftCount,
            userInfo: userInfo,
            fromAccount: (userInfo.userId ?: 0) as long,
            appId:appId
    ]
    QcloudLiveService qcloudLiveService = bean(QcloudLiveService)
    return qcloudLiveService.sendGiftImMsg(map)
}
