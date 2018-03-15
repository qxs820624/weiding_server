package zs.live.service.impl

import com.alibaba.fastjson.JSON
import groovy.util.logging.Slf4j
import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.entity.LiveRecord
import zs.live.service.GiftService
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.utils.Http
import zs.live.utils.Parallel
import zs.live.utils.Strings

/**
 * Created by Administrator on 2016/10/18.
 */
@Slf4j
@Service
class GiftServiceImpl implements GiftService{
    @Value('${gift.list.url}')
    String giftUrl;
    @Value('${gift.payment.url}')
    String giftPaymentUrl;
    @Value('${user.charminfo.url}')
    String giftCharminfoUrl;
    @Value('${user.sybinfo.url}')
    String giftSybinfoUrl;
    @Value('${usercenter.encrypt.url}')
    String usercenterEncryptUrl;
    @Value('${vest.payGift.url}')
    String vestPayGiftUrl;
    @Value('${gift.payorder.url}')
    String giftPayorderUrl               //付费直播支付接口php地址
    @Autowired
    LiveService liveService;
    @Autowired
    QcloudLiveService qcloudLiveService
    @Autowired
    LiveQcloudRedis liveQcloudRedis
    @Autowired
    GiftService giftService

    @Override
    def getGiftList(int lastId, int pageSize, String appId){
        def giftList = []
        try {
            String res = Http.post(giftUrl,[pfAppName:appId,appId:appId])
            def resJson = Strings.parseJson(res)
            resJson.body?.each{
                Map gift = [:]
                gift.giftId = it.sybgid ?: 0
                gift.giftName = it.gift_name ?: ""
                gift.imageUrl = it.gift_imageurl ?: ""
                gift.gifUrl = it.gift_gifurl ?: ""
                gift.giftPrice = it.gift_worth ?: 0
                gift.giftType = it.gift_gifurl ? 2 : 1  //展示类型1：静态图，2：gif(当gifurl不为空，则代表是gif类型)
                giftList.add(gift)
            }
        }catch (Exception e){
            e.printStackTrace()
        }
        return giftList
    }

    def giftPayment(Map map){
        Map res = [:]
        //调用php接口扣费
        try {
            Map data = [
                oprateclientid:map.serialNumber,//打赏请求id 须确保唯一性
                senduserid:map.userId,//打赏搜悦用户id
                receiveuserid:map.toUserId,//被打赏搜悦用户id
                liveid:map.liveId,//直播id
                giftid:map.giftId,//礼物id
                giftnum:map.giftCount,//礼物数量
                opId:map.opId,
                openId:map.openId,
                gm_c:URLEncoder.encode(map.encData,"UTF-8"),
                pfAppName: map.appId,
                appId:map.appId
            ]
            String str = Http.post(giftPaymentUrl,data)
            if(!str){
                res.status = 3
                res.msg = "php请求超时"
                return res
            }
            def strJson = Strings.parseJson(str)
            if(strJson){
                int code = strJson.head?.code ?: 500
                if(code == 200){
                    //php扣费成功，异步发送IM消息
                    int charmCount = 0
                    int sybCount = 0
                    try {
                        charmCount = (strJson.body?.totalCharm ?: "0") as int
                        sybCount = (strJson.body?.sendSyb ?: "0") as int
                        Parallel.run([1],{
                            LiveRecord liveRecord = liveService.findLiveByLiveId(map.liveId)
                            if(!liveRecord) {
                                liveRecord = liveService.findLiveRecordByLiveId(map.liveId)
                            }
                            Map userInfo = [
                                "nickname": map.userNickname,
                                "userImage": map.userImage,
                                "userId": map.userId
                            ]
                            Map anchorInfo = [
                                charmCount:charmCount,//主播魅力值
                                liveId:map.liveId//直播id
                            ]
                            Map imParams = [
                                roomId:liveRecord?.roomId?:0, //房间号
                                giftInfo:Strings.parseJson(map.giftInfo), //礼物信息5.3
                                giftCount:map.giftCount,//礼物个数
                                userInfo:userInfo, //打赏礼物的用户的信息
                                anchorInfo:anchorInfo,
                                fromAccount:map.userId, //此处为发送消息的userid，稍后陶陶会把参数hostUid变更为fromAccount
                                appId:map.appId
                            ]
                            String qcloudRes = qcloudLiveService.sendGiftImMsg(imParams)
                            log.info("giftPayment php success,liveid=>{},map=>{},php return=>{},qcloudRes=>{}",map.liveId,imParams,str,qcloudRes)
                        },0)
                    }catch (Exception e){
                        res.status = 0
                        res.msg = "php扣费成功，Im消息失败，java内部错误:"+e.getMessage()
                        e.printStackTrace()
                    }
                    res.status = 1
                    res.msg = "成功"
                    res.charmCount = charmCount
                    res.sybCount = sybCount
                }else if(code == 700){
                    //php扣费失败:打赏人被冻结、被打赏人被冻结
                    log.info("giftPayment php fail,map=>{},php return=>{}",map,str)
                    res.status = 700
                    res.msg = strJson.head?.msg ?: "扣费失败"
                }else{
                    //php扣费失败,其他原因
                    log.info("giftPayment php fail,map=>{},php return=>{}",map,str)
                    res.status = 4
                    res.msg = strJson.head?.msg ?: "php返回700错误，但是没有desc"
                }
            }
        }catch (Exception e){
            res.status = 0
            res.msg = "java内部错误"
            log.info("giftPayment java内部错误，Exception=>{}",e.getMessage())
        }
        return res
    }

