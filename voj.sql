-- phpMyAdmin SQL Dump
-- version 4.2.11
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: May 17, 2015 at 06:04 PM
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
(1, 'PD', 'Pending'),
(2, 'CPL', 'Compiling'),
(3, 'JUD', 'Judging'),
(4, 'AC', 'Accepted'),
(5, 'WA', 'Wrong Answer'),
(6, 'TLE', 'Time Limit Exceed'),
(7, 'OLE', 'Output Limit Exceed'),
(8, 'MLE', 'Memory Limit Exceed'),
(9, 'RE', 'Runtime Error'),
(10, 'PE', 'Presentation Error'),
(11, 'CE', 'Compile Error');

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

--
-- Dumping data for table `voj_problem_checkpoints`
--

INSERT INTO `voj_problem_checkpoints` (`problem_id`, `checkpoint_id`, `checkpoint_score`, `checkpoint_input`, `checkpoint_output`) VALUES
(1000, 0, 10, '18820 26832\r\n', '45652\r\n'),
(1000, 1, 10, '1123 5687', '6810\r\n'),
(1000, 2, 10, '15646 8688', '24334'),
(1000, 3, 10, '26975 21625', '48600'),
(1000, 4, 10, '23107 28548', '51655'),
(1000, 5, 10, '16951 22289', '39240'),
(1000, 6, 10, '8634 13146', '21780'),
(1000, 7, 10, '17574 15337', '32911'),
(1000, 8, 10, '14548 28382', '42930'),
(1000, 9, 10, '3271 17411', '20682'),
(1001, 0, 10, '100\r\nXvpxWEvuxMujM 79 90 N N 0\r\nFWGrVKwgJsImNAzO 100 75 Y Y 0\r\nXHEDY 85 95 Y Y 8\r\nSnJCAbmx 80 90 Y N 0\r\nIjsb 90 78 N N 0\r\nT 78 98 Y Y 8\r\nA 87 90 Y Y 0\r\nASVBj 85 94 N N 0\r\nFmMsZNPOCgCwygIT 85 83 N Y 0\r\nFXJDETWfFCqJnbYyxa 75 82 N Y 0\r\nEQMWSRKNvkunT 80 95 Y Y 0\r\nVdxkZpDNgfvqQOGyyOIG 80 80 Y Y 0\r\nXGIMzWKXeJMXTUwduO 95 81 N Y 0\r\nRuncagD 95 96 Y N 0\r\nTzmkvWKsUOkbBDrnc 90 96 N Y 5\r\nCXBJzEelrzdJ 90 97 N Y 0\r\nUGmNXVYsvpLgmb 85 100 N Y 0\r\nAhteTELcmbv 90 85 N N 0\r\nGAEIg 80 95 N Y 0\r\nYAHRJgqstbZaZPyww 99 85 Y Y 0\r\nQo 90 90 N N 0\r\nDtqLHiMmeHbohAxToJWP 78 75 Y Y 0\r\nXIWnP 85 75 N Y 0\r\nDoWmxImSCdk 95 76 Y N 0\r\nNLFznHxOcV 82 85 N N 0\r\nAKXkyYHWGwXvqMLKiCIR 94 95 Y Y 0\r\nGcgEAFL 79 93 N Y 0\r\nNqUjBPAYxdABnMGcQax 85 91 Y N 0\r\nXPohweOIDDHmb 83 94 Y N 0\r\nYIdB 75 93 N Y 0\r\nDBQxmSnODuYwifv 93 77 Y N 0\r\nRCpKstV 75 90 N Y 0\r\nUnu 82 81 Y N 0\r\nNGeyEHglHxHfghD 81 100 N Y 0\r\nMFLedhUZ 96 75 N Y 0\r\nYYEt 76 95 Y Y 10\r\nR 85 85 Y Y 0\r\nLHJGGF 95 87 Y Y 0\r\nPA 85 93 N Y 0\r\nGKPQfRzvnYtRqUcYYDGi 80 92 N N 0\r\nOsYZnlpdToExY 99 95 Y Y 0\r\nBK 91 85 N N 0\r\nC 83 85 Y Y 0\r\nTvp 90 90 Y Y 0\r\nFzSFDyfqaKOAp 90 85 Y N 0\r\nIIzkbLEYbncWzCZCIiJr 94 90 N N 0\r\nYOwtTfAQymJqjkkh 90 80 Y Y 0\r\nAjZSUNidohrGDgCBpo 91 86 Y N 0\r\nQRhbctxBn 79 75 N Y 0\r\nEIkGwpsyW 85 96 N N 0\r\nIJh 85 81 N Y 0\r\nXe 82 80 Y Y 7\r\nBbLcNTB 95 80 N N 0\r\nMPCNZTyWmaZ 84 85 N N 0\r\nChrB 95 95 N N 0\r\nTlIYwpIbVnurzacD 95 90 N N 8\r\nThywwHUaoxX 99 78 N Y 0\r\nQGnzLidvEBjIcmGIAdI 86 96 N Y 0\r\nDNpKPSUyprOeJFmU 81 93 N Y 0\r\nHYfptVzlESrFRUVdgL 93 92 Y N 0\r\nHcblndXi 90 89 Y N 0\r\nNWBJjdgcI 87 85 N Y 0\r\nShnYCYURiyKoVTq 85 99 N N 0\r\nZbc 75 80 Y Y 0\r\nEGvxpyULGNPQ 90 91 N N 0\r\nTdjasZOAvV 75 90 N Y 6\r\nJA 93 80 N Y 0\r\nXkCWQCTpXjZgCKflzh 86 83 N Y 0\r\nEsNOQfgeERv 75 81 N Y 0\r\nSuyWV 95 81 N Y 0\r\nLJJbpohSV 90 98 N Y 0\r\nMPGJLKflSBtZHKEtcsU 100 99 Y N 0\r\nAQrPmmXO 89 95 Y Y 0\r\nYDCmkIYBPOy 95 91 N Y 10\r\nXLiSnATGAHSSF 95 83 N N 0\r\nSGyOHlvb 90 84 Y Y 0\r\nMRiXqCxTvwToRyyEJgB 95 100 Y Y 0\r\nZNrctMuIhBMLmmi 85 78 N Y 0\r\nHdTpUsEc 98 75 Y Y 0\r\nRAhDEmlvNNicCa 75 92 Y Y 0\r\nIKyNgnEfzGQFnBMd 85 80 N Y 0\r\nJNtN 95 90 N N 7\r\nSBeSjxfxvmQIH 84 91 Y Y 0\r\nEregtaBcUOPuFfOWAxw 95 81 Y N 0\r\nBjcxDhbbJvDLXMVyn 87 79 N Y 0\r\nViFUYKFDOZegK 90 80 Y Y 0\r\nWsr 95 86 Y N 0\r\nJgdPGCFiUwUUfAA 80 92 Y Y 0\r\nO 96 97 Y N 0\r\nJWfAfenceiSgJq 95 85 N N 0\r\nQUJCpMvjCViQ 80 85 Y Y 0\r\nRnVtCGNCmLqmtdly 83 95 Y N 0\r\nQKnLkS 87 80 N Y 0\r\nMuQsSfe 90 97 N Y 0\r\nSkvAUshiLLErXihn 93 85 Y Y 0\r\nKhEmO 88 88 Y N 0\r\nHovCo 90 80 N Y 0\r\nUxZ 75 75 Y N 6\r\nBuD 97 77 N Y 0\r\nHfSAzTvoupTcUlldPWDM 75 95 N Y 0', 'YDCmkIYBPOy\r\n15000\r\n332750'),
(1001, 1, 10, '100\r\nKSSHsCEhNHXIMxnKjszE 93 98 N Y 0\r\nKRXBnwAzDGjpfJgRpKt 95 90 Y Y 0\r\nJBOyaqxicVGB 96 84 N Y 0\r\nHolSWzxphdbM 99 80 Y Y 0\r\nYzYKojgqgraUpqNyJ 95 90 N N 0\r\nLgX 97 94 Y Y 0\r\nHCRuZbWsSDgkOEoj 80 90 N Y 0\r\nS 85 89 N Y 10\r\nAdLhdCBxwmJXRm 78 97 Y N 0\r\nQhgGp 85 87 Y N 0\r\nZFTdFawbiwDG 87 80 N Y 0\r\nJaOThlpkrRkejXMMXgbu 95 80 Y Y 0\r\nNiAntWp 85 91 Y N 0\r\nTe 81 81 Y Y 0\r\nTrFXiYBMzJ 92 87 N N 0\r\nMlhsUsruSKdrAGkUerE 97 92 Y Y 0\r\nNWpjgxcNegmyEGHlPTP 88 94 N N 5\r\nVCXJjomOzywFZBVkDR 87 75 Y N 0\r\nPxZYNvXNdbUtBIp 82 75 N Y 0\r\nTsJkMpF 85 99 N Y 0\r\nBwaIeOrqdatudl 85 80 N N 0\r\nIB 95 90 Y N 0\r\nHmPorGeOs 98 85 Y N 0\r\nDgRHzC 87 82 Y N 0\r\nWkRmkErigra 90 75 Y N 0\r\nMZdQ 85 78 N N 0\r\nNIrWcjt 81 95 Y N 0\r\nMQBfcr 85 80 Y N 0\r\nIXrLPdJhEekBRgsbkqI 95 85 Y Y 6\r\nMWAd 89 75 Y N 0\r\nBVYqUBdV 76 79 Y Y 0\r\nNhmSyMnvHxUkjye 79 90 Y N 0\r\nNUPyQqEhklWtWxeIz 80 95 Y N 0\r\nNsQbL 95 86 N N 0\r\nKEKFcpbDWQXUsQTXvFbs 95 95 N N 0\r\nZzjJWy 90 80 N Y 3\r\nTOWFblEWAuVgAxdHt 95 100 N N 0\r\nHRYFlarRQhe 100 90 N N 0\r\nSGUCaHJVg 95 87 Y Y 0\r\nCsNEZwUWERVFFxLsdyK 85 94 N N 0\r\nPZhrRpBMpm 80 85 Y N 0\r\nLsyePjMaoonOscyyUQY 82 98 Y Y 0\r\nSkPGcYDnYyIp 95 90 N N 0\r\nOaV 90 90 Y N 0\r\nW 75 90 Y N 0\r\nTJjZwq 85 75 Y Y 0\r\nPVFVBEocVjjpg 90 88 N N 0\r\nHMJecx 85 100 N Y 0\r\nPvgXSkNZQNcfJrgTzgm 90 75 N Y 0\r\nVnVXCwwJdbrusXG 89 95 Y N 0\r\nBNziq 95 76 Y Y 0\r\nMNjzTqMCtx 85 95 N N 0\r\nUihwjdZaUXWuMsytRtN 90 88 Y N 7\r\nHdXkkOHRQoG 93 85 Y N 0\r\nDHDAFValkXKFYjznnn 75 100 N N 0\r\nJskIgwiNH 85 85 Y N 0\r\nSeYSBFuVQaEUXwf 75 80 N Y 0\r\nXfGMOTDT 82 75 Y Y 0\r\nGnGatTgZBPgj 90 90 N N 0\r\nK 85 80 Y Y 0\r\nKRJVphYKwQTOMc 95 85 N N 0\r\nDFWtLGoFLXHptkI 75 92 N Y 0\r\nCayHZQXpTpyFPSuJz 87 76 N Y 0\r\nRRlRboFqAgLvzrJ 89 75 N Y 4\r\nDbWHykSi 85 84 N Y 0\r\nWQHneRqIh 90 82 Y N 0\r\nBhGejmW 90 93 Y N 0\r\nAFEEic 95 80 Y N 0\r\nMXhBgPj 96 98 Y Y 0\r\nWJsSWOCR 77 85 Y Y 0\r\nA 79 82 N N 0\r\nWy 95 85 Y Y 0\r\nOtgBaCKAVmMEFxPVcbE 84 75 N Y 0\r\nDnBaraTLVBkPJJj 85 75 Y N 0\r\nKzir 84 75 Y Y 0\r\nPxCMvWOdyZcRW 90 76 N N 0\r\nZcztKxXsrhqSDuxBeN 80 84 N Y 0\r\nEesxZKSACX 80 87 N N 0\r\nCvCT 75 85 N N 0\r\nUwrgH 76 80 Y N 0\r\nVwcGoYzhhVFWGyFzjdn 78 90 N N 0\r\nKQ 75 100 N Y 0\r\nMHJszTi 95 79 Y Y 0\r\nBPJXuWxsGGNYz 87 92 Y Y 5\r\nXWWpW 96 92 Y Y 6\r\nGKIdgeGTHWd 80 77 Y Y 0\r\nMeGS 88 75 N N 0\r\nSblEZfNCkGAkRSrzFADB 90 88 N Y 0\r\nBGLYzCxFVARuGu 87 75 N Y 0\r\nWMQkzgUafGBnqiuBgRpn 77 75 N Y 0\r\nLIWndzmjDozIMTu 80 94 N Y 0\r\nLsPuNFjIzCcppis 80 80 Y N 0\r\nJgycuqAJHELopIoZm 87 85 Y N 0\r\nUJmFKI 75 90 N N 0\r\nRdvGLr 95 85 Y Y 0\r\nWVsTKZHjDEc 90 85 N N 0\r\nVvtFQcwAErUIru 85 87 N Y 0\r\nJKTcYDOhBIJdTBG 94 85 Y N 0\r\nVJqtvLWNUdTEypjOCB 85 94 N Y 0\r\nET 80 80 N N 2', 'IXrLPdJhEekBRgsbkqI\r\n15850\r\n315050'),
(1001, 2, 10, '100\r\nXODSnS 95 81 N N 5\r\nAmAM 95 87 Y N 0\r\nKjuUtQc 79 79 N Y 0\r\nCIKwP 80 95 N N 9\r\nXWINMqSzxcSwHu 90 90 N Y 0\r\nVmyNwsWNrzQzumpkYi 93 100 Y N 0\r\nJNyOUKq 95 97 N Y 0\r\nSbcPEILVKPtjJNI 95 95 N N 0\r\nHWlCiupwDIWsD 95 85 Y N 0\r\nUMaMMna 80 80 N Y 0\r\nFUGEOWMfgVlYlkQ 90 85 N N 0\r\nZECxJOwDbf 90 91 Y N 0\r\nULSdBWvpEoGyrriHXvB 80 89 N Y 0\r\nFdnaONSBrtoKpnLcOwQ 90 86 N N 0\r\nFcuXukEfTJaepfcVLGDo 90 88 N N 0\r\nE 91 80 Y Y 1\r\nWTTcx 99 95 Y Y 0\r\nCSuFsvUjAB 92 90 N N 0\r\nSpBFMARZU 90 86 N Y 0\r\nSJ 85 95 N N 0\r\nACBZClCVEY 80 80 N Y 0\r\nWbpifAcJsMvZsIuxPb 80 80 Y Y 0\r\nLglYGtexoUrlFtvCg 97 75 Y Y 0\r\nQurAslbu 95 85 Y Y 0\r\nCEykvNKWzMSYtFFZqGxB 94 89 N Y 0\r\nFUtuwRZqZGEBsetLG 75 90 N N 5\r\nNLlUN 90 95 Y N 0\r\nXkkZELIs 95 80 N N 0\r\nRLL 97 89 Y Y 0\r\nGzphmddzEKiHp 95 80 Y Y 0\r\nJqatUa 95 99 Y N 0\r\nTlckBkdqgu 88 76 Y N 0\r\nMsANXDtZCoYYKlxJyXxd 75 80 Y N 0\r\nNoUuxKxbhIE 80 95 Y Y 0\r\nWxiiIO 96 90 Y Y 0\r\nIAVsLoFQtsAAXIwkTF 98 99 N N 0\r\nJUCx 85 75 Y N 0\r\nVmJHYTcPKUoLTILytZM 85 78 N Y 0\r\nIGcbksHLMRlngsVqz 75 90 Y Y 4\r\nLrzHFThFFAasgfcZeuw 99 96 Y Y 0\r\nXQ 87 90 N Y 0\r\nS 90 100 Y Y 0\r\nBoN 75 75 Y N 0\r\nYTUXuruVhIaqN 87 80 Y N 0\r\nRZX 80 95 N Y 0\r\nLqQdXchxqHacFo 80 90 Y Y 0\r\nWenYJQMBPFHmut 85 80 Y N 0\r\nPAwecWyvQkikNfPV 92 80 Y N 6\r\nRkihOMXBLaHAcGkSdH 90 89 N Y 0\r\nDqIDtJrPa 94 95 N Y 0\r\nHssCJPipFFBNKqSTI 90 77 Y Y 0\r\nFvwYhmlFDwYLNGJo 76 95 N Y 0\r\nCktjdwA 87 85 N Y 2\r\nXqcLtILWrH 90 95 N Y 0\r\nWUWzuUt 85 80 Y Y 0\r\nITxsJXcGHC 90 94 Y N 0\r\nQawKYU 95 85 N N 0\r\nLXadSVqxcTbaP 93 94 Y N 0\r\nSqrSt 76 85 Y N 0\r\nYVpaVs 95 75 Y Y 7\r\nJI 95 90 N N 0\r\nOgYRmoSiNWVbSdocM 85 95 N Y 0\r\nYVCxaZPHiY 99 81 Y N 0\r\nBWqPJ 99 84 N Y 0\r\nXnBWuQdMh 93 85 N N 0\r\nEwKTLdQvmUJkdD 82 76 Y Y 0\r\nU 83 81 Y N 0\r\nApIwR 95 75 N N 0\r\nYFPeOIBN 99 97 Y N 0\r\nXJxUn 95 78 N Y 0\r\nYF 75 85 Y Y 0\r\nRMEpQYLSw 100 90 N Y 5\r\nTCIJOG 84 99 Y N 0\r\nFZXWWtgXTxSZFzDeSY 80 82 N Y 0\r\nWdxvfeOc 97 75 N Y 0\r\nSIlTmjkbHKqRirpOCAgq 84 90 N N 0\r\nHFjBUPtiaUxoZc 90 85 N Y 0\r\nSCxZrJjsfqHWYQf 85 94 Y N 0\r\nVhLeCMseuz 85 85 Y Y 0\r\nSDKqshkHjxs 75 98 Y N 0\r\nOZxC 95 80 Y Y 0\r\nU 87 75 N N 0\r\nPrSIkzb 80 77 Y Y 0\r\nUwmERlxVvWA 78 80 N N 0\r\nNSKbkCGjCf 75 85 N Y 0\r\nMriCgsUio 80 95 N Y 0\r\nZWRyHTJU 87 80 Y Y 0\r\nYoQBzJtKTZ 90 86 N N 0\r\nEGK 95 96 Y Y 0\r\nCYFVbgGoFwEhcgMTUw 95 90 N Y 0\r\nGrRWiR 85 80 N Y 0\r\nNYNlVNgO 95 75 Y Y 0\r\nNNRVpAF 85 89 N Y 0\r\nFMuTUqk 95 95 Y N 0\r\nPXD 85 95 N Y 0\r\nLyFr 99 100 Y Y 10\r\nOGLltVsbdriqDg 90 80 Y N 0\r\nScAWqkBXwtWz 90 95 Y Y 0\r\nEqgEwtbpSWwIWHxAN 94 80 N Y 0\r\nYmGW 97 85 N N 0\r\n', 'LyFr\r\n15850\r\n376500'),
(1001, 3, 10, '100\r\nUFZOcQTBHjpwCMYn 90 95 Y Y 0\r\nKyrK 90 97 N N 0\r\nCysmgRwxDSNLpjrl 85 75 N Y 0\r\nELrEcHjXsUoiRd 76 93 Y N 0\r\nBJKXRcXMkoJGSfV 75 90 N N 0\r\nJlEaFDVDTdBUyQWax 80 85 N Y 0\r\nZCcbRRBkRfo 75 86 Y Y 8\r\nRkShJ 92 98 N N 0\r\nT 80 98 N N 0\r\nHGkgmL 90 96 Y Y 0\r\nLOlutboG 75 78 N N 0\r\nWPgpHocpVaFqHDsTqciN 80 75 N Y 0\r\nVPHEqLbNtBkYV 80 90 Y Y 0\r\nDHswaajQYBbZJeiqslc 97 93 N N 0\r\nZedpSTlOCjOSg 84 90 N Y 0\r\nDIJswe 90 75 Y Y 0\r\nCHPjh 90 87 Y Y 0\r\nQGdeyvvRut 99 75 Y N 0\r\nIB 85 90 N N 0\r\nAuCAAiPbUF 94 90 N N 0\r\nI 85 89 N Y 0\r\nUEGKE 93 85 Y N 0\r\nPyCqoFmtFcSItE 100 90 Y N 0\r\nPnrNRiqJ 75 95 N N 0\r\nTdtKaStTwPVWV 93 90 Y N 0\r\nX 88 76 Y N 0\r\nM 93 99 Y Y 0\r\nCS 79 93 Y N 0\r\nUAGVVOiUT 75 96 Y N 0\r\nPV 96 80 N Y 0\r\nVx 75 79 N Y 0\r\nBDgEmERNOmyX 91 75 Y N 0\r\nLJE 95 83 Y Y 0\r\nUIFGKBcTHsGxYANNHraq 85 89 Y N 0\r\nPTrTHHmcwH 85 75 Y N 0\r\nOgXBP 90 75 Y N 0\r\nWp 83 79 Y Y 0\r\nHDrhXVtEPAOR 83 75 N N 0\r\nLlpQK 80 90 Y N 0\r\nXqgkRUiFXMeWTSMIXae 95 90 Y N 0\r\nVxuemIoRgjUQe 93 93 N N 0\r\nUwS 90 76 Y N 0\r\nHOMNGhJfueorHMl 99 100 N Y 0\r\nHLawxZVlOg 80 99 N Y 0\r\nZTLejisS 80 79 Y Y 0\r\nCvvNscCnZVOiriPN 90 95 N N 0\r\nANyQeAqkYvGhFPCqWA 80 90 N Y 0\r\nUYBTL 80 85 N N 0\r\nDCBVl 82 95 N Y 0\r\nNdsOgK 76 75 N Y 0\r\nMFEuuuBChlMLjKO 90 88 N Y 0\r\nF 95 75 N N 0\r\nXMVNIZlUaUHaf 80 85 N Y 0\r\nBGTi 86 95 N Y 7\r\nTP 85 93 N Y 0\r\nTgLwEuo 85 83 Y N 0\r\nHHeXOMfgB 96 89 N Y 1\r\nDiymeUrkzGWyAJGJtm 76 100 N Y 6\r\nIfcRnqMJkslbJ 88 88 Y N 0\r\nXKYDCSfK 80 95 N N 0\r\nYO 90 92 Y Y 0\r\nZHFXiqnGIJY 75 80 Y N 0\r\nHUeQYggJJVpWayTyys 93 85 Y Y 0\r\nSnwoWt 80 92 N N 0\r\nZrKDpgLzzLRseYxwHFY 90 90 N N 0\r\nTJrFYWUrUTKp 75 82 N Y 0\r\nUkPbxhLAJPwJJgVU 97 88 N Y 0\r\nOTDoNRB 91 97 Y Y 0\r\nInnTSmiYxKKTfsUbn 80 75 Y N 0\r\nRXsvpaqbPjLXBlNaVrc 90 90 Y N 0\r\nNSBzurUwDWbAYHY 83 98 N Y 0\r\nWSTUbidxKesLHEJd 90 99 N Y 0\r\nVDKrj 100 94 N Y 0\r\nSdTmfCQEuOZ 95 82 N N 2\r\nVmKkpQPBwp 75 90 Y N 0\r\nHsZoSIMOmMxJKQl 80 80 N Y 0\r\nYLnnpg 95 87 Y N 0\r\nNZEUZGLRZCkYvb 90 90 Y Y 0\r\nUM 90 95 N Y 0\r\nTqLUjeKMZ 85 97 Y N 0\r\nMD 80 95 Y N 0\r\nYCKUZhGAAXAUZu 92 90 N Y 0\r\nTeeApAXcvDbCxqGbRbsX 85 85 N Y 0\r\nSWKpTiwqleAkXZisYF 87 92 N N 0\r\nJOuZVAqcd 88 95 N N 0\r\nAGcOK 86 89 N Y 0\r\nFtDLkGP 95 93 Y Y 0\r\nKH 83 89 N N 0\r\nTzXvxeEF 90 88 N Y 0\r\nYgQsmqAometSIsgT 84 75 Y Y 0\r\nFLglQZOCYBjbeJFEpf 100 86 Y Y 0\r\nCq 80 95 N N 0\r\nLM 90 88 N N 0\r\nRj 77 95 N N 0\r\nWdDRjsOnJ 85 95 Y Y 0\r\nBAf 88 85 N Y 0\r\nX 91 85 N N 0\r\nMfhcDIwTiiCR 95 94 Y Y 0\r\nYGxZUsVuOSVpGSAn 87 98 Y N 0\r\nTkcTzncqZDENcPOL 89 90 N N 0', 'HHeXOMfgB\r\n15000\r\n311200'),
(1001, 4, 10, '100\r\nPbc 86 85 Y N 0\r\nHXekOchIacYeRpNpqxi 75 78 N Y 0\r\nVMFuCwR 76 94 Y N 0\r\nRFESPRSwimdNOupwD 80 95 Y Y 0\r\nAkibXDoYjl 90 85 N Y 0\r\nMiwTLxayOFxAjBViRCE 86 100 N N 0\r\nTrbOXCPmjbSY 95 83 Y N 0\r\nHAAd 97 85 N N 0\r\nLZWnJ 84 75 Y N 0\r\nXRZJiIM 75 98 Y Y 9\r\nIwyd 88 89 Y N 0\r\nSgzbizBGhc 85 85 Y Y 6\r\nOFWSciGYcyCvtllNPNl 80 95 N N 0\r\nIJLZmDJulOjSTOr 85 100 N Y 0\r\nIlB 91 96 N N 8\r\nSIu 80 88 N Y 0\r\nDkhOUqwZdT 85 95 Y N 0\r\nPjXKeUUW 95 84 N N 0\r\nVmQSKvueLmRedNUMHcLO 98 75 N Y 0\r\nBXJTdaZZlsbMaa 77 80 Y Y 0\r\nJsGVsFwcu 86 85 Y N 0\r\nOvNXiblcXgTVnI 77 92 N N 0\r\nSMfFDgoG 94 93 N N 0\r\nUKkFkFNtXIzWzkPTN 95 80 N Y 0\r\nSvjEjgBcHBTRa 75 90 Y Y 0\r\nSRUoWwMmFLC 95 75 Y Y 7\r\nRqJlWkCzrahlfQTl 78 90 Y N 0\r\nXM 83 95 N N 0\r\nDZGYKrDvBa 88 94 N Y 0\r\nJMWOjjgNEtdAuFJZxkVL 75 80 Y N 0\r\nRjVuxQkpbvImBSv 89 93 N N 0\r\nShRFbbvWZNN 90 75 Y N 0\r\nBtqngENjocSpxAuZlcst 94 85 Y N 0\r\nJKGnBehLBdPB 91 90 Y Y 0\r\nCxDMfiuHactmHL 85 77 Y N 0\r\nFyMCeegreCzeGiF 80 82 Y N 0\r\nMUmmSFNEqPuGrVMJV 94 100 Y Y 0\r\nESiXBieIwXjES 85 79 Y Y 0\r\nLbEUGjSHXFqNr 80 90 N N 0\r\nBNboso 99 87 N Y 0\r\nUGRLkihagF 75 95 Y N 0\r\nJgbbaZAhFzAWWU 76 90 N Y 8\r\nFLiHlZanTCruAiLek 96 86 N N 0\r\nBbCPScsCTdaDmgWuh 94 91 Y N 0\r\nSYWHdiAzRWdckWd 80 85 Y Y 0\r\nEvwptbmXArCR 95 95 N Y 0\r\nRY 98 92 Y Y 0\r\nGQUtOFJUR 95 97 N Y 0\r\nIHgVC 95 90 Y N 0\r\nJoVSgwUKMJfBOWnR 77 80 Y Y 0\r\nWThogBEIlxzGg 76 78 N Y 0\r\nPTjePsyUXzHULWtBvxzD 79 85 N N 0\r\nPykCKPyIye 84 96 Y N 0\r\nVWKNHlIUETsruuMFymJ 75 83 N N 0\r\nXVjgfdnPfWCoQLtskHG 95 90 Y N 0\r\nFLxTWlReygXsMQOi 90 95 Y N 0\r\nVSSIBzTVdqEjb 85 83 N Y 0\r\nICfTwPOuTpJIBcuPM 79 80 Y Y 0\r\nLIfnfxyFAilueGsiXbK 100 90 N Y 0\r\nILwZpBDiuoMhXwswxgEe 91 99 N N 0\r\nHgHAeuBIcJ 91 75 Y Y 7\r\nMLB 100 84 Y N 0\r\nG 90 80 N N 0\r\nRoTyLtQwejye 80 75 N N 10\r\nPjrf 93 84 Y Y 0\r\nMQrpSTxSdjQAq 93 75 N Y 8\r\nZyejTNiikWWHHyhI 97 92 Y N 0\r\nZNUegFN 95 85 N N 0\r\nC 90 78 N N 0\r\nBIccuOyGTp 88 95 N Y 0\r\nZmHNvOxTWUMjd 90 77 N Y 0\r\nSKMaiZvuWvgEdMmzzDZ 80 90 N Y 6\r\nNqaUtbTiXhp 80 99 N Y 0\r\nTLujwKJIHAPslBSjXPF 80 75 Y N 0\r\nOnIIxittrvwrLXOg 78 78 Y N 6\r\nGeqn 77 75 N Y 0\r\nEdwUkyV 75 90 N Y 0\r\nY 82 78 N Y 0\r\nDrhUzffpm 90 88 N Y 0\r\nFeYbszANUnub 79 77 N N 0\r\nBxELfghqaNDhRG 80 94 N Y 0\r\nJbfWtaM 85 91 N Y 0\r\nFAIUHwUVMgvkWXVaBdV 94 90 N N 0\r\nDqQlBCPHLF 94 80 Y N 0\r\nQavAC 75 98 Y N 0\r\nSwkfKqtTG 80 80 Y Y 0\r\nZhCIZAGyhajH 88 91 Y Y 0\r\nLZ 84 78 Y N 0\r\nHUXJFQzmIhfFR 89 80 N Y 0\r\nJEavDBAzVy 93 86 Y N 0\r\nCNpKdOcj 87 78 N N 0\r\nYjaRmeRbQQ 88 86 Y Y 2\r\nHfLkfcP 98 80 Y N 0\r\nIuzgXQyhMgghbM 94 85 Y N 0\r\nUwPRvymKaTxLhXPLgS 87 95 Y N 0\r\nAsqWOw 85 91 Y N 0\r\nUcrOLkHfk 77 75 Y Y 0\r\nLMQGBivsyG 89 80 N Y 0\r\nLYfhBBKEvv 92 96 N N 0\r\nEKwHlSMSICqMs 79 75 Y N 0', 'IlB\r\n14000\r\n320050'),
(1001, 5, 10, '100\r\nRDVRyo 75 89 N N 0\r\nNbLorLPWFust 75 85 N Y 0\r\nPYjzHwP 99 94 Y Y 0\r\nFcfubnlnNNUPGQQks 80 83 Y N 0\r\nHlkyMwOyDtQ 90 85 Y N 0\r\nYULUAHWXNXglhEOpu 90 95 N N 0\r\nUVrGVYXgSay 87 89 Y Y 0\r\nN 82 75 Y N 0\r\nCiZdpzk 85 92 Y N 0\r\nYGoIYPAaUZB 75 95 Y N 0\r\nVsULAuYGLRJTn 97 77 Y N 0\r\nEdOairN 80 90 Y N 0\r\nQidSkeNNiZuEQMSS 95 87 N N 0\r\nCkeyipoORDPVzh 90 85 N Y 0\r\nS 86 95 Y N 0\r\nRsk 90 95 Y Y 0\r\nJoUxXo 75 85 Y N 0\r\nVEyWcKmMwEECmXUUiN 99 85 N N 0\r\nEgYwAo 75 90 Y Y 0\r\nPCPoQWeo 79 95 Y Y 6\r\nPDhTYMyNDvkr 80 75 N N 0\r\nFGeahCxoC 80 98 Y N 0\r\nGwmWBkiqsKZQmm 85 95 Y Y 0\r\nOzlKFaZupcVfJeH 80 80 N Y 0\r\nWPovsYBSwhEKRpsaPzA 93 75 N Y 0\r\nSjHddMMKtLE 91 95 Y N 0\r\nIb 80 90 Y N 0\r\nEgZA 89 80 Y N 1\r\nHJxEs 96 75 Y N 0\r\nRqhYuUtNieNUnsvhaH 92 85 N N 2\r\nRdolbbFE 95 85 N Y 0\r\nAgJKkdhVMwNueyuhMi 92 91 Y N 0\r\nMOdkuwdyuvUuKZnD 85 89 N N 0\r\nFIOpJgZoWKpCo 82 98 N Y 0\r\nLAzGkTsTKZuWcpWx 95 85 Y Y 0\r\nBGbeVtkPhUzRVPddWi 98 76 Y N 0\r\nEifucsMhQovhiHzmYrd 75 90 N N 0\r\nASwu 96 80 Y N 0\r\nAAWCaBffGJongVqwkk 76 80 Y Y 0\r\nNFcgTeyT 99 85 Y N 9\r\nDGMqW 95 100 N Y 3\r\nNmPsTEYOkY 81 80 Y Y 0\r\nKylj 95 85 Y Y 0\r\nMKEamibK 97 95 N N 0\r\nSfXlWCEcosb 95 90 N N 0\r\nMShPPnQtdnIYWCJJF 95 85 Y Y 0\r\nBxQVYzQuOtoC 85 80 N N 0\r\nOAoQGn 92 81 Y N 0\r\nHHzpC 81 88 N N 8\r\nQOXBqRdnYAs 85 95 Y Y 0\r\nVjvQGFcMjvXTSDK 95 81 Y N 0\r\nAzlksTEIhMRSJRhxy 89 75 N Y 0\r\nPrFscCCSRZFQ 84 88 Y N 0\r\nTtahPgzMrPvncCsOMMzp 90 100 N N 0\r\nPKVULSvBNLSDlQ 90 90 N Y 0\r\nAVEtFUveYxcnPfOXfHI 80 92 N N 0\r\nNmBWPsDTtvDlAt 100 80 N N 0\r\nF 80 81 Y N 0\r\nRqHsLEooY 80 90 N N 0\r\nPtOLgkhcR 85 80 N Y 0\r\nPLOHK 94 90 N N 0\r\nP 82 75 N N 7\r\nUmciCWoyT 80 78 Y Y 0\r\nGAWDweGDYskHnQk 90 100 Y N 0\r\nIJE 81 83 Y Y 0\r\nOnVFuxevWzTDA 93 86 Y N 0\r\nAHrHvyaPCfsVNmDIWWo 86 91 N Y 6\r\nKCBSLlcfJyAPBj 85 75 Y Y 0\r\nQawOMDkeJlormRnhe 90 81 Y N 0\r\nOT 90 98 Y N 0\r\nBYaPLabeEVwtrB 88 82 N Y 0\r\nVamAq 85 89 Y N 0\r\nVTiUA 95 80 N N 0\r\nSSNkaLdkxmAtP 85 95 N N 0\r\nOJB 98 91 N N 0\r\nLIqQalcebHdzj 75 96 Y Y 10\r\nWKlGLytTd 95 89 N Y 0\r\nPIXhCtLSMoCCA 96 80 Y Y 0\r\nULzmzuqKLBoAFtK 80 80 N N 0\r\nZnlCehX 77 77 Y N 0\r\nCEjxxKKGDf 90 90 N Y 0\r\nHvpDIKdEiUzWvt 90 81 Y N 0\r\nTlMuWvRTlNj 97 75 N N 8\r\nYIFvifAtXjyvDF 76 97 Y N 0\r\nMtCNoAEoqOJPv 95 90 N Y 0\r\nKflCVSqbOBK 90 81 Y Y 0\r\nFc 91 85 N Y 0\r\nXpWdeRA 89 76 Y Y 0\r\nOorwfyqXYbZjrTpclNF 84 95 N Y 0\r\nUoysYXmlvO 81 95 Y N 0\r\nAMlBgQfD 80 97 N Y 0\r\nUpOkXMuyURCCRaoCw 80 80 N N 1\r\nFtIeDqx 98 90 Y Y 0\r\nCiVc 75 95 Y Y 0\r\nBdKO 75 75 Y Y 0\r\nNhDwAd 80 85 Y N 0\r\nKUTLEFlaYoIojUVk 96 81 Y N 0\r\nCBEsPPZB 75 75 Y N 0\r\nJvE 76 93 Y Y 0\r\nCgjkSvcwDthjnprSHxx 95 75 Y Y 0', 'DGMqW\r\n15000\r\n350700'),
(1001, 6, 10, '100\r\nMzYCufzWJkGT 95 90 N Y 0\r\nKYmIQs 80 85 N Y 0\r\nNVufr 95 94 N N 0\r\nJmhCuEFOUoKgI 75 88 N N 0\r\nKyRmZdbJMfYTzbZS 78 90 Y Y 0\r\nESBjuYJuibPovnKCdyQ 95 77 Y N 0\r\nSfKyrATJa 90 85 N N 0\r\nJpPNv 80 85 N N 0\r\nQLXkCluFcKh 85 75 N Y 0\r\nTfYqUD 90 85 N N 0\r\nFAyWTFfabq 80 95 Y N 0\r\nRktoKBKAm 90 83 Y N 0\r\nFNdCECSt 95 81 Y N 2\r\nKsHzrelGZguPwheHhw 79 80 Y Y 0\r\nFprWTbg 90 95 N N 0\r\nSgXaGDAruSFH 90 75 N Y 0\r\nIw 79 92 Y N 0\r\nAj 96 75 N Y 0\r\nGRXyYlVokmXU 85 80 N N 0\r\nXYhTEVBhVhGbtlEEDapv 96 95 Y N 0\r\nMIcOWomeJ 90 75 N N 0\r\nAicIWqZoiKLlExLgYq 75 77 N Y 0\r\nMpThDPKOTcuHdAsCOj 88 75 N Y 0\r\nJhHKDBmYwDjI 90 75 Y N 0\r\nPZIp 89 78 Y Y 0\r\nKGrTUPBKfHiGdkhDyp 79 99 N Y 0\r\nIVzqVKMUtMEMNMhDZoC 77 95 Y Y 1\r\nUH 95 76 Y Y 0\r\nHZaQTPJAIDfxlBWa 97 95 N Y 0\r\nOYqqFxNEKi 92 93 Y Y 0\r\nYEIGXpWWAVdjYliI 75 95 N N 0\r\nKiEZUReyFC 76 96 N N 0\r\nAKVxHnqsq 100 84 Y N 0\r\nNeynYn 80 100 Y Y 0\r\nMGYvGdJtzgDQMJbKiqi 90 91 N Y 0\r\nCJxNdGRvszHMJ 90 100 N Y 0\r\nQaW 84 90 N Y 0\r\nYVJbiEvWMhkmwpk 95 75 N N 0\r\nJgGIdXehJoJXBFEZ 79 85 N N 0\r\nHgdZeHAia 90 95 N N 0\r\nKVRKybLWtgvMO 90 79 Y N 0\r\nFqQiwCWcYwwJFRTzs 83 81 Y Y 0\r\nExHBuhysmTCImTNM 95 85 N Y 0\r\nVRPKYyLs 90 95 Y N 0\r\nWRhPbCFTEGoIsYJ 95 85 Y Y 0\r\nYJrRSaXAyCFXthWKXb 95 90 N Y 0\r\nLSGZRyroxESqlTDLPygW 75 84 N Y 0\r\nOAzBzPhGwXBbXyceKAh 75 79 N N 0\r\nMRcf 78 75 N N 0\r\nPnocRXHxa 90 94 N N 0\r\nDSrYGnQkbkaJEj 89 83 Y Y 0\r\nMDNbsqgFkkNJcKSMltgt 92 79 N N 0\r\nXQjtBfngEEWlCWkDmWxs 99 85 N Y 0\r\nLWxGGzcULfdYz 83 91 N N 0\r\nXkkjQEZvHuxmj 80 100 Y N 0\r\nQHorTyeoyv 95 85 Y Y 0\r\nXhKjVOvliOE 95 95 N N 0\r\nHsbIENhJzHBIybuib 81 80 N N 0\r\nEx 80 83 N Y 0\r\nKOHSxFDkI 95 80 N Y 2\r\nDNiRiR 85 76 N Y 0\r\nTyyWUjDkDzrvlJxhdu 89 95 N Y 0\r\nMnadQyg 85 95 Y N 0\r\nAEhHsxsQNLlzX 89 75 N Y 0\r\nQlbGWGQiLuXQwjQdXjqw 95 91 N Y 0\r\nYeacrY 78 85 N N 0\r\nAYwyiyCg 87 95 Y N 0\r\nAIrEnhkkiMRCw 93 96 Y N 0\r\nAlerTviL 100 75 N Y 1\r\nQa 80 90 N Y 0\r\nEL 88 80 N Y 0\r\nHZhEUJKFapBYiXrIBmLl 95 80 N N 0\r\nPVTrtVL 75 85 Y N 0\r\nCrUuGaAAWY 91 80 Y Y 0\r\nF 98 90 N Y 10\r\nEDzMsdUoHZRbm 92 86 Y N 0\r\nSWgAnBqjdspqimqSnIe 80 79 N N 0\r\nTiywblZKQxphP 95 75 N N 0\r\nAbVxejWeLkARSHS 90 90 N N 0\r\nBTSNYgaM 90 85 Y N 0\r\nIBKYOiYOx 90 95 Y Y 0\r\nGkkFOl 84 96 Y N 0\r\nOfWoBlbIdoxZiFQTDqb 92 90 N N 0\r\nKjLfr 80 90 Y N 0\r\nDQ 80 80 Y Y 0\r\nVHdwANLLL 97 93 Y Y 0\r\nXZgHcNSvCmhsYr 95 87 N Y 0\r\nLglligckY 88 95 N N 0\r\nGWamLFvttqLvqYEk 75 91 Y Y 0\r\nBkIcJbGaqMsx 80 91 N N 0\r\nVzpyHxfHMkWKGLmKmBD 90 88 N N 0\r\nRnNNqDfDPe 98 90 N Y 0\r\nWKUwbrZUPSlemyHnG 95 77 N Y 0\r\nUuPiJyTrZldgFC 95 86 N N 0\r\nOINCIjHytMtjypLS 89 85 N N 0\r\nXPmwFEm 75 94 Y N 0\r\nXuop 80 75 Y Y 0\r\nXdwaEcQOwXmIvDeVFn 90 85 Y Y 0\r\nXyYOt 95 96 N Y 0\r\nOpiGinpQf 86 80 N Y 0', 'F\r\n15000\r\n324650'),
(1001, 7, 10, '100\r\nPUQUnnMCvvifcYKS 91 81 N N 0\r\nLKmTZKyzAogY 96 99 Y N 1\r\nHxgsRNdLJzcPYIi 82 92 N Y 2\r\nAjpLBYCvsxjmqY 95 91 N Y 0\r\nTqihhV 85 83 Y Y 6\r\nHRC 89 80 N N 0\r\nH 100 85 Y Y 0\r\nNixtCBiobBBcOxbGmJ 79 95 Y N 0\r\nXq 75 81 Y Y 0\r\nETtrHHnqiLdpwQKei 81 94 N Y 0\r\nNYL 95 75 Y N 0\r\nXUBCvg 95 88 Y N 0\r\nKsoPUD 90 84 N N 0\r\nXeeiHfsVEaqQrbtHlSb 90 75 Y Y 0\r\nUCUUW 98 80 N N 0\r\nARz 75 95 N N 1\r\nLRiaFqnoGFWLYokaNauW 81 91 N Y 0\r\nHyrqIxzusfvxXQ 92 95 Y N 0\r\nZugb 84 85 Y N 0\r\nHuECLLgDZETrNuxx 90 95 Y Y 0\r\nGMUeTHqzlB 94 75 Y Y 0\r\nWcaiZXCiTYUJjeUhIxz 95 92 N N 0\r\nChPCIEoAYbPSTwqUTy 98 75 N Y 0\r\nSTnIrpcGfyuqKlop 92 91 Y Y 0\r\nMhvuI 95 95 N N 0\r\nJJjOVCtL 90 99 Y Y 0\r\nQ 97 77 Y N 0\r\nVCbsLD 95 94 Y N 0\r\nWGtaOifRd 90 76 Y N 0\r\nErCwyPRnxvZaeXzyqC 80 99 Y Y 0\r\nF 80 76 Y N 0\r\nEqWWrL 87 100 N Y 4\r\nCW 95 80 Y Y 0\r\nTZYiouTkHas 75 75 Y Y 0\r\nYBqilxkGZJx 94 100 N Y 8\r\nZhQ 85 86 N N 0\r\nVqVOGkunHXMjzFhKT 82 84 Y N 0\r\nYgjIMJvrSwpjyBt 85 80 N N 0\r\nNSIjGAPKYLIb 98 85 Y Y 0\r\nPmqLFEWIgBblJnAxKmE 75 99 Y Y 0\r\nGCJlSsKJXzHCzIt 80 90 N Y 0\r\nChMGtPHhjP 97 95 N N 0\r\nUERuLLCJbpvr 75 97 Y N 0\r\nIHfRyvenjUNvZ 85 75 N N 0\r\nHrysA 86 95 Y N 0\r\nEeoJjO 88 84 N Y 0\r\nGdZaBCPYUdUa 89 85 N Y 0\r\nMKeeBYun 99 93 N Y 0\r\nSl 95 75 N Y 10\r\nKLaTAxVp 92 86 Y Y 0\r\nMaNkrMVOIxjJiYZX 80 83 Y N 0\r\nLUOedeGTBheXtSdL 91 86 N N 0\r\nQuektgzYapx 90 77 N N 0\r\nHdqsc 85 85 Y N 0\r\nEB 85 80 N Y 0\r\nDBGfXvy 95 96 Y N 0\r\nRiIQjTwl 80 81 N Y 0\r\nAyOs 85 98 Y N 0\r\nOtqaipehLnhnxXnBkx 75 90 Y Y 0\r\nOA 81 88 Y N 0\r\nT 98 92 N N 0\r\nDDYefzXNngYeeueBH 90 95 Y Y 0\r\nCOwFGmydoBP 87 75 N Y 0\r\nTMkfvMdaChWfbREF 90 75 N Y 0\r\nMurkdxrRZWbCmvOTPvX 80 99 Y Y 0\r\nCHOp 77 80 Y Y 0\r\nXaCeksUvjVYoTDDlA 95 85 N N 0\r\nQjiNbZXtTKPMqKDfC 99 90 Y N 0\r\nIRtjKjVZYFGo 95 95 Y Y 0\r\nI 80 79 N N 0\r\nBhcbBj 90 80 Y Y 0\r\nScTyrOqPI 85 91 N Y 0\r\nQvT 95 80 N N 0\r\nNtVJkwJ 85 78 Y N 0\r\nMfhGppwKNPRNMJ 75 85 Y Y 0\r\nAqjRLvrFzTTOd 88 99 N N 0\r\nVyPhUiVNUrlIKJGTIJMV 78 85 N N 0\r\nDSIhYSkoGUPvJlBWCPs 97 85 N N 0\r\nVAHle 92 87 Y Y 0\r\nIzCSMnowRthplqftD 91 100 Y N 0\r\nVOnGdpBBBeMJFP 76 80 N Y 0\r\nHZRPVHGQ 90 85 N N 0\r\nVjRxopMjUjh 95 88 Y N 0\r\nF 93 90 Y N 4\r\nKDIiqBJvyVKmYK 79 78 Y Y 0\r\nVjbvWqtedXaCIlkFa 83 85 Y N 10\r\nAzSQFau 92 99 Y N 0\r\nVo 85 98 N N 0\r\nRr 91 94 N N 0\r\nNwJjjFhgc 91 90 Y Y 0\r\nVjrlhAGmIK 85 82 Y Y 0\r\nR 80 90 N Y 0\r\nWFiVNPPGtLyYutaXuP 93 97 N N 0\r\nVE 90 98 Y Y 0\r\nJkMtq 75 85 N Y 0\r\nKnCvGPSsFGvSy 80 79 Y N 0\r\nZjNuJifeLfDCp 88 94 N N 0\r\nSSkVniNxsPaduyghLQFq 75 99 N N 0\r\nPW 78 76 N Y 0\r\nPCvt 81 80 Y N 0', 'YBqilxkGZJx\r\n15000\r\n366150'),
(1001, 8, 10, '100\r\nZWbnpvRIZYJkleTdfZm 75 82 N N 0\r\nBVfxDT 85 99 Y N 0\r\nXPigvuqORPVhH 75 97 Y Y 0\r\nDRbWXUfI 86 95 N Y 0\r\nQrdEmydUhZmVaZKHPlY 80 90 N Y 0\r\nR 80 83 Y Y 7\r\nMXJurXEyZkLbB 80 95 N N 0\r\nHQNyPigyvBddctC 95 90 N Y 0\r\nSrYvKdTfbO 95 90 Y N 0\r\nXEpMyTjZHx 75 85 N Y 0\r\nVhxFByGDdjU 82 79 N Y 0\r\nTSJF 75 85 Y N 0\r\nRaNxtaKTFUH 75 85 N N 0\r\nQ 85 90 Y Y 0\r\nSNdToBHYhIODk 95 80 N N 0\r\nZUFXPiUqfVJOjec 90 95 N Y 0\r\nTtPGxHTxSksAeChr 97 85 Y N 0\r\nTBjcMyfnmOTAFRBAy 80 95 Y N 0\r\nNeNNKuGPA 85 85 Y Y 0\r\nRjpJU 90 100 N N 0\r\nHdpCfN 96 78 Y N 0\r\nMryDEMkOfqwSYEMDzvw 84 93 Y N 0\r\nQJSTGG 85 75 N N 0\r\nOpG 75 95 Y Y 0\r\nOKvIUnohvvH 85 90 Y Y 0\r\nUvIkrDvw 85 95 Y Y 0\r\nDK 97 80 Y Y 0\r\nZhFBfNI 93 87 Y Y 0\r\nCLjLchtBYEq 95 87 N Y 0\r\nPZcwNuqupy 81 81 N Y 0\r\nSeRxDAjSxRXhmAMkv 79 85 N N 0\r\nVZVMSMwqGlJdsuE 92 90 N N 0\r\nDdYSFBVugwKUAnE 95 98 N N 0\r\nTsvFTlmekL 75 80 N N 0\r\nUvTUZWkSvmjUPDaAZ 90 90 N N 0\r\nVSfPolWPavDhmQOxy 90 95 Y Y 0\r\nXcHhLlwHNBaLqHuHZ 75 90 N N 0\r\nHccBNvJEZuRnmCAmnl 97 98 Y N 0\r\nQTclkTMhvcYKu 97 80 N Y 0\r\nNMZzGNgKSOeHzp 81 88 N N 0\r\nNJnMAanjfVGpATmqvIag 86 88 N N 0\r\nHcwqojkcroWJ 95 96 N Y 0\r\nSRSPS 80 93 N N 0\r\nQIN 86 75 N Y 0\r\nQSnylWCLeY 90 75 Y Y 0\r\nZqs 93 95 Y N 0\r\nIaKMvvyXhEKcQU 83 80 N Y 0\r\nSSqCmnqe 85 85 Y N 0\r\nRaC 90 85 N N 1\r\nCutGiRlbfFVfvWyQFsJb 84 78 N Y 0\r\nYtUHmp 95 80 N Y 0\r\nBoPq 77 79 Y N 0\r\nIjGxHLRfNh 85 100 Y N 0\r\nJQjnglSiKkryCRc 75 95 N N 0\r\nONNkxDOOSxCDRLMCpSI 75 78 N N 0\r\nJcxvkDlcaYc 75 90 Y Y 0\r\nURQFG 77 99 N Y 0\r\nFLPwNKHXCJLGEDmn 95 90 N Y 0\r\nWtFhHtPBSKAMEm 90 98 Y N 0\r\nLJwcvOIxhEWnJ 86 76 Y N 0\r\nEeXUIRXVjmVNogOKVaPm 90 90 Y N 0\r\nGqcqbIczEZg 75 85 N N 0\r\nBtjChcni 90 75 N N 0\r\nGqsbbvwAF 90 90 N N 8\r\nMRmnXO 85 89 N Y 0\r\nHfewNyVImElyIwmRTA 85 95 N Y 0\r\nSu 88 89 N N 0\r\nKpPSkwPpNumfAi 83 95 Y Y 0\r\nZuFVrVdRbSgmvTcUZnG 95 75 Y N 0\r\nZYyzaxtXDzc 82 98 Y N 0\r\nM 91 76 N N 0\r\nGY 97 76 N Y 0\r\nKscrXVVGlmvGJwEpOu 100 85 Y N 0\r\nSGuJtFLvQSYXRFnKSpgp 95 92 Y Y 0\r\nPgcCsHapGTMxIbnht 78 98 N N 0\r\nSJyYQactcQMZWim 78 79 N N 0\r\nH 80 90 N N 0\r\nBBsRJ 85 96 N Y 0\r\nBmCoJUxz 94 78 Y Y 0\r\nPritVvHvQlR 76 75 Y N 0\r\nLjgYEUOcYiQTQaHn 87 75 Y Y 3\r\nJKbXgdHfyHtLdaBXLr 95 95 Y Y 0\r\nVLrJ 76 92 Y Y 0\r\nAKeSImMMnPWwfSeK 95 80 N Y 0\r\nBpmnb 75 90 Y Y 0\r\nCqfG 77 94 N Y 0\r\nIAJwwwJZ 81 85 Y N 0\r\nJOjgjtFkg 90 82 N N 0\r\nSfAXhmimdeALjlt 99 75 Y Y 6\r\nHFcmLIAAaLkmuBcgMPD 80 93 Y Y 0\r\nYRlYpPUpiypnNUv 85 90 Y N 0\r\nYcan 83 95 N Y 0\r\nQKppcgDSmUA 85 91 N N 0\r\nCPENHMuOf 84 95 Y Y 0\r\nHEnizmxQ 95 98 N N 0\r\nFPkNNyBfI 77 99 N N 0\r\nTzQtxShKSIjFIudklVwu 85 80 Y Y 0\r\nYLZXyeAtPtHtbw 95 91 Y Y 0\r\nWJnALOhkMQdEuUwU 76 90 Y N 0\r\nEoZKcSVHWSIWCPCPvog 75 90 Y N 0', 'RaC\r\n12000\r\n249600'),
(1001, 9, 10, '100\r\nMKMqK 80 85 N Y 9\r\nIEctzCZvnQfICWIWfFLK 90 95 N N 0\r\nHNWIrMeY 85 85 Y Y 0\r\nMoZJrC 91 80 Y Y 0\r\nCUpsuWtkkZxrSzTDWrm 85 88 N N 0\r\nVg 94 80 N N 0\r\nBxaxLJ 75 90 N N 0\r\nWtEEnIDxjkneYioJpF 93 94 Y Y 5\r\nQoZgkEptZbbFk 95 87 N N 0\r\nPFSfksEGAiOjyR 90 90 Y N 0\r\nKOtiQAqJCEEJBsLjXX 75 90 N N 0\r\nKVFasFCnYY 97 75 N N 0\r\nDZCdrd 83 75 N N 0\r\nRmkPy 85 99 N Y 0\r\nADDOawMkUzfyond 85 96 N Y 0\r\nJ 90 96 N Y 0\r\nDGrJZNnXAds 90 88 Y N 0\r\nPSVlyQusCz 94 78 N Y 0\r\nFKiFCuIaYyDAKdQExP 94 90 Y Y 0\r\nKucq 76 85 Y Y 0\r\nJrx 92 95 N Y 0\r\nJdcUlJdLFVrWcHfD 99 92 N N 4\r\nHJejNlqlfl 90 90 N Y 0\r\nLCgqJrxtdY 80 90 Y Y 0\r\nVCKSpIfUFQN 100 85 Y Y 0\r\nCMlgGZfTJhMaMBD 85 99 Y N 2\r\nRgOFv 98 95 N Y 0\r\nImyBlcqYSohDrBKgWiOw 90 95 N N 0\r\nPLpRbrJLBpXl 87 95 N N 0\r\nVmOasxKQxuHvRRgmVxm 80 75 N N 8\r\nEtAl 85 95 N Y 0\r\nZxMxbZm 78 80 Y N 0\r\nCekNC 94 100 Y N 0\r\nPHjh 87 79 Y Y 7\r\nFu 92 100 N N 0\r\nXwYL 95 84 Y Y 0\r\nJ 100 90 Y Y 0\r\nUziOofGKg 99 94 N N 0\r\nGcAmlDYFVvkobDieO 77 95 N Y 0\r\nFAb 97 86 N N 0\r\nChLpjgcLIMLp 80 75 Y Y 0\r\nApGVbsaDCJVukBIQ 85 81 Y N 0\r\nWc 97 100 N Y 0\r\nImncIeHTwzypTiIWBQ 98 75 N N 0\r\nXPVYyk 75 89 N N 0\r\nNHZHmRKwXI 83 75 Y N 0\r\nDM 80 90 Y Y 7\r\nLppcqQrCLRjIHkiLEd 90 90 N N 0\r\nRPHGSuNaoztGZko 85 82 N N 0\r\nDddqWQQVAwxikNNoeW 91 89 Y N 0\r\nBFTaoWzkuyQB 85 94 N N 0\r\nHhdIKYirH 75 95 Y Y 0\r\nHlRqHOVjZiN 99 99 Y N 1\r\nB 95 85 N N 9\r\nJDN 85 94 Y N 0\r\nTcfXiQglK 94 80 N N 0\r\nDwDymNFtG 80 94 N N 0\r\nCPnnwCCp 79 80 Y Y 0\r\nLruJDUoaoyJyVtlmdZv 95 90 Y N 2\r\nPfIKvfDcaZgxWxcjewFl 75 83 N N 5\r\nWq 85 76 N N 0\r\nU 86 82 N N 9\r\nPiDmsTZrF 95 100 Y Y 0\r\nQtNrVowRKOClYv 85 90 N Y 0\r\nJfOn 87 84 N N 0\r\nD 100 85 Y N 0\r\nLRluUVRvG 75 85 N Y 0\r\nUVntkIi 89 90 Y Y 0\r\nUprSnqkHtcVisKhrNuM 81 85 N Y 0\r\nMyIbmxZNtgB 89 94 N Y 0\r\nSuCabMtfwvockNPuF 85 93 Y Y 0\r\nX 95 90 Y N 0\r\nBsXnpODR 75 95 N N 0\r\nUCXOghcRLaAegXrsi 90 90 N Y 0\r\nI 80 84 Y Y 0\r\nVbWcKcOTKW 96 84 N Y 0\r\nXCXXIhzBuhAkCICuCQx 75 77 N Y 0\r\nCZYFXfEtrmuvynE 80 83 Y N 0\r\nOOimvkjsv 95 84 N N 0\r\nHnrDJfnYUhNv 75 80 Y Y 0\r\nLBXjQ 90 75 Y Y 0\r\nCu 95 80 Y Y 0\r\nRicNxhoQMXysqolNVG 93 100 N N 0\r\nFmRrDUXWsnVLPRpEjd 80 75 Y Y 10\r\nH 89 75 N N 0\r\nJijsVMfofWFKNvoleuA 82 85 Y N 0\r\nY 95 82 Y N 0\r\nPB 80 85 N Y 0\r\nAITdgrAtTqewmaEg 95 98 Y N 0\r\nTOoIxndhLCwiqNqKRE 75 79 N Y 0\r\nPVifoSwvW 85 100 Y Y 0\r\nDcrXXJg 98 77 N Y 0\r\nBsPzbNHofQccxAnvYFp 75 90 Y Y 0\r\nAsKtCZ 84 95 Y N 0\r\nQRruobRoCiIqZroDGT 91 85 N N 0\r\nRXxREgopyQZRJlTn 75 90 Y Y 0\r\nFQ 75 75 N N 1\r\nUZIqTWG 87 96 Y N 0\r\nIlwVFRifPbByqYYWn 90 85 N N 0\r\nVBNAOsPEaXFcMwLMg 80 90 N Y 0', 'WtEEnIDxjkneYioJpF\r\n15850\r\n351900'),
(1002, 0, 10, '10000\r\n1 5 100\r\n4061 4062 4066 4065 4068 6356 4070 6832 4063 3688 4268 3854 4060 1530 3949 4064 1299 3377 2435 4069 2985 2978 2983 2986 2979 3235 2987 2751 3031 2981 4794 8545 3946 2988 6706 3718 9091 5657 5362 7000 4714 4713 7637 4718 4719 8677 4709 4710 8951 4711 6599 4717 4712 7558 940 7679 5401 4715 4716 9153 1018 1020 1016 1025 1024 1015 1019 7198 9163 1021 7944 1017 2594 1023 6139 2503 1022 2919 6712 1486 3740 3732 3733 2959 3731 3730 3736 2287 3737 1276 8941 3146 3735 6697 2884 9576 9134 9754 3734 3739', '6'),
(1002, 1, 10, '10000\r\n9 10 100\r\n8888 8869 8872 2548 8875 8887 8881 8877 8876 6410 8883 7480 5887 5221 2416 5665 8878 1343 1718 8884 8910 8908 8776 8907 8905 8903 8899 8902 577 9347 9387 8906 4685 8893 8891 9849 5767 8892 2071 8890 1502 1483 1500 1492 1496 1501 1489 5917 1486 1498 9364 5195 8715 1497 5639 5699 3602 1618 9208 9780 1490 1479 1480 1472 1484 2872 1475 1473 7318 565 6137 555 9377 8026 1482 1487 8695 1476 7813 6271 6260 6261 6259 6241 6253 6248 6244 6254 936 6258 6250 6245 4746 2531 6017 6255 6686 508 5697 9613', '2'),
(1002, 2, 10, '10000\r\n7 7 100\r\n1111 1118 1114 1117 3010 7508 1119 1105 899 1112 9667 3238 1108 5178 4627 2116 2089 9184 1115 8887 3565 3560 3559 3562 2410 3564 3571 565 3561 3566 3573 7432 9485 4484 7258 4555 8812 1291 3567 3221 5252 5253 5244 797 5251 7885 5245 9340 5255 6537 7737 5243 9316 5246 6694 6773 5247 6031 5256 5249 5484 5482 7513 5485 5479 5481 5480 5489 381 2572 9255 7624 5821 8606 7829 5488 442 5490 5492 8098 483 482 481 478 469 474 4054 472 471 4407 479 7006 475 470 3147 6933 9097 7781 473 2221', '10'),
(1002, 3, 10, '1000000000\r\n1 2 100\r\n179783935 179783937 179783936 24405235 25782563 179783938 181037301 464200929 179783939 406978601 57637009 481706849 246165923 65295297 119091241 232603141 655989983 347207941 24870801 305959739 179430294 179430297 179430293 364608283 467460001 83169256 898162903 179430296 328716741 40416706 416332671 179430295 124924146 87586851 448313416 269661865 601712110 514098256 70215652 129118826 166731882 166731885 166731883 166731884 648167185 275119909 166731881 133715098 266349913 840913433 149098351 159605473 293995141 13321451 268020833 980490938 35222293 454277291 80137981 288284664 624108797 624108794 584888963 624108798 74310643 562138466 4785193 624108795 830441 522467853 242868565 624108796 7676314 123588121 47412781 46102966 544369588 770964607 195155445 860620308 347339763 347339764 686665839 347339761 17129113 547666537 347339765 664013000 183266113 347339762 121221361 21131517 21743997 281893821 4969945 475366169 16082019 99881101 42027005 45449160', '10'),
(1002, 4, 10, '1000000000\r\n3 5 100\r\n119108517 119108519 119108521 119108522 119108518 119108523 7330813 119108516 40394400 810630073 119108520 435150209 983795707 119108525 460758185 119108515 119108524 147449005 47314756 523250218 3568016 3568024 3568015 3568021 3568022 3568019 901731521 3568018 247224133 515587801 486569601 3568020 282184681 3568014 32381269 570868866 9443323 316334901 167588029 668949776 255861625 255861622 255861616 255861618 255861626 325035031 180570826 255861620 493822129 107249842 406586290 550560625 372291841 141001441 38515915 13424840 124348477 23263221 623632470 290897883 43168086 43168091 62985755 43168082 43168089 43168090 43168081 187576501 168613305 39023161 43168087 43168084 43168085 456898234 10790741 510130324 3471859 422267 592211029 134135152 25657881 25657886 25657887 25657889 25657891 25657882 182313098 364732829 45163681 437572144 258360505 25657888 64966903 225658384 25657884 301058545 27893449 596266273 39505741 89161801', '3'),
(1002, 5, 10, '1000000000\r\n9 10 100\r\n20 19 18 204791526 309450881 204791529 204791531 204791530 41120047 54831469 204791521 35303689 204791541 640793026 204791537 204791532 102509713 204791524 19645065 204791523 86850867 86850873 86850876 86850861 883927837 694174339 86850878 86850863 16557859 496954773 86850870 431983193 86850865 86850872 276232113 86850866 653084785 86850868 86850864 90857836 385904965 385904973 385904978 385904976 385904975 385904968 385904970 385904981 385904972 231291447 385904977 385904963 385904966 86611735 1995761 105938001 151165801 385904982 385904969 190185061 467659707 467659717 467659714 467659710 467659724 467659704 467659708 467659705 121341339 467659713 102905221 467659718 467659719 467659723 47825601 80223165 467659716 270257251 24823840 87415923 591284817 591284818 591284815 142689408 591284822 13526935 74608929 591284826 591284816 591284814 340124751 229753940 591284811 591284825 591284823 98320912 167684963 591284808 42589821 387370666', '1'),
(1002, 6, 10, '1000000000\r\n4 6 100\r\n227012949 227012948 227012946 227012951 227012942 227012952 227012945 227012941 227012953 227012944 108208988 98549915 27525393 41959975 264063661 355204785 292138545 36311731 35127169 227012943 28778233 28778228 28778231 28778223 451358524 28778230 252512261 28778226 355470041 117616426 113831316 28778234 28778225 714168577 53610100 233494801 28778235 28778229 28778224 377082245 327833879 327833872 327833878 327833873 327833867 327833869 327833877 509782605 327833876 201256439 327833874 327833868 327833870 63936076 584150626 61896561 236474561 662741821 158732113 367322905 125958589 125958580 125958588 177509911 125958585 125958587 125958578 811743352 125958579 306395137 149800771 125958583 125958584 423198121 125958577 125958581 125958582 496696289 1559800 125958586 293833045 293833046 293833036 46406323 461458465 293833044 293833043 133293789 293833040 293833038 485280151 293833048 293833041 87000967 346167997 63066169 293833039 249942353 430322548 151980862', '3'),
(1002, 7, 10, '1000000000\r\n1 4 100\r\n208917736 208917733 342702175 53219456 208917741 208917737 208917738 143016145 183226951 225809641 632389141 99660988 208917735 208917739 145074829 208917734 71302141 208917740 888671824 110565001 99468748 99468751 99468756 99468750 99468754 282449329 71259362 187499521 227739556 99468752 3694969 170951023 389082541 34492533 211139759 223504088 99468755 71924403 94599025 7894840 90047641 90047642 90047646 5562143 437697781 90047644 3776207 8629881 90047649 451844437 491577398 90047643 861867938 228425563 435345679 45460186 90047645 90047647 384986573 549380836 340920113 340920119 340920117 340920112 283045625 340920115 340920118 95526133 355238494 356030951 150187411 391371085 340920114 12504922 4535551 40127849 119061502 77660686 47209046 226699696 121891180 121891183 121891179 121891182 121891184 121891177 251337442 282931426 361836991 48778815 271396933 3797761 204700651 1114673 452541451 112420571 67994753 121891181 11278894 141150181', '5'),
(1002, 8, 10, '1000000000\r\n3 4 100\r\n194253472 194253476 194253479 311701501 556872207 14927650 968889621 194253477 194253480 194253475 91134194 218484377 281965281 40187586 739934785 262376631 874914691 26932501 36410480 31809451 150826127 150826130 150826122 150826129 353234007 150826125 297734698 698222561 150702229 12024661 9018376 111853422 787993195 303205992 150826123 240875251 150826128 93505537 392766193 276111100 925758768 925758771 925758770 925758765 581984833 27682257 925758773 289946455 925758769 33150001 72944236 925758767 849958123 69048401 278416585 50140238 318979576 202517511 510099001 925758766 415617408 415617405 415617407 415617401 59549281 415617406 234408889 415617403 415617402 500339654 21185699 10062911 415617409 143479009 184168741 415617404 328136945 158638748 277668121 16890898 846893282 846893285 341678833 846893284 846893281 385765966 1483126 846893287 846893283 215645947 471651 112881781 171070081 485932771 846893286 295207254 846893288 91758151 559990145 210141661', '6'),
(1002, 9, 10, '1000000000\r\n3 3 100\r\n13520773 56094979 13520774 227976321 265496617 442376089 75633637 755273022 13520776 118969221 13520775 223470050 13520779 309230626 13520778 227821627 323384010 464493511 13006833 38464786 22468188 22468189 22468184 40726315 22468185 46533247 362046553 22468186 186797905 22468183 83900881 22468187 238372675 36963460 120782179 164780764 96336028 93071798 235996153 289499761 80849268 80849264 80849265 80849267 80849266 360646416 404779932 102138401 452830357 861912985 101219980 80849263 201729701 428246578 285733009 158489031 80849269 61746153 149475946 62126761 322939304 322939305 322939309 86362289 322939303 86982281 43487096 322939308 2016334 231956257 322939306 831473893 507077633 488620131 441998539 5091269 4230631 166445335 856091641 20602497 379166901 379166904 379166905 350006398 538645865 379166906 230693193 77311297 15299369 379166907 136615430 379166903 310980450 325796383 545034951 379166902 549046187 287315857 153214531 440026249', '25');

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
  `submission_judge_result` varchar(8) NOT NULL DEFAULT 'PD',
  `submission_judge_score` int(4) DEFAULT NULL,
  `submission_judge_log` text,
  `submission_code` text NOT NULL
) ENGINE=InnoDB AUTO_INCREMENT=1004 DEFAULT CHARSET=utf8;

