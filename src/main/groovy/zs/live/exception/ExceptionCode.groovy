package zs.live.exception;

/**
 * Created by Administrator on 2017/1/10.
 */
public class ExceptionCode {


    public enum DataExNo  implements CoreEnum{
        /**
         * 参数格式不正确或取值超出范围
         */
        PARAM_ILLEGAL("903");


        private final String value;

        private DataExNo(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum SystemExNo  implements CoreEnum{
        /**
         * 网络请求错误
         */
        NET_CONNECT("910"),
        /**
         * 网络请求错误
         */
        MYSQL_CONNECT("911"),

        /**
         * redis
         */
        REDIS_SERVICE_ERROR("914"),

        /**mongo
         *
         */
        MONGO_SERVICE_ERROR("915"),

        /**mongo
         *
         */
        KAFKA_SERVICE_ERROR("916"),


        /**腾讯云
         *
         */
        QCLOUD_SERVICE_ERROR("920"),


        /**小鱼
         *
         */
        XIAOYU_SERVICE_ERROR("930");


        private final String value;

        private SystemExNo(String value) {
            this.value = value;
        }
        /**
         * Get the integer value of this enum value, as defined in the Thrift IDL.
         */
        public String getValue() {
            return value;
        }
    }
}
