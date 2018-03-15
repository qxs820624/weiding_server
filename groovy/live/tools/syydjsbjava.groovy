def html = """
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
</head>
<body>
工具<br>
<a href="/live/tools/decrypt.groovy">解密接口</a><br>
<a href="/live/tools//live.data.view.groovy">查询数据</a><br>

修复数据<br>
<a href="/live/tools/live.get.backvideo.one.groovy">根据liveId生成回放</a><br>
<a href="/live/tools/live.video.merge.groovy">根据forwshowId合并视频</a><br>
<a href="/live/tools/video.file.delete.groovy">根据fielId清除视频</a><br>
<a href="/live/tools/foreshow.update.liveRecordInfo.groovy">修改直播回放地址（只支持带预告的直播）</a><br>

<br><br><br>
接口展示<br>
填充马甲的评论数据：/live/tools/update.vest.comment.redis.groovy<br>
马甲用户的填充：/live/tools/vest.user.list.redis.groovy<br>
获得粉丝或关注列表接口：/live/fans.list.groovy<br>
<---------------------------------><br>
if (res ==2) {
//        msg="打赏用户不存在"
        msg ="打赏失败（2）"
    } else if (res ==3) {
//        msg="被打赏用户不存在"
        msg ="打赏失败（3）"
    } else if (res ==4) {
//        msg="直播不存在"
        msg ="打赏失败（4）"
    } else if (res ==5) {
//        msg="主播不在该直播间"
        msg ="打赏失败（5）"
    } else if (res ==6) {
//        msg="打赏人不在该直播间"
        msg ="打赏失败（6）"
    }<br>
<---------------------------------><br>
推流:https://yun.tim.qq.com/v3/openim/videorelay<br>
祝大家春节快乐!!!<br>
</body>
</html>
"""
out << html
