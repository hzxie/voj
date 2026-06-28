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
 * The test class for DateUtils.
 *
 * @author Haozhe Xie
 */
public class DateUtilsTest {
  /** Test case: tests the getPreviousDate(int) method. Test data: a period of 7 days. Expected: a date that falls on the same day as 7 days before now. */
  @Test
  public void testGetPreviousDateForOneWeek() {
    Calendar expected = new GregorianCalendar();
    expected.add(Calendar.DATE, -7);

    assertSameDay(expected.getTime(), DateUtils.getPreviousDate(7));
  }

  /** Test case: tests the getPreviousDate(int) method. Test data: a period of 30 days. Expected: a date that falls on the same day as one month before now. */
  @Test
  public void testGetPreviousDateForOneMonth() {
    Calendar expected = new GregorianCalendar();
    expected.add(Calendar.MONTH, -1);

    assertSameDay(expected.getTime(), DateUtils.getPreviousDate(30));
  }

  /** Test case: tests the getPreviousDate(int) method. Test data: an unrecognized period. Expected: a date that falls on the same day as one year before now. */
  @Test
  public void testGetPreviousDateForOneYear() {
    Calendar expected = new GregorianCalendar();
    expected.add(Calendar.YEAR, -1);

    assertSameDay(expected.getTime(), DateUtils.getPreviousDate(365));
  }

  /** Test case: tests the getDateBefore(int) method. Test data: 10 days. Expected: a date that falls on the same day as 10 days before now. */
  @Test
  public void testGetDateBefore() {
    Calendar expected = new GregorianCalendar();
    expected.add(Calendar.DATE, -10);

    assertSameDay(expected.getTime(), DateUtils.getDateBefore(10));
  }

  /** Test case: tests the getDateBefore(int) method. Test data: 0 days. Expected: a date that falls on today. */
  @Test
  public void testGetDateBeforeZeroDays() {
    assertSameDay(new Date(), DateUtils.getDateBefore(0));
  }

  /** Asserts that two Date objects fall on the same calendar day. */
  private void assertSameDay(Date expected, Date actual) {
    Calendar a = new GregorianCalendar();
    a.setTime(expected);
    Calendar b = new GregorianCalendar();
    b.setTime(actual);

    Assertions.assertEquals(a.get(Calendar.YEAR), b.get(Calendar.YEAR));
    Assertions.assertEquals(a.get(Calendar.DAY_OF_YEAR), b.get(Calendar.DAY_OF_YEAR));
  }
}
