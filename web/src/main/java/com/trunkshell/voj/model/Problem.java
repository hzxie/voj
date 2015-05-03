package com.trunkshell.voj.model;

import java.io.Serializable;

/**
 * 试题的Model.
 * 对应数据库中的voj_problems数据表.
 * 
 * @author Xie Haozhe
 */
public class Problem implements Serializable {
	/**
	 * Problem的默认构造函数.
	 */
	public Problem() { }
	
	/**
	 * Problem的构造函数.
	 * @param isPublic - 试题是否公开
	 * @param problemName - 试题的名称
	 * @param timeLimit - 最大运行时间
	 * @param memoryLimit - 最大运行内存
	 * @param description - 试题描述
	 * @param inputFormat - 试题输入格式描述
	 * @param outputFormat - 试题输出描述
	 * @param sampleInput - 试题样例输入
	 * @param sampleOutput - 试题样例输出
	 * @param hint - 试题提示
	 */
	public Problem(boolean isPublic, String problemName, int timeLimit, int memoryLimit,  
					String description, String inputFormat, String outputFormat, 
					String sampleInput, String sampleOutput, String hint) { 
		this.isPublic = isPublic;
		this.problemName = problemName;
		this.timeLimit = timeLimit;
		this.memoryLimit = memoryLimit;
		this.description = description;
		this.inputFormat = inputFormat;
		this.outputFormat = outputFormat;
		this.sampleInput = sampleInput;
		this.sampleOutput = sampleOutput;
		this.hint = hint;
	}
	
	/**
	 * Problem类的构造函数.
	 * @param problemId - 试题唯一标识符
	 * @param isPublic - 试题是否公开
	 * @param problemName - 试题的名称
	 * @param timeLimit - 最大运行时间
	 * @param memoryLimit - 最大运行内存
	 * @param description - 试题描述
	 * @param inputFormat - 试题输入描述
	 * @param outputFormat - 试题输出描述
	 * @param sampleInput - 试题样例输入
	 * @param sampleOutput - 试题样例输出
	 * @param hint - 试题提示
	 */
	public Problem(int problemId, boolean isPublic, String problemName, int timeLimit, 
					int memoryLimit, String description, String inputFormat, String outputFormat, 
					String sampleInput, String sampleOutput, String hint) { 
		this(isPublic, problemName, timeLimit, memoryLimit, description, inputFormat, outputFormat, sampleInput, sampleOutput, hint);
		this.problemId = problemId;
	}
	
	/**
	 * 获取试题唯一标识符.
	 * @return 试题唯一标识符
	 */
	public long getProblemID() {
		return problemId;
	}

	/**
	 * 设置试题唯一标识符.
	 * @param problemId - 试题唯一标识符
	 */
	public void setProblemID(long problemId) {
		this.problemId = problemId;
	}
	
	/**
	 * 获取试题是否公开的.
	 * @return 试题是否公开
	 */
	public boolean isPublic() {
		return isPublic;
	}

