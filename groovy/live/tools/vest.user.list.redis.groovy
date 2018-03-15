import zs.live.ApiUtils
import zs.live.dao.mysql.LiveRes
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.service.VestUserService

ApiUtils.processNoEncry{
    LiveRes liveRes = bean(LiveRes)
    LiveQcloudRedis liveQcloudRedis = bean(LiveQcloudRedis)
    VestUserService vestUserService=bean(VestUserService)
    List vestList = liveRes.findAllVestUserList()
    //liveQcloudRedis.setAllVestUserList(vestList)
    vestUserService.initAllVestUserInRedis(vestList)
    println("运行结束！")
}

