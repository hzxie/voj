package com.trunkshell.voj.mapper;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.One;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import com.trunkshell.voj.model.JudgeResult;
import com.trunkshell.voj.model.Language;
import com.trunkshell.voj.model.Problem;
import com.trunkshell.voj.model.Submission;
import com.trunkshell.voj.model.User;

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
		 @Result(property = "problem", column = "problem_id", javaType=Problem.class, one = @One(select="com.trunkshell.voj.mapper.ProblemMapper.getProblem")),
		 @Result(property = "user", column = "uid", javaType=User.class, one = @One(select="com.trunkshell.voj.mapper.UserMapper.getUserUsingUid")),
		 @Result(property = "language", column = "language_id", javaType=Language.class, one = @One(select="com.trunkshell.voj.mapper.LanguageMapper.getLanguageUsingId")),
		 @Result(property = "submitTime", column = "submission_submit_time"),
		 @Result(property = "executeTime", column = "submission_execute_time"),
		 @Result(property = "usedTime", column = "submission_used_time"),
		 @Result(property = "usedMemory", column = "submission_used_memory"),
		 @Result(property = "judgeResult", column = "submission_judge_result", javaType=JudgeResult.class, one = @One(select="com.trunkshell.voj.mapper.JudgeResultMapper.getJudgeResultUsingSlug")),
		 @Result(property = "judgeScore", column = "submission_judge_score"),
		 @Result(property = "judgeLog", column = "submission_judge_log"),
		 @Result(property = "code", column = "submission_code"),
	})
	public Submission getSubmission(@Param("submissionId") long submissionId);
}
