/* Verwandlung Online Judge - A cross-platform judge online system
 * Copyright (C) 2018 Haozhe Xie <cshzxie@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *                              _ooOoo_  
 *                             o8888888o  
 *                             88" . "88  
 *                             (| -_- |)  
 *                             O\  =  /O  
 *                          ____/`---'\____  
 *                        .'  \\|     |//  `.  
 *                       /  \\|||  :  |||//  \  
 *                      /  _||||| -:- |||||-  \  
 *                      |   | \\\  -  /// |   |  
 *                      | \_|  ''\---/''  |   |  
 *                      \  .-\__  `-`  ___/-. /  
 *                    ___`. .'  /--.--\  `. . __  
 *                 ."" '<  `.___\_<|>_/___.'  >'"".  
 *                | | :  `- \`.;`\ _ /`;.`/ - ` : | |  
 *                \  \ `-.   \_ __\ /__ _/   .-` /  /  
 *           ======`-.____`-.___\_____/___.-`____.-'======  
 *                              `=---=' 
 *
 *                          HERE BE BUDDHA
 *
 */
package org.verwandlung.voj.web.service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.mapper.LanguageMapper;
import org.verwandlung.voj.web.mapper.ProblemMapper;
import org.verwandlung.voj.web.mapper.SubmissionMapper;
import org.verwandlung.voj.web.messenger.MessageSender;
import org.verwandlung.voj.web.model.Language;
import org.verwandlung.voj.web.model.Problem;
import org.verwandlung.voj.web.model.Submission;
import org.verwandlung.voj.web.model.User;

/**
 * 提交类(Submission)的业务逻辑层.
 * @author Haozhe Xie
 */
@Service
@Transactional
public class SubmissionService {
	/**
	 * [此方法仅供管理员使用]
	 * 获取指定时间内提交的数量.
	 * @param startTime - 统计起始时间
	 * @param endTime - 统计结束时间
	 * @return 指定时间内提交的数量
	 */
	public long getNumberOfSubmissionsUsingDate(Date startTime, Date endTime) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:MM:ss");
		String startTimeString = "0000-00-00 00:00:00";
		String endTimeString = "9999-12-31 23:59:59";
		
