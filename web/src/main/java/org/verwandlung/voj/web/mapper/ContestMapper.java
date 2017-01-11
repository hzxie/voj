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
	 * 获取考试的总数量.
	 * @return 考试的总数量
	 */
	long getNumberOfContests();

	/**
	 * 获取考试列表.
	 * @param offset - 起始考试的偏移量(offset)
	 * @param limit - 需要获取考试的数量
	 * @return 预期的考试对象
	 */
	List<Contest> getContests(@Param("offset") long offset, @Param("limit") int limit);

	/**
	 * 根据考试的唯一标识符获取考试.
	 * @param contestId - 考试的唯一标识符
	 * @return 预期的考试对象
	 */
	Contest getContestsUsingId(long contestId);

	/**
	 * 创建考试.
	 * @param contest - 待创建的考试对象
	 */
	int createContest(Contest contest);

	/**
	 * 更新考试.
	 * @param contest - 待更新的考试对象
	 */
	int updateContest(Contest contest);

	/**
	 * 删除考试.
	 * @param contestId - 考试的唯一标识符
	 */
	int deleteContest(long contestId);
}
