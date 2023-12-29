/**
 * 
 */
package com.oneclicktech.spring.util;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
 
 

/**
 * @author kg255035
 *
 */
public class DateUtil extends DateUtils {

	/**
	 * @param args
	 */
	private static final Logger logger = Logger.getLogger("DateUtil");
	
	public static String getISODateFromTimeFormat(Date date) {
		Date newDate =  DateUtil.stringToDate(DateUtil.dateToString(date, "yyyy-MM-dd 00:01"),  "yyyy-MM-dd HH:mm");
		return DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(newDate);
	}
	
	public static String getISODateToTimeFormat(Date date) {
		Date newDate =  DateUtil.stringToDate(DateUtil.dateToString(date, "yyyy-MM-dd 23:59"),  "yyyy-MM-dd HH:mm");
		return DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(newDate);
	}
	
	public static Date convertToDateViaSqlTimestamp(LocalDateTime dateToConvert) {
	    return java.sql.Timestamp.valueOf(dateToConvert);
	}
	
	public static String getNewISODateToTimeFormat(Date date) {
		Date newDate =  DateUtil.stringToDate(DateUtil.dateToString(date, "yyyy-MM-dd 23:59"),  "yyyy-MM-dd HH:mm");
		return DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.format(newDate);
	}
	
	public static Date stringToDate(String dateStr) {
		Date date = new Date(dateStr);
 		return date;
	}
	
	public static Date stringToDate(String dateStr, String pattern) {
		try {
			SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
			Date parseDate = dateFormat.parse(dateStr);
			return parseDate;
		} catch (Exception e) { 
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
		
		return null;
	}

	public static String dateToString(java.util.Date date, String dateFormat) {
		if (date == null)
			return "-";
		SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
		return formatter.format(date);
	}
	 
	public static String getDateNowInPattern(String pattern) {
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		return sdf.format(new Date());
	}
	
	public static Date getDateInManilaPH() { 
		ZoneId asiaManila = ZoneId.of("Asia/Manila"); 
		Date nowUtc = new Date();
		TimeZone asiaManilaTimeZone = TimeZone.getTimeZone(asiaManila);
		Calendar nowAsiaManila = Calendar.getInstance(asiaManilaTimeZone);
		nowAsiaManila.setTime(nowUtc);
		return nowAsiaManila.getTime();
		
	}
	 
	public static String getDateWithPattern(Date myDate, String pattern) {
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		return sdf.format(myDate);
	}
	
	public static Date getStringToDate(String dateStr, String pattern) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat(pattern);
			Date theDate = sdf.parse(dateStr);
			return theDate;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static Date getDateNowPlusTime(int calendarField, int addTime) {
		Calendar calen = Calendar.getInstance();
		calen.add(calendarField, addTime);
		return calen.getTime();
	}
	
	
	public static Date getDateNowPlusTime(Date date, int calendarField, int addTime) {
		Calendar calen = Calendar.getInstance();
		calen.setTime(date);
		calen.add(calendarField, addTime);
		return calen.getTime();
	}
	
	public static Date getDateNowPlusTime(TimeZone timeZone, int calendarField, int addTime) {
		Calendar calen = Calendar.getInstance(timeZone);
		calen.add(calendarField, addTime);
		return calen.getTime();
	}
	 
	
	public static long getTimeDiffByTimezone(TimeZone fromTimeZone, TimeZone toTimeZone) {
		Calendar fromCalen = Calendar.getInstance(fromTimeZone);
  		Calendar toCalen = Calendar.getInstance(toTimeZone);
  	 	int millis = fromTimeZone.getOffset(toCalen.getTimeInMillis());
  	 	//long timeDiffHr = TimeUnit.MILLISECONDS.toHours(millis);
  	 	long timeDiffHr = TimeUnit.HOURS.convert(millis, TimeUnit.MILLISECONDS);
		return timeDiffHr;
	}
	
	public static long getTimeDiffByDate(Date fromDate, Date toDate) {
		System.out.println("fromDate: " + fromDate.toString());
		System.out.println("toDate:   " + toDate.toString());
	    long diffInMillies = Math.abs(toDate.getTime() - fromDate.getTime());
	    long diff = TimeUnit.MILLISECONDS.toHours(diffInMillies);
		return diff;
	}
	
	
	public static Date getDateNowSetTime(int calendarField, int dateTime) {
		Calendar calen = Calendar.getInstance();
		calen.set(calendarField, dateTime);
		return calen.getTime();
	}
	
	public static Date getDateInISO_OFFSET(String isoDate) { 
		try {
			Date dateInISOOffset = DateUtils.parseDateStrictly(isoDate, "yyyy-MM-dd'T'HH:mm:ssXXX");
			return dateInISOOffset;
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
  		return null;
	}
	
	 
	public static boolean isWithinDate(Date fromDate, Date toDate, Date receiveDate) {
 
		return	(fromDate.getTime() <= receiveDate.getTime() && receiveDate.getTime() <= toDate.getTime()); 
  	} 
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
//		String dateStr = DateUtil.dateToString(new Date(), "MM/dd/yyyy");
//		Date dateNow = DateUtil.stringToDate(dateStr, "MM/dd/yyyy");
//		 Date dateInPH = DateUtil.getDateInManilaPH();
//	    String requestDate = DateUtil.getDateWithPattern(dateInPH, "dd-MM-yyyy");
//		String requestTime = DateUtil.getDateWithPattern(dateInPH, "HH:mm a");
		
		System.out.println(DateUtil.getDateInISO_OFFSET("2023-10-21T15:10:45+08:00"));
	}

}
