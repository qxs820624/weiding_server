package zs.live.service

/**
 * Created by Administrator on 2016/11/14.
 */
interface ImportBackVideoService {

    /**
     * 导入回看数据
     */
    void importBackVideoService(long liveId);
    def insertForeshowLiveData(List liveId, String imageUrl)
    /**
     * 导入有问题时删除导入数据
     */
    void deleteImport();
}
