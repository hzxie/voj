-- Verwandlung Online Judge - database schema (DDL only).
-- Split out of the legacy voj.sql. Reference rows live in seed.sql,
-- sample content in demo.sql, and test fixtures in test-data.sql.
SET NAMES utf8mb4;
SET SQL_MODE = "STRICT_TRANS_TABLES";
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `voj_bulletin_board_messages`;
DROP TABLE IF EXISTS `voj_contests`;
DROP TABLE IF EXISTS `voj_contest_contestants`;
DROP TABLE IF EXISTS `voj_contest_submissions`;
DROP TABLE IF EXISTS `voj_discussion_replies`;
DROP TABLE IF EXISTS `voj_discussion_reply_votes`;
DROP TABLE IF EXISTS `voj_discussion_threads`;
DROP TABLE IF EXISTS `voj_discussion_topics`;
DROP TABLE IF EXISTS `voj_email_validation`;
DROP TABLE IF EXISTS `voj_judge_results`;
DROP TABLE IF EXISTS `voj_languages`;
DROP TABLE IF EXISTS `voj_options`;
DROP TABLE IF EXISTS `voj_problems`;
DROP TABLE IF EXISTS `voj_problem_difficulty`;
DROP TABLE IF EXISTS `voj_problem_categories`;
DROP TABLE IF EXISTS `voj_problem_category_relationships`;
DROP TABLE IF EXISTS `voj_problem_checkpoints`;
DROP TABLE IF EXISTS `voj_problem_tags`;
DROP TABLE IF EXISTS `voj_problem_tag_relationships`;
DROP TABLE IF EXISTS `voj_submissions`;
DROP TABLE IF EXISTS `voj_usermeta`;
DROP TABLE IF EXISTS `voj_users`;
DROP TABLE IF EXISTS `voj_user_groups`;

