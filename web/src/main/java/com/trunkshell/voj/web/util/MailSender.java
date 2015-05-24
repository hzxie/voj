package com.trunkshell.voj.web.util;

import java.util.Map;

import javax.mail.internet.MimeMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Component;
import org.springframework.ui.velocity.VelocityEngineUtils;

/**
 * 邮件发送服务.
 * @author Xie Haozhe
 */
@Component
public class MailSender {
	/**
	 * MailSender的构造函数.
	 * @param velocityEngine - Velocity模板解析引擎对象
	 * @param mailSender - JavaMailSender对象
	 */
	@Autowired
	public MailSender(VelocityEngine velocityEngine, JavaMailSender mailSender) {
		this.velocityEngine = velocityEngine;
		this.mailSender = mailSender;
	}
	
	/**
	 * 解析电子邮件模板内容.
	 * @param templateLocation - 电子邮件模板相对路径
	 * @param model - 电子邮件的附加信息
	 * @return 解析后的电子邮件内容
	 */
	public String getMailContent(String templateLocation, Map<String, Object> model) {
		return VelocityEngineUtils.
				mergeTemplateIntoString(velocityEngine, templateLocation, defaultEncoding, model);
	}
	
	/**
	 * 发送电子邮件到指定收件人.
	 * @param recipient - 收件人的电子邮件地址
	 * @param subject - 邮件的主题
	 * @param body - 邮件的内容
	 */
	public void sendMail(final String recipient, final String subject, final String body) { 
		final MimeMessagePreparator preparator = new MimeMessagePreparator() {
			public void prepare(MimeMessage mimeMessage) throws Exception {
				MimeMessageHelper message = new MimeMessageHelper(mimeMessage);
				message.setTo(recipient);
				message.setFrom("noreply@testzilla.org");
				message.setSubject(subject);
				message.setText(body, true);
			}
		};
		new Thread(new Runnable() {
			public void run() {
				mailSender.send(preparator);
				logger.info(String.format("An Email{Recipient: %s, Subject: %s} has been sent.", 
						new Object[] {recipient, subject}));
			}
		}).start();
	}

	/**
	 * 自动注入的VelocityEngine对象.
	 * 用于解析Email模板.
	 */
	private final VelocityEngine velocityEngine;
	
	/**
	 * 自动注入的JavaMailSender对象.
	 * 用于发送电子邮件.
	 */
	private final JavaMailSender mailSender;
	
	/**
	 * 电子邮件默认编码.
	 */
	private final String defaultEncoding = "UTF-8"; 
	
	/**
	 * 日志记录器.
	 */
	private static final Logger logger = LogManager.getLogger(MailSender.class);
}
