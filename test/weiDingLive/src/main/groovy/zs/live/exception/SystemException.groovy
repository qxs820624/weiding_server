package zs.live.exception;

/**
 *
 * @author liyongguang
 * @date 2017/1/10.
 */
public class SystemException extends CoreException {





    public SystemException(final Throwable cause,final String message, final Object... args) {
        super(cause,message,  args);
    }

    public SystemException(final CoreEnum message, final Object... args) {
        super(message, args);
    }
}
