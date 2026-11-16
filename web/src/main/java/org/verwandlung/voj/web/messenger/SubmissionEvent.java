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
package org.verwandlung.voj.web.messenger;

import org.springframework.context.ApplicationEvent;

/**
 * The submission record event message.
 *
 * @author Haozhe Xie
 */
public class SubmissionEvent extends ApplicationEvent {
  /**
   * The constructor of SubmissionEvent.
   *
   * @param source - the message publishing source
   * @param submissionId - the unique identifier of the submission record
   * @param judgeResult - the current judge result
   * @param message - the judge message
   * @param isCompleted - whether the judging is completed
   */
  public SubmissionEvent(
      Object source, long submissionId, String judgeResult, String message, boolean isCompleted) {
    this(source, submissionId, judgeResult, message, isCompleted, null);
  }

  /**
   * The constructor of SubmissionEvent for a message that names the judging it belongs to.
   *
   * @param source - the source of the event
   * @param submissionId - the unique identifier of the submission
   * @param judgeResult - the judge result
   * @param message - the message to show
   * @param isCompleted - whether judging has completed
   * @param executeTime - the claim of the judging that sent the message: the start time it set on
   *     the submission, in milliseconds; null when the judger did not send it
   */
  public SubmissionEvent(
      Object source,
      long submissionId,
      String judgeResult,
      String message,
      boolean isCompleted,
      Long executeTime) {
    super(source);
    this.submissionId = submissionId;
    this.judgeResult = judgeResult;
    this.message = message;
    this.isCompleted = isCompleted;
    this.executeTime = executeTime;
  }

  /**
   * Gets the unique identifier of the submission record.
   *
   * @return the unique identifier of the submission record
   */
  public long getSubmissionId() {
    return submissionId;
  }

  /**
   * Gets the current judge result.
   *
   * @return the current judge result
   */
  public String getJudgeResult() {
    return judgeResult;
  }

  /**
   * Gets the judge message.
   *
   * @return the judge message
   */
  public String getMessage() {
    return message;
  }

  /**
   * Gets whether the judging is completed.
   *
   * @return whether the judging is completed
   */
  public boolean isCompleted() {
    return isCompleted;
  }

  /**
   * Gets the claim of the judging that sent the message.
   *
   * @return the start time the judging set on the submission, in milliseconds, or null if unknown
   */
  public Long getExecuteTime() {
    return executeTime;
  }

  /** The unique identifier of the submission record. */
  private final long submissionId;

  /** The current judge result. */
  private final String judgeResult;

  /** The judge message. */
  private final String message;

  /** Whether the judging is completed. */
  private final boolean isCompleted;

  /** The claim of the judging that sent the message, or null if unknown. */
  private final Long executeTime;

  /** The unique serialization identifier. */
  private static final long serialVersionUID = 4808514481370058683L;
}
