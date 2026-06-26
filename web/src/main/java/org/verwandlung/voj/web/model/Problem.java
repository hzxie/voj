/* Verwandlung Online Judge - A cross-platform judge online system
 * Copyright (C) 2014-2026 Haozhe Xie <root@haozhexie.com>
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
 */
package org.verwandlung.voj.web.model;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * The model of a problem. Maps to the voj_problems table in the database.
 *
 * @author Haozhe Xie
 */
public class Problem implements Serializable {
  /** Default constructor of the problem. */
  public Problem() {}

  /**
   * Constructor of the problem.
   *
   * @param isPublic - whether the problem is public
   * @param problemName - the name of the problem
   * @param timeLimit - the maximum running time
   * @param memoryLimit - the maximum running memory
   * @param description - the description of the problem
   * @param inputFormat - the description of the problem's input format
   * @param outputFormat - the description of the problem's output format
   * @param sampleInput - the sample input of the problem
   * @param sampleOutput - the sample output of the problem
   * @param hint - the hint of the problem
   */
  public Problem(
      boolean isPublic,
      String problemName,
      int timeLimit,
      int memoryLimit,
      String description,
      String inputFormat,
      String outputFormat,
      String sampleInput,
      String sampleOutput,
      String hint) {
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
   * Constructor of the Problem class.
   *
   * @param problemId - the unique identifier of the problem
   * @param isPublic - whether the problem is public
   * @param problemName - the name of the problem
   * @param timeLimit - the maximum running time
   * @param memoryLimit - the maximum running memory
   * @param description - the description of the problem
   * @param inputFormat - the description of the problem's input format
   * @param outputFormat - the description of the problem's output format
   * @param sampleInput - the sample input of the problem
   * @param sampleOutput - the sample output of the problem
   * @param hint - the hint of the problem
   */
  public Problem(
      long problemId,
      boolean isPublic,
      String problemName,
      int timeLimit,
      int memoryLimit,
      String description,
      String inputFormat,
      String outputFormat,
      String sampleInput,
      String sampleOutput,
      String hint) {
    this(
        isPublic,
        problemName,
        timeLimit,
        memoryLimit,
        description,
        inputFormat,
        outputFormat,
        sampleInput,
        sampleOutput,
        hint);
    this.problemId = problemId;
  }

  /**
   * Gets the unique identifier of the problem.
   *
   * @return the unique identifier of the problem
   */
  public long getProblemId() {
    return problemId;
  }

  /**
   * Sets the unique identifier of the problem.
   *
   * @param problemId - the unique identifier of the problem
   */
  public void setProblemId(long problemId) {
    this.problemId = problemId;
  }

  /**
   * Gets whether the problem is public.
   *
   * @return whether the problem is public
   */
  public boolean isPublic() {
    return isPublic;
  }

  /**
   * Sets whether the problem is public.
   *
   * @param isPublic - whether the problem is public
   */
  public void setPublic(boolean isPublic) {
    this.isPublic = isPublic;
  }

  /**
   * Gets the name of the problem.
   *
   * @return the name of the problem
   */
  public String getProblemName() {
    return problemName;
  }

  /**
   * Sets the name of the problem.
   *
   * @param problemName - the name of the problem
   */
  public void setProblemName(String problemName) {
    this.problemName = problemName;
  }

  /**
   * Gets the difficulty of the problem.
   *
   * @return the difficulty of the problem
   */
  public ProblemDifficulty getProblemDifficulty() {
    return problemDifficulty;
  }

  /**
   * Sets the difficulty of the problem.
   *
   * @param problemDifficulty - the difficulty of the problem
   */
  public void setProblemDifficulty(ProblemDifficulty problemDifficulty) {
    this.problemDifficulty = problemDifficulty;
  }

  /**
   * Gets the tags of the problem.
   *
   * @return the tags of the problem
   */
  public List<ProblemTag> getProblemTags() {
    return problemTags;
  }

  /**
   * Sets the tags of the problem.
   *
   * @param problemTags - the tags of the problem
   */
  public void setProblemTags(List<ProblemTag> problemTags) {
    this.problemTags = problemTags;
  }

  /**
   * Gets the total number of submissions for the problem.
   *
   * @return the total number of submissions for the problem
   */
  public long getTotalSubmission() {
    return totalSubmission;
  }

  /**
   * Sets the total number of submissions for the problem.
   *
   * @param totalSubmission - the total number of submissions for the problem
   */
  public void setTotalSubmission(long totalSubmission) {
    this.totalSubmission = totalSubmission;
  }

  /**
   * Gets the total number of accepted submissions for the problem.
   *
   * @return the total number of accepted submissions for the problem
   */
  public long getAcceptedSubmission() {
    return acceptedSubmission;
  }

  /**
   * Sets the total number of accepted submissions for the problem.
   *
   * @param acceptedSubmission - the total number of accepted submissions for the problem
   */
  public void setAcceptedSubmission(long acceptedSubmission) {
    this.acceptedSubmission = acceptedSubmission;
  }

  /**
   * Gets the maximum running time.
   *
   * @return the maximum running time
   */
  public int getTimeLimit() {
    return timeLimit;
  }

  /**
   * Sets the maximum running time.
   *
   * @param timeLimit - the maximum running time
   */
  public void setTimeLimit(int timeLimit) {
    this.timeLimit = timeLimit;
  }

  /**
   * Gets the maximum running memory.
   *
   * @return the maximum running memory
   */
  public int getMemoryLimit() {
    return memoryLimit;
  }

  /**
   * Sets the maximum running memory.
   *
   * @param memoryLimit - the maximum running memory
   */
  public void setMemoryLimit(int memoryLimit) {
    this.memoryLimit = memoryLimit;
  }

  /**
   * Gets the description of the problem.
   *
   * @return the description of the problem
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description of the problem.
   *
   * @param description - the description of the problem
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Gets the description of the input format.
   *
   * @return the description of the input format
   */
  public String getInputFormat() {
    return inputFormat;
  }

  /**
   * Sets the description of the input format.
   *
   * @param inputFormat - the description of the input format
   */
  public void setInpuFormatt(String inputFormat) {
    this.inputFormat = inputFormat;
  }

  /**
   * Gets the description of the output format.
   *
   * @return the description of the output format
   */
  public String getOutputFormat() {
    return outputFormat;
  }

  /**
   * Sets the description of the output format.
   *
   * @param outputFormat - the description of the output format
   */
  public void setOutputFormat(String outputFormat) {
    this.outputFormat = outputFormat;
  }

  /**
   * Gets the sample input.
   *
   * @return the sample input
   */
  public String getSampleInput() {
    return sampleInput;
  }

  /**
   * Sets the sample input.
   *
   * @param sampleInput - the sample input
   */
  public void setSampleInput(String sampleInput) {
    this.sampleInput = sampleInput;
  }

  /**
   * Gets the sample output.
   *
   * @return the sample output
   */
  public String getSampleOutput() {
    return sampleOutput;
  }

  /**
   * Sets the sample output.
   *
   * @param sampleOutput - the sample output
   */
  public void setSampleOutput(String sampleOutput) {
    this.sampleOutput = sampleOutput;
  }

  /**
   * Gets the hint of the problem.
   *
   * @return the hint of the problem
   */
  public String getHint() {
    return hint;
  }

  /**
   * Sets the hint of the problem.
   *
   * @param hint - the hint of the problem
   */
  public void setHint(String hint) {
    this.hint = hint;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return String.format(
        "Problem: [ProblemID=%s, isPublic=%s, ProblemName=%s, TotalSubmission=%s, "
            + "AcceptedSubmission=%s, TimeLimit=%s, MemoryLimit=%s, Description=%s, "
            + "InputFormat=%s, OutputFormat=%s, SampleInput=%s, SampleOutput=%s, Hint=%s]",
        new Object[] {
          problemId,
          isPublic,
          problemName,
          totalSubmission,
          acceptedSubmission,
          timeLimit,
          memoryLimit,
          description,
          inputFormat,
          outputFormat,
          sampleInput,
          sampleOutput,
          hint
        });
  }

  /** The unique identifier of the problem. */
  private long problemId;

  /** Whether the problem is public. */
  private boolean isPublic;

  /** The name of the problem. */
  private String problemName;

  /** The difficulty of the problem (normalized lookup: id + slug + name). */
  private ProblemDifficulty problemDifficulty;

  /** The tags of the problem. */
  private List<ProblemTag> problemTags;

  /** The total number of submissions for the problem. */
  private long totalSubmission;

  /** The total number of accepted submissions for the problem. */
  private long acceptedSubmission;

  /** The maximum running time. */
  @JsonIgnore private int timeLimit;

  /** The maximum running memory. */
  @JsonIgnore private int memoryLimit;

  /** The description of the problem. */
  @JsonIgnore private String description;

  /** The input format. */
  @JsonIgnore private String inputFormat;

  /** The output format. */
  @JsonIgnore private String outputFormat;

  /** The sample input. */
  @JsonIgnore private String sampleInput;

  /** The sample output. */
  @JsonIgnore private String sampleOutput;

  /** The hint of the problem. */
  @JsonIgnore private String hint;

  /** The unique serialization identifier. */
  private static final long serialVersionUID = 4703482016721365341L;
}
