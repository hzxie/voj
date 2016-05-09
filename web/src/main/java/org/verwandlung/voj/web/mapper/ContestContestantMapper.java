package org.verwandlung.voj.web.mapper;

import org.apache.ibatis.annotations.Param;
import org.verwandlung.voj.web.model.ContestContestant;

import java.util.List;

/**
 * ContestContestant Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface ContestContestantMapper {
	/**
	 * 获取某个考试的参赛人数.
	 * @param contestId - 考试的唯一标识符
	 * @return 某个考试的参赛人数
	 */
	public long getNumberOfContestantsOfContest(long contestId);

	/**
	 * 获取某个OI赛制的考试的参赛者列表(按得分由高到底排列).
	 * @param contestId - 考试的唯一标识符
	 * @param offset - 起始参赛者的偏移量(offset)
	 * @param limit - 需要获取参赛者的数量
	 * @return 某个考试的参赛者列表
	 */
	public List<ContestContestant> getContestantsOfContestForOi(
			@Param("contestId") long contestId,
			@Param("offset") long offset, @Param("limit") int limit);

	/**
	 * 获取某个选手在某个考试的参赛记录.
	 * @param contestId - 考试的唯一标识符
	 * @param contestantUid - 参赛者的用户唯一标识符
	 * @return 对应的某个选手在某个考试的参赛记录
	 */
	public ContestContestant getContestantOfContest(
			@Param("contestId") long contestId,
			@Param("contestantUid") long contestantUid);

	/**
	 * 创建参赛记录(用于参加比赛).
	 * @param contestContestant - 待创建参赛记录
	 */
	public void createContestContestant(ContestContestant contestContestant);

	/**
	 * 更新参赛记录(用于参加比赛).
	 * @param contestContestant - 待更新参赛记录
	 */
	public void updateContestContestant(ContestContestant contestContestant);

	/**
	 * 删除参赛记录.
	 * @param contestId - 考试的唯一标识符
	 * @param contestantUid - 参赛者的用户唯一标识符
	 */
	public void deleteContestContestant(
			@Param("contestId") long contestId,
			@Param("contestantUid") long contestantUid);
}
