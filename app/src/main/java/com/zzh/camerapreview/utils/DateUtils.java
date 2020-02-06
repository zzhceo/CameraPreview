package com.zzh.camerapreview.utils;

import android.net.ParseException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {
    private static final String TAG = "DateUtils";
    public static final String FORMAT_NO_DIVISION_DATE = "yyyyMMddHHmmss";
    public static final String FORMAT_HAS_DIVISION_DATE = "yyyy-MM-dd HH:mm:ss";

    // currentTime要转换的long类型的时间
    // formatType要转换的时间格式yyyy-MM-dd HH:mm:ss//yyyy年MM月dd日 HH时mm分ss秒
    public static String longToDateString(long currentTime, String formatType)
            throws ParseException {
        String tempFormatType = formatType;
        if (TextUtils.isEmpty(formatType))
            tempFormatType = FORMAT_HAS_DIVISION_DATE;
        Date dateOld = new Date(currentTime); // 根据long类型的毫秒数生命一个date类型的时间
        return dateToString(dateOld, tempFormatType);
    }

    // formatType格式为yyyy-MM-dd HH:mm:ss//yyyy年MM月dd日 HH时mm分ss秒
    // data Date类型的时间
    public static String dateToString(Date data, String formatType) {
        return new SimpleDateFormat(formatType).format(data);
    }

    // get old distanceDay days string of format
    public static String getOldDate(int distanceDay) {
        SimpleDateFormat dft = new SimpleDateFormat(FORMAT_HAS_DIVISION_DATE);
        Date beginDate = new Date();
        Calendar date = Calendar.getInstance();
        date.setTime(beginDate);
        date.set(Calendar.DATE, date.get(Calendar.DATE) - distanceDay);    //加号为N天前
//        date.set(Calendar.DATE, date.get(Calendar.DATE) + distanceDay); //加号为N天后
        Date endDate = null;
        try {
            endDate = dft.parse(dft.format(date.getTime()));
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }
        String oldDate = dft.format(endDate);
        Log.i(TAG, "getOldDate oldDate=" + oldDate);
        return oldDate;
    }

    // get old distanceDay days string of format
    public static long getOldDateMillis(int distanceDay) {
        Date beginDate = new Date();
        Calendar date = Calendar.getInstance();
        date.setTime(beginDate);
        date.set(Calendar.DATE, date.get(Calendar.DATE) - distanceDay);    //加号为N天前
//        date.set(Calendar.DATE, date.get(Calendar.DATE) + distanceDay); //加号为N天后
        long timeInMillis = date.getTimeInMillis();
        Log.i(TAG, "getOldDateMillis timeInMillis=" + timeInMillis
                + ", timeInMillis to string=" + longToDateString(timeInMillis, FORMAT_HAS_DIVISION_DATE));
        return timeInMillis;
    }

    //获得当天0点时间， 毫秒
    public static long getTimesMorning() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    //获得当天24点时间
    public static long getTimesNight() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 24);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    // strTime要转换的String类型的时间
    // formatType时间格式
    // strTime的时间格式和formatType的时间格式必须相同
    public static long stringToLong(String strTime, String formatType)
            throws ParseException {
        Date date = stringToDate(strTime, formatType); // String类型转成date类型
        if (date == null) {
            return 0;
        } else {
            long currentTime = dateToLong(date); // date类型转成long类型
            return currentTime;
        }
    }

    // date要转换的date类型的时间
    public static long dateToLong(Date date) {
        return date.getTime();
    }

    // strTime要转换的string类型的时间，formatType要转换的格式yyyy-MM-dd HH:mm:ss//yyyy年MM月dd日
    // HH时mm分ss秒，
    // strTime的时间格式必须要与formatType的时间格式相同
    public static Date stringToDate(String strTime, String formatType)
            throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat(formatType);
        Date date = null;
        try {
            date = formatter.parse(strTime);
        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    // only uid is system has used
    public static void changeSystemTime(long time) {
        try {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(time);
            long when = c.getTimeInMillis();
            SystemClock.setCurrentTimeMillis(when);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
