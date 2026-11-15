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
package org.verwandlung.voj.web.service;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.verwandlung.voj.web.mapper.ContestContestantMapper;
import org.verwandlung.voj.web.mapper.ContestMapper;
import org.verwandlung.voj.web.mapper.ContestSubmissionMapper;
import org.verwandlung.voj.web.mapper.ProblemMapper;
import org.verwandlung.voj.web.model.*;
import org.verwandlung.voj.web.util.JsonUtils;

import java.util.*;

/**
 * The business logic layer of the contest.
 *
 * @author Haozhe Xie
 */
@Service
@Transactional
public class ContestService {
  /**
   * Gets the list of contests.
   *
   * @param keyword - the keyword of the contest
   * @param isPublicOnly - whether to fetch only the published contests
   * @param offset - the cursor of the first contest
   * @param limit - the number of contests to fetch
   * @return a List object containing Contest objects
   */
  public List<Contest> getContests(String keyword, boolean isPublicOnly, long offset, int limit) {
    return contestMapper.getContests(keyword, isPublicOnly, offset, limit);
  }

  /**
   * [For administrators only] Gets the total number of contests matching a keyword.
   *
   * @param keyword - the keyword of the contest (may be empty)
   * @return the total number of matching contests
   */
  public long getNumberOfContests(String keyword) {
    return contestMapper.getNumberOfContests(keyword);
  }

  /**
   * Gets the number of contests that are currently live (started but not yet ended).
   *
   * @return the number of live contests
   */
  public long getNumberOfLiveContests() {
    return contestMapper.getNumberOfLiveContests();
  }

  /**
   * [For administrators only] Creates a contest.
   *
   * @param contestName - the name of the contest
   * @param contestNotes - the notes/description of the contest
   * @param startTime - the start time of the contest
   * @param endTime - the end time of the contest
   * @param scoring - the scoring rule of the contest (ICPC / IOI / Codeforces)
   * @param problems - the contest problems (a JSON-formatted array of problem IDs)
   * @param status - the publication status of the contest (PUBLISHED / DRAFT / HIDDEN)
   * @param penalty - the penalty (in minutes) per wrong submission
   * @param freeze - the scoreboard freeze duration (in minutes) before the end
   * @param isRated - whether the contest is rated
   * @param openRegistration - whether the contest is open for registration
   * @return a Map containing the creation result and validation flags
   */
  public Map<String, Object> createContest(
      String contestName,
      String contestNotes,
      Date startTime,
      Date endTime,
      String scoring,
      String problems,
      String status,
      int penalty,
      int freeze,
      boolean isRated,
      boolean openRegistration) {
    Map<String, Object> result = validateContest(contestName, startTime, endTime);
    if ((Boolean) result.get("isSuccessful")) {
      Contest contest =
          new Contest(
              contestName, contestNotes, startTime, endTime, getContestMode(scoring), problems);
      contest.setStatus(PublicationStatus.normalize(status));
      contest.setScoring(scoring);
      contest.setPenalty(penalty);
      contest.setFreeze(freeze);
      contest.setRated(isRated);
      contest.setOpenRegistration(openRegistration);
      contestMapper.createContest(contest);
    }
    return result;
  }

  /**
   * [For administrators only] Edits an existing contest.
   *
   * @param contestId - the unique identifier of the contest
   * @param contestName - the name of the contest
   * @param contestNotes - the notes/description of the contest
   * @param startTime - the start time of the contest
   * @param endTime - the end time of the contest
   * @param scoring - the scoring rule of the contest (ICPC / IOI / Codeforces)
   * @param problems - the contest problems (a JSON-formatted array of problem IDs)
   * @param status - the publication status of the contest (PUBLISHED / DRAFT / HIDDEN)
   * @param penalty - the penalty (in minutes) per wrong submission
   * @param freeze - the scoreboard freeze duration (in minutes) before the end
   * @param isRated - whether the contest is rated
   * @param openRegistration - whether the contest is open for registration
   * @return a Map containing the edit result and validation flags
   */
  public Map<String, Object> editContest(
      long contestId,
      String contestName,
      String contestNotes,
      Date startTime,
      Date endTime,
      String scoring,
      String problems,
      String status,
      int penalty,
      int freeze,
      boolean isRated,
      boolean openRegistration) {
    Contest contest = contestMapper.getContest(contestId);
    Map<String, Object> result = validateContest(contestName, startTime, endTime);
    result.put("isContestExists", contest != null);
    if (contest == null) {
      result.put("isSuccessful", false);
      return result;
    }
    if ((Boolean) result.get("isSuccessful")) {
      contest.setContestName(contestName);
      contest.setContestNotes(contestNotes);
      contest.setStartTime(startTime);
      contest.setEndTime(endTime);
      contest.setContestMode(getContestMode(scoring));
      contest.setProblems(problems);
      contest.setStatus(PublicationStatus.normalize(status));
      contest.setScoring(scoring);
      contest.setPenalty(penalty);
      contest.setFreeze(freeze);
      contest.setRated(isRated);
      contest.setOpenRegistration(openRegistration);
      contestMapper.updateContest(contest);
    }
    return result;
  }

