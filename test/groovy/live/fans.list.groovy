import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 场景：获得粉丝或关注列表接口
 访问地址：http://域名/live/fans.list.groovy
 参数：
 {
 userId: 当前用户
 lastId: 0 为第一页，其他页为第一页返回的id值的最后一个
 operType: 1 : 我的粉丝 ，2 : 我关注的
 }
 返回值：
 说明：followType 1代表单方面关注，2代表互相关注
 {
 "head": {
 "status": 200,
 "hasMore": false
 },
 "body":{
 "list":[
 {
 "id":123,
 "userId":20005,
 "userName":"lilihua"，
 "nickname":"张守",
 "userLogo":"http://www.sohu.com/11231234/myhead.pic",
 "followType":1
 },
 {
 "id":125,
 "userId":20006,
 "userName":"lilihua"，
 "nickname":"张历史",
 "userLogo":"http://www.sohu.com/11231234/myhead2.pic",
 "followType":2
 }
 ]
 }
 }
 * */
ApiUtils.processNoEncry {

    Assert.isNotBlankParam(params, "userId")
    Assert.isNotBlankParam(params, "lastId")
    Assert.isNotBlankParam(params, "operType")

    String appId=Strings.getAppId(params)
    LiveService liveService = getBean(LiveService)

    long userId
    long lastId
    int operType;
    try {
        userId = params.userId as long;
        lastId = params.lastId as long;
        operType = params.operType as int;
    } catch (NumberFormatException e) {
        throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "参数转换错误");
    }

    int fansType = 1
    int followType = 0
    int pageSize = (params.psize ?: 10) as int
    if(pageSize > 500){//最大就取500条，防止pc或者其他应用调用接口给的分页值太大
        pageSize = 500
    }
    def list;
    def count;
    if (operType == 1) {
       // 1 : 我的粉丝
        list = liveService.getMyFans(userId, fansType, followType, lastId, pageSize,appId);
        if (list && list.size() >= pageSize) {
            //只有当list的size等于pageSize时才有可能有更多的元素，这时查询剩余元素的个数是不是也大于pageSize
            count = liveService.getMyFansCount(userId, fansType, followType, lastId,appId);
            if (count > pageSize) {
                binding.setVariable('head', [
                    hasMore: true,
                ])
            }
        }
        return [list: list];
    } else if (operType == 2) {
        //2 : 我关注的
        list = liveService.getMyFollowings(userId, fansType, followType, lastId, pageSize,appId);
        if (list && list.size() >= pageSize) {
            //只有当list的size等于pageSize时才有可能有更多的元素，这时查询剩余元素的个数是不是也大于pageSize
            count = liveService.getMyFollowingsCount(userId, fansType, followType, lastId,appId);
            if (count > pageSize) {
                binding.setVariable('head', [
                    hasMore: true,
                ])
            }
        }
        return [list: list];
    }

    throw new ApiException(ApiException.STATUS_WRONG_PARAMS, "operType参数错误");
}
