import zs.live.ApiUtils
import zs.live.service.QcloudLiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 互动直播回放的评论列表接口
 * */
ApiUtils.processNoEncry {
    Assert.eitherOrJudgeParam(params, "foreshowId","liveId")

    String appId=Strings.getAppId(params)
    QcloudLiveService qcloudLiveService = getBean(QcloudLiveService)

    params.appId = appId
    int psize = (params.psize ?: 20) as int
    params.psize = psize + 1
    params.lastId = (params.lastId ?: 0) as long
    def commentList =  qcloudLiveService.findLiveRecordCommentList(params as Map)
    boolean hasMore = false
    if(commentList && commentList.size() > psize){
        hasMore = true
        commentList = commentList.subList(0, psize)
    }
    binding.setVariable('head', [hasMore: hasMore])
    return [commentList: commentList]
}
