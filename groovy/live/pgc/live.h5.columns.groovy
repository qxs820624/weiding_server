import com.alibaba.fastjson.JSON
import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.entity.LiveForeshow
import zs.live.service.LiveForeshowService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 会议直播详情页中h5栏目列表
 */
ApiUtils.process({
    Assert.eitherOrJudgeParam(params, "foreshowId","liveId")

    long foreshowId = (params.foreshowId?:0) as long
    long liveId = (params.liveId?:0) as long
    long userId = (params.userId?:0) as long
    String appId=Strings.getAppId(params)

    LiveForeshowService liveForeshowService = bean(LiveForeshowService)
    LiveForeshow liveForeshow = liveForeshowService.get(foreshowId);
    if(!liveForeshow){
        throw new ApiException(700,"预告不存在")
    }
    String ruleJson = liveForeshow.ruleJson
    def res = [:]
    String briefUrl = liveForeshowService.briefUrl+"?userid=${userId}&foreshowId=${foreshowId}" +
        "&liveId=${liveId}&appId=${appId}&time="+System.currentTimeMillis()
    def map = [[category:"brief",title:"简介",url:briefUrl],
               [category:"comment",title:"交流",url:""],
               [category:"goods",title:"商品",url:""]
    ]
    def columns = Strings.parseJson(ruleJson)?.columns
    if(columns){
        int isShow = (columns.isShow?:0) as int
        if(isShow == 1){
            map.addAll(columns?.column)
        }
    }
    res.columns = map
    return res
})
