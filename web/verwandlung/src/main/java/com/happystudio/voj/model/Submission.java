package com.happystudio.voj.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * 评测记录的Model.
 * @author Xie Haozhe
 */
@Entity
@Table(name = "voj_submissions")
public class Submission implements Serializable {
	/**
	 * 评测记录类的默认构造函数.
	 */
	public Submission() { }
	
	/**
	 * 评测记录类的默认函数.
	 * @param problem - 评测对应的试题对象
	 * @param user - 评测提交者的用户对象
	 * @param language - 提交所使用的语言对象
	 * @param submitTime - 评测提交时间
	 * @param executeTime - 评测开始执行时间
	 * @param usedTime - 评测运行总时间
	 * @param usedMemory - 评测运行占用最大内存
	 * @param judgeResult - 评测结果
	 * @param judgeScore - 评测运行得分
	 * @param judgeLog - 评测运行日志
	 * @param code - 评测所执行的代码
	 */
	public Submission(Problem problem, User user, Language language, Date submitTime,  Date executeTime, 
			int usedTime, int usedMemory, JudgeResult judgeResult, int judgeScore,  String judgeLog, String code) { 
		this.problem = problem;
		this.user = user;
		this.language = language;
		this.submitTime = submitTime;
		this.executeTime = executeTime;
		this.usedTime = usedTime;
		this.usedMemory = usedMemory;
		this.judgeResult = judgeResult;
		this.judgeScore = judgeScore;
		this.judgeLog = judgeLog;
		this.code = code;
	}
	
	/**
	 * 评测记录类的默认函数.
	 * @param submissionID - 评测记录的唯一标识符
	 * @param problem - 评测对应的试题对象
	 * @param user - 评测提交者的用户对象
	 * @param language - 提交所使用的语言对象
	 * @param submitTime - 评测提交时间
	 * @param executeTime - 评测开始执行时间
	 * @param usedTime - 评测运行总时间
	 * @param usedMemory - 评测运行占用最大内存
	 * @param judgeResult - 评测结果
	 * @param judgeScore - 评测运行得分
	 * @param judgeLog - 评测运行日志
	 * @param code - 评测所执行的代码
	 */
	public Submission(int submissionID, Problem problem, User user, Language language, Date submitTime, 
			Date executeTime, int usedTime, int usedMemory, JudgeResult judgeResult, int judgeScore, 
			String judgeLog, String code) { 
		this.submissionID = submissionID;
		this.problem = problem;
		this.user = user;
		this.language = language;
		this.submitTime = submitTime;
		this.executeTime = executeTime;
		this.usedTime = usedTime;
		this.usedMemory = usedMemory;
		this.judgeResult = judgeResult;
		this.judgeScore = judgeScore;
		this.judgeLog = judgeLog;
		this.code = code;
	}
	
	/**
	 * 获取评测记录的唯一标识符.
	 * @return 评测记录的唯一标识符
	 */
	public int getSubmissionID() {
		return submissionID;
	}
	
	/**
	 * 设置评测记录的唯一标识符.
	 * @param submissionID - 评测记录的唯一标识符.
	 */
	public void setSubmissionID(int submissionID) {
		this.submissionID = submissionID;
	}
	
	/**
	 * 获取评测对应的试题对象.
	 * @return 试题对象
	 */
	public Problem getProblem() {
		return problem;
	}
	
	/**
	 * 设置评测对应的试题对象.
	 * @param problem - 试题对象
	 */
	public void setProblem(Problem problem) {
		this.problem = problem;
	}
	
	/**
	 * 获取评测提交者的用户对象.
	 * @return 用户对象
	 */
	public User getUser() {
		return user;
	}
	
	/**
	 * 设置评测提交者的用户对象.
	 * @param user - 用户对象
	 */
	public void setUser(User user) {
		this.user = user;
	}
	
	/**
	 * 获取提交所使用的语言对象.
	 * @return 提交所使用的语言对象
	 */
	public Language getLanguage() {
		return language;
	}
	
	/**
	 * 设置提交所使用的语言对象.
	 * @param language - 提交所使用的语言对象
	 */
	public void setLanguage(Language language) {
		this.language = language;
	}
	
	/**
	 * 获取评测提交时间.
	 * @return 评测提交时间
	 */
	public Date getSubmitTime() {
		return submitTime;
	}
	
	/**
	 * 设置评测提交时间.
	 * @param submitTime - 评测提交时间
	 */
	public void setSubmitTime(Date submitTime) {
		this.submitTime = submitTime;
	}
	
	/**
	 * 获取评测开始执行时间.
	 * @return 评测开始执行时间
	 */
	public Date getExecuteTime() {
		return executeTime;
	}
	
