package zs.live.common

import org.apache.ivy.core.module.descriptor.License

/**
 * Created by Administrator on 2016/10/11.
 */
enum LiveStatus {

    SUCCESS("成功",1),
    FAIL("失败",2),
    GetSignIdError("获取signId失败",100)
    private String description;
    private int status;

    public LiveStatus(String description,int status){
        this.description = description
        this.status = status
    }
    public String getDescription() {
        return description;
    }

    public int getStatus() {
        return status;
    }
}