		if ( startTime != null ) {
			startTimeString = sdf.format(startTime);
		}
		if ( endTime != null ) {
			endTimeString = sdf.format(endTime);
		}
		return submissionMapper.getNumberOfSubmissionsUsingDate(startTimeString, endTimeString);
	}

	/**
	 * 获取某个用户对某个试题的提交记录的数量.
	 * @param problemId - 试题的唯一标识符
	 * @param username - 用户的用户名
	 * @return 某个用户对某个试题提交的数量
	 */
	public long getNumberOfSubmissionsUsingProblemIdAndUsername(long problemId, String username) {
		return submissionMapper.getNumberOfSubmissionsUsingProblemIdAndUsername(problemId, username);
	}
	
	/**
	 * [此方法仅供管理员使用]
	 * 获取最新提交记录的唯一标识符
	 * @return 最新提交记录的唯一标识符
	 */
	public long getLatestSubmissionId() {
		return submissionMapper.getLatestSubmissionId();
	}
	
	/**
	 * 获取指定时间内提交的数量.
	 * @param  startTime - 统计起始时间
	 * @param  endTime - 统计结束时间
	 * @param  uid - 用户的唯一标识符
	 * @param  isAcceptedOnly - 是否只统计通过的提交记录
	 * @return 包含时间和提交次数的键值对 Map
	 */
	public Map<String, Long> getNumberOfSubmissionsUsingDate(Date startTime, Date endTime, long uid, boolean isAcceptedOnly) {
		long differenceInSeconds = (endTime.getTime() - startTime.getTime()) / 1000;
		if ( differenceInSeconds > 32 * 86400  ) {
			return getNumberOfSubmissionsGroupByMonth(startTime, endTime, uid, isAcceptedOnly);
		}
		return getNumberOfSubmissionsGroupByDay(startTime, endTime, uid, isAcceptedOnly);
	}
	
	/**
	 * 获取指定时间内提交的数量, 并按月份汇总.
	 * @param  startTime - 统计起始时间
	 * @param  endTime - 统计结束时间
	 * @param  uid - 用户的唯一标识符
	 * @param  isAcceptedOnly - 是否只统计通过的提交记录
	 * @return 包含月份和提交次数的键值对 Map
	 */
	private Map<String, Long> getNumberOfSubmissionsGroupByMonth(Date startTime, Date endTime, long uid, boolean isAcceptedOnly) {
		// 获取包含日期区间的空列表
		Map<String, Long> numberOfSubmissions = new LinkedHashMap<String, Long>();
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(startTime);
		while ( calendar.getTime().before(endTime) ) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM");
			calendar.add(Calendar.MONTH, 1);
			Date targetDate = calendar.getTime();
			numberOfSubmissions.put(sdf.format(targetDate), (long) 0);
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:MM:ss");
		String startTimeString = sdf.format(startTime);
		String endTimeString = sdf.format(endTime);
		List<Map<String, Object>> submissions = submissionMapper.getNumberOfSubmissionsGroupByMonth(startTimeString, endTimeString, uid, isAcceptedOnly);
		
		for ( Map<String, Object> e : submissions ) {
			// 形如 201512 的数值型数据 
			Integer month = (Integer)e.get("month");
			String monthString = month.toString();
			long submissionTimes = (Long)e.get("submissions");
			
			monthString = monthString.substring(0, 4) + "/" + monthString.substring(4);
			numberOfSubmissions.put(monthString, submissionTimes);
		}
		return numberOfSubmissions;
	}
	
	/**
	 * 获取指定时间内提交的数量, 并按天数汇总.
	 * @param  startTime - 统计起始时间
	 * @param  endTime - 统计结束时间
	 * @param  uid - 用户的唯一标识符
	 * @param  isAcceptedOnly - 是否只统计通过的提交记录
	 * @return 包含日期和提交次数的键值对 Map
	 */
	private Map<String, Long> getNumberOfSubmissionsGroupByDay(Date startTime, Date endTime, long uid, boolean isAcceptedOnly) {
		// 获取包含日期区间的空列表
		Map<String, Long> numberOfSubmissions = new LinkedHashMap<String, Long>();
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(startTime);
		while ( calendar.getTime().before(endTime) ) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
			calendar.add(Calendar.DATE, 1);
			Date targetDate = calendar.getTime();
			numberOfSubmissions.put(sdf.format(targetDate), (long) 0);
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
		String startTimeString = sdf.format(startTime);
		String endTimeString = sdf.format(endTime);
		List<Map<String, Object>> submissions = submissionMapper.getNumberOfSubmissionsGroupByDay(startTimeString, endTimeString, uid, isAcceptedOnly);
		
		for ( Map<String, Object> e : submissions ) {
			String date = ((java.sql.Date)e.get("date")).toString().replace('-', '/');
			long submissionTimes = (Long)e.get("submissions");
			numberOfSubmissions.put(date, submissionTimes);
		}
		return numberOfSubmissions;
	}
	
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
	 * @param problemId - 试题的唯一标识符
	 * @param username - 用户的用户名
	 * @param limit - 每次加载评测记录的数量
	 * @return 试题列表(List<Submission>对象)
	 */
	public List<Submission> getSubmissions(long problemId, String username, int limit) {
		return submissionMapper.getSubmissions(problemId, username, limit);
	}
	
	/**
	 * 获取评测记录列表.
	 * 用于异步加载评测记录.
	 * @param problemId - 试题的唯一标识符
	 * @param username - 用户的用户名
	 * @param offset - 评测记录唯一标识符的起始序号
	 * @param limit - 每次加载评测记录的数量
	 * @return 试题列表(List<Submission>对象)
	 */
	public List<Submission> getSubmissions(long problemId, String username, long offset, int limit) {
		return submissionMapper.getSubmissionsUsingOffset(problemId, username, offset, limit);
	}
	
	/**
	 * 获取最新的评测记录列表.
	 * 用于定时获取最新的评测记录.
	 * @param problemId - 试题的唯一标识符
	 * @param username - 用户的用户名
	 * @param offset - 评测记录唯一标识符的起始序号
	 * @param limit - 每次加载评测记录的数量
	 * @return 试题列表(List<Submission>对象)
	 */
	public List<Submission> getLatestSubmissions(long problemId, String username, long offset, int limit) {
		return submissionMapper.getLatestSubmissionsUsingOffset(problemId, username, offset, limit);
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
	 * 获取某个用户全部试题的评测结果.
	 * @param userId - 用户的唯一标识符
	 * @return 某个用户全部试题的评测结果
	 */
	public Map<Long, Submission> getSubmissionOfUser(long userId) {
		long problemIdLowerBound = problemMapper.getLowerBoundOfProblems();
		long problemIdUpperBound = problemMapper.getUpperBoundOfProblems();
		
		return getSubmissionOfProblems(userId, problemIdLowerBound, problemIdUpperBound);
	}
	
	/**
	 * 获取某个用户在某个试题ID区间段内的评测结果.
	 * @param userId - 用户的唯一标识符
	 * @param problemIdLowerBound - 试题ID区间的下界
	 * @param problemIdUpperBound - 试题ID区间的上界
	 * @return 某个试题ID区间段内的通过的评测结果
	 */
	public Map<Long, Submission> getSubmissionOfProblems(long userId, long problemIdLowerBound, long problemIdUpperBound) {
		Map<Long, Submission> submissionOfProblems = new HashMap<>();
		List<Submission> latestSubmission = submissionMapper.getLatestSubmissionOfProblems(userId, problemIdLowerBound, problemIdUpperBound);
		List<Submission> acceptedSubmission = submissionMapper.getAcceptedSubmissionOfProblems(userId, problemIdLowerBound, problemIdUpperBound);
		
		for ( Submission s : latestSubmission ) {
			long problemId = s.getProblem().getProblemId();
			submissionOfProblems.put(problemId, s);
		}
		// 使用已通过的评测记录更新部分值
		for ( Submission s : acceptedSubmission ) {
			long problemId = s.getProblem().getProblemId();
			submissionOfProblems.put(problemId, s);
		}
		return submissionOfProblems;
	}
	
	/**
	 * 获取用户评测记录概况.
	 * @param userId - 用户的唯一标识符
	 * @return 一个包含用户评测记录概况的HashMap
	 */
	public Map<String, Long> getSubmissionStatsOfUser(long userId) {
		long acceptedSubmission = submissionMapper.getAcceptedSubmissionUsingUserId(userId);
		long totalSubmission = submissionMapper.getTotalSubmissionUsingUserId(userId);
		long  acRate = 0;
		if ( totalSubmission != 0 ) {
			acRate = acceptedSubmission * 100 / totalSubmission;
		}
		
		Map<String, Long> submissionStats = new HashMap<>(4, 1);
		submissionStats.put("acceptedSubmission", acceptedSubmission);
		submissionStats.put("totalSubmission", totalSubmission);
		submissionStats.put("acRate", acRate);
		return submissionStats;
	}
	
	/**
	 * 创建提交记录, 并将评测任务加入消息队列.
	 * @param user - 已登录的用户对象
	 * @param problemId - 试题的唯一标识符
	 * @param languageSlug - 编程语言的别名
	 * @param code - 代码
	 * @param isCsrfTokenValid - CSRF的Token是否正确
	 * @return 一个包含提交记录创建结果的Map<String, Object>对象, 并包含创建的提交记录的唯一标识符.
	 */
	public Map<String, Object> createSubmission(User user, long problemId, 
			String languageSlug, String code, boolean isCsrfTokenValid) {
		Problem problem = problemMapper.getProblem(problemId);
		Language language = languageMapper.getLanguageUsingSlug(languageSlug);
		
		Submission submission = new Submission(problem, user, language, code);
		
		@SuppressWarnings("unchecked")
		Map<String, Object> result = (Map<String, Object>) getSubmissionCreationResult(submission, isCsrfTokenValid);
		boolean isSuccessful = (Boolean)result.get("isSuccessful");
		if ( isSuccessful ) {
			submissionMapper.createSubmission(submission);
			
			long submissionId = submission.getSubmissionId();
			createSubmissionTask(submissionId);
			result.put("submissionId", submissionId);
		}
		return result;
	}
	
	/**
	 * 验证提交记录数据.
	 * @param submission - 待创建的提交记录对象
	 * @param isCsrfTokenValid - CSRF的Token是否正确
	 * @return 一个包含提交记录的验证结果的Map<String, Boolean>对象
	 */
	private Map<String, ? extends Object> getSubmissionCreationResult(Submission submission, boolean isCsrfTokenValid) {
		Map<String, Boolean> result = new HashMap<>(6, 1);
		String code = submission.getCode();
		result.put("isUserLogined", submission.getUser() != null);
		result.put("isProblemExists", submission.getProblem() != null);
		result.put("isLanguageExists", submission.getLanguage() != null);
		result.put("isCodeEmpty", code == null || code.length() == 0);
		result.put("isCsrfTokenValid", isCsrfTokenValid);
		
		boolean isSuccessful = result.get("isUserLogined")	&&  result.get("isProblemExists") &&
							   result.get("isLanguageExists") && !result.get("isCodeEmpty")	 &&
							   result.get("isCsrfTokenValid");
		result.put("isSuccessful", isSuccessful);
		return result;
	}
	
	/**
	 * 创建评测任务, 将提交的信息提交至消息队列.
	 * @param submissionId - 提交记录的唯一标识符
	 */
	public void createSubmissionTask(long submissionId) {
		Map<String, Object> mapMessage = new HashMap<>();
		mapMessage.put("event", "SubmissionCreated");
		mapMessage.put("submissionId", submissionId);
		
		messageSender.sendMessage(mapMessage);
	}
	
	/**
	 * 使用提交记录的唯一标识符删除提交记录.
	 * @param submissionId - 提交记录的唯一标识符
	 * @return 提交记录是否被删除
	 */
	public boolean deleteSubmission(long submissionId) {
		submissionMapper.deleteSubmission(submissionId);
		return true;
	}
	
	/**
	 * 自动注入的SubmissionMapper对象.
	 */
	@Autowired
	private SubmissionMapper submissionMapper;
	
	/**
	 * 自动注入的ProblemMapper对象.
	 */
	@Autowired
	private ProblemMapper problemMapper;
	
	/**
	 * 自动注入的LanguageMapper对象.
	 */
	@Autowired
	private LanguageMapper languageMapper;
	
	/**
	 * 自动注入的MessageSender对象.
	 */
	@Autowired
	private MessageSender messageSender;
}
