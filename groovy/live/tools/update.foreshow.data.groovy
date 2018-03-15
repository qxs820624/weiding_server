import zs.live.ApiUtils
import zs.live.service.ImportBackVideoService
import zs.live.utils.Strings

ApiUtils.processNoEncry{
    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File("groovy/pics.txt"))));
    List liveId = []
    String str = "";
    String imageUrl = ""
    try {
        while ((str = reader.readLine()) != null) {
            if(str == null || str.isEmpty()){
                continue;
            }
            List strs = Strings.splitToList(str,"&");
            liveId = Strings.splitToList(strs.get(0).toString(),",")
            imageUrl = strs.get(1)
            System.out.println("liveId===>"+liveId+",imageUrl=======>"+imageUrl)
            ImportBackVideoService importBackVideoService = getBean(ImportBackVideoService.class)
            importBackVideoService.insertForeshowLiveData(liveId,imageUrl)
        }
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

