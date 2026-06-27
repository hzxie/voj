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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.verwandlung.voj.web.mapper.DiscussionReplyMapper;
import org.verwandlung.voj.web.mapper.DiscussionReplyVoteMapper;
import org.verwandlung.voj.web.mapper.DiscussionThreadMapper;
import org.verwandlung.voj.web.mapper.DiscussionTopicMapper;
import org.verwandlung.voj.web.mapper.OptionMapper;
import org.verwandlung.voj.web.mapper.ProblemMapper;
import org.verwandlung.voj.web.mapper.SubmissionMapper;
import org.verwandlung.voj.web.model.*;
import org.verwandlung.voj.web.util.HtmlTextFilter;
import org.verwandlung.voj.web.util.OffensiveWordFilter;

import java.util.*;

/**
 * The business logic layer of the discussion.
 *
 * @author Haozhe Xie
 */
@Service
@Transactional
public class DiscussionService {
  /** The option name of the number of reports at which a discussion thread is hidden. */
  private static final String OPTION_REPORT_HIDE_THRESHOLD = "discussionReportHideThreshold";

  /** The option name of the minimum number of solved problems required to vote. */
  private static final String OPTION_MIN_SOLVED_TO_VOTE = "discussionMinSolvedToVote";

  /** The option name of the minimum number of solved problems required to report. */
  private static final String OPTION_MIN_SOLVED_TO_REPORT = "discussionMinSolvedToReport";

  /**
   * Gets all discussion topics.
   *
   * @return a List object containing all discussion topics
   */
  public List<DiscussionTopic> getDiscussionTopics() {
    return discussionTopicMapper.getDiscussionTopics();
  }

  /**
   * Gets the hierarchical list of discussion topics.
   *
   * @return a Map object containing discussion topics and their inheritance relationships
   */
  public Map<DiscussionTopic, List<DiscussionTopic>> getDiscussionTopicsWithHierarchy() {
    List<DiscussionTopic> DiscussionTopics = getDiscussionTopics();
    Map<Integer, List<DiscussionTopic>> DiscussionTopicsIndexer = new HashMap<>();
    Map<DiscussionTopic, List<DiscussionTopic>> DiscussionTopicsHierarchy = new LinkedHashMap<>();

    // Add the top-level discussion topics (without a parent) to the list
    for (DiscussionTopic dt : DiscussionTopics) {
      if (dt.getParentDiscussionTopicId() == 0) {
        List<DiscussionTopic> subDiscussionTopics = new ArrayList<>();
        DiscussionTopicsHierarchy.put(dt, subDiscussionTopics);
        DiscussionTopicsIndexer.put(dt.getDiscussionTopicId(), subDiscussionTopics);
      }
    }
    // Add the remaining discussion topics to the list
    for (DiscussionTopic dt : DiscussionTopics) {
      int parentDiscussionTopicId = dt.getParentDiscussionTopicId();
      if (parentDiscussionTopicId != 0) {
        List<DiscussionTopic> subDiscussionTopics =
            DiscussionTopicsIndexer.get(parentDiscussionTopicId);
        if (subDiscussionTopics != null) {
          subDiscussionTopics.add(dt);
        }
      }
    }
    return DiscussionTopicsHierarchy;
  }

  /**
   * Gets the solution discussion of a problem.
   *
   * @param problemId - the unique identifier of the problem
   * @return the DiscussionThread object of the problem's solution discussion
   */
  public DiscussionThread getSolutionThreadOfProblem(long problemId) {
    return discussionThreadMapper.getSolutionThreadOfProblem(problemId);
  }

  /**
   * Gets all discussion threads under a problem.
   *
   * @param problemId - the unique identifier of the problem
   * @param offset - the cursor of the first thread
   * @param limit - the number of threads to fetch
   * @return a List object containing DiscussionThread objects
   */
  public List<DiscussionThread> getDiscussionThreadsOfProblem(
      long problemId, long offset, int limit) {
    return discussionThreadMapper.getDiscussionThreads(
        problemId, 0, offset, limit, getReportHideThreshold());
  }

