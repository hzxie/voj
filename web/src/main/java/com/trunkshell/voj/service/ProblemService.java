package com.trunkshell.voj.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trunkshell.voj.mapper.ProblemMapper;
import com.trunkshell.voj.model.Problem;

/**
 * 试题类(Problem)的业务逻辑层.
 * @author Xie Haozhe
 */
@Service
@Transactional
public class ProblemService {
	/**
	 * 通过试题的唯一标识符获取试题的详细信息.
	 * @param problemId - 试题的唯一标识符
	 * @return 试题的详细信息
	 */
	public Problem getProblem(long problemId) {
		return problemMapper.getProblem(problemId);
	}
	
	/**
	 * 获取试题列表.
	 * @param offset - 试题唯一标识符的起始序号
	 * @param limit - 每次加载试题的数量
	 * @return 试题列表(List<Problem>对象)
	 */
	public List<Problem> getProblems(long offset, int limit) {
		return problemMapper.getProblems(offset, limit);
	}
	
	/**
	 * 获取试题的总数量.
	 * @return 试题的总数量
	 */
	public long getNumberOfProblems() {
		return problemMapper.getNumberOfProblems();
	}
	
	/**
     * 自动注入的ProblemMapper对象.
     */
    @Autowired
    private ProblemMapper problemMapper;
}
