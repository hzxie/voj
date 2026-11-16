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
package org.verwandlung.voj.judger.core;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * The outcome of a judging, as recorded with its submission: the judge result, the figures and the
 * judge log shown to the user (Markdown).
 *
 * @param judgeResult - the slug of the judge result
 * @param usedTime - the CPU time of all runs (ms)
 * @param usedMemory - the most memory a run used (KB)
 * @param score - the score of the submission
 * @param log - the judge log
 * @author Haozhe Xie
 */
public record Outcome(String judgeResult, int usedTime, int usedMemory, int score, String log) {
  /**
   * The outcome of a submission that could not be judged for a reason that is not its own.
   *
   * @param reason - why, shown to the user
   * @return the System Error
   */
  public static Outcome systemError(String reason) {
    return new Outcome("SE", 0, 0, 0, String.format("System Error.\n\n%s\n", reason));
  }

  /**
   * The outcome of a submission that did not compile.
   *
   * @param compilerOutput - what the compiler printed
   * @return the Compile Error
   */
  public static Outcome compileError(String compilerOutput) {
    // The compiler's output as a Markdown indented code block (every line indented by 4 spaces):
    // line breaks are kept, and tokens such as "#include <windows.h>" are not taken for HTML.
    return new Outcome(
        "CE",
        0,
        0,
        0,
        "Compile Error.\n\n"
            + compilerOutput.replaceAll("(?m)^", "    ")
            + "\n\nCompile Error, Time = 0 ms, Memory = 0 KB, Score = 0.\n");
  }

  /**
   * The outcome of a submission that compiled and ran on every checkpoint: the judge result of the
   * first checkpoint it did not pass (Accepted if none), the time of all runs, the most memory of
   * a run and the score it earned.
   *
   * @param results - its results on the checkpoints, in their order, not empty
   * @param judgeResultNames - the name of a judge result, by its slug
   * @return the outcome
   */
  public static Outcome of(List<CheckpointResult> results, UnaryOperator<String> judgeResultNames) {
    if (results.isEmpty()) {
      throw new IllegalArgumentException("A submission is judged against one checkpoint at least");
    }
    String judgeResult = CheckpointResult.ACCEPTED;
    int usedTime = 0;
    int usedMemory = 0;
    int score = 0;
    StringBuilder log = new StringBuilder("Compile Successfully.\n\n");
    int index = 0;
    for (CheckpointResult result : results) {
      if (judgeResult.equals(CheckpointResult.ACCEPTED) && !result.isAccepted()) {
        judgeResult = result.judgeResult();
      }
      usedTime += result.usedTime();
      usedMemory = Math.max(usedMemory, result.usedMemory());
      score += result.score();
      log.append(
          String.format(
              "- Test Point #%d: %s, Time = %d ms, Memory = %d KB, Score = %d\n",
              index++,
              judgeResultNames.apply(result.judgeResult()),
              result.usedTime(),
              result.usedMemory(),
              result.score()));
    }
    log.append(
        String.format(
            "\n%s, Time = %d ms, Memory = %d KB, Score = %d\n",
            judgeResultNames.apply(judgeResult), usedTime, usedMemory, score));
    return new Outcome(judgeResult, usedTime, usedMemory, score, log.toString());
  }
}