  /**
   * Gets all discussion threads within a discussion topic.
   *
   * @param discussionTopicSlug - the unique English abbreviation of the discussion topic
   * @param offset - the cursor of the first thread
   * @param limit - the number of threads to fetch
   * @return a List object containing DiscussionThread objects
   */
  public List<DiscussionThread> getDiscussionThreadsOfTopic(
      String discussionTopicSlug, long offset, int limit) {
    int discussionTopicId = 0;
    if (discussionTopicSlug != null && !discussionTopicSlug.isEmpty()) {
      DiscussionTopic dt = discussionTopicMapper.getDiscussionTopicUsingSlug(discussionTopicSlug);
      discussionTopicId = dt.getDiscussionTopicId();
    }
    return discussionThreadMapper.getDiscussionThreads(
        0, discussionTopicId, offset, limit, getReportHideThreshold());
  }

  /**
   * Gets the replies of a discussion thread.
   *
   * @param discussionThreadId - the unique identifier of the discussion thread
   * @param currentUserUid - the unique identifier of the currently logged-in user (-1 means not
   *     logged in)
   * @param offset - the cursor of the first reply
   * @param limit - the number of replies to fetch
   * @return a List object containing the discussion thread replies
   */
  public List<DiscussionReply> getDiscussionRepliesOfThread(
      long discussionThreadId, long currentUserUid, long offset, int limit) {
    List<DiscussionReply> replies =
        discussionReplyMapper.getDiscussionRepliesUsingThreadId(discussionThreadId, offset, limit);
    for (DiscussionReply dr : replies) {
      // Filter offensive content in the reply
      String replyContent = dr.getDiscussionReplyContent();
      replyContent = offensiveWordFilter.filter(HtmlTextFilter.filter(replyContent));
      dr.setDiscussionReplyContent(replyContent);
      // Aggregate the voting information of the reply for the current user
      populateVoteStatistics(dr, currentUserUid);
    }
    return replies;
  }

  /**
   * Populates the aggregated voting information (vote counts and the current user's own vote) of a
   * discussion reply, reading directly from the discussion reply vote table.
   *
   * @param discussionReply - the discussion reply whose voting statistics are populated
   * @param currentUserUid - the unique identifier of the currently logged-in user (-1 means not
   *     logged in)
   */
  private void populateVoteStatistics(DiscussionReply discussionReply, long currentUserUid) {
    long discussionReplyId = discussionReply.getDiscussionReplyId();
    discussionReply.setNumberOfVoteUp(
        discussionReplyVoteMapper.getNumberOfVotes(
            discussionReplyId, DiscussionReplyVote.VOTE_UP));
    discussionReply.setNumberOfVoteDown(
        discussionReplyVoteMapper.getNumberOfVotes(
            discussionReplyId, DiscussionReplyVote.VOTE_DOWN));
    if (currentUserUid != -1) {
      Integer currentUserVote =
          discussionReplyVoteMapper.getVoteType(discussionReplyId, currentUserUid);
      discussionReply.setIsVotedUp(
          currentUserVote != null && currentUserVote == DiscussionReplyVote.VOTE_UP);
      discussionReply.setIsVotedDown(
          currentUserVote != null && currentUserVote == DiscussionReplyVote.VOTE_DOWN);
    }
  }

  /**
   * [For administrators only] Gets a page of all discussion threads (no topic/problem filter and no
   * report-based hiding), feeding the discussion admin list.
   *
   * @param offset - the cursor of the first thread
   * @param limit - the number of threads to fetch
   * @return a list of discussion threads
   */
  public List<DiscussionThread> getAllDiscussionThreads(long offset, int limit) {
    return discussionThreadMapper.getDiscussionThreads(0, 0, offset, limit, 0);
  }

  /**
   * [For administrators only] Gets the total number of discussion threads.
   *
   * @return the total number of discussion threads
   */
  public long getNumberOfDiscussionThreads() {
    return discussionThreadMapper.getNumberOfDiscussionThreads();
  }

  /**
   * Gets a discussion thread object by its unique identifier (no offensive-word filtering applied,
   * used by the administration moderation view).
   *
   * @param discussionThreadId - the unique identifier of the discussion thread
   * @return the corresponding discussion thread object, or a null reference
   */
  public DiscussionThread getDiscussionThreadUsingThreadId(long discussionThreadId) {
    return discussionThreadMapper.getDiscussionThreadUsingThreadId(discussionThreadId);
  }

