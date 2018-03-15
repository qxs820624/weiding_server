package zs.live.service.impl

import com.baidubce.BceClientConfiguration
import com.baidubce.auth.DefaultBceCredentials
import com.baidubce.services.bos.BosClient
import com.baidubce.services.bos.BosClientConfiguration
import com.baidubce.services.bos.model.CompleteMultipartUploadRequest
import com.baidubce.services.bos.model.CompleteMultipartUploadResponse
import com.baidubce.services.bos.model.InitiateMultipartUploadRequest
import com.baidubce.services.bos.model.InitiateMultipartUploadResponse
import com.baidubce.services.bos.model.PartETag
import com.baidubce.services.bos.model.UploadPartRequest
import com.baidubce.services.bos.model.UploadPartResponse
import com.baidubce.services.media.MediaClient
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Service
import zs.live.service.BaiduCloudService

/**
 * Created by zhougc  on 2018/1/10.
 * @Description: 百度云存储的视频上传及转码
 */
@Service
@Slf4j
class BaiduCloudServiceImpl implements BaiduCloudService{
    static final String ACCESS_KEY_ID="e27d999ab5074a05aee95ca7bf8386c9";
    static final String SECRET_ACCESS_KEY="0426fd1969094742be9e7f3ffc973a9a";
    static final String PRESET_NAME="bce.video_hls_transmux"; //转化为m3u8的模板
    static final String BUCKET_NAME_TEST="syvideotest";
    static final String BUCKET_NAME="syvideo";
    @Override
    String uploadFile(String bucketName,String sourceFilePath,String targetFileName) {
        BosClientConfiguration config = new BosClientConfiguration();
        config.setCredentials(new DefaultBceCredentials(ACCESS_KEY_ID, SECRET_ACCESS_KEY));
        BosClient client = new BosClient(config);
        InitiateMultipartUploadRequest initiateMultipartUploadRequest =
                new InitiateMultipartUploadRequest(bucketName, targetFileName);
        InitiateMultipartUploadResponse initiateMultipartUploadResponse =
                client.initiateMultipartUpload(initiateMultipartUploadRequest);
        List<PartETag> partETags=this.uploadFileHandle(client, initiateMultipartUploadResponse,sourceFilePath,
                         bucketName,targetFileName);
        CompleteMultipartUploadRequest completeMultipartUploadRequest =
                new CompleteMultipartUploadRequest(bucketName, targetFileName,
                        initiateMultipartUploadResponse.getUploadId(), partETags);
        // 完成分块上传
        CompleteMultipartUploadResponse completeMultipartUploadResponse =
                client.completeMultipartUpload(completeMultipartUploadRequest);
        String etag=completeMultipartUploadResponse.getETag();
        log.info("========百度云文件上传完成，源文件名："+sourceFilePath+"===返回etag:"+etag+"==目标文件名："+targetFileName);
        return etag
    }

    @Override
    String mp4TranscodeM3u8(String bucketName, String sourceFileName, String targetFileName) {
        BceClientConfiguration config = new BceClientConfiguration();
        config.setCredentials(new DefaultBceCredentials(ACCESS_KEY_ID, SECRET_ACCESS_KEY));
        MediaClient client = new MediaClient(config);
        String pipelineName="syvideoupload8";//队列名称
        String sourceBucket=bucketName;//由于转化的文件在同一个文件夹下，所以sourceBucket和targetBucket相同
        String targetBucket=bucketName;
        this.createPipeline (client, pipelineName,sourceBucket,targetBucket,20);
        String jobId=this.createJob(client,pipelineName,sourceFileName,targetFileName,PRESET_NAME);
        return jobId;
    }

   private List<PartETag> uploadFileHandle(BosClient client,InitiateMultipartUploadResponse initiateMultipartUploadResponse,
                                           String sourceFilePath,String bucketName,String targetFileName){
        final long partSize = 1024 * 1024 * 5L;
        File partFile = new File(sourceFilePath);
        // 计算分块数目
        int partCount = (int) (partFile.length() / partSize);
        if (partFile.length() % partSize != 0){
            partCount++;
        }
        // 新建一个List保存每个分块上传后的ETag和PartNumber
        List<PartETag> partETags = new ArrayList<PartETag>();
        try{
            for(int i = 0; i < partCount; i++){
                // 获取文件流
                FileInputStream fis = new FileInputStream(partFile);
                // 跳到每个分块的开头
                long skipBytes = partSize * i;
                fis.skip(skipBytes);
                // 计算每个分块的大小
                long size = partSize < partFile.length() - skipBytes ?
                        partSize : partFile.length() - skipBytes;
                // 创建UploadPartRequest，上传分块
                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(bucketName);
                uploadPartRequest.setKey(targetFileName);
                uploadPartRequest.setUploadId(initiateMultipartUploadResponse.getUploadId());
                uploadPartRequest.setInputStream(fis);
                uploadPartRequest.setPartSize(size);
                uploadPartRequest.setPartNumber(i + 1);
                UploadPartResponse uploadPartResponse = client.uploadPart(uploadPartRequest);
                partETags.add(uploadPartResponse.getPartETag());
                // 关闭文件
                fis.close();
            }
        }catch (Exception e){
            log.info("=====baidu upload file error:"+e.getMessage());
        }
        return  partETags;
    }

    private void createPipeline (MediaClient client, String pipelineName,
                                String sourceBucket, String targetBucket, int capacity) {
        // 新建一个Pipeline
        client.createPipeline(pipelineName, sourceBucket, targetBucket, capacity);
    }
    private String createJob(MediaClient client, String pipelineName,
                          String sourceKey, String targetKey, String presetName) {
        String jobId = client.createTranscodingJob(pipelineName, sourceKey, targetKey, presetName).getJobId();
        return jobId;
    }
    public static  void main(String[] args){
        BaiduCloudServiceImpl service=new BaiduCloudServiceImpl();
        service.uploadFile("syvideotest","d://dahai.mp3","mydahai.mp3");
    }
}
