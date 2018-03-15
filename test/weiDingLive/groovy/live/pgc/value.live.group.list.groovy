import zs.live.ApiUtils
import zs.live.common.LiveCommon
import zs.live.dao.mysql.LiveRes
import zs.live.dao.mysql.QcloudLiveRes
import zs.live.entity.LiveForeshow
import zs.live.service.LiveForeshowService
import zs.live.service.LiveService
import zs.live.service.ShortURLService
import zs.live.utils.Assert
import zs.live.utils.DateUtil
import zs.live.utils.Strings

/**
 * 应用场景：直播分类查看
 */
ApiUtils.processNoEncry {
    Assert.isNotBlankParam(params,"userId")
    Assert.isNotBlankParam(params,"liveSerieId");
    String appId=Strings.getAppId(params)


    LiveService liveService = getBean(LiveService)
    ShortURLService shortURLService = getBean(ShortURLService)
    LiveForeshowService liveForeshowService = getBean(LiveForeshowService)
    QcloudLiveRes qcloudLiveRes = getBean(QcloudLiveRes)

    def parentId= params.liveSerieId
    long userId= params.userId as long;
    int psize = (params.psize ?: 10) as int
    String sortInfo = params.sortInfo
    String vc = params?.vc
    def from = params.from?:'app'

    def map = [
        parentId:parentId,
        psize:psize,
        sortInfo:sortInfo,
        userId:userId,
        appId:appId,
        vc:vc,
        from:from //h5 app
    ]
    def foreshowList = liveService.getValueLiveList(map)
    def liveSerieInfo = [:]
    if (!sortInfo || sortInfo=='{}'){
        def userForeshow = liveService.getUserForeshow(userId,parentId as long,appId);//是否已关注
        def payStatus = liveForeshowService.checkForeshowPayment([userId:userId,foreshowIds:parentId]);//是否付费
        LiveForeshow foreshow = liveForeshowService.get(parentId as long) //专栏信息
        int watchCount = 0
        map.psize = 1000
        map.nocahche = 'nocahche'
        def resultList = liveService.getValueLiveList(map)
        resultList.each {
            watchCount += (it.watchCount?:0 as int)
        }
        if (foreshow){
            def extendInfo = [:]
            if (foreshow.isCost == 1){
                extendInfo = qcloudLiveRes.findLiveRecordPayExtendByLiveId(parentId as long,1)
            }
            int isSale = 0
            if((extendInfo?.saleStartTime ? (extendInfo.saleStartTime+"000") as long: 0)<=System.currentTimeMillis()
                && System.currentTimeMillis()<=(extendInfo?.saleEndTime ? (extendInfo.saleEndTime + "000") as long: 0)){
                isSale = 1
            }
            def shortUrl = shortURLService.getShortUrlLiveGroup([foreshowId: foreshow.foreshowId,appId: appId, userId: userId])
            liveSerieInfo = [
                userForeshowStatus:userForeshow?1:0, //0:未关注 1.已关注
                liveSerieId:parentId,
                title: foreshow.title,
                description: foreshow.description,
                descriptionHtml: foreshow.descriptionHtml,
                isCost: foreshow.isCost,//1：付费
                isSale: isSale,//1：促销
                price:  extendInfo?.price?:0,
                rmbPrice:extendInfo?.price?Strings.getDivideNum(extendInfo.price,"100"):0,
                rmbSalePrice:extendInfo?.salePrice?Strings.getDivideNum(extendInfo.salePrice,"100"):0,
                salePrice:extendInfo?.salePrice?:0,
                saleStartTime:extendInfo?.saleStartTime?:0,
                saleEndTime:extendInfo?.saleEndTime?:0,
                appAccount: extendInfo?.mpAccount ?: "",
                watchCount: watchCount,
                liveThumb: foreshow.imgUrl,
                liveNum:resultList.size(),
                isPaid:payStatus.get(parentId as String)?:0,//0 未购买 >0 已购买
                shortUrl: shortUrl
            ]
        }
    }

    def hasMore = true
    if (foreshowList.size()<psize){
        hasMore = false
    }
    binding.setVariable('head', [hasMore: hasMore])

    return [
        liveSerieInfo:liveSerieInfo,       //类别
        liveList:foreshowList         //直播、暂停、系列
    ]
}