  /**
   * Checks whether a discussion thread should be hidden because its replies have accumulated enough
   * reports. The threshold is read from the {@code discussionReportHideThreshold} option; a value of
   * zero or below disables hiding.
   *
   * @param discussionThreadId - the unique identifier of the discussion thread
   * @return whether the discussion thread should be hidden
   */
  public boolean isDiscussionThreadHidden(long discussionThreadId) {
    int threshold = getReportHideThreshold();
    if (threshold <= 0) {
      return false;
    }
    return discussionReplyVoteMapper.getNumberOfReportsOfThread(discussionThreadId) >= threshold;
  }

  /**
   * Gets a discussion reply object by its unique identifier.
   *
   * @param discussionReplyId - the unique identifier of the discussion reply
   * @return the corresponding discussion reply object, or a null reference
   */
  public DiscussionReply getDiscussionReplyUsingReplyId(long discussionReplyId) {
    return discussionReplyMapper.getDiscussionReplyUsingReplyId(discussionReplyId);
  }

  /**
   * [For administrators only] Gets the most-reported discussion replies, feeding the dashboard's
   * moderation queue and the discussion admin.
   *
   * @param limit - the maximum number of rows to return
   * @return an ordered list of reported-reply rows (most reports first)
   */
  public List<Map<String, Object>> getReportedReplies(int limit) {
    return discussionReplyVoteMapper.getReportedReplies(limit);
  }

  /**
   * [For administrators only] Gets the number of distinct replies that have at least one report.
   *
   * @return the number of reported replies
   */
  public int getNumberOfReportedReplies() {
    return discussionReplyVoteMapper.getNumberOfReportedReplies();
  }

  /**
   * Votes on a discussion reply.
   *
   * @param discussionThreadId - the unique identifier of the discussion thread
   * @param discussionReplyId - the unique identifier of the discussion reply
   * @param currentUser - the currently logged-in user
   * @param voteUp - the vote up status (+1 means the user upvoted the reply, -1 means the user
   *     cancelled the upvote, 0 means no action)
   * @param voteDown - the vote down status (+1 means the user downvoted the reply, -1 means the
   *     user cancelled the downvote, 0 means no action)
   * @return the voting result of the discussion reply
   */
  public Map<String, Boolean> voteDiscussionReply(
      long discussionThreadId,
      long discussionReplyId,
      User currentUser,
      int voteUp,
      int voteDown) {
    DiscussionReply discussionReply =
        discussionReplyMapper.getDiscussionReplyUsingReplyId(discussionReplyId);
    Map<String, Boolean> result = new HashMap<>();
    result.put(
        "isDiscussionReplyExists",
        discussionReply != null && discussionReply.getDiscussionThreadId() == discussionThreadId);
    result.put("isVoteValid", voteUp >= -1 && voteUp <= 1 && voteDown >= -1 && voteDown <= 1);
    result.put("isLoggedIn", currentUser != null);
    result.put("isNotSelfVote", !isOwnReply(discussionReply, currentUser));
    result.put(
        "isReputationEnough",
        hasEnoughReputation(currentUser, getIntOption(OPTION_MIN_SOLVED_TO_VOTE, 1)));

    boolean isSuccessful =
        result.get("isDiscussionReplyExists")
            && result.get("isVoteValid")
            && result.get("isLoggedIn")
            && result.get("isNotSelfVote")
            && result.get("isReputationEnough");
    result.put("isSuccessful", isSuccessful);

    if (result.get("isSuccessful")) {
      // Each vote is a row keyed by (reply, voter, vote_type). Casting an up/down vote first clears
      // the opposite vote so that a user holds at most one up/down vote per reply, then inserts the
      // new one (idempotent via the primary key). Report rows (vote_type = 2) live in the same table
      // and are untouched here, so a user's vote and report on the same reply are independent.
      long uid = currentUser.getUid();
      if (voteUp == 1) {
        discussionReplyVoteMapper.withdrawVote(discussionReplyId, uid, DiscussionReplyVote.VOTE_DOWN);
        discussionReplyVoteMapper.castVote(
            new DiscussionReplyVote(discussionReplyId, uid, DiscussionReplyVote.VOTE_UP));
      } else if (voteUp == -1) {
        discussionReplyVoteMapper.withdrawVote(discussionReplyId, uid, DiscussionReplyVote.VOTE_UP);
      }
      if (voteDown == 1) {
        discussionReplyVoteMapper.withdrawVote(discussionReplyId, uid, DiscussionReplyVote.VOTE_UP);
        discussionReplyVoteMapper.castVote(
            new DiscussionReplyVote(discussionReplyId, uid, DiscussionReplyVote.VOTE_DOWN));
      } else if (voteDown == -1) {
        discussionReplyVoteMapper.withdrawVote(
            discussionReplyId, uid, DiscussionReplyVote.VOTE_DOWN);
      }
    }
    return result;
  }