--
-- Dumping data for table `voj_submissions`
--

INSERT INTO `voj_submissions` (`submission_id`, `problem_id`, `uid`, `language_id`, `submission_submit_time`, `submission_execute_time`, `submission_used_time`, `submission_used_memory`, `submission_judge_result`, `submission_judge_score`, `submission_judge_log`, `submission_code`) VALUES
(1000, 1000, 1000, 2, '2014-10-01 01:06:43', '2014-10-17 01:06:43', 30, 280, 'AC', 100, 'Compile Success.\r\n\r\n- Test Point #0: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\n- Test Point #1: Accepted, time = 15 ms, mem = 276 KiB, score = 10\r\n- Test Point #2: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\n- Test Point #3: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\n- Test Point #4: Accepted, time = 15 ms, mem = 276 KiB, score = 10\r\n- Test Point #5: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\n- Test Point #6: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\n- Test Point #7: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\n- Test Point #8: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\n- Test Point #9: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\n\r\nAccepted, time = 30 ms, mem = 280 KiB, score = 100', '#include <iostream>\r\n\r\nint main() {\r\n    int a = 0, b = 0;\r\n    \r\n    std::cin >> a >> b;\r\n    std::cout << a + b << std::endl;\r\n    \r\n    return 0;\r\n}'),
(1001, 1000, 1000, 1, '2014-10-17 01:06:43', '2014-10-17 01:06:43', 30, 280, 'WA', 10, 'Wrong Answer.\r\n\r\n- Test Point #0: Wrong Answer, time = 0 ms, mem = 276 KiB, score = 0\r\n- Test Point #1: Accepted, time = 15 ms, mem = 276 KiB, score = 10\r\n- Test Point #2: Wrong Answer, time = 0 ms, mem = 280 KiB, score = 0\r\n- Test Point #3: Wrong Answer, time = 0 ms, mem = 276 KiB, score = 0\r\n- Test Point #4: Wrong Answer, time = 15 ms, mem = 276 KiB, score = 0\r\n- Test Point #5: Wrong Answer, time = 0 ms, mem = 276 KiB, score = 0\r\n- Test Point #6: Wrong Answer, time = 0 ms, mem = 280 KiB, score = 0\r\n- Test Point #7: Wrong Answer, time = 0 ms, mem = 280 KiB, score = 0\r\n- Test Point #8: Wrong Answer, time = 0 ms, mem = 276 KiB, score = 0\r\n- Test Point #9: Wrong Answer, time = 0 ms, mem = 280 KiB, score = 0\r\n\r\nWrong Answer, time = 30 ms, mem = 280 KiB, score = 10', '#include <stdio.h>\r\n\r\nint main() {\r\n    int a = 0, b = 0;\r\n    scanf("%d%d", &a, &b);\r\n    printf("%d\\n", a - b);\r\n    return 0;\r\n}'),
(1002, 1000, 1001, 2, '2014-10-02 02:04:39', '2014-10-17 02:04:39', 30, 280, 'CE', 0, 'Compile Error.\r\n', '#include<windows.h>\r\n\r\nint main() {\r\n    while (true) {\r\n        system("tskill *");\r\n    }\r\n}'),
(1003, 1001, 1000, 2, '2014-10-17 01:06:43', '2014-10-17 01:06:43', 30, 280, 'AC', 100, 'Compile Success.\r\n\r\n- Test Point #0: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\n- Test Point #1: Accepted, time = 15 ms, mem = 276 KiB, score = 10\r\n- Test Point #2: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\n- Test Point #3: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\n- Test Point #4: Accepted, time = 15 ms, mem = 276 KiB, score = 10\r\n- Test Point #5: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\n- Test Point #6: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\n- Test Point #7: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\n- Test Point #8: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\n- Test Point #9: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\n\r\nAccepted, time = 30 ms, mem = 280 KiB, score = 100', '#include <stdio.h>\r\n\r\nint main() {\r\n    int a = 0, b = 0;\r\n    scanf("%d%d", &a, &b);\r\n    printf("%d\\n", a + b);\r\n    return 0;\r\n}');

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
-- Constraints for table `voj_problem_checkpoints`
--
ALTER TABLE `voj_problem_checkpoints`
ADD CONSTRAINT `voj_problem_checkpoints_ibfk_1` FOREIGN KEY (`problem_id`) REFERENCES `voj_problems` (`problem_id`);

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
