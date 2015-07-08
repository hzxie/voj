package com.trunkshell.voj.web.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.trunkshell.voj.web.messenger.MessageSender;

/**
 * MessageSender的测试类.
 * 注意: 仅供开发调试使用.
 * @author Xie Haozhe
 */
/*
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:test-spring-context.xml"})
*/
public class MessageSenderTest {
    /**
     * 测试用例: 测试sendMessage()方法
     * 测试数据: N/a
     * 预期结果: ActiveMQ的消息队列中产生预期的消息
     */
    /* @Test */
    public void testSendMessage() {
        Map<String, Object> mapMessage = new HashMap<String, Object>();
        mapMessage.put("message", "Hello World");
        messageSender.sendMessage(mapMessage);
    }
    
    /**
     * 自动注入的MessageSender对象.
     */
    @Autowired
    private MessageSender messageSender;
}
