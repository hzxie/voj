package com.happystudio.voj.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.happystudio.voj.dao.ProblemDao;
import com.happystudio.voj.model.Problem;

/**
 * 试题Service对象. 为Controller提供服务.
 * @author Xie Haozhe
 */
@Service
@Transactional
public class ProblemService {
	/**
	 * 通过试题的唯一标识符获取试题的详细信息.
	 * @param problemID - 试题的唯一标识符
	 * @return 试题的详细信息
	 */
	public Problem getProblem(int problemID) {
		return problemDao.getProblem(problemID);
	}
	
	/**
	 * 获取试题列表.
	 * @param offset - 试题起始序号
	 * @return 试题列表(List<Problem>对象)
	 */
	public List<Problem> getProblems(int offset, int limit) {
		return problemDao.getProblems(offset, limit);
	}
	
	/**
	 * 获取试题的总数量.
	 * @return 试题的总数量
	 */
	public long getNumberOfProblems() {
		return problemDao.getNumberOfProblems();
	}
	
	/**
     * 自动注入的UserDAO对象.
     */
    @Autowired
    private ProblemDao problemDao;
}
