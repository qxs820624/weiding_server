import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.service.LiveForeshowService
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * php后台添加、修改 互动/会议直播预告
 * @param foreshowId 新增时候不传 修改时传
 * @param title 专栏名称
 * @param imgUrl 专栏封面
 * @param cateId 类别ID
 * @param parentId 所属专栏
 * @param beginTime
 * @param urlTag 跳转类型，21：圈子列表页，120:H5页面，24：圈贴详情页,10:srp新闻详情页
 * @param url
 * @param operaType  1 新增 2  修改
 */
ApiUtils.processNoEncry {

    String operaType = params.operaType
    if ("2" == operaType){
        Assert.isNotBlankParam(params, "foreshowId")   //
    }
    System.out.println("params======="+params)
    String appId=Strings.getAppId(params)
    String title = params.title ?: ""
    String imgUrl = params.imgUrl ?: ""
    String beginTimeStr = params.beginTime ?: ""
    String userName = params.userName ?: ""
    String nickname = params.nickname
    String userImage = params.userImage
    String url = params.url
    String keyword = params.keyword ?: ""
    String srpId = params.srpId ?:""
    String interestList = params.interestList
    String srpList = params.srpList
    long liveId = (params.liveId ?: 0 )as long
    String columns = params.columns


    long foreshowId
    long cateId
    long userId
    int foreshowType
    long parentId
    int urlTag
    long newsId
    String pgcTitle
    int showTitleInList
    int notShowInClient
    try {
        if ("2" == operaType){
            foreshowId = (params.foreshowId ?:0)as long
        }
        cateId = (params.cateId ?:0)as long
        userId = (params.userId ?:0)as long
        foreshowType = (params.foreshowType ?:0)as int
        parentId = (params.parentId ?:0) as long
        urlTag = (params.urlTag ?:0) as int
        newsId = (params.newsId ?:0) as long
        pgcTitle = params.pgcTitle ?: ""
        showTitleInList=(params.showTitleInList ?:0) as int
        notShowInClient=(params.notShowInClient ?:0) as int
    } catch (NumberFormatException e) {
        throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "参数转换错误");
    }



    LiveForeshowService liveForeshowService = getBean(LiveForeshowService)
    ApiUtils.log.info("foreshow.update.groovy params:{}", params)
    def paramMap= [
        "appId":appId, "foreshowId":foreshowId, "cateId":cateId, "foreshowType":foreshowType, "parentId":parentId,
        "userId":userId,"userName":userName,"nickname":nickname,"userImage":userImage, "title":title,"imgUrl":imgUrl,
        "beginTimeStr":beginTimeStr,"urlTag":urlTag,"url":url,"keyword":keyword, "srpId":srpId, "newsId":newsId,
        "pgcTitle":pgcTitle, "liveId":liveId,"interestList":interestList,"srpList":srpList,showTitleInList:showTitleInList,
        notShowInClient:notShowInClient,columns:columns
    ];

    Map result = [:];
    int res = liveForeshowService.saveOrUpdate(paramMap)
    if (res) {
        result.status=1
        result.msg="成功"
    } else{
        result.status=0
        result.msg="失败"
    }

    return result
}