	/**
	 * 获取评测运行总时间.
	 * @return 评测运行总时间
	 */
	public int getUsedTime() {
		return usedTime;
	}
	
	/**
	 * 设置评测运行总时间.
	 * @param usedTime - 评测运行总时间
	 */
	public void setUsedTime(int usedTime) {
		this.usedTime = usedTime;
	}
	
	/**
	 * 获取评测运行占用最大内存.
	 * @return 评测运行占用最大内存
	 */
	public int getUsedMemory() {
		return usedMemory;
	}
	
	/**
	 * 设置评测运行占用最大内存.
	 * @param usedMemory - 评测运行占用最大内存
	 */
	public void setUsedMemory(int usedMemory) {
		this.usedMemory = usedMemory;
	}
	
	/**
	 * 获取评测结果.
	 * @return 评测结果
	 */
	public JudgeResult getJudgeResult() {
		return judgeResult;
	}
	
	/**
	 * 设置评测结果.
	 * @param judgeResult - 评测结果
	 */
	public void setJudgeResult(JudgeResult judgeResult) {
		this.judgeResult = judgeResult;
	}
	
	/**
	 * 获取评测运行得分.
	 * @return 评测运行得分
	 */
	public int getJudgeScore() {
		return judgeScore;
	}
	
	/**
	 * 设置评测运行得分.
	 * @param judgeScore - 评测运行得分
	 */
	public void setJudgeScore(int judgeScore) {
		this.judgeScore = judgeScore;
	}
	
	/**
	 * 获取评测运行日志.
	 * @return 评测运行日志
	 */
	public String getJudgeLog() {
		return judgeLog;
	}
	
	/**
	 * 设置评测运行日志
	 * @param judgeLog - 评测运行日志
	 */
	public void setJudgeLog(String judgeLog) {
		this.judgeLog = judgeLog;
	}
	
	/**
	 * 设置评测所执行的代码.
	 * @return 评测所执行的代码
	 */
	public String getCode() {
		return code;
	}
	
	/**
	 * 设置评测所执行的代码.
	 * @param code - 评测所执行的代码
	 */
	public void setCode(String code) {
		this.code = code;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("Submission [ID=%d, Problem={%s}, User={%s}, Language={%s}, SubmitTime={%s}, "
				+ "ExecuteTime={%s}, UsedTime=%d, UsedMemory=%d, JudgeResult={%s}, JudgeScore=%d, "
				+ "JudgeLog=%s, Code=%s]",
                new Object[] { submissionID, problem, user, language, submitTime, executeTime, usedTime, usedMemory,
						judgeResult, judgeScore, judgeLog, code});
	}
	
	/**
	 * 评测记录的唯一标识符. 
	 */
	@Id
    @GeneratedValue
	@Column(name = "submission_id")
	int submissionID;
	
	/**
	 * 评测对应的试题对象.
	 */
	@ManyToOne(targetEntity = Problem.class)
    @JoinColumn(name = "problem_id")
	Problem problem;
	
	/**
	 * 评测提交者的用户对象. 
	 */
	@ManyToOne(targetEntity = User.class)
    @JoinColumn(name = "uid")
	User user;
	
	/**
	 * 提交所使用的语言对象.
	 */
	@ManyToOne(targetEntity = Language.class)
    @JoinColumn(name = "language_id")
	Language language;
	
	/**
	 * 评测提交时间.
	 */
	@Column(name = "submission_submit_time")
	Date submitTime;
	
	/**
	 * 评测开始执行时间.
	 */
	@Column(name = "submission_execute_time")
	Date executeTime;
	
	/**
	 * 评测运行总时间.
	 */
	@Column(name = "submission_used_time")
	int usedTime;
	
	/**
	 * 评测运行占用最大内存.
	 */
	@Column(name = "submission_used_memory")
	int usedMemory;
	
	/**
	 * 评测结果.
	 */
	@ManyToOne(targetEntity = JudgeResult.class)
    @JoinColumn(name = "submission_judge_result", referencedColumnName="judge_result_slug")
	JudgeResult judgeResult;
	
	/**
	 * 评测运行得分.
	 */
	@Column(name = "submission_judge_score")
	int judgeScore;
	
	/**
	 * 评测运行日志.
	 */
	@Column(name = "submission_judge_log")
	String judgeLog;
	
	/**
	 * 评测所执行的代码.
	 */
	@Column(name = "submission_code")
	String code;
	
	/**
	 * 唯一的序列化标识符.
	 */
	private static final long serialVersionUID = -1984683070957842963L;
}
