import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 应用场景：显示直播预告列表接口 ，未到开始时间的直播预告
 *
 */
ApiUtils.processNoEncry {
    Assert.isNotBlankParam(params,"userId")

    int psize = (params.psize ?: 10) as int
    String appId = Strings.getAppId(params)
    long userId = params.userId as long

    Map map = [
        psize: psize+1,
        appId: appId,
        userId: userId,
        srpId: params.srpId,
        vc:params.vc
    ]

    LiveService liveService = bean(LiveService.class)

    def foreshowList=liveService.findNotStartedLiveForeshowList(map)

    return [ foreshowList: foreshowList]
}