	/**
	 * 设置试题是否公开.
	 * @param isPublic - 试题是否公开
	 */
	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}

	/**
	 * 获取试题名称.
	 * @return 试题名称
	 */
	public String getProblemName() {
		return problemName;
	}

	/**
	 * 设置试题名称.
	 * @param problemName - 试题名称
	 */
	public void setProblemName(String problemName) {
		this.problemName = problemName;
	}

	/**
	 * 获取试题提交总数.
	 * @return 试题提交总数
	 */
	public long getTotalSubmission() {
		return totalSubmission;
	}

	/**
	 * 设置试题提交总数.
	 * @param totalSubmission - 试题提交总数
	 */
	public void setTotalSubmission(long totalSubmission) {
		this.totalSubmission = totalSubmission;
	}

	/**
	 * 获取试题提交通过总数.
	 * @return 试题提交通过总数
	 */
	public long getAcceptedSubmission() {
		return acceptedSubmission;
	}

	/**
	 * 设置试题提交通过总数
	 * @param acceptedSubmission - 试题提交通过总数
	 */
	public void setAcceptedSubmission(long acceptedSubmission) {
		this.acceptedSubmission = acceptedSubmission;
	}

	/**
	 * 获取最大运行时间.
	 * @return 最大运行时间
	 */
	public int getTimeLimit() {
		return timeLimit;
	}

	/**
	 * 设置最大运行时间.
	 * @param timeLimit - 最大运行时间
	 */
	public void setTimeLimit(int timeLimit) {
		this.timeLimit = timeLimit;
	}

	/**
	 * 获取最大运行内存.
	 * @return 最大运行内存
	 */
	public int getMemoryLimit() {
		return memoryLimit;
	}

	/**
	 * 设置最大运行内存.
	 * @param memoryLimit
	 */
	public void setMemoryLimit(int memoryLimit) {
		this.memoryLimit = memoryLimit;
	}
	
	/**
	 * 获取试题描述.
	 * @return 试题描述
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * 设置试题描述.
	 * @param description - 试题描述
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * 获取输入格式描述.
	 * @return 输入格式描述
	 */
	public String getInputFormat() {
		return inputFormat;
	}

	/**
	 * 设置输入格式描述.
	 * @param inputFormat - 输入格式描述
	 */
	public void setInpuFormatt(String inputFormat) {
		this.inputFormat = inputFormat;
	}

	/**
	 * 获取输出格式描述.
	 * @return 输出格式描述
	 */
	public String getOutputFormat() {
		return outputFormat;
	}

	/**
	 * 设置输出格式描述.
	 * @param outputFormat - 输出格式描述
	 */
	public void setOutputFormat(String outputFormat) {
		this.outputFormat = outputFormat;
	}

	/**
	 * 获取样例输入.
	 * @return 样例输入
	 */
	public String getSampleInput() {
		return sampleInput;
	}

	/**
	 * 设置样例输入.
	 * @param sampleInput - 样例输入
	 */
	public void setSampleInput(String sampleInput) {
		this.sampleInput = sampleInput;
	}

	/**
	 * 获取样例输出.
	 * @return 样例输出
	 */
	public String getSampleOutput() {
		return sampleOutput;
	}

	/**
	 * 设置样例输出.
	 * @param sampleOutput - 样例输出
	 */
	public void setSampleOutput(String sampleOutput) {
		this.sampleOutput = sampleOutput;
	}

	/**
	 * 获取试题提示.
	 * @return 试题提示
	 */
	public String getHint() {
		return hint;
	}

	/**
	 * 设置试题提示.
	 * @param hint - 试题提示
	 */
	public void setHint(String hint) {
		this.hint = hint;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("Problem: [ProblemID=%s, isPublic=%s, ProblemName=%s, TotalSubmission=%s, "
							+ "AcceptedSubmission=%s, TimeLimit=%s, MemoryLimit=%s, Description=%s, "
							+ "InputFormat=%s, OutputFormat=%s, SampleInput=%s, SampleOutput=%s, Hint=%s]", 
				new Object[] { problemId, isPublic, problemName, totalSubmission, acceptedSubmission, 
								timeLimit, memoryLimit, description, inputFormat, outputFormat, sampleInput, 
								sampleOutput, hint});
	}

	/**
	 * 试题的唯一标识符.
	 */
	private long problemId;
	
	/**
	 * 试题是否公开.
	 */
	private boolean isPublic;
	
	/**
	 * 试题名称. 
	 */
	private String problemName;
	
	/**
	 * 试题的总提交总数.
	 */
	private long totalSubmission;
	
	/**
	 * 试题的提交通过总数.
	 */
	private long acceptedSubmission;
	
	/**
	 * 最大运行时间.
	 */
	private int timeLimit;
	
	/**
	 * 最大运行内存.
	 */
	private int memoryLimit;
	
	/**
	 * 试题描述.
	 */
	private String description;
	
	/**
	 * 输入格式.
	 */
	private String inputFormat;
	
	/**
	 * 输出格式.
	 */
	private String outputFormat;
	
	/**
	 * 样例输入.
	 */
	private String sampleInput;
	
	/**
	 * 样例输出.
	 */
	private String sampleOutput;
	
	/**
	 * 试题提示.
	 */
	private String hint;
	
	/**
	 * 唯一的序列化标识符.
	 */
	private static final long serialVersionUID = 4703482016721365340L;
}
