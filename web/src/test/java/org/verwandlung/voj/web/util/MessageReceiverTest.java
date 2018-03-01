package org.verwandlung.voj.web.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.verwandlung.voj.web.messenger.MessageReceiver;

/**
 * MessageReceiver的测试类.
 * 注意: 仅供开发调试使用.
 * @author Haozhe Xie
 */
/*
@ExtendWith(SpringExtension.class)
@ContextConfiguration({"classpath:test-spring-context.xml"})
*/
public class MessageReceiverTest {
	/* @Test */
	public void testOnMessage() throws InterruptedException {
		Thread.sleep(5000);
	}
	
	/**
	 * 自动注入的MessageReceiver对象.
	 */
	@Autowired
	private MessageReceiver messageReceiver;
}
