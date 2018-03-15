import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.service.LiveForeshowService
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * php后台修改 互动/会议直播预告(已经开始)
 * @param foreshowId 新增时候不传 修改时传
 * @param title 专栏名称
 * @param imgUrl 专栏封面
 * @param cateId 类别ID
 * @param parentId 所属专栏
 * @param interestList:'[{"srpId":"abc","keyword":"美容圈"}]', //  5.5.2新增参数 圈子
 * @param srpList:'[{"srpId":"abc","keyword":"马云"}]' // 5.5.2新增参数 SRP
 */
ApiUtils.processNoEncry {
        Assert.isNotBlankParam(params, "foreshowId")   //

    String title = params.title ?: ""
    String imgUrl = params.imgUrl ?: ""
    String interestList = params.interestList ?: ""
    String srpList = params.srpList ?: ""
    String columns = params.columns


    long foreshowId
    long cateId
    long parentId
    int showTitleInList
    int notShowInClient
    try {
        foreshowId = (params.foreshowId ?:0)as long
        cateId = (params.cateId ?:0)as long
        parentId = (params.parentId ?:0) as long
        showTitleInList=(params.showTitleInList ?:0) as int
        notShowInClient=(params.notShowInClient ?:0) as int
    } catch (NumberFormatException e) {
        throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "参数转换错误");
    }



    LiveForeshowService liveForeshowService = getBean(LiveForeshowService)
    ApiUtils.log.info("foreshow.update.started.groovy params:{}", params)
    def paramMap= [
        foreshowId:foreshowId, cateId:cateId, parentId:parentId, title:title,imgUrl:imgUrl,
        interestList:interestList,srpList:srpList,showTitleInList:showTitleInList, notShowInClient:notShowInClient,
        columns:columns
    ];

    Map result = [:];
    int res = liveForeshowService.updateForeshowStarted(paramMap)
    if (res) {
        result.status=1
        result.msg="成功"
    } else{
        result.status=0
        result.msg="失败"
    }

    return result
}
