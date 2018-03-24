import zs.live.ApiUtils
import zs.live.service.LiveService
import zs.live.service.QcloudLiveService
import zs.live.service.impl.QcloudLiveCommon
import zs.live.utils.Strings

//String str = """
//{"qcloud_appid":"1251961939","app_key":"AKID5dtXCCu7dzMyCaRlfpWgFK7NElSn4wqK","app_secret":"4AkwGnZjb3aLePysfzrE6SWlyVHula9s","sdkappid":"1400016891","account":"adminjava","account_type":"6335","public_key":"-----BEGIN PUBLIC KEY-----
//MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEWlARNfycHEZ47Rdt+I2lWOupFrpNf9TK
//uR7qdnIzw+tKhLouZxmiSQPBToLivDAzbEYHbdCqAohaPyk5p6BEXQ==
//-----END PUBLIC KEY-----","private_key":"-----BEGIN PRIVATE KEY-----
//MIGEAgEAMBAGByqGSM49AgEGBSuBBAAKBG0wawIBAQQgse1jM31b3IFfxzLMsgIN
//giDruwI7YkwbA3gV4fDvY1ihRANCAARaUBE1/JwcRnjtF234jaVY66kWuk1/1Mq5
//Hup2cjPD60qEui5nGaJJA8FOguK8MDNsRgdt0KoCiFo/KTmnoERd
//-----END PRIVATE KEY-----"}
//"""
//
//def json = Strings.parseJson(str)
//System.out.println(json.private_key)

ApiUtils.processNoEncry(){
    LiveService liveService = bean(LiveService)
    return liveService.getUserInfo(1)

}
