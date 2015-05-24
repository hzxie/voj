package com.trunkshell.voj.judger.mapper;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.One;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.trunkshell.voj.judger.model.Language;
import com.trunkshell.voj.judger.model.Submission;

/**
 * Submission Data Access Object.
 * @author Xie Haozhe
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface SubmissionMapper {
	/**
     * 通过评测记录唯一标识符获取试题对象.
     * @param submissionID - 评测记录的唯一标识符
     * @return 一个评测记录对象
     */
	@Select("SELECT * FROM voj_submissions WHERE submission_id = #{submissionId}")
	@Options(useCache = true)
	@Results({
		@Result(property = "submissionId", column = "submission_id"),
		@Result(property = "problemId", column = "problem_id"),
		@Result(property = "uid", column = "uid"),
		@Result(property = "language", column = "language_id", javaType=Language.class, one = @One(select="com.trunkshell.voj.judger.mapper.LanguageMapper.getLanguageUsingId")),
		@Result(property = "submitTime", column = "submission_submit_time"),
		@Result(property = "executeTime", column = "submission_execute_time"),
		@Result(property = "usedTime", column = "submission_used_time"),
		@Result(property = "usedMemory", column = "submission_used_memory"),
		@Result(property = "judgeResultSlug", column = "submission_judge_result"),
		@Result(property = "judgeScore", column = "submission_judge_score"),
		@Result(property = "judgeLog", column = "submission_judge_log"),
		@Result(property = "code", column = "submission_code"),
	})
	public Submission getSubmission(@Param("submissionId") long submissionId);
	
	/**
	 * 更新提交记录.
	 * @param submission - 待更新的提交记录对象
	 */
	@Update("UPDATE voj_submissions SET problem_id = #{problem.problemId}, uid = #{user.uid}, language_id = #{language.languageId}, submission_submit_time = #{submitTime}, submission_execute_time = #{executeTime}, submission_used_time = #{usedTime}, submission_used_memory = #{usedMemory}, submission_judge_result = #{judgeResult.judgeResultSlug}, submission_judge_score = #{judgeScore}, submission_judge_log = #{judgeLog}, submission_code = #{code} WHERE submission_id = #{submissionId}")
	@Options(flushCache = true)
	public void updateSubmission(Submission submission);
}
