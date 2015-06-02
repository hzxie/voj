package com.trunkshell.voj.web.mapper;

import java.util.List;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.trunkshell.voj.web.model.Problem;

/**
 * Problem Data Access Object.
 * @author Xie Haozhe
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface ProblemMapper {
	/**
	 * 获取试题的总数量.
	 * @return (公开)试题的总数量
	 */
	@Select("SELECT COUNT(*) FROM voj_problems WHERE problem_is_public = true")
	@Options(useCache = true)
	public long getNumberOfProblems();
	
	/**
	 * 通过试题唯一标识符获取试题对象.
	 * @param problemId - 试题的唯一标识符
	 * @return 一个试题对象
	 */
	@Select("SELECT " + ALL_FIELDS + " FROM voj_problems p WHERE problem_id = #{problemId}")
	@Options(useCache = true)
	@Results({
		 @Result(property = "problemId", column = "problem_id"),
		 @Result(property = "isPublic", column = "problem_is_public"),
		 @Result(property = "problemName", column = "problem_name"),
		 @Result(property = "totalSubmission", column = "total_submission"),
		 @Result(property = "acceptedSubmission", column = "accepted_submission"),
		 @Result(property = "timeLimit", column = "problem_time_limit"),
		 @Result(property = "memoryLimit", column = "problem_memory_limit"),
		 @Result(property = "description", column = "problem_description"),
		 @Result(property = "inputFormat", column = "problem_input_format"),
		 @Result(property = "outputFormat", column = "problem_output_format"),
		 @Result(property = "sampleInput", column = "problem_sample_input"),
		 @Result(property = "sampleOutput", column = "problem_sample_output"),
		 @Result(property = "hint", column = "problem_hint")
	})
	public Problem getProblem(@Param("problemId") long problemId);
	
	/**
	 * 通过试题唯一标识符获取某个范围内的所有试题.
	 * @param offset - 试题唯一标识符的起始编号
	 * @param limit - 需要获取的试题的数量
	 * @return 某个范围内的所有试题
	 */
	@Select("SELECT " + SUMMERY_FIELDS + " FROM voj_problems p WHERE problem_id >= #{problemId} AND problem_is_public = true LIMIT #{limit}")
	@Options(useCache = true)
	@Results({
		 @Result(property = "problemId", column = "problem_id"),
		 @Result(property = "problemName", column = "problem_name"),
		 @Result(property = "totalSubmission", column = "total_submission"),
		 @Result(property = "acceptedSubmission", column = "accepted_submission")
	})
	public List<Problem> getProblems(@Param("problemId") long offset, @Param("limit") int limit);
	
	public List<Problem> getProblemsUsingCategoryID(int categoryID, long offset, int limit);
	
	public List<Problem> getProblemsUsingTagID(int tagID, long offset, int limit);
	
	public List<Problem> getProblemsUsingKeyword(String keyword, long offset, int limit);
	
	/**
	 * 创建一个新的试题对象.
	 * @param problem - 试题对象
	 * @return 操作是否成功完成
	 */
	@Insert("INSERT INTO voj_problems (problem_is_public, problem_name, problem_time_limit, problem_memory_limit, problem_description, problem_input_format, problem_output_format, problem_sample_input, problem_sample_output, problem_hint) VALUES (#{isPublic}, #{problemName}, #{timeLimit}, #{memoryLimit}, #{description}, #{inputFormat}, #{outputFormat}, #{sampleInput}, #{sampleOutput}, #{hint})")
	@Options(useGeneratedKeys = true, keyProperty = "problemId", keyColumn = "problem_id", flushCache = true)
	public void createProblem(Problem problem);
	
	/**
	 * 更新试题信息.
	 * @param problem - 试题对象
	 * @return 操作是否成功完成
	 */
	@Update("UPDATE voj_problems SET problem_is_public = #{isPublic}, problem_name = #{problemName}, problem_time_limit = #{timeLimit}, problem_memory_limit = #{memoryLimit}, problem_description = #{description}, problem_input_format = #{inputFormat}, problem_output_format = #{outputFormat}, problem_sample_input = #{sampleInput}, problem_sample_output = #{sampleOutput}, problem_hint = #{hint} WHERE problem_id = #{problemId}")
	@Options(flushCache = true)
	public void updateProblem(Problem problem);
	
	/**
	 * 通过试题的唯一标识符删除一个试题对象.
	 * @param problemId - 试题的唯一标识符
	 * @return 操作是否成功完成
	 */
	@Delete("DELETE FROM voj_problems WHERE problem_id = #{problemId}")
	@Options(flushCache = true)
	public void deleteProblem(long problemId);

	/**
	 * 获取试题提交总次数的SQL子查询语句. 
	 */
	final String TOTAL_SUBMISSION_FIELD = "(SELECT COUNT(*) FROM voj_submissions s WHERE s.problem_id = p.problem_id) AS total_submission";
	
	/**
	 * 获取试题通过提交次数的SQL子查询语句. 
	 */
	final String ACCEPTED_SUBMISSION_FIELD = "(SELECT COUNT(DISTINCT(uid)) FROM voj_submissions s WHERE s.problem_id = p.problem_id AND s.submission_judge_result = 'AC') AS accepted_submission";
	
	/**
	 * 试题所有属性的SQL表达式.
	 */
	final String ALL_FIELDS = "*, " + TOTAL_SUBMISSION_FIELD + ", " + ACCEPTED_SUBMISSION_FIELD;
	
	/**
	 * 试题必需属性的SQL表达式.
	 */
	final String SUMMERY_FIELDS = "problem_id, problem_name, " + TOTAL_SUBMISSION_FIELD + ", " + ACCEPTED_SUBMISSION_FIELD;
}
