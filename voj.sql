-- phpMyAdmin SQL Dump
-- version 4.1.6
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: 2014-10-16 14:21:22
-- 服务器版本： 5.6.16
-- PHP Version: 5.5.9

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Database: `voj`
--

-- --------------------------------------------------------

--
-- 表的结构 `voj_categories`
--

CREATE TABLE IF NOT EXISTS `voj_categories` (
  `category_id` int(4) NOT NULL AUTO_INCREMENT,
  `category_slug` varchar(32) NOT NULL,
  `category_name` varchar(32) NOT NULL,
  `category_parent_id` int(4) NOT NULL,
  PRIMARY KEY (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- 表的结构 `voj_contests`
--

CREATE TABLE IF NOT EXISTS `voj_contests` (
  `contest_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `contest_name` varchar(128) NOT NULL,
  `contest_start_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `contest_end_time` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`contest_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- 表的结构 `voj_contest_problems`
--

CREATE TABLE IF NOT EXISTS `voj_contest_problems` (
  `contest_id` bigint(20) NOT NULL,
  `problem_id` bigint(20) NOT NULL,
  PRIMARY KEY (`contest_id`,`problem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- 表的结构 `voj_discussion`
--

CREATE TABLE IF NOT EXISTS `voj_discussion` (
  `discussion_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `problem_id` bigint(20) NOT NULL,
  `uid` bigint(20) NOT NULL,
  `discussion_name` varchar(128) NOT NULL,
  `discussion_create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `discussion_content` text NOT NULL,
  PRIMARY KEY (`discussion_id`),
  KEY `problem_id` (`problem_id`,`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- 表的结构 `voj_discussion_replies`
--

CREATE TABLE IF NOT EXISTS `voj_discussion_replies` (
  `discussion_reply_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `discussion_id` bigint(20) NOT NULL,
  `uid` int(11) NOT NULL,
  `discussion_reply_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `discussion_reply_vote_up` int(11) NOT NULL,
  `discussion_reply_vote_down` int(11) NOT NULL,
  `discussion_reply_content` text NOT NULL,
  PRIMARY KEY (`discussion_reply_id`),
  KEY `discussion_id` (`discussion_id`,`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- 表的结构 `voj_languages`
--

CREATE TABLE IF NOT EXISTS `voj_languages` (
  `language_id` int(4) NOT NULL AUTO_INCREMENT,
  `language_slug` varchar(16) NOT NULL,
  `language_name` varchar(16) NOT NULL,
  PRIMARY KEY (`language_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=7 ;

--
-- 转存表中的数据 `voj_languages`
--

INSERT INTO `voj_languages` (`language_id`, `language_slug`, `language_name`) VALUES
(1, 'c', 'C'),
(2, 'cpp', 'C++'),
(3, 'java', 'Java'),
(4, 'pascal', 'Pascal'),
(5, 'python', 'Python'),
(6, 'ruby', 'Ruby');

-- --------------------------------------------------------

--
-- 表的结构 `voj_options`
--

CREATE TABLE IF NOT EXISTS `voj_options` (
  `option_id` int(8) NOT NULL,
  `option_name` varchar(32) NOT NULL,
  `option_value` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- 表的结构 `voj_problems`
--

CREATE TABLE IF NOT EXISTS `voj_problems` (
  `problem_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `problem_is_public` tinyint(1) NOT NULL DEFAULT '1',
  `problem_name` varchar(128) NOT NULL,
  `category_id` int(4) NOT NULL,
  `problem_time_limit` int(8) NOT NULL,
  `problem_memory_limit` int(8) NOT NULL,
  `problem_source` varchar(128) DEFAULT NULL,
  `problem_description` text NOT NULL,
  `problem_input` text NOT NULL,
  `problem_output` text NOT NULL,
  `problem_sample_input` text NOT NULL,
  `problem_sample_output` text NOT NULL,
  `problem_hint` text,
  PRIMARY KEY (`problem_id`),
  KEY `category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- 表的结构 `voj_problem_checkpoints`
--

CREATE TABLE IF NOT EXISTS `voj_problem_checkpoints` (
  `problem_id` bigint(20) NOT NULL,
  `checkpoint_id` int(4) NOT NULL,
  `checkpoint_score` int(4) NOT NULL,
  `checkpoint_input` text NOT NULL,
  `checkpoint_output` text NOT NULL,
  PRIMARY KEY (`problem_id`,`checkpoint_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- 表的结构 `voj_problem_tags`
--

CREATE TABLE IF NOT EXISTS `voj_problem_tags` (
  `problem_id` bigint(20) NOT NULL,
  `tag_id` bigint(20) NOT NULL,
  PRIMARY KEY (`problem_id`,`tag_id`),
  KEY `ojs_problem_tags_ibfk_2` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- 表的结构 `voj_submission`
--

CREATE TABLE IF NOT EXISTS `voj_submission` (
  `submission_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `problem_id` bigint(20) NOT NULL,
  `uid` bigint(20) NOT NULL,
  `submission_language` varchar(16) NOT NULL,
  `submission_submit_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `submission_execute_time` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `submission_used_time` int(8) NOT NULL,
  `submission_used_memory` int(8) NOT NULL,
  `submission_runtime_score` int(4) NOT NULL,
  `submission_runtime_result` varchar(8) NOT NULL,
  `submission_runtime_log` text NOT NULL,
  `submission_code` text NOT NULL,
  PRIMARY KEY (`submission_id`),
  KEY `problem_id` (`problem_id`,`uid`),
  KEY `uid` (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- 表的结构 `voj_tags`
--

CREATE TABLE IF NOT EXISTS `voj_tags` (
  `tag_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `tag_slug` varchar(32) NOT NULL,
  `tag_name` varchar(32) NOT NULL,
  PRIMARY KEY (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- 表的结构 `voj_users`
--

CREATE TABLE IF NOT EXISTS `voj_users` (
  `uid` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(16) NOT NULL,
  `password` varchar(32) NOT NULL,
  `email` varchar(64) NOT NULL,
  `user_group_id` int(4) NOT NULL,
  `prefer_language_id` int(4) NOT NULL,
  PRIMARY KEY (`uid`),
  UNIQUE KEY `username` (`username`),
  KEY `user_group_id` (`user_group_id`,`prefer_language_id`),
  KEY `prefer_language_id` (`prefer_language_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- 表的结构 `voj_user_groups`
--

CREATE TABLE IF NOT EXISTS `voj_user_groups` (
  `user_group_id` int(4) NOT NULL AUTO_INCREMENT,
  `user_group_slug` varchar(16) NOT NULL,
  `user_group_name` varchar(16) NOT NULL,
  PRIMARY KEY (`user_group_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=4 ;

--
-- 转存表中的数据 `voj_user_groups`
--

INSERT INTO `voj_user_groups` (`user_group_id`, `user_group_slug`, `user_group_name`) VALUES
(1, 'users', '用户'),
(2, 'judgers', '评测机'),
(3, 'administrators', '管理员');

--
-- 限制导出的表
--

--
-- 限制表 `voj_problems`
--
ALTER TABLE `voj_problems`
  ADD CONSTRAINT `voj_problems_ibfk_1` FOREIGN KEY (`category_id`) REFERENCES `voj_categories` (`category_id`);

--
-- 限制表 `voj_problem_tags`
--
ALTER TABLE `voj_problem_tags`
  ADD CONSTRAINT `voj_problem_tags_ibfk_1` FOREIGN KEY (`problem_id`) REFERENCES `voj_problems` (`problem_id`),
  ADD CONSTRAINT `voj_problem_tags_ibfk_2` FOREIGN KEY (`tag_id`) REFERENCES `voj_tags` (`tag_id`);

--
-- 限制表 `voj_submission`
--
ALTER TABLE `voj_submission`
  ADD CONSTRAINT `voj_submission_ibfk_1` FOREIGN KEY (`problem_id`) REFERENCES `voj_problems` (`problem_id`),
  ADD CONSTRAINT `voj_submission_ibfk_2` FOREIGN KEY (`uid`) REFERENCES `voj_users` (`uid`);

--
-- 限制表 `voj_users`
--
ALTER TABLE `voj_users`
  ADD CONSTRAINT `voj_users_ibfk_2` FOREIGN KEY (`prefer_language_id`) REFERENCES `voj_languages` (`language_id`),
  ADD CONSTRAINT `voj_users_ibfk_1` FOREIGN KEY (`user_group_id`) REFERENCES `voj_user_groups` (`user_group_id`);

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
