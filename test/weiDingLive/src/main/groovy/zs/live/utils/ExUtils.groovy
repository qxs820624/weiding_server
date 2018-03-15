package zs.live.utils

import zs.live.exception.CoreEnum
import zs.live.exception.DataException
import zs.live.exception.SystemException

/**
 * Created by Administrator on 2017/1/18.
 */
class ExUtils {


    /**
     * 根据code，生成系统异常
     *
     * @param code
     *            异常的编码除System.之外的其他位数
     * @param args
     *            参数
     * @return 系统异常
     */
    public static SystemException systemEx(final CoreEnum enu, final Object... args) {
        return new SystemException(enu, args);
    }

    /**
     * 根据code，生成系统异常
     *
     * @param e
     *            包装的其他异常
     * @param code
     *            异常的编码除System.之外的其他位数
     * @param args
     *            参数
     * @return 系统异常
     */
    public static SystemException systemEx(final Throwable e, final CoreEnum enu,
                                           final Object... args) {
        return new SystemException(e, enu.getValue(), args);
    }


    /**
     * 根据code，生成DataException
     *
     * @param e
     *            包装的其他异常
     * @param code
     *            异常的编码除Data.之外的其他位数
     * @param args
     *            参数
     * @return 数据异常
     */
    public static DataException dataEx(final Throwable e, final String code,
                                       final Object... args) {
        return new DataException(e, code, args);
    }

    /**
     * 根据code，生成DataException
     *
     * @param code
     *            异常的编码除Data.之外的其他位数
     * @param args
     *            参数
     * @return 数据异常
     */
    public static DataException dataEx(final String code) {
        return new DataException(code);
    }

    /**
     * 根据code，生成DataException
     *
     * @param code
     *            异常的编码除Data.之外的其他位数
     * @param args
     *            参数
     * @return 数据异常
     */
    public static DataException dataEx(final CoreEnum enu, final Object... args) {
        return new DataException(enu, args);
    }





}
