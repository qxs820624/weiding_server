import zs.live.ApiUtils
import zs.live.service.LiveForeshowService
import zs.live.utils.Assert

ApiUtils.processNoEncry {
    Assert.isNotBlankParam(params, "foreshowId")     //操作类型
    Assert.isNotBlankParam(params, "cateId")     //操作类

    long foreshowId = (params.foreshowId ?: 0) as long
    long cateId = (params.cateId ?: 0) as long

    LiveForeshowService liveForeshowService = getBean(LiveForeshowService)
    def res = liveForeshowService.setForeshowCateId(foreshowId, cateId)
    return [status: res];

}
