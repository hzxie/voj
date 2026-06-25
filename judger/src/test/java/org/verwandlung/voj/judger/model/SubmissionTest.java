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

import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class for the Submission model.
 *
 * @author Haozhe Xie
 */
public class SubmissionTest {
  /**
   * Test case: tests the parameterized constructor and getters. Expected result: the getters return
   * the values passed to the constructor.
   */
  @Test
  public void testConstructorAndGetters() {
    Problem problem = new Problem(1000L, true, "A+B", 1000, 65536, "", "", "", "", "", "");
    Language language = new Language("cpp", "C++", "g++", "./a.out");
    Date submitTime = new Date(1000L);
    Date executeTime = new Date(2000L);

    Submission submission =
        new Submission(
            100L,
            problem,
            7L,
            language,
            submitTime,
            executeTime,
            150,
            2048,
            "AC",
            100,
            "log",
            "int main() {}");

    Assertions.assertEquals(100L, submission.getSubmissionId());
    Assertions.assertSame(problem, submission.getProblem());
    Assertions.assertEquals(7L, submission.getUid());
    Assertions.assertSame(language, submission.getLanguage());
    Assertions.assertEquals(submitTime, submission.getSubmitTime());
    Assertions.assertEquals(executeTime, submission.getExecuteTime());
    Assertions.assertEquals(150, submission.getUsedTime());
    Assertions.assertEquals(2048, submission.getUsedMemory());
    Assertions.assertEquals("AC", submission.getJudgeResultSlug());
    Assertions.assertEquals(100, submission.getJudgeScore());
    Assertions.assertEquals("log", submission.getJudgeLog());
    Assertions.assertEquals("int main() {}", submission.getCode());
  }

  /**
   * Test case: tests the default constructor together with the setters. Expected result: the
   * getters return the values set by the setters.
   */
  @Test
  public void testSetters() {
    Problem problem = new Problem();
    Language language = new Language();
    Date submitTime = new Date(3000L);
    Date executeTime = new Date(4000L);

    Submission submission = new Submission();
    submission.setSubmissionId(101L);
    submission.setProblem(problem);
    submission.setUid(8L);
    submission.setLanguage(language);
    submission.setSubmitTime(submitTime);
    submission.setExecuteTime(executeTime);
    submission.setUsedTime(200);
    submission.setUsedMemory(4096);
    submission.setJudgeResultSlug("WA");
    submission.setJudgeScore(0);
    submission.setJudgeLog("wrong");
    submission.setCode("code");

    Assertions.assertEquals(101L, submission.getSubmissionId());
    Assertions.assertSame(problem, submission.getProblem());
    Assertions.assertEquals(8L, submission.getUid());
    Assertions.assertSame(language, submission.getLanguage());
    Assertions.assertEquals(submitTime, submission.getSubmitTime());
    Assertions.assertEquals(executeTime, submission.getExecuteTime());
    Assertions.assertEquals(200, submission.getUsedTime());
    Assertions.assertEquals(4096, submission.getUsedMemory());
    Assertions.assertEquals("WA", submission.getJudgeResultSlug());
    Assertions.assertEquals(0, submission.getJudgeScore());
    Assertions.assertEquals("wrong", submission.getJudgeLog());
    Assertions.assertEquals("code", submission.getCode());
  }

  /** Test case: tests toString. Expected result: a string containing the submission identifier. */
  @Test
  public void testToString() {
    Submission submission = new Submission();
    submission.setSubmissionId(101L);
    Assertions.assertTrue(submission.toString().contains("101"));
  }
}
