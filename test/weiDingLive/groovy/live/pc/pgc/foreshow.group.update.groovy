import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.service.LiveForeshowService
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 *  php后台新建、修改直播系列———梁占峰
 *   http://域名/live/pc/pgc/foreshow.group.update.groovy
 请求参数：
 {
 foreshowId: 123 //新增时候不传 修改时传
 title: 333,//专栏名称
 imgUrl:123,//专栏封面
 cateId: 333,//类别ID
 description: 333,//简介
 descriptionHtml: 333,//富文本简介
 isCost：1，//是否付费，0：否，1：付费
 isSale：1,//是否促销，0：否，1：促销
 price：1000，//价格 单位：分
 salePrice:800，//价格 单位：分
 saleStartTime://促销开始时间 单位：秒
 saleEndTime://促销结束时间 单位：秒
 showIn：//展示规则，0:全部,1:h5显示，2：客户端展示
 operaType: //1 新增 2  修改
 interestList:'[{"srpId":"abc","keyword":"美容圈"}]', // 5.5.2新增参数
 srpList:'[{"srpId":"def","keyword":"马云"}]' // 5.5.2新增参数
 }

 返回值:

 {
 "head": {
 "status": 200,
 "hasMore": false
 },
 "body": {
 "msg": "成功"
 }
 }
 */

ApiUtils.processNoEncry {
    Assert.isNotBlankParam(params, "operaType")     //操作类型
    String operaType = params.operaType ?: ""
    if("2".equals(operaType)){
        Assert.isNotBlankParam(params, "foreshowId")   //
    }
    String title = params.title
    String imgUrl = params.imgUrl
    String description = params.description
    String descriptionHtml = params.descriptionHtml
    int isCost = (params.isCost?:0) as int //是否付费，0：否，1：付费
    int isSale = (params.isSale?:0) as int //是否促销，0：否，1：促销
    int showIn = (params.showIn?:0) as int //展示规则，0:全部,1:h5显示，2：客户端展示
    String price = params.price //价格 单位：分
    String salePrice = params.salePrice //促销价格 单位：分
    int isSplit = (params.isSplit?:0) as int //是否分账，0：否，1：分账
    String splitPrice = params.splitPrice //分账金额
    int isSaleSplit = (params.isSaleSplit?:0) as int //是否促销分账，0：否，1：分账
    String saleSplitPrice = params.saleSplitPrice //促销分账金额
    def tryTime = params.tryTime //试看时间
    String saleStartTime = params.saleStartTime
    String saleEndTime = params.saleEndTime
    String appId = Strings.getAppId(params)
    String interestList = params.interestList
    String srpList = params.srpList
    ApiUtils.log.info("wangtf foreshow.group.update.groovy params:{}", params)
    long foreshowId = 0
    long cateId
    try {
        if ("2" == operaType){
            foreshowId = (params.foreshowId ?:0)as long
        }
        cateId = (params.cateId ?:0)as long
    } catch (NumberFormatException e) {
        throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "参数转换错误");
    }
    if(operaType != "1" && operaType != "2") {
        throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "操作类型错误");
    }

    LiveForeshowService liveForeshowService = getBean(LiveForeshowService)
    def paramMap= [
        "appId":appId, "foreshowId":foreshowId, "title":title, "imgUrl":imgUrl, "cateId":cateId,
        "description":description,"descriptionHtml":descriptionHtml,"isCost":isCost,"isSale":isSale,
        "price":price,"salePrice":salePrice,"saleStartTime":saleStartTime,"saleEndTime":saleEndTime,
        "showIn":showIn,"interestList":interestList, "srpList":srpList,"isSplit":isSplit,"splitPrice":splitPrice,
        "isSaleSplit":isSaleSplit,"saleSplitPrice":saleSplitPrice,"tryTime":tryTime
    ];
    def res = liveForeshowService.saveOrUpdateGroup(paramMap)
    Map result = [:];
    if (res && res.status) {
        result.status=1
        result.foreshowId=res.foreshowId
        result.msg="成功"
    } else{
        result.status=0
        result.msg="失败"
    }
    System.out.println("============"+result)
    return result
}