CREATE TABLE `voj_bulletin_board_messages` (
  `message_id` bigint(20) NOT NULL,
  `message_title` varchar(128) NOT NULL,
  `message_body` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `message_create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `voj_contests` (
  `contest_id` bigint(20) NOT NULL,
  `contest_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `contest_notes` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `contest_start_time` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `contest_end_time` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `contest_mode` varchar(4) COLLATE utf8mb4_unicode_ci NOT NULL,
  `contest_problems` text COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `voj_contest_contestants` (
  `contest_id` bigint(20) NOT NULL,
  `contestant_uid` bigint(20) NOT NULL,
  `code_snippet` text COLLATE utf8mb4_unicode_ci
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `voj_contest_submissions` (
  `contest_id` bigint(20) NOT NULL,
  `submission_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `voj_discussion_replies` (
  `discussion_reply_id` bigint(20) NOT NULL,
  `discussion_thread_id` bigint(20) NOT NULL,
  `discussion_reply_uid` bigint(20) NOT NULL,
  `discussion_reply_time` timestamp DEFAULT CURRENT_TIMESTAMP,
  `discussion_reply_content` text COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `voj_discussion_reply_votes` (
  `discussion_reply_id` bigint(20) NOT NULL,
  `voter_uid` bigint(20) NOT NULL,
  `vote_type` tinyint(4) NOT NULL COMMENT '1 = upvote, -1 = downvote, 2 = report',
  `vote_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `voj_discussion_threads` (
  `discussion_thread_id` bigint(20) NOT NULL,
  `discussion_thread_creator_uid` bigint(20) NOT NULL,
  `discussion_thread_create_time` timestamp DEFAULT CURRENT_TIMESTAMP,
  `problem_id` bigint(20) DEFAULT NULL,
  `discussion_topic_id` int(8) NOT NULL,
  `discussion_thread_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `voj_discussion_topics` (
  `discussion_topic_id` int(8) NOT NULL,
  `discussion_topic_slug` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `discussion_topic_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `discussion_parent_topic_id` int(8) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `voj_email_validation` (
  `email` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `token` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expire_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `voj_judge_results` (
  `judge_result_id` int(4) NOT NULL,
  `judge_result_slug` varchar(4) COLLATE utf8mb4_unicode_ci NOT NULL,
  `judge_result_name` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `voj_languages` (
  `language_id` int(4) NOT NULL,
  `language_slug` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `language_name` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `language_compile_command` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `language_run_command` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `voj_options` (
  `option_id` int(8) NOT NULL,
  `option_name` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `option_value` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_autoload` tinyint(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `voj_problems` (
  `problem_id` bigint(20) NOT NULL,
  `problem_is_public` tinyint(1) NOT NULL DEFAULT '1',
  `problem_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `problem_difficulty_id` int(4) NOT NULL DEFAULT '1',
  `problem_time_limit` int(8) NOT NULL,
  `problem_memory_limit` int(8) NOT NULL,
  `problem_description` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `problem_input_format` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `problem_output_format` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `problem_sample_input` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `problem_sample_output` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `problem_hint` text COLLATE utf8mb4_unicode_ci
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `voj_problem_difficulty` (
  `problem_difficulty_id` int(4) NOT NULL,
  `problem_difficulty_slug` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `problem_difficulty_name` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `voj_problem_categories` (
  `problem_category_id` int(4) NOT NULL,
  `problem_category_slug` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `problem_category_name` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `problem_category_parent_id` int(4) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `voj_problem_category_relationships` (
  `problem_id` bigint(20) NOT NULL,
  `problem_category_id` int(4) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `voj_problem_checkpoints` (
  `problem_id` bigint(20) NOT NULL,
  `checkpoint_id` int(4) NOT NULL,
  `checkpoint_exactly_match` tinyint(1) NOT NULL,
  `checkpoint_score` int(4) NOT NULL,
  `checkpoint_input` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `checkpoint_output` longtext COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `voj_problem_tags` (
  `problem_tag_id` bigint(20) NOT NULL,
  `problem_tag_slug` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `problem_tag_name` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `voj_problem_tag_relationships` (
  `problem_id` bigint(20) NOT NULL,
  `problem_tag_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `voj_submissions` (
  `submission_id` bigint(20) NOT NULL,
  `problem_id` bigint(20) NOT NULL,
  `uid` bigint(20) NOT NULL,
  `language_id` int(4) NOT NULL,
  `submission_submit_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `submission_execute_time` timestamp NULL DEFAULT NULL,
  `submission_used_time` int(8) DEFAULT NULL,
  `submission_used_memory` int(8) DEFAULT NULL,
  `submission_judge_result` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PD',
  `submission_judge_score` int(4) DEFAULT NULL,
  `submission_judge_log` text COLLATE utf8mb4_unicode_ci,
  `submission_code` text COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `voj_usermeta` (
  `meta_id` bigint(20) NOT NULL,
  `uid` bigint(20) NOT NULL,
  `meta_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `meta_value` text COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `voj_users` (
  `uid` bigint(20) NOT NULL,
  `username` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_group_id` int(4) NOT NULL,
  `prefer_language_id` int(4) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `voj_user_groups` (
  `user_group_id` int(4) NOT NULL,
  `user_group_slug` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_group_name` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `voj_problem_difficulty`
  ADD PRIMARY KEY (`problem_difficulty_id`),
  ADD UNIQUE KEY `difficulty_slug` (`problem_difficulty_slug`);

ALTER TABLE `voj_bulletin_board_messages`
  ADD PRIMARY KEY (`message_id`);

ALTER TABLE `voj_contests`
  ADD PRIMARY KEY (`contest_id`);

ALTER TABLE `voj_contest_contestants`
  ADD PRIMARY KEY (`contest_id`,`contestant_uid`),
  ADD KEY `contestant_uid` (`contestant_uid`);

ALTER TABLE `voj_contest_submissions`
  ADD PRIMARY KEY (`submission_id`),
  ADD KEY `contest_id` (`contest_id`);

ALTER TABLE `voj_discussion_replies`
  ADD PRIMARY KEY (`discussion_reply_id`),
  ADD KEY `discussion_id` (`discussion_thread_id`,`discussion_reply_uid`),
  ADD KEY `discussion_reply_uid` (`discussion_reply_uid`);

ALTER TABLE `voj_discussion_reply_votes`
  ADD PRIMARY KEY (`discussion_reply_id`,`voter_uid`,`vote_type`),
  ADD KEY `voter_uid` (`voter_uid`);

ALTER TABLE `voj_discussion_threads`
  ADD PRIMARY KEY (`discussion_thread_id`),
  ADD KEY `discussion_topic_id` (`discussion_topic_id`),
  ADD KEY `discussion_creator_uid` (`discussion_thread_creator_uid`),
  ADD KEY `problem_id` (`problem_id`);

ALTER TABLE `voj_discussion_topics`
  ADD PRIMARY KEY (`discussion_topic_id`),
  ADD UNIQUE KEY `discussion_topic_slug` (`discussion_topic_slug`),
  ADD KEY `discussion_parent_topic_id` (`discussion_parent_topic_id`);

ALTER TABLE `voj_email_validation`
  ADD PRIMARY KEY (`email`);

ALTER TABLE `voj_judge_results`
  ADD PRIMARY KEY (`judge_result_id`),
  ADD UNIQUE KEY `runtime_result_slug` (`judge_result_slug`);

ALTER TABLE `voj_languages`
  ADD PRIMARY KEY (`language_id`),
  ADD UNIQUE KEY `language_slug` (`language_slug`);

ALTER TABLE `voj_options`
  ADD PRIMARY KEY (`option_id`),
  ADD UNIQUE KEY `option_name` (`option_name`);

ALTER TABLE `voj_problems`
  ADD PRIMARY KEY (`problem_id`);

ALTER TABLE `voj_problem_categories`
  ADD PRIMARY KEY (`problem_category_id`),
  ADD UNIQUE KEY `category_slug` (`problem_category_slug`);

ALTER TABLE `voj_problem_category_relationships`
  ADD PRIMARY KEY (`problem_id`,`problem_category_id`),
  ADD KEY `voj_problem_category_relationships_ibfk_2` (`problem_category_id`);

ALTER TABLE `voj_problem_checkpoints`
  ADD PRIMARY KEY (`problem_id`,`checkpoint_id`);

ALTER TABLE `voj_problem_tags`
  ADD PRIMARY KEY (`problem_tag_id`),
  ADD UNIQUE KEY `problem_tag_slug` (`problem_tag_slug`);

ALTER TABLE `voj_problem_tag_relationships`
  ADD PRIMARY KEY (`problem_id`,`problem_tag_id`),
  ADD KEY `problem_tag_id` (`problem_tag_id`);

ALTER TABLE `voj_submissions`
  ADD PRIMARY KEY (`submission_id`),
  ADD KEY `problem_id` (`problem_id`,`uid`),
  ADD KEY `uid` (`uid`),
  ADD KEY `submission_language_id` (`language_id`),
  ADD KEY `submission_runtime_result` (`submission_judge_result`);

ALTER TABLE `voj_usermeta`
  ADD PRIMARY KEY (`meta_id`),
  ADD KEY `uid` (`uid`);

ALTER TABLE `voj_users`
  ADD PRIMARY KEY (`uid`),
  ADD UNIQUE KEY `username` (`username`),
  ADD UNIQUE KEY `email` (`email`),
  ADD KEY `user_group_id` (`user_group_id`,`prefer_language_id`),
  ADD KEY `prefer_language_id` (`prefer_language_id`);

ALTER TABLE `voj_user_groups`
  ADD PRIMARY KEY (`user_group_id`),
  ADD UNIQUE KEY `user_group_slug` (`user_group_slug`);

ALTER TABLE `voj_bulletin_board_messages`
  MODIFY `message_id` bigint(20) NOT NULL AUTO_INCREMENT;

ALTER TABLE `voj_contests`
  MODIFY `contest_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

ALTER TABLE `voj_discussion_replies`
  MODIFY `discussion_reply_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

ALTER TABLE `voj_discussion_threads`
  MODIFY `discussion_thread_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

ALTER TABLE `voj_discussion_topics`
  MODIFY `discussion_topic_id` int(8) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

ALTER TABLE `voj_judge_results`
  MODIFY `judge_result_id` int(4) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

ALTER TABLE `voj_languages`
  MODIFY `language_id` int(4) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

ALTER TABLE `voj_options`
  MODIFY `option_id` int(8) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=12;

ALTER TABLE `voj_problems`
  MODIFY `problem_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1004;

ALTER TABLE `voj_problem_categories`
  MODIFY `problem_category_id` int(4) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

ALTER TABLE `voj_problem_tags`
  MODIFY `problem_tag_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

ALTER TABLE `voj_submissions`
  MODIFY `submission_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1005;

ALTER TABLE `voj_usermeta`
  MODIFY `meta_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

ALTER TABLE `voj_users`
  MODIFY `uid` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1003;

ALTER TABLE `voj_user_groups`
  MODIFY `user_group_id` int(4) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

ALTER TABLE `voj_contest_contestants`
  ADD CONSTRAINT `voj_contest_contestants_ibfk_1` FOREIGN KEY (`contest_id`) REFERENCES `voj_contests` (`contest_id`),
  ADD CONSTRAINT `voj_contest_contestants_ibfk_2` FOREIGN KEY (`contestant_uid`) REFERENCES `voj_users` (`uid`);

ALTER TABLE `voj_contest_submissions`
  ADD CONSTRAINT `voj_contest_submissions_ibfk_1` FOREIGN KEY (`contest_id`) REFERENCES `voj_contests` (`contest_id`),
  ADD CONSTRAINT `voj_contest_submissions_ibfk_2` FOREIGN KEY (`submission_id`) REFERENCES `voj_submissions` (`submission_id`);

ALTER TABLE `voj_discussion_replies`
  ADD CONSTRAINT `voj_discussion_replies_ibfk_1` FOREIGN KEY (`discussion_thread_id`) REFERENCES `voj_discussion_threads` (`discussion_thread_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `voj_discussion_replies_ibfk_2` FOREIGN KEY (`discussion_reply_uid`) REFERENCES `voj_users` (`uid`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `voj_discussion_reply_votes`
  ADD CONSTRAINT `voj_discussion_reply_votes_ibfk_1` FOREIGN KEY (`discussion_reply_id`) REFERENCES `voj_discussion_replies` (`discussion_reply_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `voj_discussion_reply_votes_ibfk_2` FOREIGN KEY (`voter_uid`) REFERENCES `voj_users` (`uid`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `voj_discussion_threads`
  ADD CONSTRAINT `voj_discussion_thread_ibfk_1` FOREIGN KEY (`discussion_thread_creator_uid`) REFERENCES `voj_users` (`uid`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `voj_discussion_thread_ibfk_2` FOREIGN KEY (`discussion_topic_id`) REFERENCES `voj_discussion_topics` (`discussion_topic_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `voj_discussion_thread_ibfk_3` FOREIGN KEY (`problem_id`) REFERENCES `voj_problems` (`problem_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `voj_email_validation`
  ADD CONSTRAINT `voj_email_validation_ibfk_1` FOREIGN KEY (`email`) REFERENCES `voj_users` (`email`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `voj_problem_category_relationships`
  ADD CONSTRAINT `voj_problem_category_relationships_ibfk_1` FOREIGN KEY (`problem_id`) REFERENCES `voj_problems` (`problem_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `voj_problem_category_relationships_ibfk_2` FOREIGN KEY (`problem_category_id`) REFERENCES `voj_problem_categories` (`problem_category_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `voj_problem_checkpoints`
  ADD CONSTRAINT `voj_problem_checkpoints_ibfk_1` FOREIGN KEY (`problem_id`) REFERENCES `voj_problems` (`problem_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `voj_problem_tag_relationships`
  ADD CONSTRAINT `voj_problem_tag_relationships_ibfk_1` FOREIGN KEY (`problem_id`) REFERENCES `voj_problems` (`problem_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `voj_problem_tag_relationships_ibfk_2` FOREIGN KEY (`problem_tag_id`) REFERENCES `voj_problem_tags` (`problem_tag_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `voj_submissions`
  ADD CONSTRAINT `voj_submissions_ibfk_1` FOREIGN KEY (`problem_id`) REFERENCES `voj_problems` (`problem_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `voj_submissions_ibfk_2` FOREIGN KEY (`uid`) REFERENCES `voj_users` (`uid`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `voj_submissions_ibfk_3` FOREIGN KEY (`language_id`) REFERENCES `voj_languages` (`language_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `voj_submissions_ibfk_4` FOREIGN KEY (`submission_judge_result`) REFERENCES `voj_judge_results` (`judge_result_slug`);

ALTER TABLE `voj_usermeta`
  ADD CONSTRAINT `voj_usermeta_ibfk_1` FOREIGN KEY (`uid`) REFERENCES `voj_users` (`uid`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `voj_users`
  ADD CONSTRAINT `voj_users_ibfk_1` FOREIGN KEY (`user_group_id`) REFERENCES `voj_user_groups` (`user_group_id`),
  ADD CONSTRAINT `voj_users_ibfk_2` FOREIGN KEY (`prefer_language_id`) REFERENCES `voj_languages` (`language_id`);

SET FOREIGN_KEY_CHECKS = 1;
