package zs.live.utils

import groovy.util.logging.Slf4j
import org.apache.commons.lang.StringUtils

import java.security.Timestamp
import java.text.ParseException
import java.text.SimpleDateFormat

/**
 * Created by Administrator on 14-4-21.
 */
@Slf4j
class DateUtil {
    public final static String ORACLE_DATE_PATTERN = "YYYY-MM-DD HH24MISS";

    public final static String DEFAULT_DATE_PATTERN = "yyyy-MM-dd HHmmss";

    public final static String FULL_DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public final static String LONG_DATE_PATTERN = "yyyy-MM-dd HH:mm:ss:SSS";

    public final static String SHORT_DATE_PATTERN = "yyyy-MM-dd";

    public final static String DATE_PATTERN = "yyyyMMddHHmmss";

    /**
     * 私密圈申请的消息时间格式
     */
    public final static String APPLY_DATE_PATTERN = "MM月dd日 HH:mm";


    /**
     *
     */
    public DateUtil() {
    }

    public static String getFormatDate(Timestamp timeStamp) {
        String strRet = null;
        if (timeStamp == null)
            return strRet = "&nbsp;";
        SimpleDateFormat simple = new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒");
        if (simple != null)
            strRet = simple.format(timeStamp);
        return strRet;
    }

    public static String getFormatDate(java.sql.Date date) {

        String strRet = null;
        if (date == null) {
            return strRet = "&nbsp;";
        }
        SimpleDateFormat simple = new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒");
        if (simple != null)
            strRet = simple.format(date);
        return strRet;
    }

    public static String getTodayDate(String format) {
        String strRet = null;
        SimpleDateFormat simple = new SimpleDateFormat(format);
        if (simple != null)
            strRet = simple.format(new Date());
        return strRet;
    }

    public static String getTodayDay() {
        String strRet = null;
        SimpleDateFormat simple = new SimpleDateFormat(SHORT_DATE_PATTERN);
        if (simple != null)
            strRet = simple.format(new Date());
        return strRet;
    }

    public static String getYesterDay() {
        String strRet = null;
        SimpleDateFormat simple = new SimpleDateFormat(SHORT_DATE_PATTERN);
        if (simple != null)
            strRet = simple.format(addDateByDay(new Date(), -1));
        return strRet;
    }
    public static String getTomorrowDay() {
        String strRet = null;
        SimpleDateFormat simple = new SimpleDateFormat(DATE_PATTERN);
        if (simple != null)
            strRet = simple.format(addDateByDay(new Date(), +1));
        return strRet;
    }
    public static String getDayByDate(Date date) {
        String strRet = null;
        SimpleDateFormat simple = new SimpleDateFormat(SHORT_DATE_PATTERN);
        if (simple != null)
            strRet = simple.format(date);
        return strRet;
    }

    public static java.util.Date getDay(java.util.Date date) {
        String strDate = getFormatDate(date, "yyyy-MM-dd");
        if (strDate) {
            return getDate(strDate, "yyyy-MM-dd");
        }
        return null;
    }

    public static String getFormatDate(java.util.Date date) {

        String strRet = null;
        if (date == null) {
            return strRet = "&nbsp;";
        }
        SimpleDateFormat simple = new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒");
        if (simple != null)
            strRet = simple.format(date);
        return strRet;
    }

    public static String getDefaultDate(java.util.Date date) {
        String strRet = null;
        if (date == null) {
            return strRet = "&nbsp;";
        }
        SimpleDateFormat simple = new SimpleDateFormat(DEFAULT_DATE_PATTERN);
        if (simple != null) {
            strRet = simple.format(date);
        }
        return strRet;

    }

    public static String getFormatDate(java.sql.Date date, String strFormat) {

        String strRet = null;
        if (date == null) {
            return strRet = "&nbsp;";
        }
        SimpleDateFormat simple = new SimpleDateFormat(strFormat);
        if (simple != null)
            strRet = simple.format(date);
        return strRet;
    }

