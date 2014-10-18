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
@Table(name = "voj_submission")
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
	 * @param runtimeResult - 评测结果
	 * @param runtimeScore - 评测运行得分
	 * @param runtimeLog - 评测运行日志
	 * @param code - 评测所执行的代码
	 */
	public Submission(Problem problem, User user, Language language, Date submitTime,  Date executeTime, 
			int usedTime, int usedMemory, RuntimeResult runtimeResult, int runtimeScore,  String runtimeLog, String code) { 
		this.problem = problem;
		this.user = user;
		this.language = language;
		this.submitTime = submitTime;
		this.executeTime = executeTime;
		this.usedTime = usedTime;
		this.usedMemory = usedMemory;
		this.runtimeResult = runtimeResult;
		this.runtimeScore = runtimeScore;
		this.runtimeLog = runtimeLog;
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
	 * @param runtimeResult - 评测结果
	 * @param runtimeScore - 评测运行得分
	 * @param runtimeLog - 评测运行日志
	 * @param code - 评测所执行的代码
	 */
	public Submission(int submissionID, Problem problem, User user, Language language, Date submitTime, 
			Date executeTime, int usedTime, int usedMemory, RuntimeResult runtimeResult, int runtimeScore, 
			String runtimeLog, String code) { 
		this.submissionID = submissionID;
		this.problem = problem;
		this.user = user;
		this.language = language;
		this.submitTime = submitTime;
		this.executeTime = executeTime;
		this.usedTime = usedTime;
		this.usedMemory = usedMemory;
		this.runtimeResult = runtimeResult;
		this.runtimeScore = runtimeScore;
		this.runtimeLog = runtimeLog;
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
	public RuntimeResult getRuntimeResult() {
		return runtimeResult;
	}
	
	/**
	 * 设置评测结果.
	 * @param runtimeResult - 评测结果
	 */
	public void setRuntimeResult(RuntimeResult runtimeResult) {
		this.runtimeResult = runtimeResult;
	}
	
	/**
	 * 获取评测运行得分.
	 * @return 评测运行得分
	 */
	public int getRuntimeScore() {
		return runtimeScore;
	}
	
	/**
	 * 设置评测运行得分.
	 * @param runtimeScore - 评测运行得分
	 */
	public void setRuntimeScore(int runtimeScore) {
		this.runtimeScore = runtimeScore;
	}
	
	/**
	 * 获取评测运行日志.
	 * @return 评测运行日志
	 */
	public String getRuntimeLog() {
		return runtimeLog;
	}
	
	/**
	 * 设置评测运行日志
	 * @param runtimeLog - 评测运行日志
	 */
	public void setRuntimeLog(String runtimeLog) {
		this.runtimeLog = runtimeLog;
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
				+ "ExecuteTime={%s}, UsedTime=%d, UsedMemory=%d, RuntimeResult={%s}, RuntimeScore=%d, "
				+ "RuntimeLog=%s, Code=%s]",
                new Object[] { submissionID, problem, user, language, submitTime, executeTime, usedTime, usedMemory,
						runtimeResult, runtimeScore, runtimeLog, code});
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
	@ManyToOne(targetEntity = RuntimeResult.class)
    @JoinColumn(name = "submission_runtime_result", referencedColumnName="runtime_result_slug")
	RuntimeResult runtimeResult;
	
	/**
	 * 评测运行得分.
	 */
	@Column(name = "submission_runtime_score")
	int runtimeScore;
	
	/**
	 * 评测运行日志.
	 */
	@Column(name = "submission_runtime_log")
	String runtimeLog;
	
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