  /**
   * Reports a discussion reply for abuse. A report is stored as a row in the discussion reply vote
   * table with the report vote type, and is idempotent (a user reporting the same reply twice still
   * counts once). Once the replies of a thread accumulate enough reports the thread is hidden from
   * the listings (see {@link #isDiscussionThreadHidden(long)}).
   *
   * @param discussionThreadId - the unique identifier of the discussion thread
   * @param discussionReplyId - the unique identifier of the discussion reply being reported
   * @param currentUser - the currently logged-in user
   * @return the result of the report request
   */
  public Map<String, Boolean> reportDiscussionReply(
      long discussionThreadId, long discussionReplyId, User currentUser) {
    DiscussionReply discussionReply =
        discussionReplyMapper.getDiscussionReplyUsingReplyId(discussionReplyId);
    Map<String, Boolean> result = new HashMap<>();
    result.put(
        "isDiscussionReplyExists",
        discussionReply != null && discussionReply.getDiscussionThreadId() == discussionThreadId);
    result.put("isLoggedIn", currentUser != null);
    result.put("isNotSelfReport", !isOwnReply(discussionReply, currentUser));
    result.put(
        "isReputationEnough",
        hasEnoughReputation(currentUser, getIntOption(OPTION_MIN_SOLVED_TO_REPORT, 3)));

    boolean isSuccessful =
        result.get("isDiscussionReplyExists")
            && result.get("isLoggedIn")
            && result.get("isNotSelfReport")
            && result.get("isReputationEnough");
    result.put("isSuccessful", isSuccessful);

    if (result.get("isSuccessful")) {
      discussionReplyVoteMapper.castVote(
          new DiscussionReplyVote(
              discussionReplyId, currentUser.getUid(), DiscussionReplyVote.VOTE_REPORT));
    }
    return result;
  }

  /**
   * Checks whether a discussion reply was created by a given user (used to forbid voting on or
   * reporting one's own reply).
   *
   * @param discussionReply - the discussion reply (may be null)
   * @param user - the acting user (may be null)
   * @return whether the reply belongs to the user
   */
  private boolean isOwnReply(DiscussionReply discussionReply, User user) {
    return user != null
        && discussionReply != null
        && discussionReply.getDiscussionReplyCreator() != null
        && discussionReply.getDiscussionReplyCreator().getUid() == user.getUid();
  }

  /**
   * Checks whether a user has enough reputation to participate. Reputation is measured by the number
   * of distinct problems the user has solved; administrators always pass.
   *
   * @param user - the acting user (may be null)
   * @param minSolvedProblems - the minimum number of solved problems required
   * @return whether the user meets the reputation requirement
   */
  private boolean hasEnoughReputation(User user, int minSolvedProblems) {
    if (user == null) {
      return false;
    }
    if (minSolvedProblems <= 0 || isAdministrator(user)) {
      return true;
    }
    return submissionMapper.getAcceptedSubmissionUsingUserId(user.getUid()) >= minSolvedProblems;
  }

  /**
   * Checks whether a user belongs to the administrators user group.
   *
   * @param user - the user to check
   * @return whether the user is an administrator
   */
  private boolean isAdministrator(User user) {
    return user.getUserGroup() != null
        && "administrators".equals(user.getUserGroup().getUserGroupSlug());
  }