  /**
   * Derives the underlying ranking mode (ACM / OI) from a scoring rule. IOI contests are ranked in
   * the OI style; ICPC and Codeforces contests are ranked in the ACM style.
   *
   * @param scoring - the scoring rule of the contest (ICPC / IOI / Codeforces)
   * @return the ranking mode (ACM / OI)
   */
  private String getContestMode(String scoring) {
    return "IOI".equals(scoring) ? "OI" : "ACM";
  }

  /**
   * [For administrators only] Deletes a contest.
   *
   * @param contestId - the unique identifier of the contest to delete
   * @return whether the contest was deleted
   */
  public boolean deleteContest(long contestId) {
    contestSubmissionMapper.deleteSubmissionsOfContest(contestId);
    contestContestantMapper.deleteContestantsOfContest(contestId);
    return contestMapper.deleteContest(contestId) > 0;
  }

  /**
   * Validates a contest's name and schedule.
   *
   * @param contestName - the name of the contest
   * @param startTime - the start time of the contest
   * @param endTime - the end time of the contest
   * @return a Map of validation flags including {@code isSuccessful}
   */
  private Map<String, Object> validateContest(String contestName, Date startTime, Date endTime) {
    Map<String, Object> result = new HashMap<>(4, 1);
    result.put("isNameEmpty", contestName == null || contestName.trim().isEmpty());
    result.put("isTimeValid", startTime != null && endTime != null && endTime.after(startTime));
    result.put(
        "isSuccessful", !(Boolean) result.get("isNameEmpty") && (Boolean) result.get("isTimeValid"));
    return result;
  }

  /**
   * Gets the detailed information of a contest by its unique identifier.
   *
   * @param contestId - the unique identifier of the contest
   * @return a Contest object containing the contest information
   */
  public Contest getContest(long contestId) {
    return contestMapper.getContest(contestId);
  }

  /**
   * Gets the problem list of a contest.
   *
   * @param problemIdList - a List object containing the list of contest problem IDs
   * @return a List object containing the problem information
   */
  public List<Problem> getProblemsOfContests(List<Long> problemIdList) {
    List<Problem> problems = new ArrayList<>();
    for (long problemId : problemIdList) {
      Problem p = problemMapper.getProblem(problemId);

      if (p != null) {
        problems.add(p);
      }
    }
    return problems;
  }

  /**
   * Gets a contestant's submissions for each problem in a contest.
   *
   * @param contestId - the unique identifier of the contest
   * @param contestant - the contestant
   * @return a Map object containing the user's submissions, indexed by problem ID
   */
  public Map<Long, ContestSubmission> getSubmissionsOfContestantOfContest(
      long contestId, User contestant) {
    if (contestant == null) {
      return null;
    }
    Map<Long, ContestSubmission> submissionsGroupByProblems = new HashMap<>();
    List<ContestSubmission> submissions =
        contestSubmissionMapper.getSubmissionOfContestOfContest(contestId, contestant.getUid());

    for (ContestSubmission cs : submissions) {
      long problemId = cs.getSubmission().getProblem().getProblemId();

      if (submissionsGroupByProblems.containsKey(problemId)) {
        ContestSubmission prevSubmission = submissionsGroupByProblems.get(problemId);

        if (prevSubmission.getSubmission().getJudgeResult().getJudgeResultSlug().equals("AC")
            && !cs.getSubmission().getJudgeResult().getJudgeResultSlug().equals("AC")) {
          continue;
        }
      }
      submissionsGroupByProblems.put(problemId, cs);
    }
    return submissionsGroupByProblems;
  }

