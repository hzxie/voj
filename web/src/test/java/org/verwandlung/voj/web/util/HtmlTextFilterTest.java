package org.verwandlung.voj.web.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * TextFilter的测试类.
 * @author Haozhe Xie
 */
public class HtmlTextFilterTest {
	/**
	 * 测试用例: 测试filterHtml()方法.
	 * 测试数据: 匹配的HTML的字符串.
	 * 与其结果: 返回过滤HTML的字符串
	 */
	@Test
	public void testFilterHtmlUsingMatchedHtml() {
		String text = "XSS <script type=\"text/javascript\">alert('XSS')</script>.";
		Assertions.assertEquals("XSS .", HtmlTextFilter.filter(text));
	}
	
	/**
	 * 测试用例: 测试filterHtml()方法.
	 * 测试数据: 不匹配的HTML的字符串.
	 * 与其结果: 返回过滤HTML的字符串
	 */
	@Test
	public void testFilterHtmlUsingUnmatchedHtml() {
		String text = "XSS <a href=\"http://zjhzxhz.com\">alert('XSS')</script>.";
		Assertions.assertEquals("XSS alert('XSS').", HtmlTextFilter.filter(text));
	}
}
