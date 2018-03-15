import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.service.impl.QcloudLiveCommon
import zs.live.utils.Strings

/**
 * 校验预告表中回放地址的json
 */
ApiUtils.processNoEncry(){
    LiveService liveService = bean(LiveService)
    List list = liveService.scanForeshowDataForMistake()
    return [count: list.size(),list:list]
}
