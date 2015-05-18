package com.trunkshell.voj.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * 通过WebSocket与服务器实时通信.
 * 该Controller用于管理WebSocket连接.
 * 
 * @author Xie Haozhe
 */
@Controller
@MessageMapping("/websocket")
public class WebSocketController {
	/**
	 * 实时返回评测结果.
	 * @return
	 */
	@MessageMapping("/getRealTimeJudgeResult.action/{submissionId}")
	@SendTo("/topic/judgeResult")
	public String getRealTimeJudgeResultAction(
			@DestinationVariable long submissionId) {
		return "Submission Id #" + submissionId;
	}
	
	/**
	 * 自动注入的SimpMessagingTemplate对象.
	 * 用于与Client通信.
	 */
	@Autowired
	private SimpMessagingTemplate simpMessagingtemplate;
}
