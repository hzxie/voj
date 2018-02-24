package org.verwandlung.voj.web.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.verwandlung.voj.web.mapper.BulletinBoardMessageMapper;
import org.verwandlung.voj.web.model.BulletinBoardMessage;

import java.util.List;

/**
 * 布告栏(BulletinBoard)的业务逻辑层.
 * @author Haozhe Xie
 */
@Service
@Transactional
public class BulletinBoardService {
	/**
	 * 获取布告栏消息的数量.
	 * @return 布告栏消息的数量
	 */
	public long getNumberOfBulletinBoardMessages() {
		return bulletinBoardMessageMapper.getNumberOfBulletinBoardMessages();
	}

	/**
	 * 获取布告栏消息.
	 * @param offset - 布告栏消息唯一标识符的起始编号
	 * @param limit - 获取布告栏消息的数量
	 * @return 包含布告栏消息的列表
	 */
	public List<BulletinBoardMessage> getBulletinBoardMessages(long offset, int limit) {
		return bulletinBoardMessageMapper.getBulletinBoardMessages(offset, limit);
	}

	/**
	 * 使用布告栏消息的唯一标识符获取布告栏消息.
	 * @param bulletinBoardMessageId - 布告栏消息的唯一标识符
	 * @return 特定的布告栏消息
	 */
	public BulletinBoardMessage getBulletinBoardMessage(long bulletinBoardMessageId) {
		return bulletinBoardMessageMapper.getBulletinBoardMessageUsingId(bulletinBoardMessageId);
	}

	/**
	 * 自动注入的BulletinBoardMessageMapper.
	 * 用于获取布告栏消息.
	 */
	@Autowired
	private BulletinBoardMessageMapper bulletinBoardMessageMapper;
}
