package com.trunkshell.voj.web.mapper;

import java.util.List;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Param;

import com.trunkshell.voj.web.model.Problem;

/**
 * Problem Data Access Object.
 * 
 * @author Xie Haozhe
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface ProblemMapper {
    /**
     * [此方法仅供管理员使用]
     * 获取全部试题的总数量.
     * @return 全部试题的总数量
     */
    public long getNumberOfProblems();
    
    /**
     * 获取公开试题的总数量.
     * @return 公开试题的总数量
     */
    public long getNumberOfPublicProblems();

    /**
     * [此方法仅供管理员使用]
     * 获取非公开试题的总数量.
     * @return 非公开试题的总数量
     */
    public long getNumberOfPrivateProblems();
    
    /**
     * 获取第一个试题的ID.
     * @return 第一个试题的ID
     */
    public long getLowerBoundOfProblems();
    
    /**
     * 获取最后一个试题的ID.
     * @return 最后一个试题的ID
     */
    public long getUpperBoundOfProblems();
    
    /**
     * 通过试题唯一标识符获取试题对象.
     * @param problemId - 试题的唯一标识符
     * @return 一个试题对象
     */
    public Problem getProblem(@Param("problemId") long problemId);
    
    /**
     * 通过试题唯一标识符获取某个范围内的所有试题.
     * @param offset - 试题唯一标识符的起始编号
     * @param limit - 需要获取的试题的数量
     * @return 某个范围内的所有试题
     */
    public List<Problem> getProblems(@Param("problemId") long offset, @Param("limit") int limit);
    
    public List<Problem> getProblemsUsingCategoryID(int categoryID, long offset, int limit);
    
    public List<Problem> getProblemsUsingTagID(int tagID, long offset, int limit);
    
    public List<Problem> getProblemsUsingKeyword(String keyword, long offset, int limit);
    
    /**
     * 创建一个新的试题对象.
     * @param problem - 试题对象
     * @return 操作是否成功完成
     */
    public void createProblem(Problem problem);
    
    /**
     * 更新试题信息.
     * @param problem - 试题对象
     * @return 操作是否成功完成
     */
    public void updateProblem(Problem problem);
    
    /**
     * 通过试题的唯一标识符删除一个试题对象.
     * @param problemId - 试题的唯一标识符
     * @return 操作是否成功完成
     */
    public void deleteProblem(@Param("problemId") long problemId);
}