  /**
   * Gets a contestant's submissions for a problem in a contest.
   *
   * @param contest - the contest object
   * @param problemId - the unique identifier of the problem
   * @param contestant - the contestant object
   * @return a List object containing the submissions
   */
  public List<Submission> getSubmissionsOfContestantOfContestProblem(
      Contest contest, long problemId, User contestant) {
    if (contest == null || contestant == null) {
      return null;
    }

    List<Submission> submissions = new ArrayList<>();
    if (getContestStatus(contest) != Contest.CONTEST_STATUS.READY) {
      List<ContestSubmission> css =
          contestSubmissionMapper.getSubmissionOfContestOfContestProblem(
              contest.getContestId(), problemId, contestant.getUid());
      for (ContestSubmission cs : css) {
        submissions.add(cs.getSubmission());
      }
    }
    return submissions;
  }

  /**
   * Gets the code a user temporarily saved during a contest (generally used to save code in the OI
   * mode).
   *
   * @param contest - the contest object
   * @param problemId - the unique identifier of the problem
   * @param contestant - the contestant object
   * @return the code for the corresponding problem
   */
  public Map<String, String> getCodeSnippetOfContestProblem(
      Contest contest, long problemId, User contestant) {
    if (contest == null || contestant == null) {
      return null;
    }
    if (contest.getContestMode().equals("OI")
        && getContestStatus(contest) == Contest.CONTEST_STATUS.LIVE) {
      ContestContestant cc =
          contestContestantMapper.getContestantOfContest(
              contest.getContestId(), contestant.getUid());
      // An administrator may view the problem without having attended the contest.
      if (cc == null) {
        return null;
      }
      Map<Long, Map<String, String>> codeSnippet =
          JsonUtils.toObject(
              cc.getCodeSnippet(), new TypeReference<Map<Long, Map<String, String>>>() {});

      if (codeSnippet != null && codeSnippet.containsKey(problemId)) {
        return codeSnippet.get(problemId);
      }
    }
    return null;
  }

  /**
   * Tests whether a user may view the problems of a (visible) contest: administrators always; other
   * users only once the contest has started, and, while it is running, only if they attended it.
   * After the contest ends the problems are open to everyone who can see the contest.
   *
   * @param contest - the contest
   * @param user - the user (may be null)
   * @return whether the user may view the contest's problems
   */
  public boolean isContestProblemsViewable(Contest contest, User user) {
    if (contest == null) {
      return false;
    }
    if (PublicationStatus.isAdministrator(user)) {
      return true;
    }
    Date currentTime = new Date();
    if (currentTime.before(contest.getStartTime())) {
      return false;
    }
    if (currentTime.before(contest.getEndTime())) {
      return isAttendContest(contest.getContestId(), user);
    }
    return true;
  }

