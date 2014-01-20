/*  

Copyright [2013-2014] eBay Software Foundation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/
package models.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import play.Logger;
/**
 * 
 * @author ypei
 *
 */
public class DateUtils {

	public static PeriodFormatter PERIOD_FORMATTER = new PeriodFormatterBuilder()
			.printZeroNever().appendDays().appendSuffix(" day", " days")
			.appendSeparator(", ").appendHours()
			.appendSuffix(" hour", " hours").appendSeparator(", ")
			.appendMinutes().appendSuffix(" minute", " minutes")
			.appendSeparator(", ").appendSeconds()
			.appendSuffix(" second", " seconds").toFormatter();

	/*
	 * Returns the string in the standard date format used yyyy-MM-dd
	 */
	public static String getDateStr(Date d) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String str = sdf.format(d);
		return str;
	}

	
	/*
	 * Returns the string in the standard date format used yyyy-MM-dd
	 */
	public static String getDateStrNoSpace(Date d) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		String str = sdf.format(d);
		return str;
	}
	/*
	 * Returns the string in the pattern
	 */
	public static String getDateStr(Date d, String pattern) {
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		String str = sdf.format(d);
		return str;
	}

	/**
	 * Convert the date format yyyy-MM-dd to string
	 * 
	 * @param str
	 * @return
	 */
	public static Date getDateFromStr(String str) {

		Date d = null;

		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			d = sdf.parse(str);
		} catch (Exception ex) {
			Logger.error("Exception while converting string : " + str
					+ " to date");
		}
		return d;
	}

	public static Date getDateFromStrLongCsm(String str) {

		Date d = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
			d = sdf.parse(str);
		} catch (Exception ex) {
			Logger.error("Exception while converting string : " + str
					+ " to date");
		}
		return d;
	}

	public static Date getDateFromStrLong(String str) {

		Date d = null;
		// SimpleDateFormat that works exactly like Date.toString()
		// SimpleDateFormat sdf2 =
		// new SimpleDateFormat("E MMM dd HH:mm:ss yyyy", Locale.ROOT);
		SimpleDateFormat sdf3 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");

		SimpleDateFormat sdf4 = new SimpleDateFormat(
				"yyyy-MM-dd'T'HH:mm:ss.SSS");
		try {
			d = sdf.parse(str);
		} catch (ParseException e) {

			try {
				d = sdf3.parse(str);
			} catch (ParseException e1) {

				try {
					// #ignores timezone dif
					d = sdf4.parse(str.substring(0, 24));
				} catch (ParseException e2) {
					Logger.error("Exception while converting string : " + str
							+ " to date");
					return new Date();
				}
			}
		}
		return d;
	}

	public static String getDateTimeStr(Date d) {
		if (d == null)
			return "";

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
		String str = sdf.format(d);
		return str;
	}

	public static String getDateTimeStrSdsm(Date d) {
		if (d == null)
			return "";

		if (d.getTime() == 0L)
			return "Never";
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSSZ");
		String str = sdf.format(d);
		return str;
	}
	
	// 2013/05/09-08:40:00
	public static String getDateTimeStrTsdb(Date d) {
		if (d == null)
			return "";

		if (d.getTime() == 0L)
			return "Never";
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
		String str = sdf.format(d);
		return str;
	}
	
	public static String getDateTimeStrConcise(Date d) {
		if (d == null)
			return "";

		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSSZ");
		String str = sdf.format(d);
		return str;
	}

	
	
	public static Date getDateTimeFromMillis(long timeInMillis) {
		DateTime dateTime = new DateTime(timeInMillis, DateTimeZone.UTC);
		return dateTime.toDate();
	}

	/**
	 * Converts the string generated above to Date format
	 * 
	 * @param str
	 * @return
	 */
	public static Date getDateFromStrYYYYMMMDDDHHMMSSSSSZ(String str) {

		Date d = null;
		if (StringUtils.isNullOrEmpty(str))
			return null;

		try {
			SimpleDateFormat sdf = new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss.SSSZ");
			d = sdf.parse(str);
		} catch (Exception ex) {
			Logger.error(ex, "Exception while converting string to date : "
					+ str);
		}

		return d;
	}
	
	
	/**
	 * 20130512
	 * Converts the sdsm string generated above to Date format
	 * 
	 * @param str
	 * @return
	 */
	public static Date getDateFromConciseStr(String str) {

		Date d = null;
		if (StringUtils.isNullOrEmpty(str))
			return null;

		try {
			SimpleDateFormat sdf = new SimpleDateFormat(
					"yyyyMMddHHmmssSSSZ");
			d = sdf.parse(str);
		} catch (Exception ex) {
			Logger.error(ex, "Exception while converting string to date : "
					+ str);
		}

		return d;
	}

	public static Date getDateFromStrYYYYMMMDDDHHMMSSSSSZWithSpaces(String str) {

		Date d = null;
		if (StringUtils.isNullOrEmpty(str))
			return null;

		try {
			SimpleDateFormat sdf = new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss SSS Z");
			d = sdf.parse(str);
		} catch (Exception ex) {
			Logger.error(ex, "Exception while converting string to date : "
					+ str);
		}

		return d;
	}

	public static String getNowDateTimeStr() {

		return getDateTimeStr(new Date());
	}

	public static String getNowDateTimeStrSdsm() {

		return getDateTimeStrSdsm(new Date());
	}
	
	public static String getNowDateTimeStrTsdb() {

		return getDateTimeStrTsdb(new Date());
	}
	public static String get30MinAgoDateTimeStrTsdb() {

		return getDateTimeStrTsdb(get30MinAgoDate());
	}
	
	public static String getNowDateTimeStrConcise() {

		return getDateTimeStrConcise(new Date());
	}


	public static Date getNextDay(Date date) {

		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(Calendar.DATE, 1);
		return c.getTime();
	}

	public static Date dayBefore(Date d) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		cal.add(Calendar.DATE, -1);
		return cal.getTime();
	}

	/**
	 * Returns today's date in the format yyyy-mm-dd
	 * 
	 * @return
	 */
	public static String getTodaysDateStr() {
		Date d = new Date();
		return getDateStr(d);
	}

	public static Date getYesterdaysDate() {
		Calendar cal = new GregorianCalendar();
		cal.add(Calendar.DATE, -1);

		Date d = cal.getTime();
		return d;
	}
	
	
	public static Date get30MinAgoDate() {
		Calendar cal = new GregorianCalendar();
		cal.add(Calendar.MINUTE, -30);

		Date d = cal.getTime();
		return d;
	}
	

	public static Date getNDayBeforeToday(int n) {
		Calendar cal = new GregorianCalendar();
		cal.add(Calendar.DATE, (-1) * n);
		Date d = cal.getTime();
		return d;
	}

	/**
	 * 20130927: change to no space format
	 * @param n
	 * @return
	 */
	public static String getNDayBeforeTodayStr(int n) {
		return getDateStrNoSpace(getNDayBeforeToday(n));
	}

	public static String getYesterdaysDateStr() {
		return getDateStr(getYesterdaysDate());
	}

	/**
	 * Converts the standard date format yyyy-MM-dd to a simple one for charts
	 * 
	 * @param dateStr
	 * @return
	 */
	public static String getSimpleDateForCharts(String dateStr) {
		return dateStr.substring(5);
	}

	public static String getMonthInEnglish(int month) {

		switch (month) {
		case 0:
			return "January";
		case 1:
			return "February";
		case 2:
			return "March";
		case 3:
			return "April";
		case 4:
			return "May";
		case 5:
			return "June";
		case 6:
			return "July";
		case 7:
			return "August";
		case 8:
			return "September";
		case 9:
			return "October";
		case 10:
			return "November";
		case 11:
			return "December";

		}

		return "";

	}

	public long getTodaysBeginEpoch() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		return cal.getTimeInMillis();
	}

	public long getTodaysEndEpoch() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		cal.set(Calendar.MILLISECOND, 999);

		return cal.getTimeInMillis();
	}

	public static Date getDateFromEpoch(long epoch) {

		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(epoch);
		return cal.getTime();
	}

	/**
	 * Returns date in the format Apr 22, 2012 09:12 AM
	 * 
	 * @param d
	 * @return
	 */
	public static String getDateForDisplay(Date d) {
		SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy HH:mm:ss");
		return sdf.format(d);

	}

	public static String getDateForDisplayRSS(Date d) {
		SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy HH:mm:ss");
		return sdf.format(d);

	}

	public static String getDateOnlyForDisplay(Date d) {
		SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
		return sdf.format(d);

	}

	public static String getDateOnlyForDisplayPlain(Date d) {
		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy");
		return sdf.format(d);

	}

	/**
	 * Returns time difference
	 * 
	 * @param d1
	 * @param d2
	 * @return
	 */
	public static String getDateDifference(Date d1, Date d2) {
		Period period = new Period(new DateTime(d1), new DateTime(d2),
				PeriodType.yearMonthDayTime());
		return PERIOD_FORMATTER.print(period);
	}

	public static String getDurationSecFromMillis(long millis) {

		return String.format(" %.4f sec", (double) millis / 1000.00);

	}

	public static Double getDurationSecFromMillisDouble(long millis) {

		return (double) millis / 1000.00;

	}

	public static String getDurationFromTwoDates(Date startTime, Date endTime) {

		long duration = endTime.getTime() - startTime.getTime();
		return getDurationFromMillis(duration);

	}

	public static String getDurationSecFromTwoDates(Date startTime, Date endTime) {

		long duration = endTime.getTime() - startTime.getTime();
		return getDurationSecFromMillis(duration);

	}

	public static double getDurationSecFromTwoDatesDouble(Date startTime,
			Date endTime) {

		long duration = endTime.getTime() - startTime.getTime();
		return getDurationSecFromMillisDouble(duration);

	}

	public static long getDurationSecFromTwoDatesLong(Date startTime,
			Date endTime) {

		long duration = endTime.getTime() - startTime.getTime();
		return duration;

	}
	
	public static String getDurationFromMillis(long millis) {
		return String.format(
				"%d min, %d sec",
				TimeUnit.MILLISECONDS.toMinutes(millis),
				TimeUnit.MILLISECONDS.toSeconds(millis)
						- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS
								.toMinutes(millis)));
	}

	public static void main(String[] args) {
		long t1 = System.currentTimeMillis();
		long t2 = 1344409200 * 1000L;
		models.utils.LogUtils.printLogNormal("Epoch : " + t2 + " = " + getDateFromEpoch(t2));
		models.utils.LogUtils.printLogNormal("Yesterday : " + getYesterdaysDateStr());
		models.utils.LogUtils.printLogNormal("Epoch to Date Time " + t2 + " from Joda time ["
				+ getDateTimeFromMillis(t2) + "]");
		models.utils.LogUtils.printLogNormal("Epoch to Today's Time " + t1 + " using Joda Time ["
				+ getDateTimeFromMillis(t1) + "]");
	}

	public static Date getDateFromStr(String str, String datepattern) {
		Date d = null;
		if (str == null)
			return null;

		try {
			SimpleDateFormat sdf = new SimpleDateFormat(datepattern);
			d = sdf.parse(str);
		} catch (Exception ex) {
			Logger.error(ex, "Exception while converting string to date : "
					+ str);
		}

		return d;
	}

	public static long getDurationFromTwoDatesInLong(Date startTime,
			Date endTime) {

		long duration = endTime.getTime() - startTime.getTime();
		return Math.abs(duration);

	}

}
