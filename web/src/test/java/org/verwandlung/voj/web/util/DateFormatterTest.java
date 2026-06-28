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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The test class for DateFormatter.
 *
 * @author Haozhe Xie
 */
public class DateFormatterTest {
  /** Test case: tests dateTimeLongMedium(Date, String). Test data: a fixed date and the en_US slug. Expected: a long date and medium time containing the month name, day and year. */
  @Test
  public void testDateTimeLongMedium() {
    String formatted = dateFormatter.dateTimeLongMedium(fixedDate(), "en_US");
    Assertions.assertTrue(formatted.contains("March"), formatted);
    Assertions.assertTrue(formatted.contains("10"), formatted);
    Assertions.assertTrue(formatted.contains("2015"), formatted);
  }

  /** Test case: tests dateMedium(Date, String). Test data: a fixed date and the en_US slug. Expected: the medium date "Mar 10, 2015". */
  @Test
  public void testDateMedium() {
    Assertions.assertEquals("Mar 10, 2015", dateFormatter.dateMedium(fixedDate(), "en_US"));
  }

  /** Test case: tests dateLong(Date, String). Test data: a fixed date and the en_US slug. Expected: the long date "March 10, 2015". */
  @Test
  public void testDateLong() {
    Assertions.assertEquals("March 10, 2015", dateFormatter.dateLong(fixedDate(), "en_US"));
  }

  /** Test case: tests monthYear(Date, String). Test data: a fixed date and the en_US slug. Expected: the abbreviated month and year "Mar 2015". */
  @Test
  public void testMonthYear() {
    Assertions.assertEquals("Mar 2015", dateFormatter.monthYear(fixedDate(), "en_US"));
  }

  /** Test case: tests parseDateLong(String, String). Test data: a yyyy-MM-dd HH:mm:ss timestamp and the en_US slug. Expected: the long date "March 10, 2015". */
  @Test
  public void testParseDateLong() {
    Assertions.assertEquals(
        "March 10, 2015", dateFormatter.parseDateLong("2015-03-10 13:45:00", "en_US"));
  }

  /** Test case: tests parseDateLong(String, String). Test data: an unparseable string. Expected: an empty string. */
  @Test
  public void testParseDateLongWithUnparseableInput() {
    Assertions.assertEquals("", dateFormatter.parseDateLong("not-a-date", "en_US"));
  }

  /** Test case: tests parseDateLong(String, String). Test data: an empty string. Expected: an empty string. */
  @Test
  public void testParseDateLongWithEmptyInput() {
    Assertions.assertEquals("", dateFormatter.parseDateLong("", "en_US"));
  }

  /** Test case: tests every formatting method with a null date. Test data: a null date. Expected: an empty string from each method. */
  @Test
  public void testNullDateReturnsEmptyString() {
    Assertions.assertEquals("", dateFormatter.dateTimeLongMedium(null, "en_US"));
    Assertions.assertEquals("", dateFormatter.dateTimeMediumShort(null, "en_US"));
    Assertions.assertEquals("", dateFormatter.dateMedium(null, "en_US"));
    Assertions.assertEquals("", dateFormatter.monthYear(null, "en_US"));
    Assertions.assertEquals("", dateFormatter.dateShort(null, "en_US"));
    Assertions.assertEquals("", dateFormatter.dateLong(null, "en_US"));
  }

  /** Builds a timezone-stable date of 2015-03-10 13:45:00 in the default time zone. */
  private Date fixedDate() {
    Calendar calendar = new GregorianCalendar(2015, Calendar.MARCH, 10, 13, 45, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    return calendar.getTime();
  }

  /** The DateFormatter object under test. */
  private final DateFormatter dateFormatter = new DateFormatter();
}
