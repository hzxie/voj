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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * TextFilter的测试类.
 *
 * @author Haozhe Xie
 */
public class HtmlTextFilterTest {
  /** 测试用例: 测试filterHtml()方法. 测试数据: 匹配的HTML的字符串. 与其结果: 返回过滤HTML的字符串 */
  @Test
  public void testFilterHtmlUsingMatchedHtml() {
    String text = "XSS <script type=\"text/javascript\">alert('XSS')</script>.";
    Assertions.assertEquals("XSS .", HtmlTextFilter.filter(text));
  }

  /** 测试用例: 测试filterHtml()方法. 测试数据: 不匹配的HTML的字符串. 与其结果: 返回过滤HTML的字符串 */
  @Test
  public void testFilterHtmlUsingUnmatchedHtml() {
    String text = "XSS <a href=\"http://zjhzxhz.com\">alert('XSS')</script>.";
    Assertions.assertEquals("XSS alert('XSS').", HtmlTextFilter.filter(text));
  }
}
