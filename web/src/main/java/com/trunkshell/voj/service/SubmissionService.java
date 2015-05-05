package com.trunkshell.voj.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trunkshell.voj.mapper.SubmissionMapper;
import com.trunkshell.voj.model.Submission;

/**
 * 提交类(Submission)的业务逻辑层.
 * @author Xie Haozhe
 */
@Service
@Transactional
public class SubmissionService {
	/**
	 * 根据评测记录的唯一标识符获取评测记录对象.
	 * @param submissionId - 评测记录的唯一标识符
	 * @return 评测记录对象
	 */
	public Submission getSubmission(long submissionId) {
		return submissionMapper.getSubmission(submissionId);
	}
	
	/**
	 * 获取评测记录列表.
	 * @param offset - 评测记录唯一标识符的起始序号
	 * @param limit - 每次加载评测记录的数量
	 * @return 试题列表(List<Submission>对象)
	 */
	public List<Submission> getSubmissions(long offset, int limit) {
		return submissionMapper.getSubmissions(offset, limit);
	}
	
	/**
	 * 获取某个用户对某个试题的提交记录.
	 * @param problemId - 试题的唯一标识符
	 * @param userId - 用户的唯一标识符
	 * @param limit - 每次加载评测记录的数量
	 * @return 某个用户对某个试题的提交记录
	 */
	public List<Submission> getSubmissionUsingProblemIdAndUserId(long problemId, long userId, int limit) {
		return submissionMapper.getSubmissionUsingProblemIdAndUserId(problemId, userId, limit);
	}
	
	/**
	 * 获取某个用户在某个试题ID区间段内的评测结果.
	 * @param userId - 用户的唯一标识符
	 * @param problemIdLowerBound - 试题ID区间的下界
	 * @param problemIdUpperBound - 试题ID区间的上界
	 * @return 某个试题ID区间段内的通过的评测结果
	 */
	public Map<Long, String> getSubmissionOfProblems(long userId, long problemIdLowerBound, long problemIdUpperBound) {
		int hashMapSize = (int)(problemIdUpperBound - problemIdLowerBound + 1);
		Map<Long, String> submissionOfProblems = new HashMap<Long, String>(hashMapSize, 1);
		List<Submission> latestSubmission = submissionMapper.getLatestSubmissionOfProblems(userId, problemIdLowerBound, problemIdUpperBound);
		List<Submission> acceptedSubmission = submissionMapper.getAcceptedSubmissionOfProblems(userId, problemIdLowerBound, problemIdUpperBound);
		
		for ( Submission s : latestSubmission ) {
			long problemId = s.getProblem().getProblemId();
			String judgeResultSlug = s.getJudgeResult().getJudgeResultSlug();
			submissionOfProblems.put(problemId, judgeResultSlug);
		}
		// 使用已通过的评测记录更新部分值
		for ( Submission s : acceptedSubmission ) {
			long problemId = s.getProblem().getProblemId();
			String judgeResultSlug = s.getJudgeResult().getJudgeResultSlug();
			submissionOfProblems.put(problemId, judgeResultSlug);
		}
		
		return submissionOfProblems;
	}
	
	/**
	 * 获取用户评测记录概况.
	 * @param userId - 用户的唯一标识符
	 * @return 一个包含用户评测记录概况的HashMap
	 */
	public Map<String, Long> getUserSubmissionStats(long userId) {
		long acceptedSubmission = submissionMapper.getAcceptedSubmissionUsingUserId(userId);
		long totalSubmission = submissionMapper.getTotalSubmissionUsingUserId(userId);
		long  acRate = 0;
		if ( totalSubmission != 0 ) {
			acRate = acceptedSubmission * 100 / totalSubmission;
		}
		
		Map<String, Long> submissionStats = new HashMap<String, Long>(4, 1);
		submissionStats.put("acceptedSubmission", acceptedSubmission);
		submissionStats.put("totalSubmission", totalSubmission);
		submissionStats.put("acRate", acRate);
		return submissionStats;
	}
	
	/**
	 * 自动注入的SubmissionMapper对象.
	 */
	@Autowired
	private SubmissionMapper submissionMapper;
}
