import zs.live.ApiException
import zs.live.ApiUtils
import zs.live.service.GiftService
import zs.live.utils.Strings

/**
 * 分页获取礼物列表
 *
 * */
ApiUtils.process {
    int psize = (params.psize ?: 100) as int
    int pno = (params.pno ?: 1) as int
    String appId=Strings.getAppId(params)

    GiftService giftService = bean(GiftService.class)
    def list = giftService.getGiftList(pno,psize,appId)
    return [list: list];
}
