import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.ClientType
import zs.live.utils.Strings

/**
 * 直播预告跳转接口（返回直播预告跳转所需要的数据信息）
 地址：
 http://localhost:8811/live/live.foreshow.info.groovy
 参数：
 foreshowId:int
 appId:
 业务逻辑：
 跳转返回的状态：liveStatus (0,1,2,3,4,5)
 1、查询live_foreshow表，判断foreshow_status，
 2、foreshow_status=3，预告已删除	liveStatus=5
 3、foreshow_status=4,5，预告已结束，但没有回放	liveStatus = 3
 4、foreshow_status=2，预告已结束，且有回放，查询live_record_log,返回相应信息（进入回看）	liveStatus = 4
 5、以上条件都不符合，判断begin_time
 6、begin_time<当前时间，预告未开始	liveStatus = 0
 7、begin_time>当前时间，预告直播中，查询live_record，
 8、如果有直播，则返回相应信息（进入直播）	liveStatus = 1
 9、如果无直播，主播未就位 	liveStatus = 2
 */

ApiUtils.processNoEncry {
    Assert.isNotBlankParam(params, "foreshowId")
    LiveService liveService = bean(LiveService.class)
    long foreshowId = (params.foreshowId?:0) as long
    long userId = (params.userId?:0) as long
    String appId = Strings.getAppId(params)
    String vc = params.vc
    def res = liveService.getLiveForeshowSkipInfo(foreshowId,userId,appId,vc)
    ApiUtils.log.info("预告跳转获取直播状态，foreshowId:{},status:{}",foreshowId, res.liveStatus)
    return res
}
