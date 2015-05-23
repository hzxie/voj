package com.trunkshell.voj.mapper;

import java.util.List;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.One;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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
	
	/**
     * 通过试题唯一标识符获取某个范围内的所有试题.
     * @param limit - 每次加载评测记录的数量
     * @return 某个范围内的所有试题
     */
	@Select("SELECT * FROM voj_submissions ORDER BY submission_id DESC LIMIT #{limit}")
	@Options(useCache = true)
	@Results({
		 @Result(property = "submissionId", column = "submission_id"),
		 @Result(property = "problem", column = "problem_id", javaType=Problem.class, one = @One(select="com.trunkshell.voj.mapper.ProblemMapper.getProblem")),
		 @Result(property = "user", column = "uid", javaType=User.class, one = @One(select="com.trunkshell.voj.mapper.UserMapper.getUserUsingUid")),
		 @Result(property = "language", column = "language_id", javaType=Language.class, one = @One(select="com.trunkshell.voj.mapper.LanguageMapper.getLanguageUsingId")),
		 @Result(property = "submitTime", column = "submission_submit_time"),
		 @Result(property = "usedTime", column = "submission_used_time"),
		 @Result(property = "usedMemory", column = "submission_used_memory"),
		 @Result(property = "judgeResult", column = "submission_judge_result", javaType=JudgeResult.class, one = @One(select="com.trunkshell.voj.mapper.JudgeResultMapper.getJudgeResultUsingSlug")),
		 @Result(property = "judgeScore", column = "submission_judge_score")
	})
	public List<Submission> getSubmissions(@Param("limit") int limit);
	
	/**
     * 通过试题唯一标识符获取某个范围内的所有试题.
     * @param offset - 试题唯一标识符的起始编号
     * @param limit - 每次加载评测记录的数量
     * @return 某个范围内的所有试题
     */
	@Select("SELECT * FROM voj_submissions WHERE submission_id <= #{submissionId} ORDER BY submission_id DESC LIMIT #{limit}")
	@Options(useCache = true)
	@Results({
		 @Result(property = "submissionId", column = "submission_id"),
		 @Result(property = "problem", column = "problem_id", javaType=Problem.class, one = @One(select="com.trunkshell.voj.mapper.ProblemMapper.getProblem")),
		 @Result(property = "user", column = "uid", javaType=User.class, one = @One(select="com.trunkshell.voj.mapper.UserMapper.getUserUsingUid")),
		 @Result(property = "language", column = "language_id", javaType=Language.class, one = @One(select="com.trunkshell.voj.mapper.LanguageMapper.getLanguageUsingId")),
		 @Result(property = "submitTime", column = "submission_submit_time"),
		 @Result(property = "usedTime", column = "submission_used_time"),
		 @Result(property = "usedMemory", column = "submission_used_memory"),
		 @Result(property = "judgeResult", column = "submission_judge_result", javaType=JudgeResult.class, one = @One(select="com.trunkshell.voj.mapper.JudgeResultMapper.getJudgeResultUsingSlug")),
		 @Result(property = "judgeScore", column = "submission_judge_score")
	})
	public List<Submission> getSubmissionsUsingOffset(@Param("submissionId") long offset, @Param("limit") int limit);
	
	/**
     * 通过试题唯一标识符获取某个范围内的所有试题.
     * @param offset - 试题唯一标识符的起始编号
     * @param limit - 每次加载评测记录的数量
     * @return 某个范围内的所有试题
     */
	@Select("SELECT * FROM voj_submissions WHERE submission_id >= #{submissionId} ORDER BY submission_id LIMIT #{limit}")
	@Options(useCache = true)
	@Results({
		 @Result(property = "submissionId", column = "submission_id"),
		 @Result(property = "problem", column = "problem_id", javaType=Problem.class, one = @One(select="com.trunkshell.voj.mapper.ProblemMapper.getProblem")),
		 @Result(property = "user", column = "uid", javaType=User.class, one = @One(select="com.trunkshell.voj.mapper.UserMapper.getUserUsingUid")),
		 @Result(property = "language", column = "language_id", javaType=Language.class, one = @One(select="com.trunkshell.voj.mapper.LanguageMapper.getLanguageUsingId")),
		 @Result(property = "submitTime", column = "submission_submit_time"),
		 @Result(property = "usedTime", column = "submission_used_time"),
		 @Result(property = "usedMemory", column = "submission_used_memory"),
		 @Result(property = "judgeResult", column = "submission_judge_result", javaType=JudgeResult.class, one = @One(select="com.trunkshell.voj.mapper.JudgeResultMapper.getJudgeResultUsingSlug")),
		 @Result(property = "judgeScore", column = "submission_judge_score")
	})
	public List<Submission> getLatestSubmissionsUsingOffset(@Param("submissionId") long offset, @Param("limit") int limit);
	
	/**
     * 获取某个用户对某个试题的提交记录.
     * @param problemId - 试题的唯一标识符
     * @param uid - 用户的唯一标识符
     * @param limit - 每次加载评测记录的数量
     * @return 某个用户对某个试题的提交记录
     */
	@Select("SELECT * FROM voj_submissions WHERE problem_id = #{problemId} AND uid = #{uid} ORDER BY submission_id DESC LIMIT #{limit}")
	@Options(useCache = true)
	@Results({
		@Result(property = "submissionId", column = "submission_id"),
		@Result(property = "submitTime", column = "submission_submit_time"),
		@Result(property = "judgeResult", column = "submission_judge_result", javaType=JudgeResult.class, one = @One(select="com.trunkshell.voj.mapper.JudgeResultMapper.getJudgeResultUsingSlug")),
	})
	public List<Submission> getSubmissionUsingProblemIdAndUserId(@Param("problemId") long problemId, @Param("uid") long uid, @Param("limit") int limit);
	
	/**
	 * 获取某个用户在某个试题ID区间段内的最新的评测结果.
	 * @param uid - 用户的唯一标识符
	 * @param problemIdLowerBound - 试题ID区间的下界
	 * @param problemIdUpperBound - 试题ID区间的上界
	 * @return 某个试题ID区间段内的最新的评测结果
	 */
	@Select("SELECT submission_id, s1.problem_id, submission_judge_result FROM voj_submissions  s1 INNER JOIN ( SELECT MAX(submission_id) AS max_submission_id, problem_id FROM voj_submissions WHERE uid = #{uid} AND problem_id >= #{problemIdLowerBound} AND problem_id < #{problemIdUpperBound} GROUP BY problem_id ) s2 ON s1.problem_id = s2.problem_id AND s1.submission_id = s2.max_submission_id")
	@Options(useCache = true)
	@Results({
		@Result(property = "submissionId", column = "submission_id"),
		@Result(property = "problem", column = "problem_id", javaType=Problem.class, one = @One(select="com.trunkshell.voj.mapper.ProblemMapper.getProblem")),
		@Result(property = "judgeResult", column = "submission_judge_result", javaType=JudgeResult.class, one = @One(select="com.trunkshell.voj.mapper.JudgeResultMapper.getJudgeResultUsingSlug"))
	})
	public List<Submission> getLatestSubmissionOfProblems(@Param("uid") long uid, @Param("problemIdLowerBound") long problemIdLowerBound, @Param("problemIdUpperBound") long problemIdUpperBound);
	
	/**
	 * 获取某个用户在某个试题ID区间段内的通过的评测结果.
	 * @param uid - 用户的唯一标识符
	 * @param problemIdLowerBound - 试题ID区间的下界
	 * @param problemIdUpperBound - 试题ID区间的上界
	 * @return 某个试题ID区间段内的通过的评测结果
	 */
	@Select("SELECT submission_id, problem_id, submission_judge_result FROM voj_submissions WHERE uid = #{uid} AND problem_id >= #{problemIdLowerBound} AND problem_id < #{problemIdUpperBound} AND submission_judge_result = 'AC' GROUP BY problem_id")
	@Options(useCache = true)
	@Results({
		@Result(property = "submissionId", column = "submission_id"),
		@Result(property = "problem", column = "problem_id", javaType=Problem.class, one = @One(select="com.trunkshell.voj.mapper.ProblemMapper.getProblem")),
		@Result(property = "judgeResult", column = "submission_judge_result", javaType=JudgeResult.class, one = @One(select="com.trunkshell.voj.mapper.JudgeResultMapper.getJudgeResultUsingSlug"))
	})
	public List<Submission> getAcceptedSubmissionOfProblems(@Param("uid") long uid, @Param("problemIdLowerBound") long problemIdLowerBound, @Param("problemIdUpperBound") long problemIdUpperBound);
	
	/**
     * 获取某个用户通过(Accpeted)提交记录的数量.
     * @param uid - 用户的唯一标识符
     * @return 某个用户通过(Accpeted)提交记录的数量
     */
	@Select("SELECT COUNT(DISTINCT problem_id) FROM voj_submissions WHERE uid = #{uid} AND submission_judge_result = 'AC'")
	@Options(useCache = true)
	public long getAcceptedSubmissionUsingUserId(@Param("uid") long uid);
	
	/**
     * 获取某个用户全部提交记录的数量.
     * @param uid - 用户的唯一标识符
     * @return 某个用户全部提交记录的数量
     */
	@Select("SELECT COUNT(*) FROM voj_submissions WHERE uid = #{uid} AND submission_judge_result != 'PD'")
	@Options(useCache = true)
	public long getTotalSubmissionUsingUserId(@Param("uid") long uid);
	
	/**
	 * 创建提交记录.
	 * @param submission - 待创建的提交记录对象
	 */
	@Insert("INSERT INTO voj_submissions(problem_id, uid, language_id, submission_code) VALUES (#{problem.problemId}, #{user.uid}, #{language.languageId}, #{code})")
	@Options(useGeneratedKeys = true, keyProperty = "submissionId", keyColumn = "submission_id", flushCache = true)
	public void createSubmission(Submission submission);
	
	/**
	 * 更新提交记录.
	 * @param submission - 待更新的提交记录对象
	 */
	@Update("UPDATE voj_submissions SET problem_id = #{problem.problemId}, uid = #{user.uid}, language_id = #{language.languageId}, submission_submit_time = #{submitTime}, submission_execute_time = #{executeTime}, submission_used_time = #{usedTime}, submission_used_memory = #{usedMemory}, submission_judge_result = #{judgeResult.judgeResultSlug}, submission_judge_score = #{judgeScore}, submission_judge_log = #{judgeLog}, submission_code = #{code} WHERE submission_id = #{submissionId}")
	@Options(flushCache = true)
	public void updateSubmission(Submission submission);
	
	/**
	 * 通过提交记录的唯一标识符删除提交记录.
	 * @param submissionId - 提交记录的唯一标识符
	 */
	@Delete("DELETE FROM voj_submissions WHERE submission_id = #{submissionId}")
	@Options(flushCache = true)
	public void deleteSubmission(@Param("submissionId") long submissionId);
}