    public static Date getDate(java.util.Date date, String strFormat, String toFromat) {
        String strRet = null;
        Date rtnDate = null;
        SimpleDateFormat simple = new SimpleDateFormat(strFormat);
        if (simple != null) {
            strRet = simple.format(date);
        }
        simple = new SimpleDateFormat(toFromat);
        if (simple != null) {
            try {
                rtnDate = simple.parse(strRet);
            } catch (ParseException e) {
            }
        }
        return rtnDate;
    }

    public static Date getBeginDate(String date) {
        String beginDate = date + " 00:00:00";
        return DateUtil.getDate(beginDate, "yyyy-MM-dd HH:mm:ss");
    }

    public static Date getEndDate(String date) {
        String beginDate = date + " 23:59:59:999";
        return DateUtil.getDate(beginDate, "yyyy-MM-dd HH:mm:ss:SSS");
    }

    public static Date getStartCurrentDay() {
        String date = getFormatDate(new Date(), "yyyy-MM-dd 00:00:00");
        return getDate(date, "yyyy-MM-dd HH:mm:ss");
    }

    public static Date getEndCurrentDay() {
        String date = getFormatDate(new Date(), "yyyy-MM-dd 23:59:59:999");
        return getDate(date, "yyyy-MM-dd HH:mm:ss:SSS");
    }

    public static Date getStartDay(Date day, int i) {
        String date = getFormatDate(addDateByDay(day, i), "yyyy-MM-dd 00:00:00");
        return getDate(date, "yyyy-MM-dd HH:mm:ss");
    }

    public static Date getEndDay(Date day, int i) {
        String date = getFormatDate(addDateByDay(day, i), "yyyy-MM-dd 23:59:59:999");
        return getDate(date, "yyyy-MM-dd HH:mm:ss:SSS");
    }

    public static Date getNextDay() {
        String date = getFormatDate(addDateByDay(new Date(), 1), "yyyy-MM-dd 00:00:00");
        return getDate(date, "yyyy-MM-dd HH:mm:ss");
    }

    public static Date getWeekFirstDay() {
        Calendar c = Calendar.getInstance();
        c.setTime(DateUtil.getDay(c.getTime()));
        c.set(Calendar.DAY_OF_WEEK, 1);
        return c.getTime();
    }

    public static Date getNextDay(int i) {
        String date = getFormatDate(addDateByDay(new Date(), i), "yyyy-MM-dd 00:00:00");
        return getDate(date, "yyyy-MM-dd HH:mm:ss");
    }

    public static Date getNextDay(int i, String strFormat) {
        String date = getFormatDate(addDateByDay(new Date(), i), strFormat);
        return getDate(date, "yyyy-MM-dd HH:mm:ss");
    }

    public static Date getDate(Date cdate, String strFormat) {
        String date = getFormatDate(cdate, strFormat);
        return getDate(date, "yyyy-MM-dd HH:mm:ss");
    }

    public static String getFormatDate(java.util.Date date, String strFormat) {

        String strRet = null;
        if (date == null) {
            return strRet = "";
        }
        SimpleDateFormat simple = new SimpleDateFormat(strFormat);
        if (simple != null)
            strRet = simple.format(date);
        return strRet;
    }

    public static String getFormatDate(Timestamp timeStamp, String strFormat) {
        String strRet = null;
        if (timeStamp == null)
            return strRet = "&nbsp;";
        try {
            SimpleDateFormat simple = new SimpleDateFormat(strFormat);
            if (simple != null) {
                strRet = simple.format(timeStamp);
            }
        } catch (Exception e) {
            log.error("Formate Date Error" + e.getMessage(), e);
        }
        return strRet;
    }

    public static String yyMMdd() {

        SimpleDateFormat simple = new SimpleDateFormat("yyMMdd");
        Date date = new Date();
        return simple.format(date);

    }

    public static String yyMM() {

        SimpleDateFormat simple = new SimpleDateFormat("yyMM");
        Date date = new Date();
        return simple.format(date);

    }

