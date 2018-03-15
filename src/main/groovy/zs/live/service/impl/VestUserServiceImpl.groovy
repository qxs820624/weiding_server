package zs.live.service.impl

import groovy.util.logging.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.dao.redis.LiveRedis
import zs.live.service.VestUserService
import zs.live.utils.RandomUtil
import zs.live.utils.Strings

/**
 * Created by zhougc  on 2017/4/7.
 * @Description:
 */
@Service
@Slf4j
class VestUserServiceImpl implements  VestUserService{
    static final Logger vestUserLog = LoggerFactory.getLogger("vestUser")
    static final int VEST_lIST_CHILD_NUM=10//马甲列表拆分的子列表数
    static final int VEST_lIST_CHILD_LIVE_NUM=5 //针对某个直播的马甲列表
    static final String YPY_VEST_IMAGE_URL="http://user-pimg.b0.upaiyun.com/selfcreate/"
    static final String VEST_IMAGE_POSTFIX="!userhead50x50"
    static final int MAX_LIVE_VEST_USER_COUNT=200//当直播观看人数达到200时，删除一部分，保留100个

    @Autowired
    LiveRedis liveRedis
    @Autowired
    LiveQcloudRedis liveQcloudRedis


    @Override
    void initAllVestUserInRedis(List vestList){
        //key为：vest_user_list_0，vest_user_list_1。vest_user_list_9
        //值为list
        long a=System.currentTimeMillis()
        def map= splitVestList2SmallByOrder(LiveQcloudRedis.VEST_USER_LIST,vestList)
        map.keySet().each {key->
            def childVestList=map.get(key)
            long c1=System.currentTimeMillis()
            //通过管道将拆分的马甲用户列表一次性写入redis缓存
            liveRedis.zaddWithPipeline(key,childVestList)
            long c2=System.currentTimeMillis()
            vestUserLog.info("initAllVestUserInRedis zaddWithPipeline time:"+(c2-c1))
            Thread.sleep(500)
        }
        long b=System.currentTimeMillis()
        vestUserLog.info("initAllVestUserInRedis total time:"+(b-a))
    }

    @Override
    def getVestUserFromRedis(long liveId,long userId){
        def key= getVestUserKey2Live(getVestUserPreKey2Live(liveId),userId)
        def tmpVestUser=liveQcloudRedis.getVestUserInfoFromRedis(key,userId)
        def vestUserMap
        tmpVestUser.each{//只有一个，因此循环一次
            vestUserMap=Strings.parseJson(it)
            vestUserMap.userImage=fillVestUserImage(vestUserMap.userImage)
        }
       return vestUserMap
    }

    @Override
    List getVestUserList(int userCount){
        int num= RandomUtil.getRandomNumber(0, 9)
        String key= LiveQcloudRedis.VEST_USER_LIST+num
        int start=RandomUtil.getRandomNumber(0,2700)
        int end=start+userCount
        def vestUserList=liveQcloudRedis.getVestUserList(key,start,end)
        vestUserList= vestUserList.collect {
            def map=Strings.parseJson(it)
            map.userImage=fillVestUserImage(map.userImage as String)
            map
        }
        return  vestUserList
    }

    @Override
    void setVestUserListByLiveId(long liveId, List vestList) {
        def preKey=getVestUserPreKey2Live(liveId)
        def map= splitVestList2SmallByUserId(preKey,vestList)
        map.keySet().each {key->
            def childVestList=map.get(key)
            liveRedis.zaddWithPipeline(key,childVestList)
        }
    }

    @Override
    List getVestUserListByLiveId(long liveId, int vestCount) {
        def vestUserList=null
        String key
        def groupTotalCount=0
        //尝试5次，避免在早期，有的key下还没有马甲的情况
        List numList = []
        for(int i=0;i<this.VEST_lIST_CHILD_LIVE_NUM;i++){
            numList.add(i,i)
        }
        //打乱顺序，防止每次都是从第一个list获取数据
        Collections.shuffle(numList,new Random())
        for(int num:numList){
            key= getVestUserPreKey2Live(liveId)+num
            groupTotalCount=liveRedis.zcard(key)
            if(groupTotalCount>0){
                break
            }
        }
        if(groupTotalCount>vestCount){
            int start=RandomUtil.getRandomNumber(0,(groupTotalCount-vestCount-1) as int)
            int end=start+vestCount-1
            vestUserList=liveQcloudRedis.getVestUserList(key,start,end)
        }else if(vestCount!=0){
             vestUserList=liveQcloudRedis.getVestUserList(key,0,-1)
        }
        if(vestUserList){
            vestUserList= vestUserList.collect {
                def map=Strings.parseJson(it)
                map.userImage=fillVestUserImage(map.userImage as String)
                map
            }
        }
        return vestUserList
    }


