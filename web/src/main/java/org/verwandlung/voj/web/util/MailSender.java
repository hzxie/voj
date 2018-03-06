/* Verwandlung Online Judge - A cross-platform judge online system
 * Copyright (C) 2018 Haozhe Xie <cshzxie@gmail.com>
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
 *
 *
 *                              _ooOoo_  
 *                             o8888888o  
 *                             88" . "88  
 *                             (| -_- |)  
 *                             O\  =  /O  
 *                          ____/`---'\____  
 *                        .'  \\|     |//  `.  
 *                       /  \\|||  :  |||//  \  
 *                      /  _||||| -:- |||||-  \  
 *                      |   | \\\  -  /// |   |  
 *                      | \_|  ''\---/''  |   |  
 *                      \  .-\__  `-`  ___/-. /  
 *                    ___`. .'  /--.--\  `. . __  
 *                 ."" '<  `.___\_<|>_/___.'  >'"".  
 *                | | :  `- \`.;`\ _ /`;.`/ - ` : | |  
 *                \  \ `-.   \_ __\ /__ _/   .-` /  /  
 *           ======`-.____`-.___\_____/___.-`____.-'======  
 *                              `=---=' 
 *
 *                          HERE BE BUDDHA
 *
 */
package org.verwandlung.voj.web.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

/**
 * 电子邮件发送服务.
 * 
 * @author Haozhe Xie
 */
@Component
public class MailSender {
	/**
	 * MailSender的构造函数.
	 * @param freeMarkerConfigurer - FreeMarker的配置器
	 * @param mailSender - JavaMailSender对象
	 */
	@Autowired
	private MailSender(FreeMarkerConfigurer freeMarkerConfigurer, JavaMailSender mailSender) {
		this.freeMarkerConfigurer = freeMarkerConfigurer;
		this.mailSender = mailSender;
	}
	
	/**
	 * 解析电子邮件模板内容.
	 * @param templateLocation - 电子邮件模板相对路径
	 * @param model - 电子邮件的附加信息
	 * @return 解析后的电子邮件内容
	 * @throws TemplateException 
	 * @throws IOException 
	 * @throws ParseException 
	 * @throws MalformedTemplateNameException 
	 * @throws TemplateNotFoundException 
	 */
	public String getMailContent(String templateLocation, Map<String, Object> model)
			throws TemplateNotFoundException, MalformedTemplateNameException, 
				ParseException, IOException, TemplateException {
		model.put("baseUrl", baseUrl);

		return FreeMarkerTemplateUtils.processTemplateIntoString(
				freeMarkerConfigurer.getConfiguration().getTemplate(templateLocation), model);
	}
	
	/**
	 * 发送电子邮件到指定收件人.
	 * @param recipient - 收件人的电子邮件地址
	 * @param subject - 邮件的主题
	 * @param body - 邮件的内容
	 */
	public void sendMail(final String recipient, final String subject, final String body) { 
		final MimeMessagePreparator preparator = new MimeMessagePreparator() {
			public void prepare(MimeMessage mimeMessage) 
					throws MessagingException, UnsupportedEncodingException {
				MimeMessageHelper message = new MimeMessageHelper(mimeMessage);
				message.setTo(recipient);
				message.setFrom(senderMail, senderName);
				message.setSubject(subject);
				message.setText(body, true);
			}
		};
		new Thread(new Runnable() {
			public void run() {
				mailSender.send(preparator);
				LOGGER.info(String.format("An Email{Recipient: %s, Subject: %s} has been sent.", 
						new Object[] {recipient, subject}));
			}
		}).start();
	}

	/**
	 * 自动注入的Configuration对象.
	 */
	private final FreeMarkerConfigurer freeMarkerConfigurer;
	
	/**
	 * 自动注入的JavaMailSender对象.
	 * 用于发送电子邮件.
	 */
	private final JavaMailSender mailSender;
	
	/**
	 * 电子邮件发送者的地址. 
	 */
	@Value("${mail.senderMail}")
	private String senderMail;
	
	/**
	 * 电子邮件发送者的姓名. 
	 */
	@Value("${mail.senderName}")
	private String senderName;
	
	/**
	 * 网站的URL.
	 * 用于生成电子邮件中的链接.
	 */
	@Value("${url.base}")
	private String baseUrl;
	
	/**
	 * 日志记录器.
	 */
	private static final Logger LOGGER = LogManager.getLogger(MailSender.class);
}
