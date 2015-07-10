package com.trunkshell.voj.web.mapper;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import com.trunkshell.voj.web.model.JudgeResult;

/**
 * JudgeResult Data Access Object.
 * 
 * @author Xie Haozhe
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface JudgeResultMapper {
    /**
     * 通过评测结果的唯一标识符获取评测结果对象.
     * @param judgeResultId - 评测结果的唯一标识符
     * @return 预期的评测结果对象或空引用
     */
    @Select("SELECT * FROM voj_judge_results WHERE judge_result_id = #{judgeResultId}")
    @Options(useCache = true)
    @Results({
         @Result(property = "judgeResultId", column = "judge_result_id"),
         @Result(property = "judgeResultSlug", column = "judge_result_slug"),
         @Result(property = "judgeResultName", column = "judge_result_name")
    })
    public JudgeResult getJudgeResultUsingId(@Param("judgeResultId") int judgeResultId);
    
    /**
     * 通过评测结果的唯一英文缩写获取评测结果对象.
     * @param judgeResultSlug - 评测结果的唯一英文缩写
     * @return 预期的评测结果对象或空引用
     */
    @Select("SELECT * FROM voj_judge_results WHERE judge_result_slug = #{judgeResultSlug}")
    @Options(useCache = true)
    @Results({
         @Result(property = "judgeResultId", column = "judge_result_id"),
         @Result(property = "judgeResultSlug", column = "judge_result_slug"),
         @Result(property = "judgeResultName", column = "judge_result_name")
    })
    public JudgeResult getJudgeResultUsingSlug(@Param("judgeResultSlug") String judgeResultSlug);

}
