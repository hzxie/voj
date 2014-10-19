package com.happystudio.voj.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.happystudio.voj.dao.SubmissionDao;
import com.happystudio.voj.model.Submission;


/**
 * 评测记录Service对象. 为Controller提供服务.
 * @author Xie Haozhe
 */
@Service
@Transactional
public class SubmissionService {
	/**
	 * 根据评测记录的唯一标识符获取评测记录对象.
	 * @param submissionID - 评测记录的唯一标识符
	 * @return 评测记录对象
	 */
	public Submission getSubmission(int submissionID) {
		return submissionDao.getSubmission(submissionID);
	}
	
	/**
	 * 获取评测记录列表.
	 * @param offset - 评测记录唯一标识符的起始序号
	 * @param limit - 每次加载评测记录的数量
	 * @return 试题列表(List<Submission>对象)
	 */
	public List<Submission> getSubmissions(int offset, int limit) {
		return submissionDao.getSubmissions(offset, limit);
	}
	
	/**
     * 获取某个用户对某个试题的提交记录.
     * @param problemID - 试题的唯一标识符
     * @param userID - 用户的唯一标识符
     * @param limit - 每次加载评测记录的数量
     * @return 某个用户对某个试题的提交记录
     */
	public List<Submission> getSubmissionUsingProblemIDAndUserID(int problemID, int userID, int limit) {
		return submissionDao.getSubmissionUsingProblemIDAndUserID(problemID, userID, limit);
	}
	
	/**
	 * 获取用户评测记录概况.
	 * @param userID - 用户的唯一标识符
	 * @return 一个包含用户评测记录概况的HashMap
	 */
	public Map<String, Long> getUserSubmissionStats(int userID) {
		long acceptedSubmission = submissionDao.getAcceptedSubmissionUsingUserID(userID);;
		long totalSubmission = submissionDao.getTotalSubmissionUsingUserID(userID);
		long  acRate = 0;
		if ( totalSubmission != 0 ) {
			acRate = acceptedSubmission * 100 / totalSubmission;
		}
		
		Map<String, Long> submissionStats = new HashMap<String, Long>(4);
		submissionStats.put("accpetedSubmission", acceptedSubmission);
		submissionStats.put("totalSubmission", totalSubmission);
		submissionStats.put("acRate", acRate);
		return submissionStats;
	}
	
	/**
     * 自动注入的UserDAO对象.
     */
    @Autowired
    private SubmissionDao submissionDao;
}
