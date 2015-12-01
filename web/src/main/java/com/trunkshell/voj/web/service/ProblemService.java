package com.trunkshell.voj.web.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trunkshell.voj.web.mapper.CheckpointMapper;
import com.trunkshell.voj.web.mapper.ProblemCategoryMapper;
import com.trunkshell.voj.web.mapper.ProblemMapper;
import com.trunkshell.voj.web.model.Problem;
import com.trunkshell.voj.web.model.ProblemCategory;

/**
 * 试题类(Problem)的业务逻辑层.
 * @author Xie Haozhe
 */
@Service
@Transactional
public class ProblemService {
    /**
     * 获取试题的起始编号.
     * @return 试题的起始编号
     */
    public long getFirstIndexOfProblems() {
        return problemMapper.getLowerBoundOfProblems();
    }
    
    /**
     * 通过试题的唯一标识符获取试题的详细信息.
     * @param problemId - 试题的唯一标识符
     * @return 试题的详细信息
     */
    public Problem getProblem(long problemId) {
        return problemMapper.getProblem(problemId);
    }
    
    /**
     * 获取公开的试题列表.
     * @param offset - 试题唯一标识符的起始序号
     * @param keyword - 关键字
     * @param problemCategorySlug - 试题分类的唯一英文缩写
     * @param limit - 每次加载试题的数量
     * @return 试题列表(List<Problem>对象)
     */
    public List<Problem> getProblemsUsingFilters(long offset, String keyword, String problemCategorySlug, int limit) {
        ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingSlug(problemCategorySlug);
        int problemCategoryId = 0;
        if ( problemCategory != null ) {
            problemCategoryId = problemCategory.getProblemCategoryId();
        }
        return problemMapper.getProblemsUsingFilters(keyword, problemCategoryId, true, offset, limit);
    }
    
    /**
     * 获取试题的总数量.
     * @param keyword - 关键字
     * @param problemCategorySlug - 试题分类的唯一英文缩写
     * @param isPublic - 试题是否公开
     * @return 试题的总数量
     */
    public long getNumberOfProblemsUsingFilters(String keyword, String problemCategorySlug, boolean isPublic) {
        ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingSlug(problemCategorySlug);
        int problemCategoryId = 0;
        if ( problemCategory != null ) {
            problemCategoryId = problemCategory.getProblemCategoryId();
        }
        return problemMapper.getNumberOfProblemsUsingFilters(keyword, problemCategoryId, isPublic);
    }
    
    /**
     * [此方法仅供管理员使用]
     * 获取全部试题的总数量.
     * @return 全部试题的总数量
     */
    public long getNumberOfProblems() {
        return problemMapper.getNumberOfProblems();
    }
    
    /**
     * [此方法仅供管理员使用]
     * 获取系统中全部试题测试点的数量(包括私有试题).
     * @return 系统中全部试题测试点的数量
     */
    public long getNumberOfCheckpoints() {
        return checkpointMapper.getNumberOfCheckpoints();
    }
    
    /**
     * 自动注入的ProblemMapper对象.
     */
    @Autowired
    private ProblemMapper problemMapper;
    
    /**
     * 自动注入的ProblemCategoryMapper对象.
     */
    @Autowired
    private ProblemCategoryMapper problemCategoryMapper;
    
    /**
     * 自动注入的CheckpointMapper对象.
     */
    @Autowired
    private CheckpointMapper checkpointMapper;
}
