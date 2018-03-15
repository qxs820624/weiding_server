import zs.live.ApiUtils
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.service.LiveService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 获取属于某个srpId的预告表中的行数，不包含foreshow_status是3或者4的
 *  *
 SELECT *
 FROM live_foreshow_srp_relation r,live_foreshow l
 WHERE r.srp_id = 'f479751542203f3e5f4d300ddacd1127' AND r.foreshow_id=l.foreshow_id AND l.foreshow_status != 3 AND l.foreshow_status != 5 AND l.foreshow_status != 4 AND l.foreshow_type !=5

 */
ApiUtils.processNoEncry({
    Assert.isNotBlankParam(params,"srpId")

    LiveService liveService = bean(LiveService.class)

    int psize = (params.psize ?: 1000) as int
    String sortInfo = params.sortInfo
    String appId = Strings.getAppId(params)
    String srpId = params.srpId
    long userId = params.userId as long
    //直播分类
    List liveCategroyList = liveService.getLiveCategroyList(userId,appId)
    Map map = [
        userId:userId,
        appId:appId,
        psize:psize,
        sortInfo:sortInfo,
        vc:params.vc,
        srpId:srpId,
        cateList:liveCategroyList.subList(1,liveCategroyList.size())
    ]
    //直播列表
    List liveList = liveService.getValueLiveListBySrp(map)

    def hasMore = true
    if (liveList.size()<psize){
        hasMore = false
    }
    binding.setVariable('head', [hasMore: hasMore])

    def foreshowViewList = liveService.findNotStartedLiveForeshowList(map);
    int count=0;
    if(foreshowViewList)
        count+=foreshowViewList.size
    if(liveList) {
        liveList.each{
          if(it.foreshowType!=5)
              count++
            else
              count+=it.liveNum ?:0
        }
    }

     return count;
})
