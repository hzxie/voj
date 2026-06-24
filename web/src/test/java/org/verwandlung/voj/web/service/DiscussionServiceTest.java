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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.mapper.UserMapper;
import org.verwandlung.voj.web.model.DiscussionReply;
import org.verwandlung.voj.web.model.DiscussionThread;
import org.verwandlung.voj.web.model.DiscussionTopic;
import org.verwandlung.voj.web.model.User;

/**
 * DiscussionService测试类.
 *
 * <p>种子数据: 4个讨论主题 (均为顶级), 3个讨论帖子 (帖子1/2关联试题1000), 3条回复. 用户1000为管理员, 1001为评测机, 1002为普通用户.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class DiscussionServiceTest {
  /** 测试用例: 测试getDiscussionTopics()方法 测试数据: N/a 预期结果: 返回全部讨论主题 */
  @Test
  public void testGetDiscussionTopics() {
    List<DiscussionTopic> topics = discussionService.getDiscussionTopics();
    Assertions.assertEquals(4, topics.size());
  }

  /** 测试用例: 测试getDiscussionTopicsWithHierarchy()方法 测试数据: N/a 预期结果: 返回顶级主题及其(空的)子主题列表 */
  @Test
  public void testGetDiscussionTopicsWithHierarchy() {
    Map<DiscussionTopic, List<DiscussionTopic>> hierarchy =
        discussionService.getDiscussionTopicsWithHierarchy();
    Assertions.assertEquals(4, hierarchy.size());
    for (List<DiscussionTopic> children : hierarchy.values()) {
      Assertions.assertTrue(children.isEmpty());
    }
  }

  /** 测试用例: 测试getSolutionThreadOfProblem(long)方法 测试数据: 存在题解的试题 预期结果: 返回对应的题解讨论帖 */
  @Test
  public void testGetSolutionThreadOfProblemExists() {
    DiscussionThread thread = discussionService.getSolutionThreadOfProblem(1000);
    Assertions.assertNotNull(thread);
    Assertions.assertEquals(1, thread.getDiscussionThreadId());
  }

  /** 测试用例: 测试getSolutionThreadOfProblem(long)方法 测试数据: 不存在题解的试题 预期结果: 返回空引用 */
  @Test
  public void testGetSolutionThreadOfProblemNotExists() {
    Assertions.assertNull(discussionService.getSolutionThreadOfProblem(99999));
  }

  /** 测试用例: 测试getDiscussionThreadsOfProblem(long, long, int)方法 测试数据: 试题1000 预期结果: 返回关联该试题的讨论帖 */
  @Test
  public void testGetDiscussionThreadsOfProblem() {
    List<DiscussionThread> threads = discussionService.getDiscussionThreadsOfProblem(1000, 0, 10);
    Assertions.assertEquals(2, threads.size());
  }

  /** 测试用例: 测试getDiscussionThreadsOfTopic(String, long, int)方法 测试数据: 主题solutions 预期结果: 返回该主题下的讨论帖 */
  @Test
  public void testGetDiscussionThreadsOfTopic() {
    List<DiscussionThread> threads = discussionService.getDiscussionThreadsOfTopic("solutions", 0, 10);
    Assertions.assertEquals(2, threads.size());
  }

  /** 测试用例: 测试getDiscussionThreadsOfTopic(String, long, int)方法 测试数据: 空主题缩写 预期结果: 返回全部讨论帖 */
  @Test
  public void testGetDiscussionThreadsOfTopicWithEmptySlug() {
    List<DiscussionThread> threads = discussionService.getDiscussionThreadsOfTopic("", 0, 10);
    Assertions.assertEquals(3, threads.size());
  }

  /** 测试用例: 测试getDiscussionRepliesOfThread(...)方法 测试数据: 讨论帖2 预期结果: 返回该帖子的全部回复 */
  @Test
  public void testGetDiscussionRepliesOfThread() {
    List<DiscussionReply> replies = discussionService.getDiscussionRepliesOfThread(2, 1000, 0, 10);
    Assertions.assertEquals(2, replies.size());
    // 投票信息已被替换为统计结果的JSON字符串.
    Assertions.assertTrue(replies.get(0).getDiscussionReplyVotes().contains("numberOfVoteUp"));
  }

  /** 测试用例: 测试getDiscussionThreadUsingThreadId(long)方法 测试数据: 存在/不存在的帖子标识符 预期结果: 返回帖子或空引用 */
  @Test
  public void testGetDiscussionThreadUsingThreadId() {
    Assertions.assertNotNull(discussionService.getDiscussionThreadUsingThreadId(1));
    Assertions.assertNull(discussionService.getDiscussionThreadUsingThreadId(99999));
  }

  /** 测试用例: 测试getDiscussionReplyUsingReplyId(long)方法 测试数据: 存在/不存在的回复标识符 预期结果: 返回回复或空引用 */
  @Test
  public void testGetDiscussionReplyUsingReplyId() {
    Assertions.assertNotNull(discussionService.getDiscussionReplyUsingReplyId(1));
    Assertions.assertNull(discussionService.getDiscussionReplyUsingReplyId(99999));
  }

  /** 测试用例: 测试voteDiscussionReply(...)方法 测试数据: 合法的点赞操作 预期结果: 投票成功并持久化 */
  @Test
  public void testVoteDiscussionReplySuccessfully() {
    // 回复3属于帖子2, 初始无投票. 用户1002对其点赞.
    Map<String, Boolean> result =
        discussionService.voteDiscussionReply(2, 3, userWithUid(1002), 1, 0, true);
    Assertions.assertTrue(result.get("isSuccessful"));

    DiscussionReply reply = discussionService.getDiscussionReplyUsingReplyId(3);
    Assertions.assertTrue(reply.getDiscussionReplyVotes().contains("1002"));
  }

  /** 测试用例: 测试voteDiscussionReply(...)方法 测试数据: 回复与帖子不匹配 预期结果: 投票失败 */
  @Test
  public void testVoteDiscussionReplyWithMismatchedThread() {
    // 回复1属于帖子1, 此处传入帖子2.
    Map<String, Boolean> result =
        discussionService.voteDiscussionReply(2, 1, userWithUid(1002), 1, 0, true);
    Assertions.assertFalse(result.get("isDiscussionReplyExists"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试voteDiscussionReply(...)方法 测试数据: 非法的投票值 预期结果: 投票失败 */
  @Test
  public void testVoteDiscussionReplyWithInvalidVote() {
    Map<String, Boolean> result =
        discussionService.voteDiscussionReply(2, 3, userWithUid(1002), 2, 0, true);
    Assertions.assertFalse(result.get("isVoteValid"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试voteDiscussionReply(...)方法 测试数据: 用户未登录 预期结果: 投票失败 */
  @Test
  public void testVoteDiscussionReplyWithoutLogin() {
    Map<String, Boolean> result = discussionService.voteDiscussionReply(2, 3, null, 1, 0, true);
    Assertions.assertFalse(result.get("isLoggedIn"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试createDiscussionTopic(...)方法 测试数据: 合法的主题 预期结果: 创建成功, 主题数加一 */
  @Test
  public void testCreateDiscussionTopicSuccessfully() {
    Map<String, Boolean> result =
        discussionService.createDiscussionTopic("feedback", "Feedback", null);
    Assertions.assertTrue(result.get("isSuccessful"));
    Assertions.assertEquals(5, discussionService.getDiscussionTopics().size());
  }

  /** 测试用例: 测试createDiscussionTopic(...)方法 测试数据: 主题缩写为空 预期结果: 创建失败 */
  @Test
  public void testCreateDiscussionTopicWithEmptySlug() {
    Map<String, Boolean> result = discussionService.createDiscussionTopic("", "Feedback", null);
    Assertions.assertTrue(result.get("isDiscussionTopicSlugEmpty"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试updateDiscussionTopic(...)方法 测试数据: 存在的主题 预期结果: 更新成功 */
  @Test
  public void testUpdateDiscussionTopicSuccessfully() {
    Map<String, Boolean> result =
        discussionService.updateDiscussionTopic(3, "general", "General Discussion", null);
    Assertions.assertTrue(result.get("isSuccessful"));
  }

  /** 测试用例: 测试updateDiscussionTopic(...)方法 测试数据: 不存在的主题 预期结果: 更新失败 */
  @Test
  public void testUpdateDiscussionTopicNotExists() {
    Map<String, Boolean> result =
        discussionService.updateDiscussionTopic(999, "ghost", "Ghost", null);
    Assertions.assertFalse(result.get("isDiscussionTopicExists"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试deleteDiscussionTopic(int)方法 测试数据: 存在且未被引用的主题 预期结果: 删除成功 */
  @Test
  public void testDeleteDiscussionTopicSuccessfully() {
    // 主题4 (support) 未被任何讨论帖引用.
    Map<String, Boolean> result = discussionService.deleteDiscussionTopic(4);
    Assertions.assertTrue(result.get("isSuccessful"));
    Assertions.assertEquals(3, discussionService.getDiscussionTopics().size());
  }

  /** 测试用例: 测试deleteDiscussionTopic(int)方法 测试数据: 不存在的主题 预期结果: 删除失败 */
  @Test
  public void testDeleteDiscussionTopicNotExists() {
    Map<String, Boolean> result = discussionService.deleteDiscussionTopic(999);
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试createDiscussionThread(...)方法 测试数据: 合法的讨论帖 预期结果: 创建成功并返回帖子标识符 */
  @Test
  public void testCreateDiscussionThreadSuccessfully() {
    Map<String, Object> result =
        discussionService.createDiscussionThread(
            adminUser(), "qa", 0, "How to solve this?", "Any hints?", true);
    Assertions.assertTrue((Boolean) result.get("isSuccessful"));
    Assertions.assertTrue(result.containsKey("discussionThreadId"));
  }

  /** 测试用例: 测试createDiscussionThread(...)方法 测试数据: 标题为空 预期结果: 创建失败 */
  @Test
  public void testCreateDiscussionThreadWithEmptyTitle() {
    Map<String, Object> result =
        discussionService.createDiscussionThread(adminUser(), "qa", 0, "", "Content", true);
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** 测试用例: 测试createDiscussionThread(...)方法 测试数据: 主题不存在 预期结果: 创建失败 */
  @Test
  public void testCreateDiscussionThreadWithNonExistentTopic() {
    Map<String, Object> result =
        discussionService.createDiscussionThread(
            adminUser(), "not-a-topic", 0, "Title", "Content", true);
    Assertions.assertFalse((Boolean) result.get("isDiscussionTopicExists"));
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** 测试用例: 测试editDiscussionThread(...)方法 测试数据: 由帖子创建者编辑 预期结果: 编辑成功 */
  @Test
  public void testEditDiscussionThreadSuccessfully() {
    Map<String, Boolean> result =
        discussionService.editDiscussionThread(1, adminUser(), "qa", "Edited Title", true);
    Assertions.assertTrue(result.get("isSuccessful"));
  }

  /** 测试用例: 测试editDiscussionThread(...)方法 测试数据: CSRF令牌无效 预期结果: 编辑失败且不抛出异常 */
  @Test
  public void testEditDiscussionThreadWithInvalidCsrfToken() {
    Map<String, Boolean> result =
        discussionService.editDiscussionThread(1, adminUser(), "qa", "Edited Title", false);
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试editDiscussionThread(...)方法 测试数据: 帖子不存在 预期结果: 编辑失败 */
  @Test
  public void testEditDiscussionThreadNotExists() {
    Map<String, Boolean> result =
        discussionService.editDiscussionThread(99999, adminUser(), "qa", "Title", true);
    Assertions.assertFalse(result.get("isDiscussionThreadExists"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试createDiscussionReply(...)方法 测试数据: 合法的回复 预期结果: 创建成功 */
  @Test
  public void testCreateDiscussionReplySuccessfully() {
    Map<String, Object> result =
        discussionService.createDiscussionReply(1, adminUser(), "A helpful reply.", true);
    Assertions.assertTrue((Boolean) result.get("isSuccessful"));
    Assertions.assertTrue(result.containsKey("discussionReply"));
  }

  /** 测试用例: 测试createDiscussionReply(...)方法 测试数据: 回复内容为空 预期结果: 创建失败 */
  @Test
  public void testCreateDiscussionReplyWithEmptyContent() {
    Map<String, Object> result = discussionService.createDiscussionReply(1, adminUser(), "", true);
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** 测试用例: 测试createDiscussionReply(...)方法 测试数据: 讨论帖不存在 预期结果: 创建失败 */
  @Test
  public void testCreateDiscussionReplyWithNonExistentThread() {
    Map<String, Object> result =
        discussionService.createDiscussionReply(99999, adminUser(), "Reply", true);
    Assertions.assertFalse((Boolean) result.get("isDiscussionThreadExists"));
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** 测试用例: 测试editDiscussionReply(...)方法 测试数据: 由管理员编辑 预期结果: 编辑成功 */
  @Test
  public void testEditDiscussionReplyByAdministrator() {
    // 回复1由用户1001创建, 管理员(用户1000)有权编辑.
    Map<String, Boolean> result =
        discussionService.editDiscussionReply(1, adminUser(), "Edited reply content.", true);
    Assertions.assertTrue(result.get("isSuccessful"));
  }

  /** 测试用例: 测试editDiscussionReply(...)方法 测试数据: CSRF令牌无效 预期结果: 编辑失败 */
  @Test
  public void testEditDiscussionReplyWithInvalidCsrfToken() {
    Map<String, Boolean> result =
        discussionService.editDiscussionReply(1, adminUser(), "Content", false);
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试deleteDiscussionReply(...)方法 测试数据: 由管理员删除 预期结果: 删除成功 */
  @Test
  public void testDeleteDiscussionReplyByAdministrator() {
    Map<String, Boolean> result = discussionService.deleteDiscussionReply(3, adminUser(), true);
    Assertions.assertTrue(result.get("isSuccessful"));
    Assertions.assertNull(discussionService.getDiscussionReplyUsingReplyId(3));
  }

  /** 测试用例: 测试deleteDiscussionReply(...)方法 测试数据: 普通用户删除他人回复 预期结果: 删除失败 */
  @Test
  public void testDeleteDiscussionReplyByUnauthorizedUser() {
    // 回复1由用户1001创建, 普通用户1002无权删除.
    Map<String, Boolean> result =
        discussionService.deleteDiscussionReply(1, userMapper.getUserUsingUid(1002), true);
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 返回管理员用户(用户1000). */
  private User adminUser() {
    return userMapper.getUserUsingUid(1000);
  }

  /** 构造一个仅设置UID的User对象, 用作投票用户参数. */
  private User userWithUid(long uid) {
    User user = new User();
    user.setUid(uid);
    return user;
  }

  /** 待测试的DiscussionService对象. */
  @Autowired private DiscussionService discussionService;

  /** 用于获取带有用户组信息的真实用户对象. */
  @Autowired private UserMapper userMapper;
}