  /**
   * Reads the report-hide threshold option.
   *
   * @return the number of reports at which a thread is hidden (0 or below disables hiding)
   */
  private int getReportHideThreshold() {
    return getIntOption(OPTION_REPORT_HIDE_THRESHOLD, 0);
  }

  /**
   * Reads an integer-valued system option, falling back to a default value if the option is missing
   * or not a valid integer.
   *
   * @param optionName - the name of the option
   * @param defaultValue - the value to return when the option is absent or malformed
   * @return the option value as an integer
   */
  private int getIntOption(String optionName, int defaultValue) {
    Option option = optionMapper.getOption(optionName);
    if (option == null || option.getOptionValue() == null) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(option.getOptionValue().trim());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /**
   * [For administrators only] Creates a discussion topic.
   *
   * @param discussionTopicSlug - the unique English abbreviation of the discussion topic
   * @param discussionTopicName - the name of the discussion topic
   * @param parentDiscussionTopic - the parent discussion topic object (may be null)
   * @return a Map object containing the discussion topic creation result
   */
  public Map<String, Boolean> createDiscussionTopic(
      String discussionTopicSlug,
      String discussionTopicName,
      DiscussionTopic parentDiscussionTopic) {
    Map<String, Boolean> result = new HashMap<>(6, 1);
    result.put("isDiscussionTopicSlugEmpty", discussionTopicSlug.isEmpty());
    result.put("isDiscussionTopicSlugLegal", discussionTopicSlug.length() <= 128);
    result.put("isDiscussionTopicNameEmpty", discussionTopicName.isEmpty());
    result.put("isDiscussionTopicNameLegal", discussionTopicName.length() <= 128);

    boolean isSuccessful =
        !result.get("isDiscussionTopicSlugEmpty")
            && result.get("isDiscussionTopicSlugLegal")
            && !result.get("isDiscussionTopicNameEmpty")
            && result.get("isDiscussionTopicNameLegal");
    result.put("isSuccessful", isSuccessful);
    if (isSuccessful) {
      int parentDiscussionTopicId =
          parentDiscussionTopic == null ? 0 : parentDiscussionTopic.getParentDiscussionTopicId();
      DiscussionTopic dt =
          new DiscussionTopic(discussionTopicSlug, discussionTopicName, parentDiscussionTopicId);
      discussionTopicMapper.createDiscussionTopic(dt);
    }
    return result;
  }

  /**
   * [For administrators only] Edits a discussion topic.
   *
   * @param discussionTopicId - the unique identifier of the discussion topic
   * @param discussionTopicSlug - the unique English abbreviation of the discussion topic
   * @param discussionTopicName - the name of the discussion topic
   * @param parentDiscussionTopic - the parent discussion topic object (may be null)
   * @return a Map object containing the discussion topic editing result
   */
  public Map<String, Boolean> updateDiscussionTopic(
      int discussionTopicId,
      String discussionTopicSlug,
      String discussionTopicName,
      DiscussionTopic parentDiscussionTopic) {
    DiscussionTopic dt = discussionTopicMapper.getDiscussionTopicUsingId(discussionTopicId);
    Map<String, Boolean> result = new HashMap<>(7, 1);
    result.put("isDiscussionTopicExists", dt != null);
    result.put("isDiscussionTopicSlugEmpty", discussionTopicSlug.isEmpty());
    result.put("isDiscussionTopicSlugLegal", discussionTopicSlug.length() <= 128);
    result.put("isDiscussionTopicNameEmpty", discussionTopicName.isEmpty());
    result.put("isDiscussionTopicNameLegal", discussionTopicName.length() <= 128);

    boolean isSuccessful =
        result.get("isDiscussionTopicExists")
            && !result.get("isDiscussionTopicSlugEmpty")
            && result.get("isDiscussionTopicSlugLegal")
            && !result.get("isDiscussionTopicNameEmpty")
            && result.get("isDiscussionTopicNameLegal");
    result.put("isSuccessful", isSuccessful);
    if (isSuccessful) {
      int parentDiscussionTopicId =
          parentDiscussionTopic == null ? 0 : parentDiscussionTopic.getParentDiscussionTopicId();
      dt.setDiscussionTopicSlug(discussionTopicSlug);
      dt.setDiscussionTopicName(discussionTopicName);
      dt.setParentDiscussionTopicId(parentDiscussionTopicId);
      discussionTopicMapper.updateDiscussionTopic(dt);
    }
    return result;
  }

  /**
   * [For administrators only] Deletes a discussion topic.
   *
   * @param discussionTopicId - the unique identifier of the discussion topic
   * @return a Map object containing the discussion topic deletion result
   */
  public Map<String, Boolean> deleteDiscussionTopic(int discussionTopicId) {
    Map<String, Boolean> result = new HashMap<>(2, 1);
    long numberOfRowsAffected =
        discussionTopicMapper.deleteDiscussionTopicUsingId(discussionTopicId);
    result.put("isSuccessful", numberOfRowsAffected > 0);
    return result;
  }

  /**
   * Creates a discussion thread.
   *
   * @param threadCreator - the creator of the discussion thread
   * @param discussionTopicSlug - the unique English abbreviation of the thread's topic
   * @param relatedProblemId - the problem the thread is related to (may be null)
   * @param discussionThreadTitle - the title of the discussion thread
   * @param discussionThreadContent - the content of the first reply in the discussion thread
   * @return a Map object containing the discussion thread creation result
   */
  public Map<String, Object> createDiscussionThread(
      User threadCreator,
      String discussionTopicSlug,
      long relatedProblemId,
      String discussionThreadTitle,
      String discussionThreadContent) {
    DiscussionTopic discussionTopic =
        discussionTopicMapper.getDiscussionTopicUsingSlug(discussionTopicSlug);
    DiscussionThread dt =
        new DiscussionThread(
            threadCreator, discussionTopic, null, HtmlTextFilter.filter(discussionThreadTitle));
    Map<String, Object> result =
        (Map<String, Object>) getDiscussionThreadCreationResult(dt, discussionThreadContent);

    // The related problem is optional: a non-positive id means "no problem linked". When an id
    // is supplied it must resolve to an existing problem, otherwise the thread is rejected rather
    // than silently created with no problem attached.
    Problem relatedProblem = null;
    boolean isRelatedProblemExists = true;
    if (relatedProblemId > 0) {
      relatedProblem = problemMapper.getProblem(relatedProblemId);
      isRelatedProblemExists = relatedProblem != null;
    }
    result.put("isRelatedProblemExists", isRelatedProblemExists);

    boolean isSuccessful = (Boolean) result.get("isSuccessful") && isRelatedProblemExists;
    result.put("isSuccessful", isSuccessful);
    if (isSuccessful) {
      if (relatedProblem != null) {
        dt.setProblem(relatedProblem);
      }
      discussionThreadMapper.createDiscussionThread(dt);
      createDiscussionReply(dt.getDiscussionThreadId(), threadCreator, discussionThreadContent);
      result.put("discussionThreadId", dt.getDiscussionThreadId());
    }
    return result;
  }

  /**
   * Gets the creation result of a discussion thread.
   *
   * @param dt - the discussion thread to create
   * @param discussionThreadContent - the content of the thread to create (stored as a
   *     DiscussionReply object)
   * @return a Map<String, Boolean> object containing the discussion thread creation result
   */
  private Map<String, ? extends Object> getDiscussionThreadCreationResult(
      DiscussionThread dt, String discussionThreadContent) {
    Map<String, Boolean> result = new HashMap<>(9, 1);
    result.put("isThreadCreatorExists", dt.getDiscussionThreadCreator() != null);
    result.put(
        "isThreadCreatorLegal",
        dt.getDiscussionThreadCreator() != null
            && !dt.getDiscussionThreadCreator()
                .getUserGroup()
                .getUserGroupSlug()
                .equals("forbidden"));
    result.put("isDiscussionTopicExists", dt.getDiscussionTopic() != null);
    result.put("isThreadTitleEmpty", dt.getDiscussionThreadTitle().isEmpty());
    result.put("isThreadTitleLegal", dt.getDiscussionThreadTitle().length() <= 128);
    result.put("isThreadContentEmpty", discussionThreadContent.isEmpty());

    boolean isSuccessful =
        result.get("isThreadCreatorExists")
            && result.get("isThreadCreatorLegal")
            && result.get("isDiscussionTopicExists")
            && !result.get("isThreadTitleEmpty")
            && result.get("isThreadTitleLegal")
            && !result.get("isThreadContentEmpty");
    result.put("isSuccessful", isSuccessful);
    return result;
  }

  /**
   * Edits a discussion thread. Editing condition: the current user is an administrator or the thread
   * was created by the user themselves.
   *
   * @param discussionThreadId - the unique identifier of the discussion thread
   * @param currentEditor - the current editor
   * @param discussionTopicSlug - the unique English abbreviation of the thread's topic
   * @param discussionThreadTitle - the title of the discussion thread
   * @return a Map object containing the discussion thread editing result
   */
  public Map<String, Boolean> editDiscussionThread(
      long discussionThreadId,
      User currentEditor,
      String discussionTopicSlug,
      String discussionThreadTitle) {
    DiscussionThread dt =
        discussionThreadMapper.getDiscussionThreadUsingThreadId(discussionThreadId);
    Map<String, Boolean> result = new HashMap<>(7, 1);
    result.put("isDiscussionThreadExists", dt != null);
    result.put("isThreadTitleEmpty", discussionThreadTitle.isEmpty());
    result.put("isThreadTitleLegal", discussionThreadTitle.length() <= 128);

    DiscussionTopic discussionTopic =
        discussionTopicMapper.getDiscussionTopicUsingSlug(discussionTopicSlug);
    result.put("isDiscussionTopicExists", discussionTopic != null);

    boolean isSuccessful =
        result.get("isDiscussionThreadExists")
            && result.get("isDiscussionTopicExists")
            && !result.get("isThreadTitleEmpty")
            && result.get("isThreadTitleLegal");
    result.put("isSuccessful", isSuccessful);
    if (isSuccessful) {
      if (dt.getDiscussionThreadCreator().equals(currentEditor)
          || currentEditor.getUserGroup().getUserGroupSlug().equals("administrators")) {
        dt.setDiscussionTopic(discussionTopic);
        dt.setDiscussionThreadTitle(HtmlTextFilter.filter(discussionThreadTitle));
        discussionThreadMapper.updateDiscussionThread(dt);
      }
    }
    return result;
  }

  /**
   * [For administrators only] Deletes a discussion thread.
   *
   * @param discussionThreadId - the unique identifier of the discussion thread
   * @return the discussion thread deletion result
   */
  public Map<String, Boolean> deleteDiscussionThread(long discussionThreadId) {
    Map<String, Boolean> result = new HashMap<>(2, 1);
    long numberOfRowsAffected =
        discussionThreadMapper.deleteDiscussionThreadUsingThreadId(discussionThreadId);

    result.put("isSuccessful", numberOfRowsAffected > 0);
    return result;
  }

  /**
   * Creates a discussion reply.
   *
   * @param discussionThreadId - the unique identifier of the discussion thread the reply belongs to
   * @param replyCreator - the creator of the reply
   * @param replyContent - the content of the reply
   * @return a Map object containing the discussion reply creation result
   */
  public Map<String, Object> createDiscussionReply(
      long discussionThreadId, User replyCreator, String replyContent) {
    DiscussionReply dr =
        new DiscussionReply(
            discussionThreadId, replyCreator, HtmlTextFilter.filter(replyContent));

    Map<String, Object> result =
        (Map<String, Object>) getDiscussionReplyCreationResult(dr);
    if ((Boolean) result.get("isSuccessful")) {
      discussionReplyMapper.createDiscussionReply(dr);
      result.put("discussionReply", dr);
    }
    return result;
  }

  /**
   * Validates the discussion reply data.
   *
   * @param discussionReply - the discussion reply object to create
   * @return a Map object containing the validity of the discussion reply data
   */
  private Map<String, ? extends Object> getDiscussionReplyCreationResult(
      DiscussionReply discussionReply) {
    long discussionThreadId = discussionReply.getDiscussionThreadId();
    User replyCreator = discussionReply.getDiscussionReplyCreator();
    String replyContent = discussionReply.getDiscussionReplyContent();

    DiscussionThread discussionThread =
        discussionThreadMapper.getDiscussionThreadUsingThreadId(discussionThreadId);
    Map<String, Boolean> result = new HashMap<>(6, 1);
    result.put("isDiscussionThreadExists", discussionThread != null);
    result.put("isReplyCreatorExists", replyCreator != null);
    result.put(
        "isReplyCreatorLegal",
        replyCreator != null
            && !replyCreator.getUserGroup().getUserGroupSlug().equals("forbidden"));
    result.put("isReplyContentEmpty", replyContent.isEmpty());

    boolean isSuccessful =
        result.get("isDiscussionThreadExists")
            && result.get("isReplyCreatorExists")
            && result.get("isReplyCreatorLegal")
            && !result.get("isReplyContentEmpty");
    result.put("isSuccessful", isSuccessful);
    return result;
  }

  /**
   * Edits a discussion reply. Editing condition: the current user is an administrator or the reply
   * was created by the user themselves.
   *
   * @param discussionReplyId - the unique identifier of the discussion reply
   * @param currentEditor - the current editor
   * @param discussionReplyContent - the updated content of the discussion reply
   * @return a Map object containing the discussion reply editing result
   */
  public Map<String, Boolean> editDiscussionReply(
      long discussionReplyId, User currentEditor, String discussionReplyContent) {
    Map<String, Boolean> result = new HashMap<>(2, 1);
    boolean isSuccessful = false;
    DiscussionReply dr = discussionReplyMapper.getDiscussionReplyUsingReplyId(discussionReplyId);

    if (dr != null) {
      if (dr.getDiscussionReplyCreator().equals(currentEditor)
          || currentEditor.getUserGroup().getUserGroupSlug().equals("administrators")) {
        dr.setDiscussionReplyContent(HtmlTextFilter.filter(discussionReplyContent));
        discussionReplyMapper.updateDiscussionReply(dr);
        isSuccessful = true;
      }
    }
    result.put("isSuccessful", isSuccessful);
    return result;
  }

  /**
   * Deletes a discussion reply. Deletion condition: the current user is an administrator or the
   * reply was created by the user themselves.
   *
   * @param discussionReplyId - the unique identifier of the discussion reply
   * @param currentEditor - the current editor
   * @return a Map object containing the discussion reply deletion result
   */
  public Map<String, Boolean> deleteDiscussionReply(long discussionReplyId, User currentEditor) {
    Map<String, Boolean> result = new HashMap<>(2, 1);
    boolean isSuccessful = false;
    DiscussionReply dr = discussionReplyMapper.getDiscussionReplyUsingReplyId(discussionReplyId);

    if (dr != null) {
      if (dr.getDiscussionReplyCreator().equals(currentEditor)
          || currentEditor.getUserGroup().getUserGroupSlug().equals("administrators")) {
        discussionReplyMapper.deleteDiscussionReplyUsingReplyId(discussionReplyId);
        isSuccessful = true;
      }
    }
    result.put("isSuccessful", isSuccessful);
    return result;
  }

  /** The autowired DiscussionTopicMapper object, used to obtain discussion topics. */
  @Autowired private DiscussionTopicMapper discussionTopicMapper;

  /** The autowired DiscussionThreadMapper object, used to obtain discussion threads. */
  @Autowired private DiscussionThreadMapper discussionThreadMapper;

  /** The autowired DiscussionReplyMapper object, used to obtain discussion replies. */
  @Autowired private DiscussionReplyMapper discussionReplyMapper;

  /** The autowired DiscussionReplyVoteMapper object, used to obtain and persist reply votes. */
  @Autowired private DiscussionReplyVoteMapper discussionReplyVoteMapper;

  /** The autowired OptionMapper object, used to read the configurable discussion thresholds. */
  @Autowired private OptionMapper optionMapper;

  /** The autowired SubmissionMapper object, used to measure a user's reputation (solved problems). */
  @Autowired private SubmissionMapper submissionMapper;

  /** The autowired ProblemMapper object, used to obtain problems. */
  @Autowired private ProblemMapper problemMapper;

  /** The autowired OffensiveWordFilter object, used to filter offensive words in user content. */
  @Autowired private OffensiveWordFilter offensiveWordFilter;
}
