import zs.live.ApiUtils
import zs.live.dao.redis.LiveQcloudRedis
import zs.live.utils.Strings

ApiUtils.processNoEncry({
    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File("groovy/vestCommentList.txt"))));
    LiveQcloudRedis liveQcloudRedis = getBean(LiveQcloudRedis)
    String str = "";
    List commentList = []
    try {
        liveQcloudRedis.delVestCommentListRedis()
        while ((str = reader.readLine()) != null) {
            System.out.println(str)
            if(str== null || str.isEmpty()){
                continue;
            }
            commentList.add(str)
        }
        liveQcloudRedis.setVestCommentListRedis(Strings.toJson(commentList))
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        try {
            reader.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
    def commentList1 = liveQcloudRedis.getVestCommentList()
    return commentList1
})


