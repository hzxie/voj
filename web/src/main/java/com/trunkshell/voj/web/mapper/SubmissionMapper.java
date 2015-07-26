package com.trunkshell.voj.web.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Param;

import com.trunkshell.voj.web.model.Submission;

/**
 * Submission Data Access Object.
 * 
 * @author Xie Haozhe
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface SubmissionMapper {
    /**
     * [此方法仅供管理员使用]
     * 获取指定时间内提交的数量.
     * @param startTime - 统计起始时间
     * @param endTime - 统计结束时间
     * @return 指定时间内提交的数量
     */
    public long getNumberOfSubmissions(@Param("startTime") Date startTime, @Param("endTime") Date endTime);
    
    /**
     * [此方法仅供管理员使用]
     * 获取某种编程语言的提交数量.
     * @param languageId - 编程语言的唯一标识符
     * @return 某种编程语言的提交数量
     */
    public long getNumberOfSubmissionsUsingLanguage(@Param("languageId") int languageId);
    
    /**
     * 通过评测记录唯一标识符获取试题对象.
     * @param submissionID - 评测记录的唯一标识符
     * @return 一个评测记录对象
     */
    public Submission getSubmission(@Param("submissionId") long submissionId);
    
    /**
     * 通过试题唯一标识符获取某个范围内的所有试题.
     * @param limit - 每次加载评测记录的数量
     * @return 某个范围内的所有提交记录
     */
    public List<Submission> getSubmissions(@Param("limit") int limit);
    
    /**
     * 通过试题唯一标识符获取某个范围内的所有试题.
     * @param offset - 试题唯一标识符的起始编号
     * @param limit - 每次加载评测记录的数量
     * @return 某个范围内的所有提交记录
     */
    public List<Submission> getSubmissionsUsingOffset(@Param("submissionId") long offset, @Param("limit") int limit);
    
    /**
     * 通过试题唯一标识符获取某个范围内的所有试题.
     * @param offset - 试题唯一标识符的起始编号
     * @param limit - 每次加载评测记录的数量
     * @return 某个范围内的所有提交记录
     */
    public List<Submission> getLatestSubmissionsUsingOffset(@Param("submissionId") long offset, @Param("limit") int limit);
    
    /**
     * 获取某个用户对某个试题的提交记录.
     * @param problemId - 试题的唯一标识符
     * @param uid - 用户的唯一标识符
     * @param limit - 每次加载评测记录的数量
     * @return 某个用户对某个试题的提交记录
     */
    public List<Submission> getSubmissionUsingProblemIdAndUserId(@Param("problemId") long problemId, @Param("uid") long uid, @Param("limit") int limit);
    
    /**
     * 获取某个用户在某个试题ID区间段内的最新的评测结果.
     * @param uid - 用户的唯一标识符
     * @param problemIdLowerBound - 试题ID区间的下界
     * @param problemIdUpperBound - 试题ID区间的上界
     * @return 某个试题ID区间段内的最新的评测结果
     */
    public List<Submission> getLatestSubmissionOfProblems(@Param("uid") long uid, @Param("problemIdLowerBound") long problemIdLowerBound, @Param("problemIdUpperBound") long problemIdUpperBound);
    
    /**
     * 获取某个用户在某个试题ID区间段内的通过的评测结果.
     * @param uid - 用户的唯一标识符
     * @param problemIdLowerBound - 试题ID区间的下界
     * @param problemIdUpperBound - 试题ID区间的上界
     * @return 某个试题ID区间段内的通过的评测结果
     */
    public List<Submission> getAcceptedSubmissionOfProblems(@Param("uid") long uid, @Param("problemIdLowerBound") long problemIdLowerBound, @Param("problemIdUpperBound") long problemIdUpperBound);
    
    /**
     * 获取某个用户通过(Accpeted)提交记录的数量.
     * @param uid - 用户的唯一标识符
     * @return 某个用户通过(Accpeted)提交记录的数量
     */
    public long getAcceptedSubmissionUsingUserId(@Param("uid") long uid);
    
    /**
     * 获取某个用户全部提交记录的数量.
     * @param uid - 用户的唯一标识符
     * @return 某个用户全部提交记录的数量
     */
    public long getTotalSubmissionUsingUserId(@Param("uid") long uid);
    
    /**
     * 创建提交记录.
     * @param submission - 待创建的提交记录对象
     */
    public void createSubmission(Submission submission);
    
    /**
     * 更新提交记录.
     * @param submission - 待更新的提交记录对象
     */
    public void updateSubmission(Submission submission);
    
    /**
     * 通过提交记录的唯一标识符删除提交记录.
     * @param submissionId - 提交记录的唯一标识符
     */
    public void deleteSubmission(@Param("submissionId") long submissionId);
}
