import zs.live.ApiUtils
import zs.live.common.LiveCommon
import zs.live.dao.mysql.LiveRes
import zs.live.dao.mysql.QcloudLiveRes
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.entity.LiveRecord
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.service.VestUserService
import zs.live.utils.Assert
import zs.live.utils.Strings

/**
 * 修复直播缓存
 */
ApiUtils.processNoEncry{
    LiveQcloudRedis liveQcloudRedis = getBean(LiveQcloudRedis)
    QcloudLiveRes qcloudLiveRes = getBean(QcloudLiveRes)
    LiveService liveService = getBean(LiveService)
    LiveRes liveRes = getBean(LiveRes)
    VestUserService vestUserService = bean(VestUserService)

    long liveId = (params.liveId ?: 0)as long
    if(liveId){
        this.updateCountRedis(liveId,liveQcloudRedis,vestUserService)
    }else{
        List liveList = liveRes.findLiveRecordListForUpdateCountRedis()
        System.out.println("==========="+liveList.size())
        liveList?.each{
            liveId = (it.live_id ?: 0)as long
            if(liveId){
                this.updateCountRedis(liveId,liveQcloudRedis,vestUserService)
            }
        }
        System.out.println("结束运行，条数："+liveList.size())
    }

}
def updateCountRedis(long liveId,LiveQcloudRedis liveQcloudRedis,VestUserService vestUserService){

    //将live_vest_user_list的缓存数据，放入新的缓存中
    List userList = liveQcloudRedis.getVestUserListByLiveId(liveId,0,-1)
    List userListNew = []
    if(userList) {
        userList.each {
            def userInfo = Strings.parseJson(it)
            String userImage = userInfo.userImage as String
            if(!userImage.contains("!userhead50x50")){
                userInfo.userImage = vestUserService.fillVestUserImage(userImage.substring(userImage.indexOf("selfcreate/")+11))
            }
            userListNew.add(userInfo)
        }
        vestUserService.setVestUserListByLiveId(liveId, userListNew)
    }

    long totalWatchCount = liveQcloudRedis.getLiveWatherTotalCount(liveId)  //直播累计人数的pv
    long vestCount= liveQcloudRedis.getVestCount(liveId)    //马甲的pv

    //将真实观众的pv放入缓存中
    long realWatchCount = totalWatchCount-vestCount //真实观众的pv
    liveQcloudRedis.setLiveRealWatchCount(liveId,realWatchCount)

    //将头像uv的缓存放入缓存中（包括真实观众的头像和马甲头像的uv）
    long watchCount = liveQcloudRedis.getLiveWatchListUv(liveId)    //头像的uv
    liveQcloudRedis.setLiveWatchCount(liveId,watchCount)


}
