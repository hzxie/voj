package org.verwandlung.voj.web.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Calendar和Date的辅助类.
 *
 * @author Haozhe Xie
 */
public class DateUtils {
	/**
	 * 获取几天前的某一天的Date对象.
	 * @param period - 与当前时间的间隔(以天为单位)
	 * @return 几天前的某一天的Date对象
	 */
	public static Date getPreviousDate(int period) {
		Date today = new Date();
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(today);

		if ( period == 7 ) {
			calendar.add(Calendar.DATE, -7);
		} else if ( period == 30 ) {
			calendar.add(Calendar.MONTH, -1);
		} else {
			calendar.add(Calendar.YEAR, -1);
		}
		return calendar.getTime();
	}
}
