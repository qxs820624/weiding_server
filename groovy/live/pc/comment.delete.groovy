import zs.live.ApiUtils
import zs.live.dao.mysql.QcloudLiveRes
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 *  删除直播评论接口
 */
ApiUtils.processNoEncry {
    Assert.isNotBlankParam(params, "time");
    Assert.isNotBlankParam(params, "liveId");
    String appId = Strings.getAppId(params)

    long liveId = params.liveId as long
    long time = params.time as long
    ApiUtils.log.info("wangtf 删除直播评论，liveId:{},time:{}",liveId,time)
    LiveQcloudRedis liveQcloudRedis = bean(LiveQcloudRedis)
    QcloudLiveRes qcloudLiveRes = bean(QcloudLiveRes)
    def data = [liveId:liveId,time:time]
    def back = liveQcloudRedis.pushDeleteCommentDataToRedis(Strings.toJson(data))
    if((back as int) >0){
        //删除mongo库中的评论记录
        boolean result = qcloudLiveRes.deleteLiveComment(time, liveId)
        if(result){
            return [status:1, msg:"删除成功！"]
        }else {
            return [status:0, msg:"删除失败！"]
        }
    }else {
        return [status:0, msg:"删除失败！"]
    }

}
