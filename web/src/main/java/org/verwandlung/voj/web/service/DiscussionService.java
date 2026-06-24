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
import org.verwandlung.voj.web.mapper.DiscussionReplyMapper;
import org.verwandlung.voj.web.mapper.DiscussionThreadMapper;
import org.verwandlung.voj.web.mapper.DiscussionTopicMapper;
import org.verwandlung.voj.web.mapper.ProblemMapper;
import org.verwandlung.voj.web.model.*;
import org.verwandlung.voj.web.util.HtmlTextFilter;
import org.verwandlung.voj.web.util.JsonUtils;
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
    return discussionThreadMapper.getDiscussionThreads(problemId, 0, offset, limit);
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
    return discussionThreadMapper.getDiscussionThreads(0, discussionTopicId, offset, limit);
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
      // Get the voting information in the reply
      Map<String, Object> votesStatistics =
          getVoteStatisticsOfDiscussionReply(dr.getDiscussionReplyVotes(), currentUserUid);
      dr.setDiscussionReplyVotes(JsonUtils.toJsonString(votesStatistics));
    }
    return replies;
  }

  /**
   * Aggregates the voting information in a discussion reply.
   *
   * @param votes - the JSON-formatted string of the original voting information in the discussion
   *     reply
   * @param currentUserUid - the unique identifier of the currently logged-in user (-1 means not
   *     logged in)
   * @return a Map object containing the discussion reply voting information
   */
  private Map<String, Object> getVoteStatisticsOfDiscussionReply(
      String votes, long currentUserUid) {
    Map<String, Object> votesStatistics = new HashMap<>(5, 1);
    Map<String, List<Long>> voteUsers = parseVoteUsers(votes);

    List<Long> voteUpUsers = voteUsers.get("up");
    List<Long> voteDownUsers = voteUsers.get("down");
    boolean isVotedUp = currentUserUid != -1 && voteUpUsers.contains(currentUserUid);
    boolean isVotedDown = currentUserUid != -1 && voteDownUsers.contains(currentUserUid);

    votesStatistics.put("isVotedUp", isVotedUp);
    votesStatistics.put("isVotedDown", isVotedDown);
    votesStatistics.put("numberOfVoteUp", voteUpUsers.size());
    votesStatistics.put("numberOfVoteDown", voteDownUsers.size());
    return votesStatistics;
  }

  /**
   * Parses the JSON string of the discussion reply voting information into voting user lists. The
   * voting information has the format {"up": [uid, ...], "down": [uid, ...]}.
   *
   * @param votes - the JSON-formatted string of the discussion reply voting information
   * @return a Map object containing the lists of voting user UIDs under the "up" and "down" keys
   */
  private Map<String, List<Long>> parseVoteUsers(String votes) {
    return JsonUtils.toObject(votes, new TypeReference<Map<String, List<Long>>>() {});
  }

  /**
   * Gets a discussion thread object by its unique identifier.
   *
   * @param discussionThreadId - the unique identifier of the discussion thread
   * @return the corresponding discussion thread object, or a null reference
   */
  public DiscussionThread getDiscussionThreadUsingThreadId(long discussionThreadId) {
    return discussionThreadMapper.getDiscussionThreadUsingThreadId(discussionThreadId);
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
   * Votes on a discussion reply.
   *
   * @param discussionThreadId - the unique identifier of the discussion thread
   * @param discussionReplyId - the unique identifier of the discussion reply
   * @param currentUser - the currently logged-in user
   * @param voteUp - the vote up status (+1 means the user upvoted the reply, -1 means the user
   *     cancelled the upvote, 0 means no action)
   * @param voteDown - the vote down status (+1 means the user downvoted the reply, -1 means the
   *     user cancelled the downvote, 0 means no action)
   * @param isCsrfTokenValid - whether the CSRF token is valid
   * @return the voting result of the discussion reply
   */
  public Map<String, Boolean> voteDiscussionReply(
      long discussionThreadId,
      long discussionReplyId,
      User currentUser,
      int voteUp,
      int voteDown,
      boolean isCsrfTokenValid) {
    DiscussionReply discussionReply =
        discussionReplyMapper.getDiscussionReplyUsingReplyId(discussionReplyId);
    Map<String, Boolean> result = new HashMap<>();
    result.put(
        "isDiscussionReplyExists",
        discussionReply != null && discussionReply.getDiscussionThreadId() == discussionThreadId);
    result.put("isVoteValid", voteUp >= -1 && voteUp <= 1 && voteDown >= -1 && voteDown <= 1);
    result.put("isCsrfTokenValid", isCsrfTokenValid);
    result.put("isLoggedIn", currentUser != null);

    boolean isSuccessful =
        result.get("isDiscussionReplyExists")
            && result.get("isVoteValid")
            && result.get("isCsrfTokenValid")
            && result.get("isLoggedIn");
    result.put("isSuccessful", isSuccessful);

    if (result.get("isSuccessful")) {
      synchronized (this) {
        // Set the new voting result
        Map<String, List<Long>> voteUsers =
            parseVoteUsers(discussionReply.getDiscussionReplyVotes());
        List<Long> voteUpUsers = voteUsers.get("up");
        List<Long> voteDownUsers = voteUsers.get("down");
        boolean isVotedUp = voteUpUsers.contains(currentUser.getUid());
        boolean isVotedDown = voteDownUsers.contains(currentUser.getUid());

        if (voteUp == 1 && !isVotedUp) {
          if (isVotedDown) voteDownUsers.remove(currentUser.getUid());
          voteUpUsers.add(currentUser.getUid());
        } else if (voteUp == -1) {
          voteUpUsers.remove(currentUser.getUid());
        }
        if (voteDown == 1 && !isVotedDown) {
          if (isVotedUp) voteUpUsers.remove(currentUser.getUid());
          voteDownUsers.add(currentUser.getUid());
        } else if (voteDown == -1) {
          voteDownUsers.remove(currentUser.getUid());
        }
        discussionReply.setDiscussionReplyVotes(JsonUtils.toJsonString(voteUsers));
        discussionReplyMapper.updateDiscussionReply(discussionReply);
      }
    }
    return result;
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
   * @param isCsrfTokenValid - whether the CSRF token is valid
   * @return a Map object containing the discussion thread creation result
   */
  public Map<String, Object> createDiscussionThread(
      User threadCreator,
      String discussionTopicSlug,
      long relatedProblemId,
      String discussionThreadTitle,
      String discussionThreadContent,
      boolean isCsrfTokenValid) {
    DiscussionTopic discussionTopic =
        discussionTopicMapper.getDiscussionTopicUsingSlug(discussionTopicSlug);
    DiscussionThread dt =
        new DiscussionThread(
            threadCreator, discussionTopic, null, HtmlTextFilter.filter(discussionThreadTitle));
    Map<String, Object> result =
        (Map<String, Object>)
            getDiscussionThreadCreationResult(dt, discussionThreadContent, isCsrfTokenValid);
    if ((Boolean) result.get("isSuccessful")) {
      if (relatedProblemId != 0) {
        Problem relatedProblem = problemMapper.getProblem(relatedProblemId);
        dt.setProblem(relatedProblem);
      }
      discussionThreadMapper.createDiscussionThread(dt);
      createDiscussionReply(
          dt.getDiscussionThreadId(), threadCreator, discussionThreadContent, isCsrfTokenValid);
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
   * @param isCsrfTokenValid - whether the CSRF token is valid
   * @return a Map<String, Boolean> object containing the discussion thread creation result
   */
  private Map<String, ? extends Object> getDiscussionThreadCreationResult(
      DiscussionThread dt, String discussionThreadContent, boolean isCsrfTokenValid) {
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
    result.put("isCsrfTokenValid", isCsrfTokenValid);

    boolean isSuccessful =
        result.get("isThreadCreatorExists")
            && result.get("isThreadCreatorLegal")
            && result.get("isDiscussionTopicExists")
            && !result.get("isThreadTitleEmpty")
            && result.get("isThreadTitleLegal")
            && !result.get("isThreadContentEmpty")
            && result.get("isCsrfTokenValid");
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
   * @param isCsrfTokenValid - whether the CSRF token is valid
   * @return a Map object containing the discussion thread editing result
   */
  public Map<String, Boolean> editDiscussionThread(
      long discussionThreadId,
      User currentEditor,
      String discussionTopicSlug,
      String discussionThreadTitle,
      boolean isCsrfTokenValid) {
    DiscussionTopic discussionTopic = null;
    DiscussionThread dt =
        discussionThreadMapper.getDiscussionThreadUsingThreadId(discussionThreadId);
    ;
    Map<String, Boolean> result = new HashMap<>(7, 1);
    result.put("isDiscussionThreadExists", dt != null);
    result.put("isThreadTitleEmpty", discussionThreadTitle.isEmpty());
    result.put("isThreadTitleLegal", discussionThreadTitle.length() <= 128);
    result.put("isCsrfTokenValid", isCsrfTokenValid);
    result.put("isDiscussionTopicExists", false);

    if (isCsrfTokenValid) {
      discussionTopic = discussionTopicMapper.getDiscussionTopicUsingSlug(discussionTopicSlug);
      result.put("isDiscussionTopicExists", discussionTopic != null);
    }
    boolean isSuccessful =
        result.get("isDiscussionThreadExists")
            && result.get("isDiscussionTopicExists")
            && !result.get("isThreadTitleEmpty")
            && result.get("isThreadTitleLegal")
            && result.get("isCsrfTokenValid");
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
   * @param isCsrfTokenValid - whether the CSRF token is valid
   * @return a Map object containing the discussion reply creation result
   */
  public Map<String, Object> createDiscussionReply(
      long discussionThreadId, User replyCreator, String replyContent, boolean isCsrfTokenValid) {
    String discussionReplyVotes = "{ \"up\": [], \"down\": [] }";
    DiscussionReply dr =
        new DiscussionReply(
            discussionThreadId,
            replyCreator,
            HtmlTextFilter.filter(replyContent),
            discussionReplyVotes);

    Map<String, Object> result =
        (Map<String, Object>) getDiscussionReplyCreationResult(dr, isCsrfTokenValid);
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
   * @param isCsrfTokenValid - whether the CSRF token is valid
   * @return a Map object containing the validity of the discussion reply data
   */
  private Map<String, ? extends Object> getDiscussionReplyCreationResult(
      DiscussionReply discussionReply, boolean isCsrfTokenValid) {
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
    result.put("isCsrfTokenValid", isCsrfTokenValid);

    boolean isSuccessful =
        result.get("isDiscussionThreadExists")
            && result.get("isReplyCreatorExists")
            && result.get("isReplyCreatorLegal")
            && !result.get("isReplyContentEmpty")
            && result.get("isCsrfTokenValid");
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
   * @param isCsrfTokenValid - whether the CSRF token is valid
   * @return a Map object containing the discussion reply editing result
   */
  public Map<String, Boolean> editDiscussionReply(
      long discussionReplyId,
      User currentEditor,
      String discussionReplyContent,
      boolean isCsrfTokenValid) {
    Map<String, Boolean> result = new HashMap<>(2, 1);
    boolean isSuccessful = false;
    DiscussionReply dr = null;

    if (isCsrfTokenValid) {
      dr = discussionReplyMapper.getDiscussionReplyUsingReplyId(discussionReplyId);
    }
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
   * @param isCsrfTokenValid - whether the CSRF token is valid
   * @return a Map object containing the discussion reply deletion result
   */
  public Map<String, Boolean> deleteDiscussionReply(
      long discussionReplyId, User currentEditor, boolean isCsrfTokenValid) {
    Map<String, Boolean> result = new HashMap<>(2, 1);
    boolean isSuccessful = false;
    DiscussionReply dr = null;

    if (isCsrfTokenValid) {
      dr = discussionReplyMapper.getDiscussionReplyUsingReplyId(discussionReplyId);
    }
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

  /** The autowired ProblemMapper object, used to obtain problems. */
  @Autowired private ProblemMapper problemMapper;

  /** The autowired OffensiveWordFilter object, used to filter offensive words in user content. */
  @Autowired private OffensiveWordFilter offensiveWordFilter;
}
