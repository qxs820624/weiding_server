import com.qcloud.Common.VodDemo
import zs.live.ApiUtils

/**
 * Created by Administrator on 2017/1/12 0012.
 */
ApiUtils.processNoEncry{

    String files = params.files
    String fileId = VodDemo.updload(files)
    return fileId
}