    /**
     * 是否同一个月
     * @param date1
     * @param date2
     * @return
     */
    public static boolean isSameMonth(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        Calendar c = Calendar.getInstance();
        int m1, m2;
        c.setTime(date1);
        m1 = c.get(Calendar.MONTH);
        c.setTime(date2);
        m2 = c.get(Calendar.MONTH);
        return m1 == m2;
    }

    /**
     * 是否同一天
     * @param date1
     * @param date2
     * @return
     */
    public static boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        String str1=DateUtil.getDayByDate(date1);
        String str2=DateUtil.getDayByDate(date2);
        return str1.equals(str2);
    }

    /**
     * add one get first day next month
     *
     * @param currentDate
     * @return
     */
    public static Date getNextMonthDay() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, 1);
        c.set(Calendar.DAY_OF_MONTH, 1);
        return c.getTime();
    }

    /**
     * add one get first day next month
     *
     * @param currentDate
     * @return
     */
    public static Date getPreviousMonthFirstDay() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.MONTH, -1);
        c.set(Calendar.DAY_OF_MONTH, 1);
        return c.getTime();
    }

    public static Date getPreviousMonthLastDay() {
        Calendar cl = Calendar.getInstance();
        cl.set(Calendar.DAY_OF_MONTH, 0);
        cl.set(Calendar.HOUR_OF_DAY, 23);
        cl.set(Calendar.MINUTE, 59);
        cl.set(Calendar.SECOND, 59);
        return cl.getTime();
    }

    /**
     * add one get first day next month
     *
     * @param currentDate
     * @return
     */
    public static Date getDateByCurrentMonth() {
        Calendar c = Calendar.getInstance();
        c.setTime(DateUtil.getDay(c.getTime()));
        c.set(Calendar.DAY_OF_MONTH, 1);
        return c.getTime();
    }

    /**
     * add one get first day next month
     *
     * @param currentDate
     * @return
     */
    public static Date getNextMonthDay(Date currentDate) {
        Calendar c = Calendar.getInstance();
        c.setTime(currentDate);
        c.add(Calendar.MONTH, 1);
        c.set(Calendar.DAY_OF_MONTH, 1);
        return c.getTime();
    }

    /**
     * is same month
     *
     * @param date
     * @return
     */
    public static boolean isCurrentMonth(Date date) {
        if (date == null) {
            return false;
        }
        Calendar c = Calendar.getInstance();
        int m1 = c.get(Calendar.MONTH);
        c.setTime(date);
        int m2 = c.get(Calendar.MONTH);
        return m1 == m2;
    }

    public static int getWeekDay() {
        int weekday;
        Calendar tmp = Calendar.getInstance();
        weekday = tmp.get(Calendar.DAY_OF_WEEK) - 1;
        return weekday;
    }

    public static String getWeekText() {
        String txt = "星期";
        int weekday;
        Calendar tmp = Calendar.getInstance();
        weekday = tmp.get(Calendar.DAY_OF_WEEK) - 1;
        switch (weekday) {
            case 1:
                txt += "一";
                break;
            case 2:
                txt += "二";
                break;
            case 3:
                txt += "三";
                break;
            case 4:
                txt += "四";
                break;
            case 5:
                txt += "五";
                break;
            case 6:
                txt += "六";
                break;
            case 0:
                txt += "日";
                break;
        }
        return txt;
    }

    public static String strTime() {
        String strRet = null;
        Date date = new Date();
        SimpleDateFormat simple = new SimpleDateFormat("HH:mm:ss");
        if (simple != null) {
            strRet = simple.format(date);
        }
        return strRet;
    }

    public static String strDate() {
        String strDate = null;
        Date date = new Date();
        SimpleDateFormat simple = new SimpleDateFormat("yyyy-MM-dd");
        if (simple != null) {
            strDate = simple.format(date);
        }
        return strDate;

    }

    public static Date parseDate(String time, String strFormat, String dateFlag) {
        java.util.Date date = null;
        if (time == null || time.equals("") || dateFlag.equals("")) {
            return date = null;
        } else {
            if (dateFlag && dateFlag.equals("start"))
                time =time.trim() + " 00:00:00";
            if (dateFlag && dateFlag.equals("end"))
                time = time.trim() + " 23:59:59";
        }
        try {
            SimpleDateFormat simple = new SimpleDateFormat(strFormat);
            date = simple.parse(time);
        } catch (Exception e) {
            log.error("Get Date Error!" + e.getMessage(), e);
        }
        return date;
    }


    public static Date getDate(String time, String strFormat) {
        java.util.Date date = null;
        if (time == null || time.equals("")) {
            return date = null;
        }
        try {
            SimpleDateFormat simple = new SimpleDateFormat(strFormat);
            date = simple.parse(time);
        } catch (Exception e) {
            log.error("Get Date Error!" + e.getMessage(), e);
        }
        return date;
    }



    /**
     * get date HH:mm:ss
     * @param date
     * @param strHMSFormat
     * @return
     */
    public static Date getDateByHMS(Date date, String strHMSFormat) {
        try {
            String time = getFormatDate(date, SHORT_DATE_PATTERN + " " + strHMSFormat);
            SimpleDateFormat simple = new SimpleDateFormat(FULL_DATE_PATTERN);
            date = simple.parse(time);
        } catch (Exception e) {
            log.error("Get Date Error!" + e.getMessage(), e);
        }
        return date;
    }

    /**
     * set date by HH:mm:ss
     * @param date
     * @param hh
     * @param mm
     * @param ss
     * @return
     */
    public static Date getDate(Date date, int hh, int mm, int ss) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, hh);
        cal.set(Calendar.MINUTE, mm);
        cal.set(Calendar.SECOND, ss);
        return cal.getTime();
    }

    /**
     * absolute date
     * @param date
     * @param day
     * @return
     */
    public static Date absoluteDate(Date date, int day) {
        if (date == null) {
            return new Date();
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_YEAR, day);
        return cal.getTime();
    }


    /**
     * srcoll date to other day
     * @param day
     * @return
     */
    public static Date absoluteDate(int day) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_YEAR, day);
        return cal.getTime();
    }


    public static Date getDateByHMS(String strHMSFormat) {
        return getDateByHMS(new Date(), strHMSFormat);
    }

    public static Timestamp getTimestampDate(String time, String strFormat) {
        Timestamp tsmp = null;
        if (time == null) {
            return tsmp = null;
        }

        time =time.trim();
        try {
            SimpleDateFormat simple = new SimpleDateFormat(strFormat);
            java.util.Date date = simple.parse(time);
            tsmp = new Timestamp(date.getTime());
        } catch (Exception e) {
            log.error("Get Timestamp Error!" + e.getMessage(), e);
        }
        return tsmp;
    }

    public static Date addDateByDay(Date time, int add_day) {

        if (time == null) {
            return new Date();
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        cal.add(Calendar.DAY_OF_YEAR, add_day);
        return cal.getTime();
    }

    public static Date addDateByDay(Timestamp time, int add_day) {

        if (time == null) {
            return new Date();
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        cal.add(Calendar.DAY_OF_YEAR, add_day);
        return cal.getTime();
    }

    /**
     * 去时分秒
     *
     * @param date
     * @param add_day
     * @return
     */
    public static java.util.Date getRollDay(Date date, int add_day) {
        return getDay(addDateByDay(date, add_day));
    }

    /**
     * 带时分秒
     *
     * @param date
     * @param add_day
     * @return
     */
    public static java.util.Date getRollDate(Date date, int add_day) {
        return addDateByDay(date, add_day);
    }

    public static long between(Date b, Date e) {
        if (b == null || e == null) {
            return 0;
        }
        return (e.getTime() - b.getTime()) / 1000;
    }

    public static Date addDateByMonth(Date time, int add_month) {

        if (time == null) {
            return new Date();
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        cal.add(Calendar.MONTH, add_month);
        return cal.getTime();
    }

    public static Date addHourByDay(Date time, int add_Hour) {

        if (time == null) {
            return new Date();
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        cal.add(Calendar.HOUR_OF_DAY, add_Hour);
        return cal.getTime();
    }

    public static Date addDateByMinute(Date time, int add_minute) {
        if (time == null) {
            return new Date();
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        cal.add(Calendar.MINUTE, add_minute);
        return cal.getTime();
    }

    public static Date addHourBySecond(Date time, int add_second) {
        if (time == null) {
            return new Date();
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        cal.add(Calendar.SECOND, add_second);
        return cal.getTime();
    }

    public static Date getThisWeekFirstDay() {
        Calendar cl = Calendar.getInstance();
        cl.roll(Calendar.DAY_OF_YEAR, -cl.get(Calendar.DAY_OF_WEEK) + 2);
        String date = getFormatDate(cl.getTime(), "yyyy-MM-dd 00:00:00");
        return getDate(date, "yyyy-MM-dd HH:mm:ss");
    }

    public static Date getThisWeekLastDay() {
        Calendar cl = Calendar.getInstance();
        cl.roll(Calendar.DAY_OF_YEAR, -cl.get(Calendar.DAY_OF_WEEK) + 8);
        String date = getFormatDate(cl.getTime(), "yyyy-MM-dd 23:59:59:999");
        return getDate(date, "yyyy-MM-dd HH:mm:ss:SSS");
    }

    public static Date getPreviousWeekFirstDay() {
        Calendar cl = Calendar.getInstance();
        cl.roll(Calendar.DAY_OF_YEAR, -cl.get(Calendar.DAY_OF_WEEK) - 5);
        String date = getFormatDate(cl.getTime(), "yyyy-MM-dd 00:00:00");
        return getDate(date, "yyyy-MM-dd HH:mm:ss");
    }

    public static Date getPreviousWeekLastDay() {
        Calendar cl = Calendar.getInstance();
        cl.roll(Calendar.DAY_OF_YEAR, -cl.get(Calendar.DAY_OF_WEEK) + 1);
        String date = getFormatDate(cl.getTime(), "yyyy-MM-dd 23:59:59:999");
        return getDate(date, "yyyy-MM-dd HH:mm:ss:SSS");
    }

    public static Date getThisMonthFirstDay() {
        Calendar cl = Calendar.getInstance();
        cl.set(Calendar.DAY_OF_MONTH, 1);
        String date = getFormatDate(cl.getTime(), "yyyy-MM-dd 00:00:00");
        return getDate(date, "yyyy-MM-dd HH:mm:ss");
    }

    public static Date getThisMonthLastDay() {
        Calendar cl = Calendar.getInstance();
        cl.add(Calendar.MONTH, 1);
        cl.set(Calendar.DATE, 0);
        String date = getFormatDate(cl.getTime(), "yyyy-MM-dd 23:59:59:999");
        return getDate(date, "yyyy-MM-dd HH:mm:ss:SSS");
    }

    public static Date getMonthFirstDay(Date now) {
        Calendar cl = Calendar.getInstance();
        cl.setTime(now);
        cl.set(Calendar.DAY_OF_MONTH, 1);
        String date = getFormatDate(cl.getTime(), "yyyy-MM-dd 00:00:00");
        return getDate(date, "yyyy-MM-dd HH:mm:ss");
    }

    public static Date getMonthLastDay(Date now) {
        Calendar cl = Calendar.getInstance();
        cl.setTime(now);
        cl.add(Calendar.MONTH, 1);
        cl.set(Calendar.DATE, 0);
        String date = getFormatDate(cl.getTime(), "yyyy-MM-dd 23:59:59:999");
        return getDate(date, "yyyy-MM-dd HH:mm:ss:SSS");
    }

    public static int getMonthOfLastDay(int year, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

    }

    public static int getMonthOfFirstDay(int year, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        return calendar.getActualMinimum(Calendar.DAY_OF_MONTH);
    }
    //获得时间戳   13位
    public static long getTimestamp(String dateTime){
        SimpleDateFormat format = new SimpleDateFormat(FULL_DATE_PATTERN);
        Date date = new Date();
        if(dateTime){
            date = format.parse(dateTime);
        }
        return  date.getTime()
    }
    public static Date getDateByLong(long millis) {
        Date da = new Date(millis);
       return da
    }

    //获得第二天某个时间点的过期时间
    public static long getExpireTimeForNextDay(int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_YEAR, 1);
        return  new BigDecimal(cal.getTimeInMillis()/1000).toLong()
    }

    public static int getRemainTime(int day){
        SimpleDateFormat sdf = new SimpleDateFormat(SHORT_DATE_PATTERN);
        Date date = sdf.parse(sdf.format(new Date()));
        Calendar   calendar   =   new   GregorianCalendar();
        calendar.setTime(date);
        calendar.add(calendar.DATE,day);//把日期往后增加一天.整数往后推,负数往前移动
        date=calendar.getTime();   //这个时间就是日期往后推一天的结果
        def time = (date.getTime()-new Date().getTime())/1000;
        return time
    }

    def static String  computingTime(String t,boolean isTimeMillis){
        try {
            long times
            if(!isTimeMillis){
                times = Long.parseLong(t.toString().trim())*1000
            }else {
                times = Long.parseLong(t.toString().trim())
            }
            if (times < 1000) {
                return ""
            }
            long diff = (System.currentTimeMillis() - times) / 1000
            if (diff < 60) {
                return "刚刚"
            }
            diff = Math.round(diff / 60)
            if (diff < 60) {
                return diff + " 分钟前"
            }
            diff = Math.round(diff / 60)
            if (diff < 24) {
                return diff + " 小时前"
            }
            diff = Math.round(diff / 24)
            if (diff == 1) {
                return "昨天"
            }
            return new SimpleDateFormat("yyyy-M-d").format(new Date(times))
        } catch (Exception e) {
            //e.printStackTrace()
            return ""
        }
    }

    public static String secToTime(int time) {
        int minute = time/60;
        int sec = time%60;

        StringBuffer sb = new StringBuffer()
        if(minute > 0){
            sb.append((minute<10)?"0" + minute:minute).append(":").append((sec<10)?"0" + sec:sec)
        }
        else {
            sb.append("00:").append((sec<10)?"0" + sec:sec)
        }
        return sb.toString()
    }

    public static String timerFormat(String timer) {

        String content = null;
        String match = "[1-9]+[0-9]*";
        if (StringUtils.isBlank(timer) || "0".equals(timer)
            || "null".equals(timer)) {
            content = "";
        } else if (timer.matches(match) && timer.length() == 10) {
            content = new SimpleDateFormat("yyyy-MM-dd").format(new Date(Long
                .parseLong(timer) * 1000));
        } else if (timer.length() > 10 && timer.indexOf(" ") != -1) {
            content = timer.substring(0, timer.indexOf(" ") - 1);
        } else {
            content = timer;
        }
        return content;
    }

    /**
     * 将13位的时间戳转换为yyyy-MM-dd HH:mm:ss格式的时间
     * @param time
     * @return
     */
    public static String getFormatDteByLong(long time){
        if (!time)
            return ""
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateTime = df.format(time);
        return dateTime
    }

    /**
     * 得到几天后的时间
     *
     * @param d
     * @param day
     * @return
     */
    public static Date getDateAfter(Date d, int day) {
        Calendar now = Calendar.getInstance();
        now.setTime(d);
        now.set(Calendar.DATE, now.get(Calendar.DATE) + day);
        return now.getTime();
    }

    /**
     * 返回小时
     * @param date
     * @return
     */
    public static int getHour(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * 返回分钟
     * @param date
     * @return
     */
    public static int getMinute(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.MINUTE);

    }


}
