package zs.live.exception



/**
 * 自定义异常基类，处理status和message的关联
 */
class CoreException extends RuntimeException{

    Integer status;

    /**
     * 文字，也可以是code值,需要与message文件对应
     */
    String message;
    /**
     * 对应message的参数
     */
    protected Object[] args;



    public CoreException(final Throwable cause,final String message, final Object... args) {
        super(message,cause)
        this.message = message;
        this.args = args;
    }

    public CoreException(final CoreEnum enu, final Object... args) {
        super(enu.getValue())
        this.message = enu.getValue();
        this.args = args;
    }

    public CoreException(final String message, final Object... args) {
        super(message)
        this.message = message;
        this.args = args;
    }

    /**
     *
     * 返回本地化的Message
     *
     * @return 本地化的Message
     */

    @Override
    public String getMessage() {
        return message;
    }

    /**
     * @Title:  setArgs <BR>
     * @Description: please write your description <BR>
     * @return: Object[] <BR>
     */
    public void setArgs(Object[] args) {
        this.args = args;
    }


    public Object[] getArgs() {
        return args;
    }

}
