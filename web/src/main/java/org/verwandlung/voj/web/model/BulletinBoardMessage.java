package org.verwandlung.voj.web.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 布告栏消息的Model.
 * 对应数据库中的voj_bulletin_board_messages数据表.
 * 
 * @author Haozhe Xie
 */
public class BulletinBoardMessage implements Serializable {
	/**
	 * BulletinBoardMessage的默认构造函数.
	 */
	public BulletinBoardMessage() { }

	/**
	 * BulletinBoardMessage的构造函数.
	 * @param messageTitle - 布告栏消息的标题
	 * @param messageBody - 布告栏消息的内容
	 * @param messageCreateTime - 布告栏消息的创建时间
	 */
	public BulletinBoardMessage(String messageTitle, String messageBody, Date messageCreateTime) {
		this.messageTitle = messageTitle;
		this.messageBody = messageBody;
		this.messageCreateTime = messageCreateTime;
	}

	/**
	 * BulletinBoardMessage的构造函数.
	 * @param messageId - 布告栏消息的唯一标识符
	 * @param messageTitle - 布告栏消息的标题
	 * @param messageBody - 布告栏消息的内容
	 * @param messageCreateTime - 布告栏消息的创建时间
	 */
	public BulletinBoardMessage(long messageId, String messageTitle, String messageBody, Date messageCreateTime) {
		this(messageTitle, messageBody, messageCreateTime);
		this.messageId = messageId;
	}

	/**
	 * 获取布告栏消息的唯一标识符.
	 * @return 布告栏消息的唯一标识符
	 */
	public long getMessageId() {
		return messageId;
	}

	/**
	 * 设置布告栏消息的唯一标识符.
	 * @param messageId - 布告栏消息的唯一标识符
	 */
	public void setMessageId(long messageId) {
		this.messageId = messageId;
	}

	/**
	 * 获取布告栏消息的标题.
	 * @return 布告栏消息的标题
	 */
	public String getMessageTitle() {
		return messageTitle;
	}

	/**
	 * 设置布告栏消息的标题.
	 * @param messageTitle - 布告栏消息的标题
	 */
	public void setMessageTitle(String messageTitle) {
		this.messageTitle = messageTitle;
	}

	/**
	 * 获取布告栏消息的内容.
	 * @return 布告栏消息的内容
	 */
	public String getMessageBody() {
		return messageBody;
	}

	/**
	 * 设置布告栏消息的内容.
	 * @param messageBody - 布告栏消息的内容
	 */
	public void setMessageBody(String messageBody) {
		this.messageBody = messageBody;
	}

	/**
	 * 获取布告栏消息的创建时间.
	 * @return 布告栏消息的创建时间
	 */
	public Date getMessageCreateTime() {
		return messageCreateTime;
	}

	/**
	 * 设置布告栏消息的创建时间.
	 * @param messageCreateTime - 布告栏消息的创建时间
	 */
	public void setMessageCreateTime(Date messageCreateTime) {
		this.messageCreateTime = messageCreateTime;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("BulletinBoardMessage [ID=%d, Title=%s, Body=%s, CreateTime=%s]",
				new Object[] {messageId, messageTitle, messageBody, messageCreateTime});
	}

	/**
	 * 布告栏消息的唯一标识符.
	 */
	private long messageId;

	/**
	 * 布告栏消息的标题.
	 */
	private String messageTitle;

	/**
	 * 布告栏消息的内容.
	 */
	private String messageBody;

	/**
	 * 布告栏消息的创建时间.
	 */
	private Date messageCreateTime;
}
