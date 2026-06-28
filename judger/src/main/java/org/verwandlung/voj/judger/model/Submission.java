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
package org.verwandlung.voj.judger.model;

import java.io.Serializable;
import java.util.Date;

/**
 * The model of a submission. Maps to the voj_submissions table in the database.
 *
 * @author Haozhe Xie
 */
public class Submission implements Serializable {
  /** Default constructor of the submission. */
  public Submission() {}

  /**
   * Constructor of the submission.
   *
   * @param submissionId - the unique identifier of the submission
   * @param problem - the problem object the submission corresponds to
   * @param uid - the unique identifier of the user who made the submission
   * @param language - the language object used by the submission
   * @param submitTime - the time the submission was submitted
   * @param executeTime - the time the judging started
   * @param usedTime - the total running time of the judging
   * @param usedMemory - the maximum memory used during judging
   * @param judgeResultSlug - the unique identifier of the judge result
   * @param judgeScore - the score of the judging
   * @param judgeLog - the log of the judging
   * @param code - the code executed during judging
   */
  public Submission(
      long submissionId,
      Problem problem,
      long uid,
      Language language,
      Date submitTime,
      Date executeTime,
      int usedTime,
      int usedMemory,
      String judgeResultSlug,
      int judgeScore,
      String judgeLog,
      String code) {
    this.submissionId = submissionId;
    this.problem = problem;
    this.uid = uid;
    this.language = language;
    this.submitTime = submitTime;
    this.executeTime = executeTime;
    this.usedTime = usedTime;
    this.usedMemory = usedMemory;
    this.judgeResultSlug = judgeResultSlug;
    this.judgeScore = judgeScore;
    this.judgeLog = judgeLog;
    this.code = code;
  }

  /**
   * Gets the unique identifier of the submission.
   *
   * @return the unique identifier of the submission
   */
  public long getSubmissionId() {
    return submissionId;
  }

  /**
   * Sets the unique identifier of the submission.
   *
   * @param submissionId - the unique identifier of the submission
   */
  public void setSubmissionId(long submissionId) {
    this.submissionId = submissionId;
  }

  /**
   * Gets the problem object the submission corresponds to.
   *
   * @return the problem object
   */
  public Problem getProblem() {
    return problem;
  }

  /**
   * Sets the problem object the submission corresponds to.
   *
   * @param problem - the problem object
   */
  public void setProblem(Problem problem) {
    this.problem = problem;
  }

  /**
   * Gets the unique identifier of the user who made the submission.
   *
   * @return the unique identifier of the user
   */
  public long getUid() {
    return uid;
  }

  /**
   * Sets the unique identifier of the user who made the submission.
   *
   * @param uid - the unique identifier of the user
   */
  public void setUid(long uid) {
    this.uid = uid;
  }

  /**
   * Gets the language object used by the submission.
   *
   * @return the language object used by the submission
   */
  public Language getLanguage() {
    return language;
  }

  /**
   * Sets the language object used by the submission.
   *
   * @param language - the language object used by the submission
   */
  public void setLanguage(Language language) {
    this.language = language;
  }

  /**
   * Gets the time the submission was submitted.
   *
   * @return the time the submission was submitted
   */
  public Date getSubmitTime() {
    return submitTime;
  }

  /**
   * Sets the time the submission was submitted.
   *
   * @param submitTime - the time the submission was submitted
   */
  public void setSubmitTime(Date submitTime) {
    this.submitTime = submitTime;
  }

  /**
   * Gets the time the judging started.
   *
   * @return the time the judging started
   */
  public Date getExecuteTime() {
    return executeTime;
  }

  /**
   * Sets the time the judging started.
   *
   * @param executeTime - the time the judging started
   */
  public void setExecuteTime(Date executeTime) {
    this.executeTime = executeTime;
  }

  /**
   * Gets the total running time of the judging.
   *
   * @return the total running time of the judging
   */
  public int getUsedTime() {
    return usedTime;
  }

