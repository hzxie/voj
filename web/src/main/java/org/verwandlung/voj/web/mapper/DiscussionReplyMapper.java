package org.verwandlung.voj.web.mapper;

import org.apache.ibatis.annotations.Param;
import org.verwandlung.voj.web.model.DiscussionReply;

import java.util.List;

/**
 * DiscussionReply Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface DiscussionReplyMapper {
	/**
	 * 根据讨论回复的唯一标识符获取DiscussionReply对象.
	 * @param discussionReplyId - 讨论回复的唯一标识符
	 * @return 预期的DiscussionReply对象或空引用
	 */
	DiscussionReply getDiscussionReplyUsingReplyId(@Param("discussionReplyId") long discussionReplyId);

	/**
	 * 获取某个讨论帖子下的全部回复, 并分页显示.
	 * @param discussionThreadId - 讨论帖子的唯一标识符
	 * @param offset 起始回复的游标
	 * @param limit 获取回复的数量
	 * @return 包含讨论话题回复的List对象
	 */
	List<DiscussionReply> getDiscussionRepliesUsingThreadId(
		@Param("discussionThreadId") long discussionThreadId,
		@Param("offset") long offset, @Param("limit") int limit);

	/**
	 * 创建讨论回复.
	 * @param discussionReply - 待创建的DiscussionReply对象
	 */
	int createDiscussionReply(DiscussionReply discussionReply);

	/**
	 * 更新讨论回复.
	 * @param discussionReply - 待更新的DiscussionReply对象
	 */
	int updateDiscussionReply(DiscussionReply discussionReply);

	/**
	 * 删除讨论回复.
	 * @param discussionReplyId - 待删除回复的唯一标识符
	 */
	int deleteDiscussionReplyUsingReplyId(long discussionReplyId);
}
