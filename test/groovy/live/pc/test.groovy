import zs.live.ApiUtils
import zs.live.service.LiveService

ApiUtils.processNoEncry ({
  LiveService liveService=bean(LiveService)
    liveService.updateForeshowStatusBegin()

})
