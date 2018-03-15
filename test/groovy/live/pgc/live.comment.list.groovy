import zs.live.ApiUtils
import zs.live.dao.mysql.QcloudLiveRes
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 会议直播评论列表
 */
ApiUtils.processNoEncry({
    Assert.isNotBlankParam(params, "liveId")
    int  psize = (params.psize ?: 10) as int
    params.psize = psize + 1
    params.lastId = (params.lastId ?: 0) as long

    QcloudLiveService cloudLiveService = bean(QcloudLiveService)
    List commentList = cloudLiveService.findLiveCommentList(params as Map)
    boolean hasMore = false
    if(commentList && commentList.size() > psize){
        hasMore = true
        commentList = commentList.subList(0, psize)
    }
    binding.setVariable('head', [hasMore: hasMore])
    return [commentList: commentList]
})
