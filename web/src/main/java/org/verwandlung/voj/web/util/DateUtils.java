/* Verwandlung Online Judge - A cross-platform judge online system
 * Copyright (C) 2014-2026 Haozhe Xie <root@haozhexie.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.verwandlung.voj.web.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * The helper class for Calendar and Date.
 *
 * @author Haozhe Xie
 */
public class DateUtils {
  /**
   * Gets the Date object of a day several days ago.
   *
   * @param period - the interval from the current time (in days)
   * @return the Date object of a day several days ago
   */
  public static Date getPreviousDate(int period) {
    Date today = new Date();
    Calendar calendar = new GregorianCalendar();
    calendar.setTime(today);

    if (period == 7) {
      calendar.add(Calendar.DATE, -7);
    } else if (period == 30) {
      calendar.add(Calendar.MONTH, -1);
    } else {
      calendar.add(Calendar.YEAR, -1);
    }
    return calendar.getTime();
  }

  /**
   * Gets the Date object of an exact number of days before now (day-precise, unlike {@link
   * #getPreviousDate(int)} which buckets to week/month/year). Used by the activity heat-map.
   *
   * @param days - the number of days before now
   * @return the Date object of {@code days} days ago
   */
  public static Date getDateBefore(int days) {
    Calendar calendar = new GregorianCalendar();
    calendar.add(Calendar.DATE, -days);
    return calendar.getTime();
  }
}
