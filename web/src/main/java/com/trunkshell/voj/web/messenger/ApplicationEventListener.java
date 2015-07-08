package com.trunkshell.voj.web.messenger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 应用程序事件监听器.
 * 负责将消息队列中的消息转发至控制器.
 * 
 * @author Xie Haozhe
 */
@Component
public class ApplicationEventListener {
    /**
     * 提交事件的处理器.
     * @param event - 提交记录事件
     * @throws IOException 
     */
    @EventListener
    public void submissionEventHandler(SubmissionEvent event) throws IOException {
        long submissionId = event.getSubmissionId();
        String judgeResult = event.getJudgeResult();
        String message = event.getMessage();
        boolean isCompleted = event.isCompleted();
        SseEmitter sseEmitter = sseEmitters.get(submissionId);
        
        if ( sseEmitter == null ) {
            LOGGER.warn(String.format("CANNOT get the SseEmitter for submission #%d.", submissionId));
            return;
        }
        Map<String, String> mapMessage = new HashMap<String, String>(3, 1);
        mapMessage.put("judgeResult", judgeResult);
        mapMessage.put("message", message);
        sseEmitter.send(mapMessage);
        
        if ( isCompleted ) {
            sseEmitter.complete();
            removeSseEmitters(submissionId);
        }
    }
    
    /**
     * 注册Server Sent Event的发送者对象.
     * @param submissionId - 提交记录的唯一标识符
     * @param sseEmitter - Server Sent Event的发送者对象
     */
    public void addSseEmitters(long submissionId, SseEmitter sseEmitter) {
        sseEmitters.put(submissionId, sseEmitter);
    }
    
    /**
     * 移除Server Sent Event的发送者对象.
     * @param submissionId - 提交记录的唯一标识符
     */
    private void removeSseEmitters(long submissionId) {
        sseEmitters.remove(submissionId);
        
        for ( Entry<Long, SseEmitter> mapEntry : sseEmitters.entrySet() ) {
            long currentSubmissionId = mapEntry.getKey();
            if ( currentSubmissionId < submissionId ) {
                sseEmitters.remove(currentSubmissionId);
            }
        }
    }
    
    /**
     * SseEmitter对象的列表.
     * Map中的Key表示提交记录的唯一标识符.
     * Map中的Value表示对应的SseEmitter对象, 用于推送实时评测信息.
     */
    private static Map<Long, SseEmitter> sseEmitters = new Hashtable<Long, SseEmitter>();
    
    /**
     * 日志记录器.
     */
    private static final Logger LOGGER = LogManager.getLogger(ApplicationEventListener.class);
}
