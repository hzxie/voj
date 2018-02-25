package org.verwandlung.voj.web.mapper;

import org.apache.ibatis.annotations.Param;
import org.verwandlung.voj.web.model.ContestSubmission;

import java.util.List;

/**
 * ContestSubmission Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface ContestSubmissionMapper {
	/**
	 * 获取某场竞赛的提交记录.
	 * @param contestId - 竞赛的唯一标识符
	 * @return 包含竞赛提交记录(ContestSubmission)的List对象
	 */
	List<ContestSubmission> getSubmissionsOfContest(@Param("contestId") long contestId);

	/**
	 * 获取某场竞赛AC的提交记录.
	 * @param contestId - 竞赛的唯一标识符
	 * @return 包含AC竞赛提交记录(ContestSubmission)的List对象
	 */
	List<ContestSubmission> getAcceptedSubmissionsOfContest(@Param("contestId") long contestId);

	/**
	 * 获取某场竞赛某个参赛者的提交记录.
	 * @param contestId - 竞赛的唯一标识符
	 * @param  contestantUid - 参赛者用户的唯一标识符
	 * @return 包含某竞赛某参赛者提交记录(ContestSubmission)的List对象
	 */
	List<ContestSubmission> getSubmissionOfContestOfContest(
		@Param("contestId") long contestId, @Param("contestantUid") long contestantUid);

	/**
	 * 创建提交记录.
	 * @param contestSubmission - 待创建的提交记录
	 */
	int createContestSubmission(ContestSubmission contestSubmission);

	/**
	 * 删除竞赛提交记录.
	 * @param contestId - 竞赛的唯一标识符
	 * @param submissionId - 提交记录的唯一标识符
	 */
	int deleteContestSubssion(@Param("contestId") long contestId, @Param("submissionId") long submissionId);
}
