package com.trunkshell.voj.controller;

import java.io.Serializable;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import com.trunkshell.voj.util.CsrfProtector;

/**
 * 通过WebSocket与服务器实时通信.
 * 该Controller用于管理WebSocket连接.
 * 
 * @author Xie Haozhe
 */
@Controller
public class WebSocketController {
	/**
	 * 验证WebSocket会话的合法性.
	 * TODO SimpMessageHeaderAccessor 无法获取Session, 待修复.
	 * @param headerAccessor - SimpMessageHeaderAccessor对象, 用于获取Session
	 * @param message - WebSocket中传输的消息对象
	 * @return 包含会话验证结果的Message对象
	 */
	@MessageMapping("/authorization.action")
	@SendToUser("/message/authorization")
	public Message authorizationAction(
			SimpMessageHeaderAccessor headerAccessor, Message message) {
		String csrfToken = message.getValue();
		Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
		Boolean isCsrfTokenValid = CsrfProtector.isCsrfTokenValid(csrfToken, sessionAttributes);
		
		return new Message("isCsrfTokenValid", isCsrfTokenValid.toString());
	}
	
	/**
	 * 推送获取评测结果.
	 * @param submissionId - 评测记录的唯一标识符
	 * @return 包含实时评测结果信息的Message对象
	 */
	@SubscribeMapping("/getRealTimeJudgeResult.action/{submissionId}")
	public Message getRealTimeJudgeResultAction(
			@DestinationVariable long submissionId) {
		return new Message("Key", "Value # " + submissionId);
	}
	
	/* 消息结构体内部类声明 */
	private static class Message implements Serializable {
		/**
		 * Message类的默认构造函数.
		 */
		@SuppressWarnings("unused")
		public Message() { }
		
		/**
		 * Message类的构造函数.
		 * @param key - 消息名称
		 * @param value - 消息内容
		 */
		public Message(String key, String value) {
			this.key = key;
			this.value = value;
		}
		
		/**
		 * 获取消息名称.
		 * @return 消息名称
		 */
		@SuppressWarnings("unused")
		public String getKey() {
			return key;
		}
		
		/**
		 * 获取消息内容
		 * @return 消息内容
		 */
		public String getValue() {
			return value;
		}
		
		/**
		 * 消息名称.
		 */
		private String key;
		
		/**
		 * 消息内容.
		 */
		private String value;
		
		/**
		 * 序列化标识符.
		 */
		private static final long serialVersionUID = -3430525797548136557L;
	}
	
	/**
	 * 日志记录器.
	 */
	@SuppressWarnings("unused")
	private static final Logger logger = LogManager.getLogger(WebSocketController.class);
}
