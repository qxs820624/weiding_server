package zs.live;

public class ApiException extends RuntimeException {

    public static final int STATUS_SUCCESS = 200;
    public static final int STATUS_UNKNOWN_ERROR = 500;  // 未知错误
    public static final int STATUS_WRONG_PARAMS = 500;   // 错误的参数
    public static final int STATUS_DUPLICATE_DATA = 600; // 重复的数据
    public static final int STATUS_BUSINESS_ERROR = 600;   // 自定义错误
    public static final int STATUS_INVALID_TOKEN = 601;  // 无效的token
    public static final int STATUS_EMAIL_UNACTIVATED = 602;   // 邮箱未激活
    public static final int STATUS_NICK_NAME_EXISTS = 603;   // 用户昵称已存在
    public static final int STATUS_IMEI_NOT_DIFFERENT = 604; //IMEI设备号不相同
    public static final int STATUS_FORESHOW_CREATED_CANNOT_DELETE = 604; //会议已经创建预告，无法删除
    public static final int STATUS_GET_BODY_MSG = 700; //客户端取body中的提示语
    public static final int STATUS_CONTENT_RESTRICT = 701;  // 发帖限制
    static final int STATUS_UPGRADE_REMIND=800; //低版本提醒升级到高版本
    static final int STATUS_UPGRADE_SKIP_HTML5 = 801;  //低版本升级提醒并跳转到HTML5页面

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    Integer status;
    String description;
    public static ThreadLocal<ApiException> exceptions = new  ThreadLocal<>();

    public ApiException(Integer stauts, String message, Throwable e) {
        super(message, e);
        this.status = stauts;
        exceptions.set(this);
    }

    public ApiException(Integer stauts, String message) {
        super(message);
        this.status = stauts;
        exceptions.set(this);
    }

    public ApiException(Integer stauts, String message, String description) {
        super(message);
        this.status = stauts;
        this.description = description;
        exceptions.set(this);
    }
}