    def getUserCharmCount(def userInfo,String appId){
        def charmMap = [:]
        try {
            String userName = userInfo?.userName ?: ""
            if(userName){
                try {
                    String res = Http.post(giftCharminfoUrl,[username:userName,pfAppName:appId,appId:appId])
                    def resJson = Strings.parseJson(res)
                    if(resJson){
                        charmMap.charmCount = resJson.body?.totalCharm ?: 0
                    }
                }catch (Exception e){
                    charmMap.msg="调用php接口失败"
                    log.info("getUserCharmCount 调用php接口失败，userId=>{}，Exception=>{}",userInfo.userId,e.getMessage())
                    e.printStackTrace()
                }
            }
        }catch (Exception e){
            charmMap.msg="getUserCharmCount 接口内部错误"
            log.info("getUserCharmCount 接口内部错误，userId=>{}，Exception=>{}",userInfo.userId,e.getMessage())
        }
        return charmMap
    }

    def getUserSybCount(Long userId,String opId,String openId,String appId){
        def charmMap = [:]
        try {
            def userInfo = liveService.getUserInfo(userId)
            String userName = userInfo?.userName ?: ""
            if(userName){
                //加密
                Map paramsMap = [:]
                if(opId && openId){
                    String encData = this.usercenterEncrypt(opId,openId,[username:userName,pfAppName:appId,appId:appId])
                    if(encData){
                        paramsMap = [gm_c: encData, opId: opId, openId: openId]
                    }else{
                        charmMap.msg="加密失败"
                        log.info("getUserSybCount 用户中心加密失败，userId=>{},appId=>{},opId=>{},openId=>{}",userId,appId,opId,openId)
                    }
                }else{
                    paramsMap = [username:userName,pfAppName: appId,appId:appId]
                }
                if(paramsMap) {
                    try {
                        String res = Http.post(giftSybinfoUrl,paramsMap)
                        log.info("getUserSybCount from php,userName=>{},appId=>{},php return=>{}",userName,appId,res)
                        def resJson = Strings.parseJson(res)
                        if(resJson){
                            charmMap.charmCount = resJson.body?.totalCharm ?: 0
                            charmMap.sybCount = resJson.body?.syb ?: 0
                        }
                    }catch (Exception e){
                        charmMap.msg="调用php接口失败"
                        log.info("getUserSybCount 调用php接口失败，userId=>{},opId=>{},openId=>{}，Exception=>",userId,opId,openId,e.getMessage())
                    }
                }else{
                    log.info("getUserSybCount 参数非法，userId=>{},opId=>{},openId=>{}",userId,opId,openId)
                }
            }else{
                log.info("getUserSybCount userId:{},userInfo:{}",userId,userInfo)
            }
        }catch (Exception e){
            charmMap.msg="getUserSybCount 接口内部错误"
            e.printStackTrace()
        }
        return charmMap
    }

    @Override
    def addVestPayGiftOrder(Map giftMap) {
        log.info("wangtf 马甲打赏礼物生成订单，url:{},params:{}",vestPayGiftUrl, giftMap)
        return Http.post(vestPayGiftUrl, giftMap)
    }

    String usercenterEncrypt(String opId,String openId,Map data){
        String encData = ""
        try {
            Map postParam = [
                appid:"10022",
                appkey:"e8712e37-8e90-11e6-ac81-a08cdba8b9a4",
                opId:opId,
                openId:openId,
                str: JSON.toJSONString(data)
            ]
            String res = Http.post(usercenterEncryptUrl,postParam)
            def resJson = Strings.parseJson(res)
            if(resJson){
                encData = resJson.body?.data ?: ""
            }
        }catch (Exception e){
            e.printStackTrace()
        }
        return encData
    }

}
