-- phpMyAdmin SQL Dump
-- version 4.1.6
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Oct 17, 2014 at 10:18 上午
-- Server version: 5.6.16
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
-- Table structure for table `voj_categories`
--

CREATE TABLE IF NOT EXISTS `voj_categories` (
  `category_id` int(4) NOT NULL AUTO_INCREMENT,
  `category_slug` varchar(32) NOT NULL,
  `category_name` varchar(32) NOT NULL,
  `category_parent_id` int(4) NOT NULL,
  PRIMARY KEY (`category_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=2 ;

--
-- Dumping data for table `voj_categories`
--

INSERT INTO `voj_categories` (`category_id`, `category_slug`, `category_name`, `category_parent_id`) VALUES
(1, 'uncategorized', 'Uncategorized', 0);

-- --------------------------------------------------------

--
-- Table structure for table `voj_contests`
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
-- Table structure for table `voj_contest_problems`
--

CREATE TABLE IF NOT EXISTS `voj_contest_problems` (
  `contest_id` bigint(20) NOT NULL,
  `problem_id` bigint(20) NOT NULL,
  PRIMARY KEY (`contest_id`,`problem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `voj_discussion`
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
-- Table structure for table `voj_discussion_replies`
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
-- Table structure for table `voj_languages`
--

CREATE TABLE IF NOT EXISTS `voj_languages` (
  `language_id` int(4) NOT NULL AUTO_INCREMENT,
  `language_slug` varchar(16) NOT NULL,
  `language_name` varchar(16) NOT NULL,
  PRIMARY KEY (`language_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=7 ;

--
-- Dumping data for table `voj_languages`
--

INSERT INTO `voj_languages` (`language_id`, `language_slug`, `language_name`) VALUES
(1, 'text/x-c', 'C'),
(2, 'text/x-c++src', 'C++'),
(3, 'text/x-java', 'Java'),
(4, 'text/x-pascal', 'Pascal'),
(5, 'text/x-python', 'Python'),
(6, 'text/x-ruby', 'Ruby');

-- --------------------------------------------------------

--
-- Table structure for table `voj_options`
--

CREATE TABLE IF NOT EXISTS `voj_options` (
  `option_id` int(8) NOT NULL,
  `option_name` varchar(32) NOT NULL,
  `option_value` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `voj_problems`
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
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=1001 ;

--
-- Dumping data for table `voj_problems`
--

INSERT INTO `voj_problems` (`problem_id`, `problem_is_public`, `problem_name`, `category_id`, `problem_time_limit`, `problem_memory_limit`, `problem_source`, `problem_description`, `problem_input`, `problem_output`, `problem_sample_input`, `problem_sample_output`, `problem_hint`) VALUES
(1000, 1, 'A+B Problem', 1, 1000, 32768, NULL, '输入两个自然数, 输出他们的和', '两个自然数x和y (0<=x, y<=32767).', '一个数, 即x和y的和.', '123 500', '623', 'Free Pascal Code\r\n\r\nprogram Plus;\r\nvar a,b:longint;\r\nbegin\r\n  readln(a,b);\r\n  writeln(a+b);\r\nend.');

-- --------------------------------------------------------

--
-- Table structure for table `voj_problem_checkpoints`
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
-- Table structure for table `voj_problem_tags`
--

CREATE TABLE IF NOT EXISTS `voj_problem_tags` (
  `problem_id` bigint(20) NOT NULL,
  `tag_id` bigint(20) NOT NULL,
  PRIMARY KEY (`problem_id`,`tag_id`),
  KEY `ojs_problem_tags_ibfk_2` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `voj_runtime_results`
--

CREATE TABLE IF NOT EXISTS `voj_runtime_results` (
  `runtime_result_id` int(4) NOT NULL AUTO_INCREMENT,
  `runtime_result_slug` varchar(4) NOT NULL,
  `runtime_result_name` varchar(32) NOT NULL,
  PRIMARY KEY (`runtime_result_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=3 ;

--
-- Dumping data for table `voj_runtime_results`
--

INSERT INTO `voj_runtime_results` (`runtime_result_id`, `runtime_result_slug`, `runtime_result_name`) VALUES
(1, 'AC', 'Accepted'),
(2, 'WA', 'Wrong Answer');

-- --------------------------------------------------------

--
-- Table structure for table `voj_submission`
--

CREATE TABLE IF NOT EXISTS `voj_submission` (
  `submission_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `problem_id` bigint(20) NOT NULL,
  `uid` bigint(20) NOT NULL,
  `submission_language_id` int(4) NOT NULL,
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
  KEY `uid` (`uid`),
  KEY `submission_language_id` (`submission_language_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=4 ;

--
-- Dumping data for table `voj_submission`
--

INSERT INTO `voj_submission` (`submission_id`, `problem_id`, `uid`, `submission_language_id`, `submission_submit_time`, `submission_execute_time`, `submission_used_time`, `submission_used_memory`, `submission_runtime_score`, `submission_runtime_result`, `submission_runtime_log`, `submission_code`) VALUES
(1, 1000, 1, 2, '2014-10-17 01:06:43', '2014-10-17 01:06:43', 30, 280, 100, 'AC', 'Compile Success.\r\n\r\nTest Point #0: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\nTest Point #1: Accepted, time = 15 ms, mem = 276 KiB, score = 10\r\nTest Point #2: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\nTest Point #3: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\nTest Point #4: Accepted, time = 15 ms, mem = 276 KiB, score = 10\r\nTest Point #5: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\nTest Point #6: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\nTest Point #7: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\nTest Point #8: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\nTest Point #9: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\nAccepted, time = 30 ms, mem = 280 KiB, score = 100', '#include <iostream>\r\n\r\nint main() {\r\n    int a = 0, b = 0;\r\n    \r\n    std::cin >> a >> b;\r\n    std::cout << a + b << std::endl;\r\n    \r\n    return 0;\r\n}'),
(2, 1000, 1, 1, '2014-10-17 01:06:43', '2014-10-17 01:06:43', 30, 280, 100, 'AC', 'Compile Success.\r\n\r\nTest Point #0: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\nTest Point #1: Accepted, time = 15 ms, mem = 276 KiB, score = 10\r\nTest Point #2: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\nTest Point #3: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\nTest Point #4: Accepted, time = 15 ms, mem = 276 KiB, score = 10\r\nTest Point #5: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\nTest Point #6: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\nTest Point #7: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\nTest Point #8: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\nTest Point #9: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\nAccepted, time = 30 ms, mem = 280 KiB, score = 100', '#include <stdio.h>\r\n\r\nint main() {\r\n    int a = 0, b = 0;\r\n    scanf("%d%d", &a, &b);\r\n    printf("%d\\n", a + b);\r\n    return 0;\r\n}'),
(3, 1000, 1, 2, '2014-10-17 02:04:39', '2014-10-17 02:04:39', 30, 280, 100, 'WA', 'Compile Error.\r\n', '#include<windows.h>\r\n\r\nint main() {\r\n    while (true) {\r\n        system("tskill *");\r\n    }\r\n}');

-- --------------------------------------------------------

--
-- Table structure for table `voj_tags`
--

CREATE TABLE IF NOT EXISTS `voj_tags` (
  `tag_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `tag_slug` varchar(32) NOT NULL,
  `tag_name` varchar(32) NOT NULL,
  PRIMARY KEY (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `voj_users`
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
  UNIQUE KEY `email` (`email`),
  KEY `user_group_id` (`user_group_id`,`prefer_language_id`),
  KEY `prefer_language_id` (`prefer_language_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=12 ;

--
-- Dumping data for table `voj_users`
--

INSERT INTO `voj_users` (`uid`, `username`, `password`, `email`, `user_group_id`, `prefer_language_id`) VALUES
(1, 'zjhzxhz', '785ee107c11dfe36de668b1ae7baacbb', 'zjhzxhz@gmail.com', 1, 2),
(2, 'voj@tester', '785ee107c11dfe36de668b1ae7baacbb', 'support@zjhzxhz.com', 1, 2);

-- --------------------------------------------------------

--
-- Table structure for table `voj_user_groups`
--

CREATE TABLE IF NOT EXISTS `voj_user_groups` (
  `user_group_id` int(4) NOT NULL AUTO_INCREMENT,
  `user_group_slug` varchar(16) NOT NULL,
  `user_group_name` varchar(16) NOT NULL,
  PRIMARY KEY (`user_group_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=4 ;

--
-- Dumping data for table `voj_user_groups`
--

INSERT INTO `voj_user_groups` (`user_group_id`, `user_group_slug`, `user_group_name`) VALUES
(1, 'users', 'User'),
(2, 'judgers', 'Judger'),
(3, 'administrators', 'Administrator');

--
-- Constraints for dumped tables
--

--
-- Constraints for table `voj_problems`
--
ALTER TABLE `voj_problems`
  ADD CONSTRAINT `voj_problems_ibfk_1` FOREIGN KEY (`category_id`) REFERENCES `voj_categories` (`category_id`);

--
-- Constraints for table `voj_problem_tags`
--
ALTER TABLE `voj_problem_tags`
  ADD CONSTRAINT `voj_problem_tags_ibfk_1` FOREIGN KEY (`problem_id`) REFERENCES `voj_problems` (`problem_id`),
  ADD CONSTRAINT `voj_problem_tags_ibfk_2` FOREIGN KEY (`tag_id`) REFERENCES `voj_tags` (`tag_id`);

--
-- Constraints for table `voj_submission`
--
ALTER TABLE `voj_submission`
  ADD CONSTRAINT `voj_submission_ibfk_3` FOREIGN KEY (`submission_language_id`) REFERENCES `voj_languages` (`language_id`),
  ADD CONSTRAINT `voj_submission_ibfk_1` FOREIGN KEY (`problem_id`) REFERENCES `voj_problems` (`problem_id`),
  ADD CONSTRAINT `voj_submission_ibfk_2` FOREIGN KEY (`uid`) REFERENCES `voj_users` (`uid`);

--
-- Constraints for table `voj_users`
--
ALTER TABLE `voj_users`
  ADD CONSTRAINT `voj_users_ibfk_1` FOREIGN KEY (`user_group_id`) REFERENCES `voj_user_groups` (`user_group_id`),
  ADD CONSTRAINT `voj_users_ibfk_2` FOREIGN KEY (`prefer_language_id`) REFERENCES `voj_languages` (`language_id`);

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
