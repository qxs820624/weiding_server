import zs.live.ApiUtils
import zs.live.dao.mysql.LiveSynData

ApiUtils.processNoEncry({
    LiveSynData liveSynData = getBean(LiveSynData)
    liveSynData.synFans();
})
