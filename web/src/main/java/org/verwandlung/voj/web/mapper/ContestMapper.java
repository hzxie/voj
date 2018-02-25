package org.verwandlung.voj.web.mapper;

import org.apache.ibatis.annotations.Param;
import org.verwandlung.voj.web.model.Contest;

import java.util.List;

/**
 * Contest Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface ContestMapper {
	/**
	 * [此方法仅供管理员使用]
	 * 获取竞赛的总数量.
	 * @param keyword - 竞赛的关键词
	 * @return 竞赛的总数量
	 */
	long getNumberOfContests(@Param("keyword") String keyword);

	/**
	 * 获取竞赛列表.
	 * @param keyword - 竞赛的关键词
	 * @param offset - 起始竞赛的偏移量(offset)
	 * @param limit - 需要获取竞赛的数量
	 * @return 预期的竞赛对象
	 */
	List<Contest> getContests(@Param("keyword") String keyword, 
		@Param("offset") long offset, @Param("limit") int limit);

	/**
	 * 根据竞赛的唯一标识符获取竞赛.
	 * @param contestId - 竞赛的唯一标识符
	 * @return 预期的竞赛对象
	 */
	Contest getContest(long contestId);

	/**
	 * 创建竞赛.
	 * @param contest - 待创建的竞赛对象
	 */
	int createContest(Contest contest);

	/**
	 * 更新竞赛.
	 * @param contest - 待更新的竞赛对象
	 */
	int updateContest(Contest contest);

	/**
	 * 删除竞赛.
	 * @param contestId - 竞赛的唯一标识符
	 */
	int deleteContest(long contestId);
}
