-- phpMyAdmin SQL Dump
-- version 4.2.11
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: May 17, 2015 at 05:12 PM
-- Server version: 5.6.21
-- PHP Version: 5.6.3

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
`category_id` int(4) NOT NULL,
  `category_slug` varchar(32) NOT NULL,
  `category_name` varchar(32) NOT NULL,
  `category_parent_id` int(4) NOT NULL
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

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
`contest_id` bigint(20) NOT NULL,
  `contest_name` varchar(128) NOT NULL,
  `contest_start_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `contest_end_time` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `voj_contest_problems`
--

CREATE TABLE IF NOT EXISTS `voj_contest_problems` (
  `contest_id` bigint(20) NOT NULL,
  `problem_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `voj_discussion_replies`
--

CREATE TABLE IF NOT EXISTS `voj_discussion_replies` (
`discussion_reply_id` bigint(20) NOT NULL,
  `discussion_threads_id` bigint(20) NOT NULL,
  `discussion_reply_uid` bigint(20) NOT NULL,
  `discussion_reply_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `discussion_reply_vote_up` bigint(20) NOT NULL DEFAULT '0',
  `discussion_reply_vote_down` bigint(20) NOT NULL DEFAULT '0',
  `discussion_reply_content` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `voj_discussion_threads`
--

CREATE TABLE IF NOT EXISTS `voj_discussion_threads` (
`discussion_threads_id` bigint(20) NOT NULL,
  `discussion_topic_id` int(8) NOT NULL,
  `disscussion_creator_uid` bigint(20) NOT NULL,
  `problem_id` bigint(20) DEFAULT NULL,
  `discussion_threads_name` varchar(128) NOT NULL,
  `discussion_create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `discussion_content` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `voj_discussion_topics`
--

CREATE TABLE IF NOT EXISTS `voj_discussion_topics` (
`discussion_topic_id` int(8) NOT NULL,
  `discussion_topic_slug` varchar(128) NOT NULL,
  `discussion_topic_name` varchar(128) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `voj_judge_results`
--

CREATE TABLE IF NOT EXISTS `voj_judge_results` (
`judge_result_id` int(4) NOT NULL,
  `judge_result_slug` varchar(4) NOT NULL,
  `judge_result_name` varchar(32) NOT NULL
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8;

--
-- Dumping data for table `voj_judge_results`
--

INSERT INTO `voj_judge_results` (`judge_result_id`, `judge_result_slug`, `judge_result_name`) VALUES
(1, 'AC', 'Accepted'),
(2, 'WA', 'Wrong Answer'),
(3, 'TLE', 'Time Limit Exceed'),
(4, 'OLE', 'Output Limit Exceed'),
(5, 'MLE', 'Memory Limit Exceed'),
(6, 'RE', 'Runtime Error'),
(7, 'PE', 'Presentation Error'),
(8, 'CE', 'Compile Error');

-- --------------------------------------------------------

--
-- Table structure for table `voj_languages`
--

CREATE TABLE IF NOT EXISTS `voj_languages` (
`language_id` int(4) NOT NULL,
  `language_slug` varchar(16) NOT NULL,
  `language_name` varchar(16) NOT NULL
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8;

--
-- Dumping data for table `voj_languages`
--

INSERT INTO `voj_languages` (`language_id`, `language_slug`, `language_name`) VALUES
(1, 'text/x-csrc', 'C'),
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
`problem_id` bigint(20) NOT NULL,
  `problem_is_public` tinyint(1) NOT NULL DEFAULT '1',
  `problem_name` varchar(128) NOT NULL,
  `problem_time_limit` int(8) NOT NULL,
  `problem_memory_limit` int(8) NOT NULL,
  `problem_description` text NOT NULL,
  `problem_input_format` text NOT NULL,
  `problem_output_format` text NOT NULL,
  `problem_sample_input` text NOT NULL,
  `problem_sample_output` text NOT NULL,
  `problem_hint` text
) ENGINE=InnoDB AUTO_INCREMENT=1003 DEFAULT CHARSET=utf8;

--
-- Dumping data for table `voj_problems`
--

INSERT INTO `voj_problems` (`problem_id`, `problem_is_public`, `problem_name`, `problem_time_limit`, `problem_memory_limit`, `problem_description`, `problem_input_format`, `problem_output_format`, `problem_sample_input`, `problem_sample_output`, `problem_hint`) VALUES
(1000, 1, 'A+B Problem', 1000, 32768, '输入两个自然数, 输出他们的和', '两个自然数x和y (0<=x, y<=32767).', '一个数, 即x和y的和.', '123 500', '623', '## Free Pascal Code\r\n\r\n```\r\nprogram Plus;\r\nvar a,b:longint;\r\nbegin\r\n  readln(a,b);\r\n  writeln(a+b);\r\nend.\r\n```'),
(1001, 1, '谁拿了最多奖学金', 1000, 32768, '某校的惯例是在每学期的期末考试之后发放奖学金。发放的奖学金共有五种，获取的条件各自不同：\r\n1) 院士奖学金，每人8000元，期末平均成绩高于80分（>80），并且在本学期内发表1篇或1篇以上论文的学生均可获得；\r\n2) 五四奖学金，每人4000元，期末平均成绩高于85分（>85），并且班级评议成绩高于80分（>80）的学生均可获得；\r\n3) 成绩优秀奖，每人2000元，期末平均成绩高于90分（>90）的学生均可获得；\r\n4) 西部奖学金，每人1000元，期末平均成绩高于85分（>85）的西部省份学生均可获得；\r\n5) 班级贡献奖，每人850元，班级评议成绩高于80分（>80）的学生干部均可获得；\r\n只要符合条件就可以得奖，每项奖学金的获奖人数没有限制，每名学生也可以同时获得多项奖学金。例如姚林的期末平均成绩是87分，班级评议成绩82分，同时他还是一位学生干部，那么他可以同时获得五四奖学金和班级贡献奖，奖金总数是4850元。\r\n现在给出若干学生的相关数据，请计算哪些同学获得的奖金总数最高（假设总有同学能满足获得奖学金的条件）。', '输入的第一行是一个整数N（1 <= N <= 100），表示学生的总数。接下来的N行每行是一位学生的数据，从左向右依次是姓名，期末平均成绩，班级评议成绩，是否是学生干部，是否是西部省份学生，以及发表的论文数。姓名是由大小写英文字母组成的长度不超过20的字符串（不含空格）；期末平均成绩和班级评议成绩都是0到100之间的整数（包括0和100）；是否是学生干部和是否是西部省份学生分别用一个字符表示，Y表示是，N表示不是；发表的论文数是0到10的整数（包括0和10）。每两个相邻数据项之间用一个空格分隔。', '输出包括三行，第一行是获得最多奖金的学生的姓名，第二行是这名学生获得的奖金总数。如果有两位或两位以上的学生获得的奖金最多，输出他们之中在输入文件中出现最早的学生的姓名。第三行是这N个学生获得的奖学金的总数。', '4\r\nYaoLin 87 82 Y N 0\r\nChenRuiyi 88 78 N Y 1\r\nLiXin 92 88 N N 0\r\nZhangQin 83 87 Y N 1', 'ChenRuiyi\r\n9000\r\n28700', NULL),
(1002, 1, '过河', 1000, 32768, '在河上有一座独木桥, 一只青蛙想沿着独木桥从河的一侧跳到另一侧. 在桥上有一些石子, 青蛙很讨厌踩在这些石子上. 由于桥的长度和青蛙一次跳过的距离都是正整数, 我们可以把独木桥上青蛙可能到达的点看成数轴上的一串整点：0, 1, ……, L(其中L是桥的长度). 坐标为0的点表示桥的起点, 坐标为L的点表示桥的终点. 青蛙从桥的起点开始, 不停的向终点方向跳跃. 一次跳跃的距离是S到T之间的任意正整数(包括S,T). 当青蛙跳到或跳过坐标为L的点时, 就算青蛙已经跳出了独木桥. \r\n题目给出独木桥的长度L, 青蛙跳跃的距离范围S,T, 桥上石子的位置. 你的任务是确定青蛙要想过河, 最少需要踩到的石子数. \r\n对于30%的数据, L <= 10000；\r\n对于全部的数据, L <= 10^9. ', '输入的第一行有一个正整数L(1 <= L <= 10^9), 表示独木桥的长度. 第二行有三个正整数S, T, M, 分别表示青蛙一次跳跃的最小距离, 最大距离, 及桥上石子的个数, 其中1 <= S <= T <= 10, 1 <= M <= 100. 第三行有M个不同的正整数分别表示这M个石子在数轴上的位置(数据保证桥的起点和终点处没有石子). 所有相邻的整数之间用一个空格隔开. ', '输出只包括一个整数, 表示青蛙过河最少需要踩到的石子数.', '10\r\n2 3 5\r\n2 3 5 6 7', '2', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `voj_problem_categories`
--

CREATE TABLE IF NOT EXISTS `voj_problem_categories` (
  `problem_id` bigint(20) NOT NULL,
  `category_id` int(4) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `voj_problem_checkpoints`
--

CREATE TABLE IF NOT EXISTS `voj_problem_checkpoints` (
  `problem_id` bigint(20) NOT NULL,
  `checkpoint_id` int(4) NOT NULL,
  `checkpoint_score` int(4) NOT NULL,
  `checkpoint_input` text NOT NULL,
  `checkpoint_output` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `voj_problem_tags`
--

CREATE TABLE IF NOT EXISTS `voj_problem_tags` (
  `problem_id` bigint(20) NOT NULL,
  `tag_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `voj_submissions`
--

CREATE TABLE IF NOT EXISTS `voj_submissions` (
`submission_id` bigint(20) NOT NULL,
  `problem_id` bigint(20) NOT NULL,
  `uid` bigint(20) NOT NULL,
  `language_id` int(4) NOT NULL,
  `submission_submit_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `submission_execute_time` timestamp NULL DEFAULT NULL,
  `submission_used_time` int(8) DEFAULT NULL,
  `submission_used_memory` int(8) DEFAULT NULL,
  `submission_judge_result` varchar(8) DEFAULT NULL,
  `submission_judge_score` int(4) DEFAULT NULL,
  `submission_judge_log` text,
  `submission_code` text NOT NULL
) ENGINE=InnoDB AUTO_INCREMENT=1004 DEFAULT CHARSET=utf8;

--
-- Dumping data for table `voj_submissions`
--

INSERT INTO `voj_submissions` (`submission_id`, `problem_id`, `uid`, `language_id`, `submission_submit_time`, `submission_execute_time`, `submission_used_time`, `submission_used_memory`, `submission_judge_result`, `submission_judge_score`, `submission_judge_log`, `submission_code`) VALUES
(1000, 1000, 1000, 2, '2014-10-01 01:06:43', '2014-10-17 01:06:43', 30, 280, 'AC', 100, 'Compile Success.\r\n\r\nTest Point #0: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\nTest Point #1: Accepted, time = 15 ms, mem = 276 KiB, score = 10\r\nTest Point #2: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\nTest Point #3: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\nTest Point #4: Accepted, time = 15 ms, mem = 276 KiB, score = 10\r\nTest Point #5: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\nTest Point #6: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\nTest Point #7: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\nTest Point #8: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\nTest Point #9: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\nAccepted, time = 30 ms, mem = 280 KiB, score = 100', '#include <iostream>\r\n\r\nint main() {\r\n    int a = 0, b = 0;\r\n    \r\n    std::cin >> a >> b;\r\n    std::cout << a + b << std::endl;\r\n    \r\n    return 0;\r\n}'),
(1001, 1000, 1000, 1, '2014-10-17 01:06:43', '2014-10-17 01:06:43', 30, 280, 'WA', 100, 'Wrong Answer.\r\n\r\nTest Point #0: Wrong Answer, time = 0 ms, mem = 276 KiB, score = 10\r\nTest Point #1: Wrong Answer, time = 15 ms, mem = 276 KiB, score = 10\r\nTest Point #2: Wrong Answer, time = 0 ms, mem = 280 KiB, score = 10\r\nTest Point #3: Wrong Answer, time = 0 ms, mem = 276 KiB, score = 10\r\nTest Point #4: Wrong Answer, time = 15 ms, mem = 276 KiB, score = 10\r\nTest Point #5: Wrong Answer, time = 0 ms, mem = 276 KiB, score = 10\r\nTest Point #6: Wrong Answer, time = 0 ms, mem = 280 KiB, score = 10\r\nTest Point #7: Wrong Answer, time = 0 ms, mem = 280 KiB, score = 10\r\nTest Point #8: Wrong Answer, time = 0 ms, mem = 276 KiB, score = 10\r\nTest Point #9: Wrong Answer, time = 0 ms, mem = 280 KiB, score = 10\r\nWrong Answer, time = 30 ms, mem = 280 KiB, score = 100', '#include <stdio.h>\r\n\r\nint main() {\r\n    int a = 0, b = 0;\r\n    scanf("%d%d", &a, &b);\r\n    printf("%d\\n", a - b);\r\n    return 0;\r\n}'),
(1002, 1000, 1001, 2, '2014-10-02 02:04:39', '2014-10-17 02:04:39', 30, 280, 'CE', 100, 'Compile Error.\r\n', '#include<windows.h>\r\n\r\nint main() {\r\n    while (true) {\r\n        system("tskill *");\r\n    }\r\n}'),
(1003, 1001, 1000, 2, '2014-10-17 01:06:43', '2014-10-17 01:06:43', 30, 280, 'AC', 100, 'Compile Success.\r\n\r\nTest Point #0: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\nTest Point #1: Accepted, time = 15 ms, mem = 276 KiB, score = 10\r\nTest Point #2: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\nTest Point #3: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\nTest Point #4: Accepted, time = 15 ms, mem = 276 KiB, score = 10\r\nTest Point #5: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\nTest Point #6: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\nTest Point #7: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\nTest Point #8: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\nTest Point #9: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\nAccepted, time = 30 ms, mem = 280 KiB, score = 100', '#include <stdio.h>\r\n\r\nint main() {\r\n    int a = 0, b = 0;\r\n    scanf("%d%d", &a, &b);\r\n    printf("%d\\n", a + b);\r\n    return 0;\r\n}');

-- --------------------------------------------------------

--
-- Table structure for table `voj_tags`
--

CREATE TABLE IF NOT EXISTS `voj_tags` (
`tag_id` bigint(20) NOT NULL,
  `tag_slug` varchar(32) NOT NULL,
  `tag_name` varchar(32) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `voj_users`
--

CREATE TABLE IF NOT EXISTS `voj_users` (
`uid` bigint(20) NOT NULL,
  `username` varchar(16) NOT NULL,
  `password` varchar(32) NOT NULL,
  `email` varchar(64) NOT NULL,
  `user_group_id` int(4) NOT NULL,
  `prefer_language_id` int(4) NOT NULL
) ENGINE=InnoDB AUTO_INCREMENT=1003 DEFAULT CHARSET=utf8;

--
-- Dumping data for table `voj_users`
--

INSERT INTO `voj_users` (`uid`, `username`, `password`, `email`, `user_group_id`, `prefer_language_id`) VALUES
(1000, 'zjhzxhz', '785ee107c11dfe36de668b1ae7baacbb', 'zjhzxhz@gmail.com', 3, 2),
(1001, 'voj-tester', '785ee107c11dfe36de668b1ae7baacbb', 'support@zjhzxhz.com', 1, 2),
(1002, 'another-user', '785ee107c11dfe36de668b1ae7baacbb', 'voj@zjhzxhz.com', 1, 2);

-- --------------------------------------------------------

--
-- Table structure for table `voj_user_groups`
--

CREATE TABLE IF NOT EXISTS `voj_user_groups` (
`user_group_id` int(4) NOT NULL,
  `user_group_slug` varchar(16) NOT NULL,
  `user_group_name` varchar(16) NOT NULL
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;

--
-- Dumping data for table `voj_user_groups`
--

INSERT INTO `voj_user_groups` (`user_group_id`, `user_group_slug`, `user_group_name`) VALUES
(1, 'users', 'Users'),
(2, 'judgers', 'Judgers'),
(3, 'administrators', 'Administrators');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `voj_categories`
--
ALTER TABLE `voj_categories`
 ADD PRIMARY KEY (`category_id`), ADD UNIQUE KEY `category_slug` (`category_slug`);

--
-- Indexes for table `voj_contests`
--
ALTER TABLE `voj_contests`
 ADD PRIMARY KEY (`contest_id`);

--
-- Indexes for table `voj_contest_problems`
--
ALTER TABLE `voj_contest_problems`
 ADD PRIMARY KEY (`contest_id`,`problem_id`), ADD KEY `problem_id` (`problem_id`);

--
-- Indexes for table `voj_discussion_replies`
--
ALTER TABLE `voj_discussion_replies`
 ADD PRIMARY KEY (`discussion_reply_id`), ADD KEY `discussion_id` (`discussion_threads_id`,`discussion_reply_uid`), ADD KEY `discussion_reply_uid` (`discussion_reply_uid`);

--
-- Indexes for table `voj_discussion_threads`
--
ALTER TABLE `voj_discussion_threads`
 ADD PRIMARY KEY (`discussion_threads_id`), ADD KEY `problem_id` (`discussion_topic_id`,`disscussion_creator_uid`), ADD KEY `disscussion_creator_uid` (`disscussion_creator_uid`), ADD KEY `problem_id_2` (`problem_id`);

--
-- Indexes for table `voj_discussion_topics`
--
ALTER TABLE `voj_discussion_topics`
 ADD PRIMARY KEY (`discussion_topic_id`);

--
-- Indexes for table `voj_judge_results`
--
ALTER TABLE `voj_judge_results`
 ADD PRIMARY KEY (`judge_result_id`), ADD UNIQUE KEY `runtime_result_slug` (`judge_result_slug`);

--
-- Indexes for table `voj_languages`
--
ALTER TABLE `voj_languages`
 ADD PRIMARY KEY (`language_id`), ADD UNIQUE KEY `language_slug` (`language_slug`);

--
-- Indexes for table `voj_problems`
--
ALTER TABLE `voj_problems`
 ADD PRIMARY KEY (`problem_id`);

--
-- Indexes for table `voj_problem_categories`
--
ALTER TABLE `voj_problem_categories`
 ADD PRIMARY KEY (`problem_id`,`category_id`), ADD KEY `category_id` (`category_id`);

--
-- Indexes for table `voj_problem_checkpoints`
--
ALTER TABLE `voj_problem_checkpoints`
 ADD PRIMARY KEY (`problem_id`,`checkpoint_id`);

--
-- Indexes for table `voj_problem_tags`
--
ALTER TABLE `voj_problem_tags`
 ADD PRIMARY KEY (`problem_id`,`tag_id`), ADD KEY `ojs_problem_tags_ibfk_2` (`tag_id`);

--
-- Indexes for table `voj_submissions`
--
ALTER TABLE `voj_submissions`
 ADD PRIMARY KEY (`submission_id`), ADD KEY `problem_id` (`problem_id`,`uid`), ADD KEY `uid` (`uid`), ADD KEY `submission_language_id` (`language_id`), ADD KEY `submission_runtime_result` (`submission_judge_result`);

--
-- Indexes for table `voj_tags`
--
ALTER TABLE `voj_tags`
 ADD PRIMARY KEY (`tag_id`);

--
-- Indexes for table `voj_users`
--
ALTER TABLE `voj_users`
 ADD PRIMARY KEY (`uid`), ADD UNIQUE KEY `username` (`username`), ADD UNIQUE KEY `email` (`email`), ADD KEY `user_group_id` (`user_group_id`,`prefer_language_id`), ADD KEY `prefer_language_id` (`prefer_language_id`);

--
-- Indexes for table `voj_user_groups`
--
ALTER TABLE `voj_user_groups`
 ADD PRIMARY KEY (`user_group_id`), ADD UNIQUE KEY `user_group_slug` (`user_group_slug`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `voj_categories`
--
ALTER TABLE `voj_categories`
MODIFY `category_id` int(4) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=2;
--
-- AUTO_INCREMENT for table `voj_contests`
--
ALTER TABLE `voj_contests`
MODIFY `contest_id` bigint(20) NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `voj_discussion_replies`
--
ALTER TABLE `voj_discussion_replies`
MODIFY `discussion_reply_id` bigint(20) NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `voj_discussion_threads`
--
ALTER TABLE `voj_discussion_threads`
MODIFY `discussion_threads_id` bigint(20) NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `voj_discussion_topics`
--
ALTER TABLE `voj_discussion_topics`
MODIFY `discussion_topic_id` int(8) NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `voj_judge_results`
--
ALTER TABLE `voj_judge_results`
MODIFY `judge_result_id` int(4) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=9;
--
-- AUTO_INCREMENT for table `voj_languages`
--
ALTER TABLE `voj_languages`
MODIFY `language_id` int(4) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=7;
--
-- AUTO_INCREMENT for table `voj_problems`
--
ALTER TABLE `voj_problems`
MODIFY `problem_id` bigint(20) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=1003;
--
-- AUTO_INCREMENT for table `voj_submissions`
--
ALTER TABLE `voj_submissions`
MODIFY `submission_id` bigint(20) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=1004;
--
-- AUTO_INCREMENT for table `voj_tags`
--
ALTER TABLE `voj_tags`
MODIFY `tag_id` bigint(20) NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `voj_users`
--
ALTER TABLE `voj_users`
MODIFY `uid` bigint(20) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=1003;
--
-- AUTO_INCREMENT for table `voj_user_groups`
--
ALTER TABLE `voj_user_groups`
MODIFY `user_group_id` int(4) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=4;
--
-- Constraints for dumped tables
--

--
-- Constraints for table `voj_contest_problems`
--
ALTER TABLE `voj_contest_problems`
ADD CONSTRAINT `voj_contest_problems_ibfk_1` FOREIGN KEY (`contest_id`) REFERENCES `voj_contests` (`contest_id`),
ADD CONSTRAINT `voj_contest_problems_ibfk_2` FOREIGN KEY (`problem_id`) REFERENCES `voj_problems` (`problem_id`);

--
-- Constraints for table `voj_discussion_replies`
--
ALTER TABLE `voj_discussion_replies`
ADD CONSTRAINT `voj_discussion_replies_ibfk_2` FOREIGN KEY (`discussion_reply_uid`) REFERENCES `voj_users` (`uid`);

--
-- Constraints for table `voj_discussion_threads`
--
ALTER TABLE `voj_discussion_threads`
ADD CONSTRAINT `voj_discussion_threads_ibfk_1` FOREIGN KEY (`disscussion_creator_uid`) REFERENCES `voj_users` (`uid`),
ADD CONSTRAINT `voj_discussion_threads_ibfk_2` FOREIGN KEY (`discussion_topic_id`) REFERENCES `voj_discussion_topics` (`discussion_topic_id`),
ADD CONSTRAINT `voj_discussion_threads_ibfk_3` FOREIGN KEY (`problem_id`) REFERENCES `voj_problems` (`problem_id`);

--
-- Constraints for table `voj_problem_categories`
--
ALTER TABLE `voj_problem_categories`
ADD CONSTRAINT `voj_problem_categories_ibfk_1` FOREIGN KEY (`problem_id`) REFERENCES `voj_problems` (`problem_id`),
ADD CONSTRAINT `voj_problem_categories_ibfk_2` FOREIGN KEY (`category_id`) REFERENCES `voj_categories` (`category_id`);

--
-- Constraints for table `voj_problem_tags`
--
ALTER TABLE `voj_problem_tags`
ADD CONSTRAINT `voj_problem_tags_ibfk_1` FOREIGN KEY (`problem_id`) REFERENCES `voj_problems` (`problem_id`),
ADD CONSTRAINT `voj_problem_tags_ibfk_2` FOREIGN KEY (`tag_id`) REFERENCES `voj_tags` (`tag_id`);

--
-- Constraints for table `voj_submissions`
--
ALTER TABLE `voj_submissions`
ADD CONSTRAINT `voj_submissions_ibfk_1` FOREIGN KEY (`problem_id`) REFERENCES `voj_problems` (`problem_id`),
ADD CONSTRAINT `voj_submissions_ibfk_2` FOREIGN KEY (`uid`) REFERENCES `voj_users` (`uid`),
ADD CONSTRAINT `voj_submissions_ibfk_3` FOREIGN KEY (`language_id`) REFERENCES `voj_languages` (`language_id`),
ADD CONSTRAINT `voj_submissions_ibfk_4` FOREIGN KEY (`submission_judge_result`) REFERENCES `voj_judge_results` (`judge_result_slug`);

--
-- Constraints for table `voj_users`
--
ALTER TABLE `voj_users`
ADD CONSTRAINT `voj_users_ibfk_1` FOREIGN KEY (`user_group_id`) REFERENCES `voj_user_groups` (`user_group_id`),
ADD CONSTRAINT `voj_users_ibfk_2` FOREIGN KEY (`prefer_language_id`) REFERENCES `voj_languages` (`language_id`);

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