  /**
   * Tests whether a user may see a submission because it was made in a contest whose problem pages
   * the user may view. Contest problems are often unpublished, and the submission pages otherwise
   * hide submissions to unpublished problems from non-administrators; this keeps a contestant's own
   * contest submissions (and, after the contest, everyone's) reachable from the contest pages.
   *
   * @param submission - the submission
   * @param user - the user (may be null)
   * @return whether the submission belongs to a contest whose problems the user may view
   */
  public boolean isSubmissionViewableThroughContest(Submission submission, User user) {
    if (submission == null || submission.getProblem() == null) {
      return false;
    }
    for (long contestId :
        contestSubmissionMapper.getContestIdsOfSubmission(submission.getSubmissionId())) {
      Contest contest = contestMapper.getContest(contestId);
      if (contest == null || !PublicationStatus.isVisibleTo(contest.getStatus(), user)) {
        continue;
      }
      List<Long> problemIds = JsonUtils.toList(contest.getProblems(), Long.class);
      if (problemIds != null
          && problemIds.contains(submission.getProblem().getProblemId())
          && isContestProblemsViewable(contest, user)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Creates a submission to a problem of a contest and adds the judging task to the message queue.
   * The contest must be visible to the user and running (start &lt;= now &lt; end), the user must
   * have attended it and the problem must be one of its problems; the problem itself may be
   * unpublished (contest problems usually are). The submission and its contest record are stored in
   * one transaction, and the judging task is only enqueued once that transaction has committed.
   *
   * @param contestId - the unique identifier of the contest
   * @param problemId - the unique identifier of the problem
   * @param user - the logged-in user (may be null)
   * @param languageSlug - the alias of the programming language
   * @param code - the code
   * @return a Map containing the same validation flags as SubmissionService.createSubmission plus
   *     {@code isContestExists}, {@code isContestLive} and {@code isAttended}, and on success the
   *     {@code submissionId}
   */
  public Map<String, Object> createSubmission(
      long contestId, long problemId, User user, String languageSlug, String code) {
    Contest contest = contestMapper.getContest(contestId);
    boolean isContestExists =
        contest != null && PublicationStatus.isVisibleTo(contest.getStatus(), user);
    Date currentTime = new Date();
    boolean isContestLive =
        isContestExists
            && !currentTime.before(contest.getStartTime())
            && currentTime.before(contest.getEndTime());
    boolean isAttended = isContestExists && isAttendContest(contestId, user);

    Map<String, Object> result = new HashMap<>(12, 1);
    result.put("isUserLogined", user != null);
    result.put("isContestExists", isContestExists);
    result.put("isContestLive", isContestLive);
    result.put("isAttended", isAttended);
    if (!isContestExists || !isContestLive || !isAttended) {
      result.put("isSuccessful", false);
      return result;
    }

    List<Long> problemIds = JsonUtils.toList(contest.getProblems(), Long.class);
    Problem problem =
        problemIds != null && problemIds.contains(problemId)
            ? problemMapper.getProblem(problemId)
            : null;
    result.putAll(submissionService.saveSubmission(user, problem, languageSlug, code));
    if ((Boolean) result.get("isSuccessful")) {
      long submissionId = (Long) result.get("submissionId");
      Submission submission = new Submission();
      submission.setSubmissionId(submissionId);
      contestSubmissionMapper.createContestSubmission(new ContestSubmission(contest, submission));
      submissionService.createSubmissionTask(submissionId, languageSlug);
    }
    return result;
  }

  /**
   * Gets the number of contestants of a contest.
   *
   * @param contestId - the unique identifier of the contest
   * @return the number of contestants of the contest
   */
  public long getNumberOfContestantsOfContest(long contestId) {
    return contestContestantMapper.getNumberOfContestantsOfContest(contestId);
  }

  /**
   * Gets the number of contests a user has attended.
   *
   * @param userId - the unique identifier of the user
   * @return the number of contests the user has attended
   */
  public long getNumberOfContestsOfUser(long userId) {
    return contestContestantMapper.getNumberOfContestsUsingContestantUid(userId);
  }

  /**
   * Gets whether a user has joined a contest.
   *
   * @param contestId - the unique identifier of the contest
   * @param currentUser - the currently logged-in user object
   * @return whether the user has joined the contest
   */
  public boolean isAttendContest(long contestId, User currentUser) {
    if (currentUser == null) {
      return false;
    }
    return contestContestantMapper.getContestantOfContest(contestId, currentUser.getUid()) != null;
  }

  /**
   * Gets the current status of a contest (not started / in progress / finished).
   *
   * @param contest - the contest to query
   * @return the current status of the contest
   */
  public Contest.CONTEST_STATUS getContestStatus(Contest contest) {
    if (contest == null) {
      return null;
    }

    Date currentTime = new Date();
    if (currentTime.before(contest.getStartTime())) {
      return Contest.CONTEST_STATUS.READY;
    } else if (currentTime.after(contest.getEndTime())) {
      return Contest.CONTEST_STATUS.DONE;
    } else if (currentTime.before(contest.getEndTime())
        && currentTime.after(contest.getStartTime())) {
      return Contest.CONTEST_STATUS.LIVE;
    }
    return null;
  }

  /**
   * Joins a contest.
   *
   * @param contestId - the unique identifier of the contest
   * @param currentUser - the currently logged-in user object
   * @return a Map object containing the status information of whether the contest was joined
   *     successfully
   */
  public Map<String, Boolean> attendContest(long contestId, User currentUser) {
    Contest contest = contestMapper.getContest(contestId);

    Map<String, Boolean> result = new HashMap<>(6, 1);
    // Unpublished (draft / hidden) contests cannot be attended.
    result.put(
        "isContestExists", contest != null && PublicationStatus.isPublic(contest.getStatus()));
    result.put("isContestReady", getContestStatus(contest) == Contest.CONTEST_STATUS.READY);
    result.put("isUserLogin", currentUser != null);
    result.put("isAttendedContest", isAttendContest(contestId, currentUser));

    boolean isSuccessful =
        result.get("isContestExists")
            && result.get("isContestReady")
            && result.get("isUserLogin")
            && !result.get("isAttendedContest");
    if (isSuccessful) {
      ContestContestant contestContestant = new ContestContestant(contest, currentUser);
      contestContestantMapper.createContestContestant(contestContestant);
    }
    result.put("isSuccessful", isSuccessful);
    return result;
  }

  /**
   * Gets the instant from which submissions are hidden from a viewer's leaderboard because of the
   * contest's scoreboard freeze ({@code contest_freeze}, in minutes before the end). The freeze is
   * in effect for non-administrators from {@code endTime - freeze} until the contest ends; after the
   * end (and for administrators at any time) the full leaderboard is shown.
   *
   * @param contest - the contest
   * @param viewer - the user viewing the leaderboard (may be null)
   * @return the freeze instant while the freeze applies to the viewer, otherwise null
   */
  public Date getScoreboardFrozenAt(Contest contest, User viewer) {
    if (contest == null || contest.getFreeze() <= 0 || PublicationStatus.isAdministrator(viewer)) {
      return null;
    }
    Date frozenAt = new Date(contest.getEndTime().getTime() - contest.getFreeze() * 60_000L);
    Date currentTime = new Date();
    if (currentTime.before(frozenAt) || !currentTime.before(contest.getEndTime())) {
      return null;
    }
    return frozenAt;
  }

  /**
   * Gets the leaderboard for the OI mode, ranked by the score (then the time) of each contestant's
   * last submission to every problem.
   *
   * @param contestId - the unique identifier of the contest
   * @param viewer - the user viewing the leaderboard (may be null); decides whether the scoreboard
   *     freeze applies
   * @return a Map object containing the contestants, their submissions indexed by contestant UID and
   *     problem ID, and the freeze instant ({@code frozenAt}, null when the board is not frozen)
   */
  public Map<String, Object> getLeaderBoardForOi(long contestId, User viewer) {
    Contest contest = contestMapper.getContest(contestId);
    Date frozenAt = getScoreboardFrozenAt(contest, viewer);

    Map<String, Object> result = new HashMap<>(4, 1);
    List<ContestContestant> contestants =
        contestContestantMapper.getContestantsOfContestForOi(
            contestId, frozenAt, 0, Integer.MAX_VALUE);
    Map<Long, Map<Long, Submission>> submissions =
        getSubmissionsGroupByContestant(
            contestSubmissionMapper.getSubmissionsOfContest(contestId), true, frozenAt);
    rankingContestants(contestants);

    result.put("contestants", contestants);
    result.put("submissions", submissions);
    result.put("frozenAt", frozenAt);
    return result;
  }

  /**
   * Gets the leaderboard for the ACM mode, ranked by the number of solved problems, then by the
   * penalty time: the time from the contest start to the first accepted submission of every solved
   * problem plus {@code contest_penalty} minutes per rejected attempt before it.
   *
   * @param contestId - the unique identifier of the contest
   * @param viewer - the user viewing the leaderboard (may be null); decides whether the scoreboard
   *     freeze applies
   * @return a Map object containing the contestants, their first accepted submissions indexed by
   *     contestant UID and problem ID, and the freeze instant ({@code frozenAt}, null when the board
   *     is not frozen)
   */
  public Map<String, Object> getLeaderBoardForAcm(long contestId, User viewer) {
    Contest contest = contestMapper.getContest(contestId);
    Date frozenAt = getScoreboardFrozenAt(contest, viewer);

    Map<String, Object> result = new HashMap<>(4, 1);
    List<ContestContestant> contestants =
        contestContestantMapper.getContestantsOfContestForAcm(
            contestId, frozenAt, 0, Integer.MAX_VALUE);
    Map<Long, Map<Long, Submission>> submissions =
        getSubmissionsGroupByContestant(
            contestSubmissionMapper.getAcceptedSubmissionsOfContest(contestId), false, frozenAt);

    // Calculate the penalty time (in seconds): the mapper returns the number of rejected attempts
    // in the Time field; the contest penalty is configured in minutes per rejected attempt.
    long penaltyPerRejection = Math.max(contest.getPenalty(), 0) * 60L;
    for (ContestContestant cc : contestants) {
      long numberOfRejected = cc.getTime();
      long penalty = numberOfRejected * penaltyPerRejection;
      Map<Long, Submission> submissionsOfContestant = submissions.get(cc.getContestant().getUid());
      if (submissionsOfContestant != null) {
        for (Submission s : submissionsOfContestant.values()) {
          long usedTimeInMilliseconds =
              s.getSubmitTime().getTime() - contest.getStartTime().getTime();
          s.setUsedTime(usedTimeInMilliseconds / 1000);
          penalty += s.getUsedTime();
        }
      }
      cc.setTime(penalty);
    }
    Collections.sort(contestants);
    rankingContestants(contestants);

    result.put("contestants", contestants);
    result.put("submissions", submissions);
    result.put("frozenAt", frozenAt);
    return result;
  }

  /**
   * Ranks the contestants.
   *
   * @param contestants - the list of contest contestants
   */
  public void rankingContestants(List<ContestContestant> contestants) {
    int currentRank = 1;
    if (contestants.size() == 0) {
      return;
    }

    contestants.get(0).setRank(currentRank);
    for (int i = 1; i < contestants.size(); ++i) {
      ContestContestant contestant = contestants.get(i);
      ContestContestant prevContestant = contestants.get(i - 1);

      if (contestant.getScore() != prevContestant.getScore()
          || contestant.getTime() != prevContestant.getTime()) {
        currentRank = i + 1;
      }
      contestant.setRank(currentRank);
    }
  }

  /**
   * Builds an index of contest submissions (contestant UID - problem ID).
   *
   * @param contestSubmissions the list containing all contest submissions, oldest first
   * @param override - whether to override an existing submission when the same problem has multiple
   *     submissions (true keeps the last submission, false the first one)
   * @param frozenAt - when not null, submissions made at or after this instant are left out
   * @return the organized contest submissions
   */
  private Map<Long, Map<Long, Submission>> getSubmissionsGroupByContestant(
      List<ContestSubmission> contestSubmissions, boolean override, Date frozenAt) {
    Map<Long, Map<Long, Submission>> submissions = new HashMap<>();

    for (ContestSubmission cs : contestSubmissions) {
      if (frozenAt != null && !cs.getSubmission().getSubmitTime().before(frozenAt)) {
        continue;
      }
      long problemId = cs.getSubmission().getProblem().getProblemId();
      long contestantUid = cs.getSubmission().getUser().getUid();

      if (!submissions.containsKey(contestantUid)) {
        submissions.put(contestantUid, new HashMap<>());
      }
      Map<Long, Submission> submissionsOfContestant = submissions.get(contestantUid);

      if (!override && submissionsOfContestant.containsKey(problemId)) {
        continue;
      }
      submissionsOfContestant.put(problemId, cs.getSubmission());
    }
    return submissions;
  }

  /** The autowired ContestMapper object. */
  @Autowired private ContestMapper contestMapper;

  /** The autowired ContestContestantMapper object. */
  @Autowired private ContestContestantMapper contestContestantMapper;

  /** The autowired ContestSubmissionMapper object. */
  @Autowired private ContestSubmissionMapper contestSubmissionMapper;

  /** The autowired ProblemMapper object, used to obtain the problem information in a contest. */
  @Autowired private ProblemMapper problemMapper;

  /**
   * The autowired SubmissionService object, used to validate, store and enqueue contest
   * submissions the same way as normal submissions.
   */
  @Autowired private SubmissionService submissionService;
}