    @Override
    long getVestUserCountByLiveId(long liveId) {
        def totalCount=0
        (0..4).each{
            String key= getVestUserPreKey2Live(liveId)+it
            totalCount+=liveRedis.zcard(key)?:0
        }
        return totalCount
    }
    @Override
    long getVestUserVirtualAndRealCountByLiveId(long liveId){
       long  virtualCount = (liveRedis.get(LiveQcloudRedis.LIVE_VEST_USER_COUNT_PERMINUTE + liveId) ?: 0) as long
       long  realCount=(getVestUserCountByLiveId(liveId)?:0)as long
        return virtualCount+realCount
    }
    @Override
    long getVestUserVirtualCountByLiveId(long liveId){
        return  (liveRedis.get(LiveQcloudRedis.LIVE_VEST_USER_COUNT_PERMINUTE + liveId) ?: 0) as long
    }

    @Override
    def delVestUserByLiveIdAndUserId(long liveId,def userInfo) {
        String preKey= getVestUserPreKey2Live(liveId)
        def key=getVestUserKey2Live(preKey,userInfo.userId)
        userInfo.userImage=splitVestUserImage(userInfo.userImage)
        return liveRedis.zrem(key,Strings.toJson(userInfo))
    }
    @Override
    void delAllVestUserByLiveId(long liveId){
        (0..4).each{
            def key=getVestUserPreKey2Live(liveId)+it
            liveRedis.del(key)
        }
    }

    @Override
    long getLiveRoomWatchCount(long liveId) {
        return liveQcloudRedis.getLiveWatchCount(liveId) + getVestUserVirtualCountByLiveId(liveId)
    }

    String getVestUserPreKey2Live(long liveId){
       return  LiveQcloudRedis.LIVE_VEST_USER_LIST_PRE+liveId+"_"
    }
   /**
     * 通过userId将vestList拆分为10个小的list，存放map中
     * @param vestList
     * @return
     */
    def splitVestList2SmallByUserId(def preKey,List vestList){
        def map=[:]
        //将vestList拆分成10个小的list,规则为：userId/10取余
        vestList.each { user->
            long userId=user.userId as long
            String key=getVestUserKey2Live(preKey,userId)
            def childVestList=map.get(key)
            if(childVestList){
                childVestList<<[userId:user.userId,nickname:user.nickname,
                                userImage:splitVestUserImage(user.userImage)]
            }else{
                childVestList=[]
                childVestList<<[userId:user.userId,nickname:user.nickname,
                                userImage:splitVestUserImage(user.userImage)]
            }
            map.put(key,childVestList)
        }
        return  map
    }
    String getVestKey(def preKey,long userId){
        int remainder=userId%VEST_lIST_CHILD_NUM
        return  preKey+remainder
    }
    String getVestUserKey2Live(def preKey,long userId){
        int remainder=userId%VEST_lIST_CHILD_LIVE_NUM
        return  preKey+remainder
    }
    String splitVestUserImage(String userImage){
        //路径：http://user-pimg.b0.upaiyun.com/selfcreate/1610/2414/59/p506.jpg!userhead50x50
        //tmpPth=1610/2414/59/p506.jpg!userhead50x50
        //返回：1610/2414/59/p506.jpg
        def tmpPath= userImage.substring(userImage.indexOf("selfcreate/")+11)
        return  tmpPath.substring(0,tmpPath.indexOf("!"))
    }
    String fillVestUserImage(String userImage){
       if(userImage){
           return YPY_VEST_IMAGE_URL+userImage+VEST_IMAGE_POSTFIX
       }
       return null
    }

    void cutterLiveWatchList(long liveId){
        def key=liveQcloudRedis.LIVE_WATCHER_LIST_KEY+liveId
        def totalCount=liveRedis.zcard(key)
        if(totalCount>=MAX_LIVE_VEST_USER_COUNT){
            liveRedis.zremrangeByRank(key,100,totalCount-1)
        }
    }
    /**
     * 按顺序将vestList拆分为10个小的list（每个存储2820个），存放map中
     * @param vestList
     * @return
     */
    def splitVestList2SmallByOrder(def preKey,List vestList){
        def MAX_COUNT=2820//每组包含的马甲数
        def map=[:]
        //将vestList拆分成10个小的list,规则为：userId/10取余
        int count=0;
        int keyFlag=0
        vestList.each { user->
            if(count==MAX_COUNT){
                count=0
                keyFlag++
            }
            String key=preKey+keyFlag
            def childVestList=map.get(key)
            if(childVestList){
                childVestList<<[userId:user.userId,nickname:user.nickname,
                                userImage:splitVestUserImage(user.userImage)]
            }else{
                childVestList=[]
                childVestList<<[userId:user.userId,nickname:user.nickname,
                                userImage:splitVestUserImage(user.userImage)]
            }
            map.put(key,childVestList)
            count++
        }
        return  map
    }
}
