package zs.live.exception;

/**
 * Created by Administrator on 2017/1/10.
 */
public class DataException extends CoreException {




    public DataException(final Throwable cause,final String message, final Object... args) {
        super(cause,message,  args);
    }

    public DataException(final CoreEnum enu, final Object... args) {
        super(enu, args);
    }

    public DataException(final String  message, final Object... args) {
        super(message, args);
    }
}
