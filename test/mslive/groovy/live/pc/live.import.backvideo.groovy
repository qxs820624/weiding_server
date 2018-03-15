import zs.live.ApiUtils
import zs.live.service.ImportBackVideoService
import zs.live.utils.Assert

ApiUtils.processNoEncry({
    String deleteString = params.deleteString;
    long liveId = params.liveIdForImport? params.liveIdForImport as long : 0
    ImportBackVideoService importBackVideoService = getBean(ImportBackVideoService)
    if("deleteVideo".equals(deleteString)){
        importBackVideoService.deleteImport()
    }else{
        Assert.isNotBlankParam(params,"liveIdForImport")
        importBackVideoService.importBackVideoService(liveId);
    }


})