  /**
   * Sets the total running time of the judging.
   *
   * @param usedTime - the total running time of the judging
   */
  public void setUsedTime(int usedTime) {
    this.usedTime = usedTime;
  }

  /**
   * Gets the maximum memory used during judging.
   *
   * @return the maximum memory used during judging
   */
  public int getUsedMemory() {
    return usedMemory;
  }

  /**
   * Sets the maximum memory used during judging.
   *
   * @param usedMemory - the maximum memory used during judging
   */
  public void setUsedMemory(int usedMemory) {
    this.usedMemory = usedMemory;
  }

  /**
   * Gets the unique English abbreviation of the judge result.
   *
   * @return the unique English abbreviation of the judge result
   */
  public String getJudgeResultSlug() {
    return judgeResultSlug;
  }

  /**
   * Sets the judge result.
   *
   * @param judgeResultSlug - the unique English abbreviation of the judge result
   */
  public void setJudgeResultSlug(String judgeResultSlug) {
    this.judgeResultSlug = judgeResultSlug;
  }

  /**
   * Gets the score of the judging.
   *
   * @return the score of the judging
   */
  public int getJudgeScore() {
    return judgeScore;
  }

  /**
   * Sets the score of the judging.
   *
   * @param judgeScore - the score of the judging
   */
  public void setJudgeScore(int judgeScore) {
    this.judgeScore = judgeScore;
  }

  /**
   * Gets the log of the judging.
   *
   * @return the log of the judging
   */
  public String getJudgeLog() {
    return judgeLog;
  }

  /**
   * Sets the log of the judging.
   *
   * @param judgeLog - the log of the judging
   */
  public void setJudgeLog(String judgeLog) {
    this.judgeLog = judgeLog;
  }

  /**
   * Gets the unique identifier of the judger that processed this submission.
   *
   * @return the unique identifier of the judger, or null if it has not been judged yet
   */
  public Long getJudgerUid() {
    return judgerUid;
  }

  /**
   * Sets the unique identifier of the judger that processed this submission.
   *
   * @param judgerUid - the unique identifier of the judger, or null if unknown
   */
  public void setJudgerUid(Long judgerUid) {
    this.judgerUid = judgerUid;
  }

  /**
   * Gets the code executed during judging.
   *
   * @return the code executed during judging
   */
  public String getCode() {
    return code;
  }

  /**
   * Sets the code executed during judging.
   *
   * @param code - the code executed during judging
   */
  public void setCode(String code) {
    this.code = code;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return String.format(
        "Submission [ID=%d, Problem={%s}, Uid={%s}, Language={%s}, SubmitTime={%s}, "
            + "ExecuteTime={%s}, UsedTime=%d, UsedMemory=%d, JudgeResultSlug={%s}, JudgeScore=%d, "
            + "JudgeLog=%s, Code=%s]",
        new Object[] {
          submissionId,
          problem,
          uid,
          language,
          submitTime,
          executeTime,
          usedTime,
          usedMemory,
          judgeResultSlug,
          judgeScore,
          judgeLog,
          code
        });
  }

  /** The unique identifier of the submission. */
  private long submissionId;

  /** The problem object the submission corresponds to. */
  private Problem problem;

  /** The unique identifier of the user who made the submission. */
  private long uid;

  /** The language object used by the submission. */
  private Language language;

  /** The time the submission was submitted. */
  private Date submitTime;

  /** The time the judging started. */
  private Date executeTime;

  /** The total running time of the judging. */
  private int usedTime;

  /** The maximum memory used during judging. */
  private int usedMemory;

  /** The judge result. */
  private String judgeResultSlug;

  /** The score of the judging. */
  private int judgeScore;

  /** The log of the judging. */
  private String judgeLog;

  /** The unique identifier of the judger that processed this submission (null if not yet judged). */
  private Long judgerUid;

  /** The code executed during judging. */
  private String code;

  /** The unique serialization identifier. */
  private static final long serialVersionUID = -6017296127470986766L;
}
