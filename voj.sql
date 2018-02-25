-- phpMyAdmin SQL Dump
-- version 4.5.2
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Apr 22, 2017 at 04:34 PM
-- Server version: 10.1.10-MariaDB
-- PHP Version: 7.0.3

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `voj`
--

-- --------------------------------------------------------

--
-- Table structure for table `voj_bulletin_board_messages`
--

CREATE TABLE `voj_bulletin_board_messages` (
  `message_id` bigint(20) NOT NULL,
  `message_title` varchar(128) NOT NULL,
  `message_body` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `message_create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `voj_contests`
--

CREATE TABLE `voj_contests` (
  `contest_id` bigint(20) NOT NULL,
  `contest_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `contest_notes` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `contest_start_time` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `contest_end_time` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `contest_mode` varchar(4) COLLATE utf8mb4_unicode_ci NOT NULL,
  `contest_problems` text COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `voj_contests`
--

INSERT INTO `voj_contests` (`contest_id`, `contest_name`, `contest_notes`, `contest_start_time`, `contest_end_time`, `contest_mode`, `contest_problems`) VALUES
(1, 'Contest #1', '', '2018-02-23 00:00:00', '2018-02-23 23:59:59', 'OI', '[1000, 1001, 1002]'),
(2, 'Contest #2', '', '1970-01-02 00:00:01', '2038-01-18 03:14:07', 'ACM', '[1001, 1003]'),
(3, 'Contest #3', '', '2038-01-18 00:00:00', '2038-01-18 02:00:00', 'ACM', '[1000, 1003]');

-- --------------------------------------------------------

--
-- Table structure for table `voj_contest_contestants`
--

CREATE TABLE `voj_contest_contestants` (
  `contest_id` bigint(20) NOT NULL,
  `contestant_uid` bigint(20) NOT NULL,
  `code_snippet` text COLLATE utf8mb4_unicode_ci
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `voj_contest_contestants`
--

INSERT INTO `voj_contest_contestants` (`contest_id`, `contestant_uid`) VALUES
(1, 1000),
(1, 1001),
(2, 1000);

-- --------------------------------------------------------

--
-- Table structure for table `voj_contest_submissions`
--

CREATE TABLE `voj_contest_submissions` (
  `contest_id` bigint(20) NOT NULL,
  `submission_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `voj_contest_submissions`
--

INSERT INTO `voj_contest_submissions` (`contest_id`, `submission_id`) VALUES
(1, 1000),
(1, 1002),
(1, 1003);

-- --------------------------------------------------------

--
-- Table structure for table `voj_discussion_replies`
--

CREATE TABLE `voj_discussion_replies` (
  `discussion_reply_id` bigint(20) NOT NULL,
  `discussion_thread_id` bigint(20) NOT NULL,
  `discussion_reply_uid` bigint(20) NOT NULL,
  `discussion_reply_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `discussion_reply_content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `discussion_reply_votes` text COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `voj_discussion_replies`
--

INSERT INTO `voj_discussion_replies` (`discussion_reply_id`, `discussion_thread_id`, `discussion_reply_uid`, `discussion_reply_time`, `discussion_reply_content`, `discussion_reply_votes`) VALUES
(1, 1, 1001, '2017-01-09 21:42:20', 'Reply content for thread #1', '{"up": [1000], "down": [1002]}'),
(2, 2, 1002, '2017-01-10 21:42:20', 'Reply content for thread #2', '{"up": [1000], "down": [1001]}'),
(3, 2, 1001, '2017-01-11 21:42:20', 'Reply content for thread #2', '{"up": [], "down": []}');

-- --------------------------------------------------------

--
-- Table structure for table `voj_discussion_threads`
--

CREATE TABLE `voj_discussion_threads` (
  `discussion_thread_id` bigint(20) NOT NULL,
  `discussion_thread_creator_uid` bigint(20) NOT NULL,
  `discussion_thread_create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `problem_id` bigint(20) DEFAULT NULL,
  `discussion_topic_id` int(8) NOT NULL,
  `discussion_thread_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `voj_discussion_threads`
--

INSERT INTO `voj_discussion_threads` (`discussion_thread_id`, `discussion_thread_creator_uid`, `discussion_thread_create_time`, `problem_id`, `discussion_topic_id`, `discussion_thread_name`) VALUES
(1, 1000, '2017-01-13 20:31:09', 1000, 1, 'Thread #1'),
(2, 1000, '2017-01-13 20:31:09', 1000, 2, 'Thread #2'),
(3, 1000, '2017-01-13 20:31:09', NULL, 1, 'Thread #3');

-- --------------------------------------------------------

--
-- Table structure for table `voj_discussion_topics`
--

CREATE TABLE `voj_discussion_topics` (
  `discussion_topic_id` int(8) NOT NULL,
  `discussion_topic_slug` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `discussion_topic_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `discussion_parent_topic_id` int(8) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `voj_discussion_topics`
--

INSERT INTO `voj_discussion_topics` (`discussion_topic_id`, `discussion_topic_slug`, `discussion_topic_name`, `discussion_parent_topic_id`) VALUES
(1, 'solutions', 'Solutions', 0),
(2, 'qa', 'Q & A', 0),
(3, 'general', 'General', 0),
(4, 'support', 'Help & Support', 0);

-- --------------------------------------------------------

--
-- Table structure for table `voj_email_validation`
--

CREATE TABLE `voj_email_validation` (
  `email` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `token` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expire_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `voj_email_validation`
--

INSERT INTO `voj_email_validation` (`email`, `token`, `expire_time`) VALUES
('support@verwandlung.org', 'Random-String-Generated', '2015-07-09 09:00:00');

-- --------------------------------------------------------

--
-- Table structure for table `voj_judge_results`
--

CREATE TABLE `voj_judge_results` (
  `judge_result_id` int(4) NOT NULL,
  `judge_result_slug` varchar(4) COLLATE utf8mb4_unicode_ci NOT NULL,
  `judge_result_name` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `voj_judge_results`
--

INSERT INTO `voj_judge_results` (`judge_result_id`, `judge_result_slug`, `judge_result_name`) VALUES
(1, 'PD', 'Pending'),
(2, 'AC', 'Accepted'),
(3, 'WA', 'Wrong Answer'),
(4, 'TLE', 'Time Limit Exceed'),
(5, 'OLE', 'Output Limit Exceed'),
(6, 'MLE', 'Memory Limit Exceed'),
(7, 'RE', 'Runtime Error'),
(8, 'PE', 'Presentation Error'),
(9, 'CE', 'Compile Error'),
(10, 'SE', 'System Error');

-- --------------------------------------------------------

--
-- Table structure for table `voj_languages`
--

CREATE TABLE `voj_languages` (
  `language_id` int(4) NOT NULL,
  `language_slug` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `language_name` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `language_compile_command` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `language_run_command` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `voj_languages`
--

INSERT INTO `voj_languages` (`language_id`, `language_slug`, `language_name`, `language_compile_command`, `language_run_command`) VALUES
(1, 'text/x-csrc', 'C', 'gcc -O2 -s -Wall -o {filename}.exe {filename}.c -lm', '{filename}.exe'),
(2, 'text/x-c++src', 'C++', 'g++ -O2 -s -Wall -std=c++11 -o {filename}.exe {filename}.cpp -lm', '{filename}.exe'),
(3, 'text/x-java', 'Java', 'javac {filename}.java', 'java -cp {filename}'),
(4, 'text/x-pascal', 'Pascal', 'fpc -O2 -Xs -Sgic -vw -o{filename}.exe {filename}.pas', '{filename}.exe'),
(5, 'text/x-python', 'Python 2', 'python -m py_compile {filename}.py', 'python {filename}.py'),
(6, 'text/x-ruby', 'Ruby', 'rbx compile {filename}.rb', 'ruby {filename}.rb ');

-- --------------------------------------------------------

--
-- Table structure for table `voj_options`
--

CREATE TABLE `voj_options` (
  `option_id` int(8) NOT NULL,
  `option_name` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `option_value` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_autoload` tinyint(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `voj_options`
--

INSERT INTO `voj_options` (`option_id`, `option_name`, `option_value`, `is_autoload`) VALUES
(1, 'websiteName', 'Verwandlung Online Judge', 1),
(2, 'description', 'Verwandlung Online Judge is a cross-platform online judge system based on Spring MVC Framework.', 1),
(3, 'copyright', '<a href="https://haozhexie.com/" target="_blank">Infinite Script</a>', 1),
(4, 'googleAnalyticsCode', '', 1),
(5, 'icpNumber', '', 1),
(6, 'policeIcpNumber', '', 1),
(7, 'allowUserRegister', '1', 0),
(8, 'offensiveWords', '["法轮","中央","六四","军区","共产党","国民党"]', 0);

-- --------------------------------------------------------

--
-- Table structure for table `voj_problems`
--

CREATE TABLE `voj_problems` (
  `problem_id` bigint(20) NOT NULL,
  `problem_is_public` tinyint(1) NOT NULL DEFAULT '1',
  `problem_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `problem_time_limit` int(8) NOT NULL,
  `problem_memory_limit` int(8) NOT NULL,
  `problem_description` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `problem_input_format` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `problem_output_format` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `problem_sample_input` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `problem_sample_output` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `problem_hint` text COLLATE utf8mb4_unicode_ci
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `voj_problems`
--

INSERT INTO `voj_problems` (`problem_id`, `problem_is_public`, `problem_name`, `problem_time_limit`, `problem_memory_limit`, `problem_description`, `problem_input_format`, `problem_output_format`, `problem_sample_input`, `problem_sample_output`, `problem_hint`) VALUES
(1000, 1, 'A+B Problem', 1000, 65536, '输入两个自然数, 输出他们的和', '两个自然数x和y (0<=x, y<=32767).', '一个数, 即x和y的和.', '123 500', '623', '## C++ Code\r\n\r\n    #include <iostream>\r\n\r\n    int main() {\r\n        int a = 0, b = 0;\r\n        std::cin >> a >> b;\r\n        std::cout << a + b << std::endl;\r\n        return 0;\r\n    }\r\n\r\n## Free Pascal Code\r\n\r\n    program Plus;\r\n    var a, b:longint;\r\n    begin\r\n        readln(a, b);\r\n        writeln(a + b);\r\n    end.\r\n\r\n## Java Code\r\n\r\n    import java.util.Scanner;\r\n\r\n    public class Main {\r\n        public static void main(String[] args) {\r\n            Scanner in = new Scanner(System.in);\r\n            int a = in.nextInt();\r\n            int b = in.nextInt();\r\n            System.out.println(a + b);\r\n        }\r\n    }\r\n'),
(1001, 1, '谁拿了最多奖学金', 1000, 65536, '某校的惯例是在每学期的期末考试之后发放奖学金。发放的奖学金共有五种，获取的条件各自不同：\r\n1) 院士奖学金，每人8000元，期末平均成绩高于80分（>80），并且在本学期内发表1篇或1篇以上论文的学生均可获得；\r\n2) 五四奖学金，每人4000元，期末平均成绩高于85分（>85），并且班级评议成绩高于80分（>80）的学生均可获得；\r\n3) 成绩优秀奖，每人2000元，期末平均成绩高于90分（>90）的学生均可获得；\r\n4) 西部奖学金，每人1000元，期末平均成绩高于85分（>85）的西部省份学生均可获得；\r\n5) 班级贡献奖，每人850元，班级评议成绩高于80分（>80）的学生干部均可获得；\r\n只要符合条件就可以得奖，每项奖学金的获奖人数没有限制，每名学生也可以同时获得多项奖学金。例如姚林的期末平均成绩是87分，班级评议成绩82分，同时他还是一位学生干部，那么他可以同时获得五四奖学金和班级贡献奖，奖金总数是4850元。\r\n现在给出若干学生的相关数据，请计算哪些同学获得的奖金总数最高（假设总有同学能满足获得奖学金的条件）。', '输入的第一行是一个整数N（1 <= N <= 100），表示学生的总数。接下来的N行每行是一位学生的数据，从左向右依次是姓名，期末平均成绩，班级评议成绩，是否是学生干部，是否是西部省份学生，以及发表的论文数。姓名是由大小写英文字母组成的长度不超过20的字符串（不含空格）；期末平均成绩和班级评议成绩都是0到100之间的整数（包括0和100）；是否是学生干部和是否是西部省份学生分别用一个字符表示，Y表示是，N表示不是；发表的论文数是0到10的整数（包括0和10）。每两个相邻数据项之间用一个空格分隔。', '输出包括三行，第一行是获得最多奖金的学生的姓名，第二行是这名学生获得的奖金总数。如果有两位或两位以上的学生获得的奖金最多，输出他们之中在输入文件中出现最早的学生的姓名。第三行是这N个学生获得的奖学金的总数。', '4\r\nYaoLin 87 82 Y N 0\r\nChenRuiyi 88 78 N Y 1\r\nLiXin 92 88 N N 0\r\nZhangQin 83 87 Y N 1', 'ChenRuiyi\r\n9000\r\n28700', NULL),
(1002, 0, '过河', 1000, 65536, '在河上有一座独木桥, 一只青蛙想沿着独木桥从河的一侧跳到另一侧. 在桥上有一些石子, 青蛙很讨厌踩在这些石子上. 由于桥的长度和青蛙一次跳过的距离都是正整数, 我们可以把独木桥上青蛙可能到达的点看成数轴上的一串整点：0, 1, ……, L(其中L是桥的长度). 坐标为0的点表示桥的起点, 坐标为L的点表示桥的终点. 青蛙从桥的起点开始, 不停的向终点方向跳跃. 一次跳跃的距离是S到T之间的任意正整数(包括S,T). 当青蛙跳到或跳过坐标为L的点时, 就算青蛙已经跳出了独木桥. \r\n题目给出独木桥的长度L, 青蛙跳跃的距离范围S,T, 桥上石子的位置. 你的任务是确定青蛙要想过河, 最少需要踩到的石子数. \r\n对于30%的数据, L <= 10000；\r\n对于全部的数据, L <= 10^9. ', '输入的第一行有一个正整数L(1 <= L <= 10^9), 表示独木桥的长度. 第二行有三个正整数S, T, M, 分别表示青蛙一次跳跃的最小距离, 最大距离, 及桥上石子的个数, 其中1 <= S <= T <= 10, 1 <= M <= 100. 第三行有M个不同的正整数分别表示这M个石子在数轴上的位置(数据保证桥的起点和终点处没有石子). 所有相邻的整数之间用一个空格隔开. ', '输出只包括一个整数, 表示青蛙过河最少需要踩到的石子数.', '10\r\n2 3 5\r\n2 3 5 6 7', '2', NULL),
(1003, 1, '等价表达式', 1000, 65536, '明明进了中学之后, 学到了代数表达式. 有一天, 他碰到一个很麻烦的选择题. 这个题目的题干中首先给出了一个代数表达式, 然后列出了若干选项, 每个选项也是一个代数表达式, 题目的要求是判断选项中哪些代数表达式是和题干中的表达式等价的. \n\n这个题目手算很麻烦, 因为明明对计算机编程很感兴趣, 所以他想是不是可以用计算机来解决这个问题. 假设你是明明, 能完成这个任务吗? \n\n这个选择题中的每个表达式都满足下面的性质: \n\n1. 表达式只可能包含一个变量''a''. \n2. 表达式中出现的数都是正整数, 而且都小于10000. \n3. 表达式中可以包括四种运算''+''(加), ''-''(减), ''\\*''(乘), ''^''(乘幂), 以及小括号''('', '')''. 小括号的优先级最高, 其次是''^'', 然后是''*'', 最后是''+''和''-''. ''+''和''-''的优先级是相同的. 相同优先级的运算从左到右进行. (注意: 运算符''+'', ''-'', ''\\*'', ''^''以及小括号''('', '')''都是英文字符)\n4. 幂指数只可能是1到10之间的正整数(包括1和10). \n5. 表达式内部, 头部或者尾部都可能有一些多余的空格. \n\n下面是一些合理的表达式的例子: \n\n    ((a^1) ^ 2)^3, a*a+a-a, ((a+a)), 9999+(a-a)*a, 1 + (a -1)^3, 1^10^9……\n\n\n- 对于30%的数据, 表达式中只可能出现两种运算符''+''和''-''；\n- 对于其它的数据, 四种运算符''+'', ''-'', ''*'', ''^''在表达式中都可能出现. \n- 对于全部的数据, 表达式中都可能出现小括号''(''和'')''. ', '输入的第一行给出的是题干中的表达式. 第二行是一个整数n(2 <= n <= 26), 表示选项的个数. 后面n行, 每行包括一个选项中的表达式. 这n个选项的标号分别是A, B, C, D……\n输入中的表达式的长度都不超过50个字符, 而且保证选项中总有表达式和题干中的表达式是等价的. ', '输出包括一行, 这一行包括一系列选项的标号, 表示哪些选项是和题干中的表达式等价的. 选项的标号按照字母顺序排列, 而且之间没有空格.', '( a + 1) ^2\n3\n(a-1)^2+4*a\na  + 1+ a\na^2 + 2 * a * 1 + 1^2 + 10 -10 +a -a', 'AC', '');

-- --------------------------------------------------------

--
-- Table structure for table `voj_problem_categories`
--

CREATE TABLE `voj_problem_categories` (
  `problem_category_id` int(4) NOT NULL,
  `problem_category_slug` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `problem_category_name` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `problem_category_parent_id` int(4) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `voj_problem_categories`
--

INSERT INTO `voj_problem_categories` (`problem_category_id`, `problem_category_slug`, `problem_category_name`, `problem_category_parent_id`) VALUES
(1, 'uncategorized', 'Uncategorized', 0),
(2, 'dynamic-programming', 'Dynamic Programming', 0);

-- --------------------------------------------------------

--
-- Table structure for table `voj_problem_category_relationships`
--

CREATE TABLE `voj_problem_category_relationships` (
  `problem_id` bigint(20) NOT NULL,
  `problem_category_id` int(4) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `voj_problem_category_relationships`
--

INSERT INTO `voj_problem_category_relationships` (`problem_id`, `problem_category_id`) VALUES
(1000, 1),
(1000, 2),
(1001, 1),
(1002, 1),
(1003, 1);

-- --------------------------------------------------------

--
-- Table structure for table `voj_problem_checkpoints`
--

CREATE TABLE `voj_problem_checkpoints` (
  `problem_id` bigint(20) NOT NULL,
  `checkpoint_id` int(4) NOT NULL,
  `checkpoint_exactly_match` tinyint(1) NOT NULL,
  `checkpoint_score` int(4) NOT NULL,
  `checkpoint_input` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `checkpoint_output` longtext COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `voj_problem_checkpoints`
--

INSERT INTO `voj_problem_checkpoints` (`problem_id`, `checkpoint_id`, `checkpoint_exactly_match`, `checkpoint_score`, `checkpoint_input`, `checkpoint_output`) VALUES
(1000, 0, 0, 10, '18820 26832\r\n', '45652\r\n'),
(1000, 1, 0, 10, '1123 5687', '6810\r\n'),
(1000, 2, 0, 10, '15646 8688', '24334'),
(1000, 3, 0, 10, '26975 21625', '48600'),
(1000, 4, 0, 10, '23107 28548', '51655'),
(1000, 5, 0, 10, '16951 22289', '39240'),
(1000, 6, 0, 10, '8634 13146', '21780'),
(1000, 7, 0, 10, '17574 15337', '32911'),
(1000, 8, 0, 10, '14548 28382', '42930'),
(1000, 9, 0, 10, '3271 17411', '20682'),
(1001, 0, 0, 10, '100\r\nXvpxWEvuxMujM 79 90 N N 0\r\nFWGrVKwgJsImNAzO 100 75 Y Y 0\r\nXHEDY 85 95 Y Y 8\r\nSnJCAbmx 80 90 Y N 0\r\nIjsb 90 78 N N 0\r\nT 78 98 Y Y 8\r\nA 87 90 Y Y 0\r\nASVBj 85 94 N N 0\r\nFmMsZNPOCgCwygIT 85 83 N Y 0\r\nFXJDETWfFCqJnbYyxa 75 82 N Y 0\r\nEQMWSRKNvkunT 80 95 Y Y 0\r\nVdxkZpDNgfvqQOGyyOIG 80 80 Y Y 0\r\nXGIMzWKXeJMXTUwduO 95 81 N Y 0\r\nRuncagD 95 96 Y N 0\r\nTzmkvWKsUOkbBDrnc 90 96 N Y 5\r\nCXBJzEelrzdJ 90 97 N Y 0\r\nUGmNXVYsvpLgmb 85 100 N Y 0\r\nAhteTELcmbv 90 85 N N 0\r\nGAEIg 80 95 N Y 0\r\nYAHRJgqstbZaZPyww 99 85 Y Y 0\r\nQo 90 90 N N 0\r\nDtqLHiMmeHbohAxToJWP 78 75 Y Y 0\r\nXIWnP 85 75 N Y 0\r\nDoWmxImSCdk 95 76 Y N 0\r\nNLFznHxOcV 82 85 N N 0\r\nAKXkyYHWGwXvqMLKiCIR 94 95 Y Y 0\r\nGcgEAFL 79 93 N Y 0\r\nNqUjBPAYxdABnMGcQax 85 91 Y N 0\r\nXPohweOIDDHmb 83 94 Y N 0\r\nYIdB 75 93 N Y 0\r\nDBQxmSnODuYwifv 93 77 Y N 0\r\nRCpKstV 75 90 N Y 0\r\nUnu 82 81 Y N 0\r\nNGeyEHglHxHfghD 81 100 N Y 0\r\nMFLedhUZ 96 75 N Y 0\r\nYYEt 76 95 Y Y 10\r\nR 85 85 Y Y 0\r\nLHJGGF 95 87 Y Y 0\r\nPA 85 93 N Y 0\r\nGKPQfRzvnYtRqUcYYDGi 80 92 N N 0\r\nOsYZnlpdToExY 99 95 Y Y 0\r\nBK 91 85 N N 0\r\nC 83 85 Y Y 0\r\nTvp 90 90 Y Y 0\r\nFzSFDyfqaKOAp 90 85 Y N 0\r\nIIzkbLEYbncWzCZCIiJr 94 90 N N 0\r\nYOwtTfAQymJqjkkh 90 80 Y Y 0\r\nAjZSUNidohrGDgCBpo 91 86 Y N 0\r\nQRhbctxBn 79 75 N Y 0\r\nEIkGwpsyW 85 96 N N 0\r\nIJh 85 81 N Y 0\r\nXe 82 80 Y Y 7\r\nBbLcNTB 95 80 N N 0\r\nMPCNZTyWmaZ 84 85 N N 0\r\nChrB 95 95 N N 0\r\nTlIYwpIbVnurzacD 95 90 N N 8\r\nThywwHUaoxX 99 78 N Y 0\r\nQGnzLidvEBjIcmGIAdI 86 96 N Y 0\r\nDNpKPSUyprOeJFmU 81 93 N Y 0\r\nHYfptVzlESrFRUVdgL 93 92 Y N 0\r\nHcblndXi 90 89 Y N 0\r\nNWBJjdgcI 87 85 N Y 0\r\nShnYCYURiyKoVTq 85 99 N N 0\r\nZbc 75 80 Y Y 0\r\nEGvxpyULGNPQ 90 91 N N 0\r\nTdjasZOAvV 75 90 N Y 6\r\nJA 93 80 N Y 0\r\nXkCWQCTpXjZgCKflzh 86 83 N Y 0\r\nEsNOQfgeERv 75 81 N Y 0\r\nSuyWV 95 81 N Y 0\r\nLJJbpohSV 90 98 N Y 0\r\nMPGJLKflSBtZHKEtcsU 100 99 Y N 0\r\nAQrPmmXO 89 95 Y Y 0\r\nYDCmkIYBPOy 95 91 N Y 10\r\nXLiSnATGAHSSF 95 83 N N 0\r\nSGyOHlvb 90 84 Y Y 0\r\nMRiXqCxTvwToRyyEJgB 95 100 Y Y 0\r\nZNrctMuIhBMLmmi 85 78 N Y 0\r\nHdTpUsEc 98 75 Y Y 0\r\nRAhDEmlvNNicCa 75 92 Y Y 0\r\nIKyNgnEfzGQFnBMd 85 80 N Y 0\r\nJNtN 95 90 N N 7\r\nSBeSjxfxvmQIH 84 91 Y Y 0\r\nEregtaBcUOPuFfOWAxw 95 81 Y N 0\r\nBjcxDhbbJvDLXMVyn 87 79 N Y 0\r\nViFUYKFDOZegK 90 80 Y Y 0\r\nWsr 95 86 Y N 0\r\nJgdPGCFiUwUUfAA 80 92 Y Y 0\r\nO 96 97 Y N 0\r\nJWfAfenceiSgJq 95 85 N N 0\r\nQUJCpMvjCViQ 80 85 Y Y 0\r\nRnVtCGNCmLqmtdly 83 95 Y N 0\r\nQKnLkS 87 80 N Y 0\r\nMuQsSfe 90 97 N Y 0\r\nSkvAUshiLLErXihn 93 85 Y Y 0\r\nKhEmO 88 88 Y N 0\r\nHovCo 90 80 N Y 0\r\nUxZ 75 75 Y N 6\r\nBuD 97 77 N Y 0\r\nHfSAzTvoupTcUlldPWDM 75 95 N Y 0', 'YDCmkIYBPOy\r\n15000\r\n332750'),
(1001, 1, 0, 10, '100\r\nKSSHsCEhNHXIMxnKjszE 93 98 N Y 0\r\nKRXBnwAzDGjpfJgRpKt 95 90 Y Y 0\r\nJBOyaqxicVGB 96 84 N Y 0\r\nHolSWzxphdbM 99 80 Y Y 0\r\nYzYKojgqgraUpqNyJ 95 90 N N 0\r\nLgX 97 94 Y Y 0\r\nHCRuZbWsSDgkOEoj 80 90 N Y 0\r\nS 85 89 N Y 10\r\nAdLhdCBxwmJXRm 78 97 Y N 0\r\nQhgGp 85 87 Y N 0\r\nZFTdFawbiwDG 87 80 N Y 0\r\nJaOThlpkrRkejXMMXgbu 95 80 Y Y 0\r\nNiAntWp 85 91 Y N 0\r\nTe 81 81 Y Y 0\r\nTrFXiYBMzJ 92 87 N N 0\r\nMlhsUsruSKdrAGkUerE 97 92 Y Y 0\r\nNWpjgxcNegmyEGHlPTP 88 94 N N 5\r\nVCXJjomOzywFZBVkDR 87 75 Y N 0\r\nPxZYNvXNdbUtBIp 82 75 N Y 0\r\nTsJkMpF 85 99 N Y 0\r\nBwaIeOrqdatudl 85 80 N N 0\r\nIB 95 90 Y N 0\r\nHmPorGeOs 98 85 Y N 0\r\nDgRHzC 87 82 Y N 0\r\nWkRmkErigra 90 75 Y N 0\r\nMZdQ 85 78 N N 0\r\nNIrWcjt 81 95 Y N 0\r\nMQBfcr 85 80 Y N 0\r\nIXrLPdJhEekBRgsbkqI 95 85 Y Y 6\r\nMWAd 89 75 Y N 0\r\nBVYqUBdV 76 79 Y Y 0\r\nNhmSyMnvHxUkjye 79 90 Y N 0\r\nNUPyQqEhklWtWxeIz 80 95 Y N 0\r\nNsQbL 95 86 N N 0\r\nKEKFcpbDWQXUsQTXvFbs 95 95 N N 0\r\nZzjJWy 90 80 N Y 3\r\nTOWFblEWAuVgAxdHt 95 100 N N 0\r\nHRYFlarRQhe 100 90 N N 0\r\nSGUCaHJVg 95 87 Y Y 0\r\nCsNEZwUWERVFFxLsdyK 85 94 N N 0\r\nPZhrRpBMpm 80 85 Y N 0\r\nLsyePjMaoonOscyyUQY 82 98 Y Y 0\r\nSkPGcYDnYyIp 95 90 N N 0\r\nOaV 90 90 Y N 0\r\nW 75 90 Y N 0\r\nTJjZwq 85 75 Y Y 0\r\nPVFVBEocVjjpg 90 88 N N 0\r\nHMJecx 85 100 N Y 0\r\nPvgXSkNZQNcfJrgTzgm 90 75 N Y 0\r\nVnVXCwwJdbrusXG 89 95 Y N 0\r\nBNziq 95 76 Y Y 0\r\nMNjzTqMCtx 85 95 N N 0\r\nUihwjdZaUXWuMsytRtN 90 88 Y N 7\r\nHdXkkOHRQoG 93 85 Y N 0\r\nDHDAFValkXKFYjznnn 75 100 N N 0\r\nJskIgwiNH 85 85 Y N 0\r\nSeYSBFuVQaEUXwf 75 80 N Y 0\r\nXfGMOTDT 82 75 Y Y 0\r\nGnGatTgZBPgj 90 90 N N 0\r\nK 85 80 Y Y 0\r\nKRJVphYKwQTOMc 95 85 N N 0\r\nDFWtLGoFLXHptkI 75 92 N Y 0\r\nCayHZQXpTpyFPSuJz 87 76 N Y 0\r\nRRlRboFqAgLvzrJ 89 75 N Y 4\r\nDbWHykSi 85 84 N Y 0\r\nWQHneRqIh 90 82 Y N 0\r\nBhGejmW 90 93 Y N 0\r\nAFEEic 95 80 Y N 0\r\nMXhBgPj 96 98 Y Y 0\r\nWJsSWOCR 77 85 Y Y 0\r\nA 79 82 N N 0\r\nWy 95 85 Y Y 0\r\nOtgBaCKAVmMEFxPVcbE 84 75 N Y 0\r\nDnBaraTLVBkPJJj 85 75 Y N 0\r\nKzir 84 75 Y Y 0\r\nPxCMvWOdyZcRW 90 76 N N 0\r\nZcztKxXsrhqSDuxBeN 80 84 N Y 0\r\nEesxZKSACX 80 87 N N 0\r\nCvCT 75 85 N N 0\r\nUwrgH 76 80 Y N 0\r\nVwcGoYzhhVFWGyFzjdn 78 90 N N 0\r\nKQ 75 100 N Y 0\r\nMHJszTi 95 79 Y Y 0\r\nBPJXuWxsGGNYz 87 92 Y Y 5\r\nXWWpW 96 92 Y Y 6\r\nGKIdgeGTHWd 80 77 Y Y 0\r\nMeGS 88 75 N N 0\r\nSblEZfNCkGAkRSrzFADB 90 88 N Y 0\r\nBGLYzCxFVARuGu 87 75 N Y 0\r\nWMQkzgUafGBnqiuBgRpn 77 75 N Y 0\r\nLIWndzmjDozIMTu 80 94 N Y 0\r\nLsPuNFjIzCcppis 80 80 Y N 0\r\nJgycuqAJHELopIoZm 87 85 Y N 0\r\nUJmFKI 75 90 N N 0\r\nRdvGLr 95 85 Y Y 0\r\nWVsTKZHjDEc 90 85 N N 0\r\nVvtFQcwAErUIru 85 87 N Y 0\r\nJKTcYDOhBIJdTBG 94 85 Y N 0\r\nVJqtvLWNUdTEypjOCB 85 94 N Y 0\r\nET 80 80 N N 2', 'IXrLPdJhEekBRgsbkqI\r\n15850\r\n315050'),
(1001, 2, 0, 10, '100\r\nXODSnS 95 81 N N 5\r\nAmAM 95 87 Y N 0\r\nKjuUtQc 79 79 N Y 0\r\nCIKwP 80 95 N N 9\r\nXWINMqSzxcSwHu 90 90 N Y 0\r\nVmyNwsWNrzQzumpkYi 93 100 Y N 0\r\nJNyOUKq 95 97 N Y 0\r\nSbcPEILVKPtjJNI 95 95 N N 0\r\nHWlCiupwDIWsD 95 85 Y N 0\r\nUMaMMna 80 80 N Y 0\r\nFUGEOWMfgVlYlkQ 90 85 N N 0\r\nZECxJOwDbf 90 91 Y N 0\r\nULSdBWvpEoGyrriHXvB 80 89 N Y 0\r\nFdnaONSBrtoKpnLcOwQ 90 86 N N 0\r\nFcuXukEfTJaepfcVLGDo 90 88 N N 0\r\nE 91 80 Y Y 1\r\nWTTcx 99 95 Y Y 0\r\nCSuFsvUjAB 92 90 N N 0\r\nSpBFMARZU 90 86 N Y 0\r\nSJ 85 95 N N 0\r\nACBZClCVEY 80 80 N Y 0\r\nWbpifAcJsMvZsIuxPb 80 80 Y Y 0\r\nLglYGtexoUrlFtvCg 97 75 Y Y 0\r\nQurAslbu 95 85 Y Y 0\r\nCEykvNKWzMSYtFFZqGxB 94 89 N Y 0\r\nFUtuwRZqZGEBsetLG 75 90 N N 5\r\nNLlUN 90 95 Y N 0\r\nXkkZELIs 95 80 N N 0\r\nRLL 97 89 Y Y 0\r\nGzphmddzEKiHp 95 80 Y Y 0\r\nJqatUa 95 99 Y N 0\r\nTlckBkdqgu 88 76 Y N 0\r\nMsANXDtZCoYYKlxJyXxd 75 80 Y N 0\r\nNoUuxKxbhIE 80 95 Y Y 0\r\nWxiiIO 96 90 Y Y 0\r\nIAVsLoFQtsAAXIwkTF 98 99 N N 0\r\nJUCx 85 75 Y N 0\r\nVmJHYTcPKUoLTILytZM 85 78 N Y 0\r\nIGcbksHLMRlngsVqz 75 90 Y Y 4\r\nLrzHFThFFAasgfcZeuw 99 96 Y Y 0\r\nXQ 87 90 N Y 0\r\nS 90 100 Y Y 0\r\nBoN 75 75 Y N 0\r\nYTUXuruVhIaqN 87 80 Y N 0\r\nRZX 80 95 N Y 0\r\nLqQdXchxqHacFo 80 90 Y Y 0\r\nWenYJQMBPFHmut 85 80 Y N 0\r\nPAwecWyvQkikNfPV 92 80 Y N 6\r\nRkihOMXBLaHAcGkSdH 90 89 N Y 0\r\nDqIDtJrPa 94 95 N Y 0\r\nHssCJPipFFBNKqSTI 90 77 Y Y 0\r\nFvwYhmlFDwYLNGJo 76 95 N Y 0\r\nCktjdwA 87 85 N Y 2\r\nXqcLtILWrH 90 95 N Y 0\r\nWUWzuUt 85 80 Y Y 0\r\nITxsJXcGHC 90 94 Y N 0\r\nQawKYU 95 85 N N 0\r\nLXadSVqxcTbaP 93 94 Y N 0\r\nSqrSt 76 85 Y N 0\r\nYVpaVs 95 75 Y Y 7\r\nJI 95 90 N N 0\r\nOgYRmoSiNWVbSdocM 85 95 N Y 0\r\nYVCxaZPHiY 99 81 Y N 0\r\nBWqPJ 99 84 N Y 0\r\nXnBWuQdMh 93 85 N N 0\r\nEwKTLdQvmUJkdD 82 76 Y Y 0\r\nU 83 81 Y N 0\r\nApIwR 95 75 N N 0\r\nYFPeOIBN 99 97 Y N 0\r\nXJxUn 95 78 N Y 0\r\nYF 75 85 Y Y 0\r\nRMEpQYLSw 100 90 N Y 5\r\nTCIJOG 84 99 Y N 0\r\nFZXWWtgXTxSZFzDeSY 80 82 N Y 0\r\nWdxvfeOc 97 75 N Y 0\r\nSIlTmjkbHKqRirpOCAgq 84 90 N N 0\r\nHFjBUPtiaUxoZc 90 85 N Y 0\r\nSCxZrJjsfqHWYQf 85 94 Y N 0\r\nVhLeCMseuz 85 85 Y Y 0\r\nSDKqshkHjxs 75 98 Y N 0\r\nOZxC 95 80 Y Y 0\r\nU 87 75 N N 0\r\nPrSIkzb 80 77 Y Y 0\r\nUwmERlxVvWA 78 80 N N 0\r\nNSKbkCGjCf 75 85 N Y 0\r\nMriCgsUio 80 95 N Y 0\r\nZWRyHTJU 87 80 Y Y 0\r\nYoQBzJtKTZ 90 86 N N 0\r\nEGK 95 96 Y Y 0\r\nCYFVbgGoFwEhcgMTUw 95 90 N Y 0\r\nGrRWiR 85 80 N Y 0\r\nNYNlVNgO 95 75 Y Y 0\r\nNNRVpAF 85 89 N Y 0\r\nFMuTUqk 95 95 Y N 0\r\nPXD 85 95 N Y 0\r\nLyFr 99 100 Y Y 10\r\nOGLltVsbdriqDg 90 80 Y N 0\r\nScAWqkBXwtWz 90 95 Y Y 0\r\nEqgEwtbpSWwIWHxAN 94 80 N Y 0\r\nYmGW 97 85 N N 0\r\n', 'LyFr\r\n15850\r\n376500'),
(1001, 3, 0, 10, '100\r\nUFZOcQTBHjpwCMYn 90 95 Y Y 0\r\nKyrK 90 97 N N 0\r\nCysmgRwxDSNLpjrl 85 75 N Y 0\r\nELrEcHjXsUoiRd 76 93 Y N 0\r\nBJKXRcXMkoJGSfV 75 90 N N 0\r\nJlEaFDVDTdBUyQWax 80 85 N Y 0\r\nZCcbRRBkRfo 75 86 Y Y 8\r\nRkShJ 92 98 N N 0\r\nT 80 98 N N 0\r\nHGkgmL 90 96 Y Y 0\r\nLOlutboG 75 78 N N 0\r\nWPgpHocpVaFqHDsTqciN 80 75 N Y 0\r\nVPHEqLbNtBkYV 80 90 Y Y 0\r\nDHswaajQYBbZJeiqslc 97 93 N N 0\r\nZedpSTlOCjOSg 84 90 N Y 0\r\nDIJswe 90 75 Y Y 0\r\nCHPjh 90 87 Y Y 0\r\nQGdeyvvRut 99 75 Y N 0\r\nIB 85 90 N N 0\r\nAuCAAiPbUF 94 90 N N 0\r\nI 85 89 N Y 0\r\nUEGKE 93 85 Y N 0\r\nPyCqoFmtFcSItE 100 90 Y N 0\r\nPnrNRiqJ 75 95 N N 0\r\nTdtKaStTwPVWV 93 90 Y N 0\r\nX 88 76 Y N 0\r\nM 93 99 Y Y 0\r\nCS 79 93 Y N 0\r\nUAGVVOiUT 75 96 Y N 0\r\nPV 96 80 N Y 0\r\nVx 75 79 N Y 0\r\nBDgEmERNOmyX 91 75 Y N 0\r\nLJE 95 83 Y Y 0\r\nUIFGKBcTHsGxYANNHraq 85 89 Y N 0\r\nPTrTHHmcwH 85 75 Y N 0\r\nOgXBP 90 75 Y N 0\r\nWp 83 79 Y Y 0\r\nHDrhXVtEPAOR 83 75 N N 0\r\nLlpQK 80 90 Y N 0\r\nXqgkRUiFXMeWTSMIXae 95 90 Y N 0\r\nVxuemIoRgjUQe 93 93 N N 0\r\nUwS 90 76 Y N 0\r\nHOMNGhJfueorHMl 99 100 N Y 0\r\nHLawxZVlOg 80 99 N Y 0\r\nZTLejisS 80 79 Y Y 0\r\nCvvNscCnZVOiriPN 90 95 N N 0\r\nANyQeAqkYvGhFPCqWA 80 90 N Y 0\r\nUYBTL 80 85 N N 0\r\nDCBVl 82 95 N Y 0\r\nNdsOgK 76 75 N Y 0\r\nMFEuuuBChlMLjKO 90 88 N Y 0\r\nF 95 75 N N 0\r\nXMVNIZlUaUHaf 80 85 N Y 0\r\nBGTi 86 95 N Y 7\r\nTP 85 93 N Y 0\r\nTgLwEuo 85 83 Y N 0\r\nHHeXOMfgB 96 89 N Y 1\r\nDiymeUrkzGWyAJGJtm 76 100 N Y 6\r\nIfcRnqMJkslbJ 88 88 Y N 0\r\nXKYDCSfK 80 95 N N 0\r\nYO 90 92 Y Y 0\r\nZHFXiqnGIJY 75 80 Y N 0\r\nHUeQYggJJVpWayTyys 93 85 Y Y 0\r\nSnwoWt 80 92 N N 0\r\nZrKDpgLzzLRseYxwHFY 90 90 N N 0\r\nTJrFYWUrUTKp 75 82 N Y 0\r\nUkPbxhLAJPwJJgVU 97 88 N Y 0\r\nOTDoNRB 91 97 Y Y 0\r\nInnTSmiYxKKTfsUbn 80 75 Y N 0\r\nRXsvpaqbPjLXBlNaVrc 90 90 Y N 0\r\nNSBzurUwDWbAYHY 83 98 N Y 0\r\nWSTUbidxKesLHEJd 90 99 N Y 0\r\nVDKrj 100 94 N Y 0\r\nSdTmfCQEuOZ 95 82 N N 2\r\nVmKkpQPBwp 75 90 Y N 0\r\nHsZoSIMOmMxJKQl 80 80 N Y 0\r\nYLnnpg 95 87 Y N 0\r\nNZEUZGLRZCkYvb 90 90 Y Y 0\r\nUM 90 95 N Y 0\r\nTqLUjeKMZ 85 97 Y N 0\r\nMD 80 95 Y N 0\r\nYCKUZhGAAXAUZu 92 90 N Y 0\r\nTeeApAXcvDbCxqGbRbsX 85 85 N Y 0\r\nSWKpTiwqleAkXZisYF 87 92 N N 0\r\nJOuZVAqcd 88 95 N N 0\r\nAGcOK 86 89 N Y 0\r\nFtDLkGP 95 93 Y Y 0\r\nKH 83 89 N N 0\r\nTzXvxeEF 90 88 N Y 0\r\nYgQsmqAometSIsgT 84 75 Y Y 0\r\nFLglQZOCYBjbeJFEpf 100 86 Y Y 0\r\nCq 80 95 N N 0\r\nLM 90 88 N N 0\r\nRj 77 95 N N 0\r\nWdDRjsOnJ 85 95 Y Y 0\r\nBAf 88 85 N Y 0\r\nX 91 85 N N 0\r\nMfhcDIwTiiCR 95 94 Y Y 0\r\nYGxZUsVuOSVpGSAn 87 98 Y N 0\r\nTkcTzncqZDENcPOL 89 90 N N 0', 'HHeXOMfgB\r\n15000\r\n311200'),
(1001, 4, 0, 10, '100\r\nPbc 86 85 Y N 0\r\nHXekOchIacYeRpNpqxi 75 78 N Y 0\r\nVMFuCwR 76 94 Y N 0\r\nRFESPRSwimdNOupwD 80 95 Y Y 0\r\nAkibXDoYjl 90 85 N Y 0\r\nMiwTLxayOFxAjBViRCE 86 100 N N 0\r\nTrbOXCPmjbSY 95 83 Y N 0\r\nHAAd 97 85 N N 0\r\nLZWnJ 84 75 Y N 0\r\nXRZJiIM 75 98 Y Y 9\r\nIwyd 88 89 Y N 0\r\nSgzbizBGhc 85 85 Y Y 6\r\nOFWSciGYcyCvtllNPNl 80 95 N N 0\r\nIJLZmDJulOjSTOr 85 100 N Y 0\r\nIlB 91 96 N N 8\r\nSIu 80 88 N Y 0\r\nDkhOUqwZdT 85 95 Y N 0\r\nPjXKeUUW 95 84 N N 0\r\nVmQSKvueLmRedNUMHcLO 98 75 N Y 0\r\nBXJTdaZZlsbMaa 77 80 Y Y 0\r\nJsGVsFwcu 86 85 Y N 0\r\nOvNXiblcXgTVnI 77 92 N N 0\r\nSMfFDgoG 94 93 N N 0\r\nUKkFkFNtXIzWzkPTN 95 80 N Y 0\r\nSvjEjgBcHBTRa 75 90 Y Y 0\r\nSRUoWwMmFLC 95 75 Y Y 7\r\nRqJlWkCzrahlfQTl 78 90 Y N 0\r\nXM 83 95 N N 0\r\nDZGYKrDvBa 88 94 N Y 0\r\nJMWOjjgNEtdAuFJZxkVL 75 80 Y N 0\r\nRjVuxQkpbvImBSv 89 93 N N 0\r\nShRFbbvWZNN 90 75 Y N 0\r\nBtqngENjocSpxAuZlcst 94 85 Y N 0\r\nJKGnBehLBdPB 91 90 Y Y 0\r\nCxDMfiuHactmHL 85 77 Y N 0\r\nFyMCeegreCzeGiF 80 82 Y N 0\r\nMUmmSFNEqPuGrVMJV 94 100 Y Y 0\r\nESiXBieIwXjES 85 79 Y Y 0\r\nLbEUGjSHXFqNr 80 90 N N 0\r\nBNboso 99 87 N Y 0\r\nUGRLkihagF 75 95 Y N 0\r\nJgbbaZAhFzAWWU 76 90 N Y 8\r\nFLiHlZanTCruAiLek 96 86 N N 0\r\nBbCPScsCTdaDmgWuh 94 91 Y N 0\r\nSYWHdiAzRWdckWd 80 85 Y Y 0\r\nEvwptbmXArCR 95 95 N Y 0\r\nRY 98 92 Y Y 0\r\nGQUtOFJUR 95 97 N Y 0\r\nIHgVC 95 90 Y N 0\r\nJoVSgwUKMJfBOWnR 77 80 Y Y 0\r\nWThogBEIlxzGg 76 78 N Y 0\r\nPTjePsyUXzHULWtBvxzD 79 85 N N 0\r\nPykCKPyIye 84 96 Y N 0\r\nVWKNHlIUETsruuMFymJ 75 83 N N 0\r\nXVjgfdnPfWCoQLtskHG 95 90 Y N 0\r\nFLxTWlReygXsMQOi 90 95 Y N 0\r\nVSSIBzTVdqEjb 85 83 N Y 0\r\nICfTwPOuTpJIBcuPM 79 80 Y Y 0\r\nLIfnfxyFAilueGsiXbK 100 90 N Y 0\r\nILwZpBDiuoMhXwswxgEe 91 99 N N 0\r\nHgHAeuBIcJ 91 75 Y Y 7\r\nMLB 100 84 Y N 0\r\nG 90 80 N N 0\r\nRoTyLtQwejye 80 75 N N 10\r\nPjrf 93 84 Y Y 0\r\nMQrpSTxSdjQAq 93 75 N Y 8\r\nZyejTNiikWWHHyhI 97 92 Y N 0\r\nZNUegFN 95 85 N N 0\r\nC 90 78 N N 0\r\nBIccuOyGTp 88 95 N Y 0\r\nZmHNvOxTWUMjd 90 77 N Y 0\r\nSKMaiZvuWvgEdMmzzDZ 80 90 N Y 6\r\nNqaUtbTiXhp 80 99 N Y 0\r\nTLujwKJIHAPslBSjXPF 80 75 Y N 0\r\nOnIIxittrvwrLXOg 78 78 Y N 6\r\nGeqn 77 75 N Y 0\r\nEdwUkyV 75 90 N Y 0\r\nY 82 78 N Y 0\r\nDrhUzffpm 90 88 N Y 0\r\nFeYbszANUnub 79 77 N N 0\r\nBxELfghqaNDhRG 80 94 N Y 0\r\nJbfWtaM 85 91 N Y 0\r\nFAIUHwUVMgvkWXVaBdV 94 90 N N 0\r\nDqQlBCPHLF 94 80 Y N 0\r\nQavAC 75 98 Y N 0\r\nSwkfKqtTG 80 80 Y Y 0\r\nZhCIZAGyhajH 88 91 Y Y 0\r\nLZ 84 78 Y N 0\r\nHUXJFQzmIhfFR 89 80 N Y 0\r\nJEavDBAzVy 93 86 Y N 0\r\nCNpKdOcj 87 78 N N 0\r\nYjaRmeRbQQ 88 86 Y Y 2\r\nHfLkfcP 98 80 Y N 0\r\nIuzgXQyhMgghbM 94 85 Y N 0\r\nUwPRvymKaTxLhXPLgS 87 95 Y N 0\r\nAsqWOw 85 91 Y N 0\r\nUcrOLkHfk 77 75 Y Y 0\r\nLMQGBivsyG 89 80 N Y 0\r\nLYfhBBKEvv 92 96 N N 0\r\nEKwHlSMSICqMs 79 75 Y N 0', 'IlB\r\n14000\r\n320050'),
(1001, 5, 0, 10, '100\r\nRDVRyo 75 89 N N 0\r\nNbLorLPWFust 75 85 N Y 0\r\nPYjzHwP 99 94 Y Y 0\r\nFcfubnlnNNUPGQQks 80 83 Y N 0\r\nHlkyMwOyDtQ 90 85 Y N 0\r\nYULUAHWXNXglhEOpu 90 95 N N 0\r\nUVrGVYXgSay 87 89 Y Y 0\r\nN 82 75 Y N 0\r\nCiZdpzk 85 92 Y N 0\r\nYGoIYPAaUZB 75 95 Y N 0\r\nVsULAuYGLRJTn 97 77 Y N 0\r\nEdOairN 80 90 Y N 0\r\nQidSkeNNiZuEQMSS 95 87 N N 0\r\nCkeyipoORDPVzh 90 85 N Y 0\r\nS 86 95 Y N 0\r\nRsk 90 95 Y Y 0\r\nJoUxXo 75 85 Y N 0\r\nVEyWcKmMwEECmXUUiN 99 85 N N 0\r\nEgYwAo 75 90 Y Y 0\r\nPCPoQWeo 79 95 Y Y 6\r\nPDhTYMyNDvkr 80 75 N N 0\r\nFGeahCxoC 80 98 Y N 0\r\nGwmWBkiqsKZQmm 85 95 Y Y 0\r\nOzlKFaZupcVfJeH 80 80 N Y 0\r\nWPovsYBSwhEKRpsaPzA 93 75 N Y 0\r\nSjHddMMKtLE 91 95 Y N 0\r\nIb 80 90 Y N 0\r\nEgZA 89 80 Y N 1\r\nHJxEs 96 75 Y N 0\r\nRqhYuUtNieNUnsvhaH 92 85 N N 2\r\nRdolbbFE 95 85 N Y 0\r\nAgJKkdhVMwNueyuhMi 92 91 Y N 0\r\nMOdkuwdyuvUuKZnD 85 89 N N 0\r\nFIOpJgZoWKpCo 82 98 N Y 0\r\nLAzGkTsTKZuWcpWx 95 85 Y Y 0\r\nBGbeVtkPhUzRVPddWi 98 76 Y N 0\r\nEifucsMhQovhiHzmYrd 75 90 N N 0\r\nASwu 96 80 Y N 0\r\nAAWCaBffGJongVqwkk 76 80 Y Y 0\r\nNFcgTeyT 99 85 Y N 9\r\nDGMqW 95 100 N Y 3\r\nNmPsTEYOkY 81 80 Y Y 0\r\nKylj 95 85 Y Y 0\r\nMKEamibK 97 95 N N 0\r\nSfXlWCEcosb 95 90 N N 0\r\nMShPPnQtdnIYWCJJF 95 85 Y Y 0\r\nBxQVYzQuOtoC 85 80 N N 0\r\nOAoQGn 92 81 Y N 0\r\nHHzpC 81 88 N N 8\r\nQOXBqRdnYAs 85 95 Y Y 0\r\nVjvQGFcMjvXTSDK 95 81 Y N 0\r\nAzlksTEIhMRSJRhxy 89 75 N Y 0\r\nPrFscCCSRZFQ 84 88 Y N 0\r\nTtahPgzMrPvncCsOMMzp 90 100 N N 0\r\nPKVULSvBNLSDlQ 90 90 N Y 0\r\nAVEtFUveYxcnPfOXfHI 80 92 N N 0\r\nNmBWPsDTtvDlAt 100 80 N N 0\r\nF 80 81 Y N 0\r\nRqHsLEooY 80 90 N N 0\r\nPtOLgkhcR 85 80 N Y 0\r\nPLOHK 94 90 N N 0\r\nP 82 75 N N 7\r\nUmciCWoyT 80 78 Y Y 0\r\nGAWDweGDYskHnQk 90 100 Y N 0\r\nIJE 81 83 Y Y 0\r\nOnVFuxevWzTDA 93 86 Y N 0\r\nAHrHvyaPCfsVNmDIWWo 86 91 N Y 6\r\nKCBSLlcfJyAPBj 85 75 Y Y 0\r\nQawOMDkeJlormRnhe 90 81 Y N 0\r\nOT 90 98 Y N 0\r\nBYaPLabeEVwtrB 88 82 N Y 0\r\nVamAq 85 89 Y N 0\r\nVTiUA 95 80 N N 0\r\nSSNkaLdkxmAtP 85 95 N N 0\r\nOJB 98 91 N N 0\r\nLIqQalcebHdzj 75 96 Y Y 10\r\nWKlGLytTd 95 89 N Y 0\r\nPIXhCtLSMoCCA 96 80 Y Y 0\r\nULzmzuqKLBoAFtK 80 80 N N 0\r\nZnlCehX 77 77 Y N 0\r\nCEjxxKKGDf 90 90 N Y 0\r\nHvpDIKdEiUzWvt 90 81 Y N 0\r\nTlMuWvRTlNj 97 75 N N 8\r\nYIFvifAtXjyvDF 76 97 Y N 0\r\nMtCNoAEoqOJPv 95 90 N Y 0\r\nKflCVSqbOBK 90 81 Y Y 0\r\nFc 91 85 N Y 0\r\nXpWdeRA 89 76 Y Y 0\r\nOorwfyqXYbZjrTpclNF 84 95 N Y 0\r\nUoysYXmlvO 81 95 Y N 0\r\nAMlBgQfD 80 97 N Y 0\r\nUpOkXMuyURCCRaoCw 80 80 N N 1\r\nFtIeDqx 98 90 Y Y 0\r\nCiVc 75 95 Y Y 0\r\nBdKO 75 75 Y Y 0\r\nNhDwAd 80 85 Y N 0\r\nKUTLEFlaYoIojUVk 96 81 Y N 0\r\nCBEsPPZB 75 75 Y N 0\r\nJvE 76 93 Y Y 0\r\nCgjkSvcwDthjnprSHxx 95 75 Y Y 0', 'DGMqW\r\n15000\r\n350700'),
(1001, 6, 0, 10, '100\r\nMzYCufzWJkGT 95 90 N Y 0\r\nKYmIQs 80 85 N Y 0\r\nNVufr 95 94 N N 0\r\nJmhCuEFOUoKgI 75 88 N N 0\r\nKyRmZdbJMfYTzbZS 78 90 Y Y 0\r\nESBjuYJuibPovnKCdyQ 95 77 Y N 0\r\nSfKyrATJa 90 85 N N 0\r\nJpPNv 80 85 N N 0\r\nQLXkCluFcKh 85 75 N Y 0\r\nTfYqUD 90 85 N N 0\r\nFAyWTFfabq 80 95 Y N 0\r\nRktoKBKAm 90 83 Y N 0\r\nFNdCECSt 95 81 Y N 2\r\nKsHzrelGZguPwheHhw 79 80 Y Y 0\r\nFprWTbg 90 95 N N 0\r\nSgXaGDAruSFH 90 75 N Y 0\r\nIw 79 92 Y N 0\r\nAj 96 75 N Y 0\r\nGRXyYlVokmXU 85 80 N N 0\r\nXYhTEVBhVhGbtlEEDapv 96 95 Y N 0\r\nMIcOWomeJ 90 75 N N 0\r\nAicIWqZoiKLlExLgYq 75 77 N Y 0\r\nMpThDPKOTcuHdAsCOj 88 75 N Y 0\r\nJhHKDBmYwDjI 90 75 Y N 0\r\nPZIp 89 78 Y Y 0\r\nKGrTUPBKfHiGdkhDyp 79 99 N Y 0\r\nIVzqVKMUtMEMNMhDZoC 77 95 Y Y 1\r\nUH 95 76 Y Y 0\r\nHZaQTPJAIDfxlBWa 97 95 N Y 0\r\nOYqqFxNEKi 92 93 Y Y 0\r\nYEIGXpWWAVdjYliI 75 95 N N 0\r\nKiEZUReyFC 76 96 N N 0\r\nAKVxHnqsq 100 84 Y N 0\r\nNeynYn 80 100 Y Y 0\r\nMGYvGdJtzgDQMJbKiqi 90 91 N Y 0\r\nCJxNdGRvszHMJ 90 100 N Y 0\r\nQaW 84 90 N Y 0\r\nYVJbiEvWMhkmwpk 95 75 N N 0\r\nJgGIdXehJoJXBFEZ 79 85 N N 0\r\nHgdZeHAia 90 95 N N 0\r\nKVRKybLWtgvMO 90 79 Y N 0\r\nFqQiwCWcYwwJFRTzs 83 81 Y Y 0\r\nExHBuhysmTCImTNM 95 85 N Y 0\r\nVRPKYyLs 90 95 Y N 0\r\nWRhPbCFTEGoIsYJ 95 85 Y Y 0\r\nYJrRSaXAyCFXthWKXb 95 90 N Y 0\r\nLSGZRyroxESqlTDLPygW 75 84 N Y 0\r\nOAzBzPhGwXBbXyceKAh 75 79 N N 0\r\nMRcf 78 75 N N 0\r\nPnocRXHxa 90 94 N N 0\r\nDSrYGnQkbkaJEj 89 83 Y Y 0\r\nMDNbsqgFkkNJcKSMltgt 92 79 N N 0\r\nXQjtBfngEEWlCWkDmWxs 99 85 N Y 0\r\nLWxGGzcULfdYz 83 91 N N 0\r\nXkkjQEZvHuxmj 80 100 Y N 0\r\nQHorTyeoyv 95 85 Y Y 0\r\nXhKjVOvliOE 95 95 N N 0\r\nHsbIENhJzHBIybuib 81 80 N N 0\r\nEx 80 83 N Y 0\r\nKOHSxFDkI 95 80 N Y 2\r\nDNiRiR 85 76 N Y 0\r\nTyyWUjDkDzrvlJxhdu 89 95 N Y 0\r\nMnadQyg 85 95 Y N 0\r\nAEhHsxsQNLlzX 89 75 N Y 0\r\nQlbGWGQiLuXQwjQdXjqw 95 91 N Y 0\r\nYeacrY 78 85 N N 0\r\nAYwyiyCg 87 95 Y N 0\r\nAIrEnhkkiMRCw 93 96 Y N 0\r\nAlerTviL 100 75 N Y 1\r\nQa 80 90 N Y 0\r\nEL 88 80 N Y 0\r\nHZhEUJKFapBYiXrIBmLl 95 80 N N 0\r\nPVTrtVL 75 85 Y N 0\r\nCrUuGaAAWY 91 80 Y Y 0\r\nF 98 90 N Y 10\r\nEDzMsdUoHZRbm 92 86 Y N 0\r\nSWgAnBqjdspqimqSnIe 80 79 N N 0\r\nTiywblZKQxphP 95 75 N N 0\r\nAbVxejWeLkARSHS 90 90 N N 0\r\nBTSNYgaM 90 85 Y N 0\r\nIBKYOiYOx 90 95 Y Y 0\r\nGkkFOl 84 96 Y N 0\r\nOfWoBlbIdoxZiFQTDqb 92 90 N N 0\r\nKjLfr 80 90 Y N 0\r\nDQ 80 80 Y Y 0\r\nVHdwANLLL 97 93 Y Y 0\r\nXZgHcNSvCmhsYr 95 87 N Y 0\r\nLglligckY 88 95 N N 0\r\nGWamLFvttqLvqYEk 75 91 Y Y 0\r\nBkIcJbGaqMsx 80 91 N N 0\r\nVzpyHxfHMkWKGLmKmBD 90 88 N N 0\r\nRnNNqDfDPe 98 90 N Y 0\r\nWKUwbrZUPSlemyHnG 95 77 N Y 0\r\nUuPiJyTrZldgFC 95 86 N N 0\r\nOINCIjHytMtjypLS 89 85 N N 0\r\nXPmwFEm 75 94 Y N 0\r\nXuop 80 75 Y Y 0\r\nXdwaEcQOwXmIvDeVFn 90 85 Y Y 0\r\nXyYOt 95 96 N Y 0\r\nOpiGinpQf 86 80 N Y 0', 'F\r\n15000\r\n324650'),
(1001, 7, 0, 10, '100\r\nPUQUnnMCvvifcYKS 91 81 N N 0\r\nLKmTZKyzAogY 96 99 Y N 1\r\nHxgsRNdLJzcPYIi 82 92 N Y 2\r\nAjpLBYCvsxjmqY 95 91 N Y 0\r\nTqihhV 85 83 Y Y 6\r\nHRC 89 80 N N 0\r\nH 100 85 Y Y 0\r\nNixtCBiobBBcOxbGmJ 79 95 Y N 0\r\nXq 75 81 Y Y 0\r\nETtrHHnqiLdpwQKei 81 94 N Y 0\r\nNYL 95 75 Y N 0\r\nXUBCvg 95 88 Y N 0\r\nKsoPUD 90 84 N N 0\r\nXeeiHfsVEaqQrbtHlSb 90 75 Y Y 0\r\nUCUUW 98 80 N N 0\r\nARz 75 95 N N 1\r\nLRiaFqnoGFWLYokaNauW 81 91 N Y 0\r\nHyrqIxzusfvxXQ 92 95 Y N 0\r\nZugb 84 85 Y N 0\r\nHuECLLgDZETrNuxx 90 95 Y Y 0\r\nGMUeTHqzlB 94 75 Y Y 0\r\nWcaiZXCiTYUJjeUhIxz 95 92 N N 0\r\nChPCIEoAYbPSTwqUTy 98 75 N Y 0\r\nSTnIrpcGfyuqKlop 92 91 Y Y 0\r\nMhvuI 95 95 N N 0\r\nJJjOVCtL 90 99 Y Y 0\r\nQ 97 77 Y N 0\r\nVCbsLD 95 94 Y N 0\r\nWGtaOifRd 90 76 Y N 0\r\nErCwyPRnxvZaeXzyqC 80 99 Y Y 0\r\nF 80 76 Y N 0\r\nEqWWrL 87 100 N Y 4\r\nCW 95 80 Y Y 0\r\nTZYiouTkHas 75 75 Y Y 0\r\nYBqilxkGZJx 94 100 N Y 8\r\nZhQ 85 86 N N 0\r\nVqVOGkunHXMjzFhKT 82 84 Y N 0\r\nYgjIMJvrSwpjyBt 85 80 N N 0\r\nNSIjGAPKYLIb 98 85 Y Y 0\r\nPmqLFEWIgBblJnAxKmE 75 99 Y Y 0\r\nGCJlSsKJXzHCzIt 80 90 N Y 0\r\nChMGtPHhjP 97 95 N N 0\r\nUERuLLCJbpvr 75 97 Y N 0\r\nIHfRyvenjUNvZ 85 75 N N 0\r\nHrysA 86 95 Y N 0\r\nEeoJjO 88 84 N Y 0\r\nGdZaBCPYUdUa 89 85 N Y 0\r\nMKeeBYun 99 93 N Y 0\r\nSl 95 75 N Y 10\r\nKLaTAxVp 92 86 Y Y 0\r\nMaNkrMVOIxjJiYZX 80 83 Y N 0\r\nLUOedeGTBheXtSdL 91 86 N N 0\r\nQuektgzYapx 90 77 N N 0\r\nHdqsc 85 85 Y N 0\r\nEB 85 80 N Y 0\r\nDBGfXvy 95 96 Y N 0\r\nRiIQjTwl 80 81 N Y 0\r\nAyOs 85 98 Y N 0\r\nOtqaipehLnhnxXnBkx 75 90 Y Y 0\r\nOA 81 88 Y N 0\r\nT 98 92 N N 0\r\nDDYefzXNngYeeueBH 90 95 Y Y 0\r\nCOwFGmydoBP 87 75 N Y 0\r\nTMkfvMdaChWfbREF 90 75 N Y 0\r\nMurkdxrRZWbCmvOTPvX 80 99 Y Y 0\r\nCHOp 77 80 Y Y 0\r\nXaCeksUvjVYoTDDlA 95 85 N N 0\r\nQjiNbZXtTKPMqKDfC 99 90 Y N 0\r\nIRtjKjVZYFGo 95 95 Y Y 0\r\nI 80 79 N N 0\r\nBhcbBj 90 80 Y Y 0\r\nScTyrOqPI 85 91 N Y 0\r\nQvT 95 80 N N 0\r\nNtVJkwJ 85 78 Y N 0\r\nMfhGppwKNPRNMJ 75 85 Y Y 0\r\nAqjRLvrFzTTOd 88 99 N N 0\r\nVyPhUiVNUrlIKJGTIJMV 78 85 N N 0\r\nDSIhYSkoGUPvJlBWCPs 97 85 N N 0\r\nVAHle 92 87 Y Y 0\r\nIzCSMnowRthplqftD 91 100 Y N 0\r\nVOnGdpBBBeMJFP 76 80 N Y 0\r\nHZRPVHGQ 90 85 N N 0\r\nVjRxopMjUjh 95 88 Y N 0\r\nF 93 90 Y N 4\r\nKDIiqBJvyVKmYK 79 78 Y Y 0\r\nVjbvWqtedXaCIlkFa 83 85 Y N 10\r\nAzSQFau 92 99 Y N 0\r\nVo 85 98 N N 0\r\nRr 91 94 N N 0\r\nNwJjjFhgc 91 90 Y Y 0\r\nVjrlhAGmIK 85 82 Y Y 0\r\nR 80 90 N Y 0\r\nWFiVNPPGtLyYutaXuP 93 97 N N 0\r\nVE 90 98 Y Y 0\r\nJkMtq 75 85 N Y 0\r\nKnCvGPSsFGvSy 80 79 Y N 0\r\nZjNuJifeLfDCp 88 94 N N 0\r\nSSkVniNxsPaduyghLQFq 75 99 N N 0\r\nPW 78 76 N Y 0\r\nPCvt 81 80 Y N 0', 'YBqilxkGZJx\r\n15000\r\n366150'),
(1001, 8, 0, 10, '100\r\nZWbnpvRIZYJkleTdfZm 75 82 N N 0\r\nBVfxDT 85 99 Y N 0\r\nXPigvuqORPVhH 75 97 Y Y 0\r\nDRbWXUfI 86 95 N Y 0\r\nQrdEmydUhZmVaZKHPlY 80 90 N Y 0\r\nR 80 83 Y Y 7\r\nMXJurXEyZkLbB 80 95 N N 0\r\nHQNyPigyvBddctC 95 90 N Y 0\r\nSrYvKdTfbO 95 90 Y N 0\r\nXEpMyTjZHx 75 85 N Y 0\r\nVhxFByGDdjU 82 79 N Y 0\r\nTSJF 75 85 Y N 0\r\nRaNxtaKTFUH 75 85 N N 0\r\nQ 85 90 Y Y 0\r\nSNdToBHYhIODk 95 80 N N 0\r\nZUFXPiUqfVJOjec 90 95 N Y 0\r\nTtPGxHTxSksAeChr 97 85 Y N 0\r\nTBjcMyfnmOTAFRBAy 80 95 Y N 0\r\nNeNNKuGPA 85 85 Y Y 0\r\nRjpJU 90 100 N N 0\r\nHdpCfN 96 78 Y N 0\r\nMryDEMkOfqwSYEMDzvw 84 93 Y N 0\r\nQJSTGG 85 75 N N 0\r\nOpG 75 95 Y Y 0\r\nOKvIUnohvvH 85 90 Y Y 0\r\nUvIkrDvw 85 95 Y Y 0\r\nDK 97 80 Y Y 0\r\nZhFBfNI 93 87 Y Y 0\r\nCLjLchtBYEq 95 87 N Y 0\r\nPZcwNuqupy 81 81 N Y 0\r\nSeRxDAjSxRXhmAMkv 79 85 N N 0\r\nVZVMSMwqGlJdsuE 92 90 N N 0\r\nDdYSFBVugwKUAnE 95 98 N N 0\r\nTsvFTlmekL 75 80 N N 0\r\nUvTUZWkSvmjUPDaAZ 90 90 N N 0\r\nVSfPolWPavDhmQOxy 90 95 Y Y 0\r\nXcHhLlwHNBaLqHuHZ 75 90 N N 0\r\nHccBNvJEZuRnmCAmnl 97 98 Y N 0\r\nQTclkTMhvcYKu 97 80 N Y 0\r\nNMZzGNgKSOeHzp 81 88 N N 0\r\nNJnMAanjfVGpATmqvIag 86 88 N N 0\r\nHcwqojkcroWJ 95 96 N Y 0\r\nSRSPS 80 93 N N 0\r\nQIN 86 75 N Y 0\r\nQSnylWCLeY 90 75 Y Y 0\r\nZqs 93 95 Y N 0\r\nIaKMvvyXhEKcQU 83 80 N Y 0\r\nSSqCmnqe 85 85 Y N 0\r\nRaC 90 85 N N 1\r\nCutGiRlbfFVfvWyQFsJb 84 78 N Y 0\r\nYtUHmp 95 80 N Y 0\r\nBoPq 77 79 Y N 0\r\nIjGxHLRfNh 85 100 Y N 0\r\nJQjnglSiKkryCRc 75 95 N N 0\r\nONNkxDOOSxCDRLMCpSI 75 78 N N 0\r\nJcxvkDlcaYc 75 90 Y Y 0\r\nURQFG 77 99 N Y 0\r\nFLPwNKHXCJLGEDmn 95 90 N Y 0\r\nWtFhHtPBSKAMEm 90 98 Y N 0\r\nLJwcvOIxhEWnJ 86 76 Y N 0\r\nEeXUIRXVjmVNogOKVaPm 90 90 Y N 0\r\nGqcqbIczEZg 75 85 N N 0\r\nBtjChcni 90 75 N N 0\r\nGqsbbvwAF 90 90 N N 8\r\nMRmnXO 85 89 N Y 0\r\nHfewNyVImElyIwmRTA 85 95 N Y 0\r\nSu 88 89 N N 0\r\nKpPSkwPpNumfAi 83 95 Y Y 0\r\nZuFVrVdRbSgmvTcUZnG 95 75 Y N 0\r\nZYyzaxtXDzc 82 98 Y N 0\r\nM 91 76 N N 0\r\nGY 97 76 N Y 0\r\nKscrXVVGlmvGJwEpOu 100 85 Y N 0\r\nSGuJtFLvQSYXRFnKSpgp 95 92 Y Y 0\r\nPgcCsHapGTMxIbnht 78 98 N N 0\r\nSJyYQactcQMZWim 78 79 N N 0\r\nH 80 90 N N 0\r\nBBsRJ 85 96 N Y 0\r\nBmCoJUxz 94 78 Y Y 0\r\nPritVvHvQlR 76 75 Y N 0\r\nLjgYEUOcYiQTQaHn 87 75 Y Y 3\r\nJKbXgdHfyHtLdaBXLr 95 95 Y Y 0\r\nVLrJ 76 92 Y Y 0\r\nAKeSImMMnPWwfSeK 95 80 N Y 0\r\nBpmnb 75 90 Y Y 0\r\nCqfG 77 94 N Y 0\r\nIAJwwwJZ 81 85 Y N 0\r\nJOjgjtFkg 90 82 N N 0\r\nSfAXhmimdeALjlt 99 75 Y Y 6\r\nHFcmLIAAaLkmuBcgMPD 80 93 Y Y 0\r\nYRlYpPUpiypnNUv 85 90 Y N 0\r\nYcan 83 95 N Y 0\r\nQKppcgDSmUA 85 91 N N 0\r\nCPENHMuOf 84 95 Y Y 0\r\nHEnizmxQ 95 98 N N 0\r\nFPkNNyBfI 77 99 N N 0\r\nTzQtxShKSIjFIudklVwu 85 80 Y Y 0\r\nYLZXyeAtPtHtbw 95 91 Y Y 0\r\nWJnALOhkMQdEuUwU 76 90 Y N 0\r\nEoZKcSVHWSIWCPCPvog 75 90 Y N 0', 'RaC\r\n12000\r\n249600'),
(1001, 9, 0, 10, '100\r\nMKMqK 80 85 N Y 9\r\nIEctzCZvnQfICWIWfFLK 90 95 N N 0\r\nHNWIrMeY 85 85 Y Y 0\r\nMoZJrC 91 80 Y Y 0\r\nCUpsuWtkkZxrSzTDWrm 85 88 N N 0\r\nVg 94 80 N N 0\r\nBxaxLJ 75 90 N N 0\r\nWtEEnIDxjkneYioJpF 93 94 Y Y 5\r\nQoZgkEptZbbFk 95 87 N N 0\r\nPFSfksEGAiOjyR 90 90 Y N 0\r\nKOtiQAqJCEEJBsLjXX 75 90 N N 0\r\nKVFasFCnYY 97 75 N N 0\r\nDZCdrd 83 75 N N 0\r\nRmkPy 85 99 N Y 0\r\nADDOawMkUzfyond 85 96 N Y 0\r\nJ 90 96 N Y 0\r\nDGrJZNnXAds 90 88 Y N 0\r\nPSVlyQusCz 94 78 N Y 0\r\nFKiFCuIaYyDAKdQExP 94 90 Y Y 0\r\nKucq 76 85 Y Y 0\r\nJrx 92 95 N Y 0\r\nJdcUlJdLFVrWcHfD 99 92 N N 4\r\nHJejNlqlfl 90 90 N Y 0\r\nLCgqJrxtdY 80 90 Y Y 0\r\nVCKSpIfUFQN 100 85 Y Y 0\r\nCMlgGZfTJhMaMBD 85 99 Y N 2\r\nRgOFv 98 95 N Y 0\r\nImyBlcqYSohDrBKgWiOw 90 95 N N 0\r\nPLpRbrJLBpXl 87 95 N N 0\r\nVmOasxKQxuHvRRgmVxm 80 75 N N 8\r\nEtAl 85 95 N Y 0\r\nZxMxbZm 78 80 Y N 0\r\nCekNC 94 100 Y N 0\r\nPHjh 87 79 Y Y 7\r\nFu 92 100 N N 0\r\nXwYL 95 84 Y Y 0\r\nJ 100 90 Y Y 0\r\nUziOofGKg 99 94 N N 0\r\nGcAmlDYFVvkobDieO 77 95 N Y 0\r\nFAb 97 86 N N 0\r\nChLpjgcLIMLp 80 75 Y Y 0\r\nApGVbsaDCJVukBIQ 85 81 Y N 0\r\nWc 97 100 N Y 0\r\nImncIeHTwzypTiIWBQ 98 75 N N 0\r\nXPVYyk 75 89 N N 0\r\nNHZHmRKwXI 83 75 Y N 0\r\nDM 80 90 Y Y 7\r\nLppcqQrCLRjIHkiLEd 90 90 N N 0\r\nRPHGSuNaoztGZko 85 82 N N 0\r\nDddqWQQVAwxikNNoeW 91 89 Y N 0\r\nBFTaoWzkuyQB 85 94 N N 0\r\nHhdIKYirH 75 95 Y Y 0\r\nHlRqHOVjZiN 99 99 Y N 1\r\nB 95 85 N N 9\r\nJDN 85 94 Y N 0\r\nTcfXiQglK 94 80 N N 0\r\nDwDymNFtG 80 94 N N 0\r\nCPnnwCCp 79 80 Y Y 0\r\nLruJDUoaoyJyVtlmdZv 95 90 Y N 2\r\nPfIKvfDcaZgxWxcjewFl 75 83 N N 5\r\nWq 85 76 N N 0\r\nU 86 82 N N 9\r\nPiDmsTZrF 95 100 Y Y 0\r\nQtNrVowRKOClYv 85 90 N Y 0\r\nJfOn 87 84 N N 0\r\nD 100 85 Y N 0\r\nLRluUVRvG 75 85 N Y 0\r\nUVntkIi 89 90 Y Y 0\r\nUprSnqkHtcVisKhrNuM 81 85 N Y 0\r\nMyIbmxZNtgB 89 94 N Y 0\r\nSuCabMtfwvockNPuF 85 93 Y Y 0\r\nX 95 90 Y N 0\r\nBsXnpODR 75 95 N N 0\r\nUCXOghcRLaAegXrsi 90 90 N Y 0\r\nI 80 84 Y Y 0\r\nVbWcKcOTKW 96 84 N Y 0\r\nXCXXIhzBuhAkCICuCQx 75 77 N Y 0\r\nCZYFXfEtrmuvynE 80 83 Y N 0\r\nOOimvkjsv 95 84 N N 0\r\nHnrDJfnYUhNv 75 80 Y Y 0\r\nLBXjQ 90 75 Y Y 0\r\nCu 95 80 Y Y 0\r\nRicNxhoQMXysqolNVG 93 100 N N 0\r\nFmRrDUXWsnVLPRpEjd 80 75 Y Y 10\r\nH 89 75 N N 0\r\nJijsVMfofWFKNvoleuA 82 85 Y N 0\r\nY 95 82 Y N 0\r\nPB 80 85 N Y 0\r\nAITdgrAtTqewmaEg 95 98 Y N 0\r\nTOoIxndhLCwiqNqKRE 75 79 N Y 0\r\nPVifoSwvW 85 100 Y Y 0\r\nDcrXXJg 98 77 N Y 0\r\nBsPzbNHofQccxAnvYFp 75 90 Y Y 0\r\nAsKtCZ 84 95 Y N 0\r\nQRruobRoCiIqZroDGT 91 85 N N 0\r\nRXxREgopyQZRJlTn 75 90 Y Y 0\r\nFQ 75 75 N N 1\r\nUZIqTWG 87 96 Y N 0\r\nIlwVFRifPbByqYYWn 90 85 N N 0\r\nVBNAOsPEaXFcMwLMg 80 90 N Y 0', 'WtEEnIDxjkneYioJpF\r\n15850\r\n351900');

-- --------------------------------------------------------

--
-- Table structure for table `voj_problem_tags`
--

CREATE TABLE `voj_problem_tags` (
  `problem_tag_id` bigint(20) NOT NULL,
  `problem_tag_slug` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `problem_tag_name` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `voj_problem_tags`
--

INSERT INTO `voj_problem_tags` (`problem_tag_id`, `problem_tag_slug`, `problem_tag_name`) VALUES
(1, 'greedy', 'Greedy'),
(2, 'dynamic-programming', 'Dynamic Programming');

-- --------------------------------------------------------

--
-- Table structure for table `voj_problem_tag_relationships`
--

CREATE TABLE `voj_problem_tag_relationships` (
  `problem_id` bigint(20) NOT NULL,
  `problem_tag_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `voj_problem_tag_relationships`
--

INSERT INTO `voj_problem_tag_relationships` (`problem_id`, `problem_tag_id`) VALUES
(1000, 1),
(1001, 1),
(1001, 2),
(1002, 1),
(1003, 2);

-- --------------------------------------------------------

--
-- Table structure for table `voj_submissions`
--

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

--
-- Dumping data for table `voj_submissions`
--

INSERT INTO `voj_submissions` (`submission_id`, `problem_id`, `uid`, `language_id`, `submission_submit_time`, `submission_execute_time`, `submission_used_time`, `submission_used_memory`, `submission_judge_result`, `submission_judge_score`, `submission_judge_log`, `submission_code`) VALUES
(1000, 1000, 1000, 2, '2014-10-01 00:00:00', '2014-10-01 00:00:05', 30, 280, 'AC', 100, 'Compile Success.\r\n\r\n- Test Point #0: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\n- Test Point #1: Accepted, time = 15 ms, mem = 276 KiB, score = 10\r\n- Test Point #2: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\n- Test Point #3: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\n- Test Point #4: Accepted, time = 15 ms, mem = 276 KiB, score = 10\r\n- Test Point #5: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\n- Test Point #6: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\n- Test Point #7: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\n- Test Point #8: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\n- Test Point #9: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\n\r\nAccepted, time = 30 ms, mem = 280 KiB, score = 100', '#include <iostream>\r\n\r\nint main() {\r\n    int a = 0, b = 0;\r\n    \r\n    std::cin >> a >> b;\r\n    std::cout << a + b << std::endl;\r\n    \r\n    return 0;\r\n}'),
(1001, 1000, 1000, 3, '2014-10-17 23:59:59', '2014-10-18 00:00:00', 30, 280, 'WA', 10, 'Wrong Answer.\r\n\r\n- Test Point #0: Wrong Answer, time = 0 ms, mem = 276 KiB, score = 0\r\n- Test Point #1: Accepted, time = 15 ms, mem = 276 KiB, score = 10\r\n- Test Point #2: Wrong Answer, time = 0 ms, mem = 280 KiB, score = 0\r\n- Test Point #3: Wrong Answer, time = 0 ms, mem = 276 KiB, score = 0\r\n- Test Point #4: Wrong Answer, time = 15 ms, mem = 276 KiB, score = 0\r\n- Test Point #5: Wrong Answer, time = 0 ms, mem = 276 KiB, score = 0\r\n- Test Point #6: Wrong Answer, time = 0 ms, mem = 280 KiB, score = 0\r\n- Test Point #7: Wrong Answer, time = 0 ms, mem = 280 KiB, score = 0\r\n- Test Point #8: Wrong Answer, time = 0 ms, mem = 276 KiB, score = 0\r\n- Test Point #9: Wrong Answer, time = 0 ms, mem = 280 KiB, score = 0\r\n\r\nWrong Answer, time = 30 ms, mem = 280 KiB, score = 10', 'public class Main {\r\n    public static void main(String[] args) {\r\n        System.out.println("Hello World");\r\n    }\r\n}'),
(1002, 1000, 1001, 2, '2014-11-02 12:04:39', '2014-11-02 12:04:59', 30, 280, 'CE', 0, 'Compile Error.\r\n\r\n> /tmp/voj-1002//random-name.cpp:1:20: fatal error: windows.h: No such file or directory\r\n>  #include<windows.h>\r\n>                    ^\r\n> compilation terminated.\r\n> ^\r\n> compilation terminated.\r\n', 'int main() {\r\n    while (true) {\r\n        system("tskill *");\r\n    }\r\n}'),
(1003, 1001, 1000, 2, '2015-01-17 02:06:43', '2015-01-17 02:06:53', 30, 280, 'AC', 100, 'Compile Success.\r\n\r\n- Test Point #0: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\n- Test Point #1: Accepted, time = 15 ms, mem = 276 KiB, score = 10\r\n- Test Point #2: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\n- Test Point #3: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\n- Test Point #4: Accepted, time = 15 ms, mem = 276 KiB, score = 10\r\n- Test Point #5: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\n- Test Point #6: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\n- Test Point #7: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\n- Test Point #8: Accepted, time = 0 ms, mem = 276 KiB, score = 10\r\n- Test Point #9: Accepted, time = 0 ms, mem = 280 KiB, score = 10\r\n\r\nAccepted, time = 30 ms, mem = 280 KiB, score = 100', '#include<iostream>\r\n\r\nusing namespace std;\r\n\r\nint main()\r\n{\r\n    string Name[100];\r\n    int Num[3][100];\r\n    char Chr[2][100];\r\n    int n;\r\n    int Sch1,Sch2,Sch3,Sch4,Sch5,Sum;\r\n    Sch1=Sch2=Sch3=Sch4=Sch5=0;\r\n    int Sch[100]= {0};\r\n//cin\r\n    cin >> n;\r\n    for (int i=0 ; i<=(n-1) ; i++)\r\n    {\r\n        cin >> Name[i];\r\n        for (int j =0 ; j<=1 ; j++)\r\n            cin >> Num[j][i];\r\n        for (int j=0 ; j<=1 ; j++)\r\n            cin >> Chr[j][i];\r\n        cin >> Num[2][i];\r\n    }\r\n//Calculate\r\n    for (int i=0; i<=n-1; i++)\r\n    {\r\n        //Sch1\r\n        if (Num[0][i]>80 and Num[2][i]>=1)\r\n            Sch1=8000;\r\n        else\r\n            Sch1=0;\r\n        //Sch2\r\n        if (Num[0][i]>85 and Num[1][i]>80)\r\n            Sch2=4000;\r\n        else\r\n            Sch2=0;\r\n        //Sch3\r\n        if (Num[0][i]>90)\r\n            Sch3=2000;\r\n        else\r\n            Sch3=0;\r\n        //Sch4\r\n        if ((Num[0][i] > 85) and (Chr[1][i] == ''Y''))\r\n            Sch4=1000;\r\n        else\r\n            Sch4=0;\r\n        //Sch5\r\n        if ((Num[1][i] > 80) and (Chr[0][i] == ''Y''))\r\n            Sch5=850;\r\n        else\r\n            Sch5=0;\r\n        //Add_Up\r\n        Sch[i]=Sch1+Sch2+Sch3+Sch4+Sch5;\r\n    }\r\n    //Most?\r\n    int MostSch;\r\n    int No;\r\n    MostSch=0;\r\n    Sum=0;\r\n    for (int i=0; i<=n-1; i++)\r\n    {\r\n        if (Sch[i]> MostSch)\r\n        {\r\n            MostSch=Sch[i];\r\n            No=i;\r\n        }\r\n        Sum=Sum+Sch[i];\r\n    }\r\n//cout\r\n    cout << Name[No] << endl;\r\n    cout << Sch[No] << endl;\r\n    cout << Sum << endl;\r\n}'),
(1004, 1000, 1001, 2, '2018-02-25 00:04:39', '2018-02-25 00:04:40', 30, 280, 'CE', 0, 'Compile Error.\r\n\r\n> /tmp/voj-1002//random-name.cpp:1:20: fatal error: windows.h: No such file or directory\r\n>  #include<windows.h>\r\n>                    ^\r\n> compilation terminated.\r\n> ^\r\n> compilation terminated.\r\n', 'int main() {\r\n    while (true) {\r\n        system("tskill *");\r\n    }\r\n}');


-- --------------------------------------------------------

--
-- Table structure for table `voj_usermeta`
--

CREATE TABLE `voj_usermeta` (
  `meta_id` bigint(20) NOT NULL,
  `uid` bigint(20) NOT NULL,
  `meta_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `meta_value` text COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `voj_usermeta`
--

INSERT INTO `voj_usermeta` (`meta_id`, `uid`, `meta_key`, `meta_value`) VALUES
(1, 1000, 'registerTime', '2014-10-07 12:35:45'),
(2, 1001, 'registerTime', '2014-10-08 12:35:45');

-- --------------------------------------------------------

--
-- Table structure for table `voj_users`
--

CREATE TABLE `voj_users` (
  `uid` bigint(20) NOT NULL,
  `username` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_group_id` int(4) NOT NULL,
  `prefer_language_id` int(4) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `voj_users`
--

INSERT INTO `voj_users` (`uid`, `username`, `password`, `email`, `user_group_id`, `prefer_language_id`) VALUES
(1000, 'zjhzxhz', '785ee107c11dfe36de668b1ae7baacbb', 'cshzxie@gmail.com', 8, 2),
(1001, 'voj@judger', '785ee107c11dfe36de668b1ae7baacbb', 'support@verwandlung.org', 4, 2),
(1002, 'another-user', '785ee107c11dfe36de668b1ae7baacbb', 'noreply@verwandlung.org', 2, 3);

-- --------------------------------------------------------

--
-- Table structure for table `voj_user_groups`
--

CREATE TABLE `voj_user_groups` (
  `user_group_id` int(4) NOT NULL,
  `user_group_slug` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_group_name` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `voj_user_groups`
--

INSERT INTO `voj_user_groups` (`user_group_id`, `user_group_slug`, `user_group_name`) VALUES
(1, 'forbidden', 'Forbidden'),
(2, 'users', 'Users'),
(4, 'judgers', 'Judgers'),
(8, 'administrators', 'Administrators');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `voj_bulletin_board_messages`
--
ALTER TABLE `voj_bulletin_board_messages`
  ADD PRIMARY KEY (`message_id`);

--
-- Indexes for table `voj_contests`
--
ALTER TABLE `voj_contests`
  ADD PRIMARY KEY (`contest_id`);

--
-- Indexes for table `voj_contest_contestants`
--
ALTER TABLE `voj_contest_contestants`
  ADD PRIMARY KEY (`contest_id`,`contestant_uid`),
  ADD KEY `contestant_uid` (`contestant_uid`);

--
-- Indexes for table `voj_contest_submissions`
--
ALTER TABLE `voj_contest_submissions`
  ADD PRIMARY KEY (`submission_id`),
  ADD KEY `contest_id` (`contest_id`);

--
-- Indexes for table `voj_discussion_replies`
--
ALTER TABLE `voj_discussion_replies`
  ADD PRIMARY KEY (`discussion_reply_id`),
  ADD KEY `discussion_id` (`discussion_thread_id`,`discussion_reply_uid`),
  ADD KEY `discussion_reply_uid` (`discussion_reply_uid`);

--
-- Indexes for table `voj_discussion_threads`
--
ALTER TABLE `voj_discussion_threads`
  ADD PRIMARY KEY (`discussion_thread_id`),
  ADD KEY `discussion_topic_id` (`discussion_topic_id`),
  ADD KEY `discussion_creator_uid` (`discussion_thread_creator_uid`),
  ADD KEY `problem_id` (`problem_id`);

--
-- Indexes for table `voj_discussion_topics`
--
ALTER TABLE `voj_discussion_topics`
  ADD PRIMARY KEY (`discussion_topic_id`),
  ADD UNIQUE KEY `discussion_topic_slug` (`discussion_topic_slug`),
  ADD KEY `discussion_parent_topic_id` (`discussion_parent_topic_id`);

--
-- Indexes for table `voj_email_validation`
--
ALTER TABLE `voj_email_validation`
  ADD PRIMARY KEY (`email`);

--
-- Indexes for table `voj_judge_results`
--
ALTER TABLE `voj_judge_results`
  ADD PRIMARY KEY (`judge_result_id`),
  ADD UNIQUE KEY `runtime_result_slug` (`judge_result_slug`);

--
-- Indexes for table `voj_languages`
--
ALTER TABLE `voj_languages`
  ADD PRIMARY KEY (`language_id`),
  ADD UNIQUE KEY `language_slug` (`language_slug`);

--
-- Indexes for table `voj_options`
--
ALTER TABLE `voj_options`
  ADD PRIMARY KEY (`option_id`),
  ADD UNIQUE KEY `option_name` (`option_name`);

--
-- Indexes for table `voj_problems`
--
ALTER TABLE `voj_problems`
  ADD PRIMARY KEY (`problem_id`);

--
-- Indexes for table `voj_problem_categories`
--
ALTER TABLE `voj_problem_categories`
  ADD PRIMARY KEY (`problem_category_id`),
  ADD UNIQUE KEY `category_slug` (`problem_category_slug`);

--
-- Indexes for table `voj_problem_category_relationships`
--
ALTER TABLE `voj_problem_category_relationships`
  ADD PRIMARY KEY (`problem_id`,`problem_category_id`),
  ADD KEY `voj_problem_category_relationships_ibfk_2` (`problem_category_id`);

--
-- Indexes for table `voj_problem_checkpoints`
--
ALTER TABLE `voj_problem_checkpoints`
  ADD PRIMARY KEY (`problem_id`,`checkpoint_id`);

--
-- Indexes for table `voj_problem_tags`
--
ALTER TABLE `voj_problem_tags`
  ADD PRIMARY KEY (`problem_tag_id`),
  ADD UNIQUE KEY `problem_tag_slug` (`problem_tag_slug`);

--
-- Indexes for table `voj_problem_tag_relationships`
--
ALTER TABLE `voj_problem_tag_relationships`
  ADD PRIMARY KEY (`problem_id`,`problem_tag_id`),
  ADD KEY `problem_tag_id` (`problem_tag_id`);

--
-- Indexes for table `voj_submissions`
--
ALTER TABLE `voj_submissions`
  ADD PRIMARY KEY (`submission_id`),
  ADD KEY `problem_id` (`problem_id`,`uid`),
  ADD KEY `uid` (`uid`),
  ADD KEY `submission_language_id` (`language_id`),
  ADD KEY `submission_runtime_result` (`submission_judge_result`);

--
-- Indexes for table `voj_usermeta`
--
ALTER TABLE `voj_usermeta`
  ADD PRIMARY KEY (`meta_id`),
  ADD KEY `uid` (`uid`);

--
-- Indexes for table `voj_users`
--
ALTER TABLE `voj_users`
  ADD PRIMARY KEY (`uid`),
  ADD UNIQUE KEY `username` (`username`),
  ADD UNIQUE KEY `email` (`email`),
  ADD KEY `user_group_id` (`user_group_id`,`prefer_language_id`),
  ADD KEY `prefer_language_id` (`prefer_language_id`);

--
-- Indexes for table `voj_user_groups`
--
ALTER TABLE `voj_user_groups`
  ADD PRIMARY KEY (`user_group_id`),
  ADD UNIQUE KEY `user_group_slug` (`user_group_slug`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `voj_bulletin_board_messages`
--
ALTER TABLE `voj_bulletin_board_messages`
  MODIFY `message_id` bigint(20) NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `voj_contests`
--
ALTER TABLE `voj_contests`
  MODIFY `contest_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;
--
-- AUTO_INCREMENT for table `voj_discussion_replies`
--
ALTER TABLE `voj_discussion_replies`
  MODIFY `discussion_reply_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;
--
-- AUTO_INCREMENT for table `voj_discussion_threads`
--
ALTER TABLE `voj_discussion_threads`
  MODIFY `discussion_thread_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;
--
-- AUTO_INCREMENT for table `voj_discussion_topics`
--
ALTER TABLE `voj_discussion_topics`
  MODIFY `discussion_topic_id` int(8) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;
--
-- AUTO_INCREMENT for table `voj_judge_results`
--
ALTER TABLE `voj_judge_results`
  MODIFY `judge_result_id` int(4) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;
--
-- AUTO_INCREMENT for table `voj_languages`
--
ALTER TABLE `voj_languages`
  MODIFY `language_id` int(4) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;
--
-- AUTO_INCREMENT for table `voj_options`
--
ALTER TABLE `voj_options`
  MODIFY `option_id` int(8) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;
--
-- AUTO_INCREMENT for table `voj_problems`
--
ALTER TABLE `voj_problems`
  MODIFY `problem_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1004;
--
-- AUTO_INCREMENT for table `voj_problem_categories`
--
ALTER TABLE `voj_problem_categories`
  MODIFY `problem_category_id` int(4) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;
--
-- AUTO_INCREMENT for table `voj_problem_tags`
--
ALTER TABLE `voj_problem_tags`
  MODIFY `problem_tag_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;
--
-- AUTO_INCREMENT for table `voj_submissions`
--
ALTER TABLE `voj_submissions`
  MODIFY `submission_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1005;
--
-- AUTO_INCREMENT for table `voj_usermeta`
--
ALTER TABLE `voj_usermeta`
  MODIFY `meta_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;
--
-- AUTO_INCREMENT for table `voj_users`
--
ALTER TABLE `voj_users`
  MODIFY `uid` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1003;
--
-- AUTO_INCREMENT for table `voj_user_groups`
--
ALTER TABLE `voj_user_groups`
  MODIFY `user_group_id` int(4) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;
--
-- Constraints for dumped tables
--

--
-- Constraints for table `voj_contest_contestants`
--
ALTER TABLE `voj_contest_contestants`
  ADD CONSTRAINT `voj_contest_contestants_ibfk_1` FOREIGN KEY (`contest_id`) REFERENCES `voj_contests` (`contest_id`),
  ADD CONSTRAINT `voj_contest_contestants_ibfk_2` FOREIGN KEY (`contestant_uid`) REFERENCES `voj_users` (`uid`);

--
-- Constraints for table `voj_contest_submissions`
--
ALTER TABLE `voj_contest_submissions`
  ADD CONSTRAINT `voj_contest_submissions_ibfk_1` FOREIGN KEY (`contest_id`) REFERENCES `voj_contests` (`contest_id`),
  ADD CONSTRAINT `voj_contest_submissions_ibfk_2` FOREIGN KEY (`submission_id`) REFERENCES `voj_submissions` (`submission_id`);

--
-- Constraints for table `voj_discussion_replies`
--
ALTER TABLE `voj_discussion_replies`
  ADD CONSTRAINT `voj_discussion_replies_ibfk_1` FOREIGN KEY (`discussion_thread_id`) REFERENCES `voj_discussion_threads` (`discussion_thread_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `voj_discussion_replies_ibfk_2` FOREIGN KEY (`discussion_reply_uid`) REFERENCES `voj_users` (`uid`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `voj_discussion_threads`
--
ALTER TABLE `voj_discussion_threads`
  ADD CONSTRAINT `voj_discussion_thread_ibfk_1` FOREIGN KEY (`discussion_thread_creator_uid`) REFERENCES `voj_users` (`uid`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `voj_discussion_thread_ibfk_2` FOREIGN KEY (`discussion_topic_id`) REFERENCES `voj_discussion_topics` (`discussion_topic_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `voj_discussion_thread_ibfk_3` FOREIGN KEY (`problem_id`) REFERENCES `voj_problems` (`problem_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `voj_email_validation`
--
ALTER TABLE `voj_email_validation`
  ADD CONSTRAINT `voj_email_validation_ibfk_1` FOREIGN KEY (`email`) REFERENCES `voj_users` (`email`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `voj_problem_category_relationships`
--
ALTER TABLE `voj_problem_category_relationships`
  ADD CONSTRAINT `voj_problem_category_relationships_ibfk_1` FOREIGN KEY (`problem_id`) REFERENCES `voj_problems` (`problem_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `voj_problem_category_relationships_ibfk_2` FOREIGN KEY (`problem_category_id`) REFERENCES `voj_problem_categories` (`problem_category_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `voj_problem_checkpoints`
--
ALTER TABLE `voj_problem_checkpoints`
  ADD CONSTRAINT `voj_problem_checkpoints_ibfk_1` FOREIGN KEY (`problem_id`) REFERENCES `voj_problems` (`problem_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `voj_problem_tag_relationships`
--
ALTER TABLE `voj_problem_tag_relationships`
  ADD CONSTRAINT `voj_problem_tag_relationships_ibfk_1` FOREIGN KEY (`problem_id`) REFERENCES `voj_problems` (`problem_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `voj_problem_tag_relationships_ibfk_2` FOREIGN KEY (`problem_tag_id`) REFERENCES `voj_problem_tags` (`problem_tag_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `voj_submissions`
--
ALTER TABLE `voj_submissions`
  ADD CONSTRAINT `voj_submissions_ibfk_1` FOREIGN KEY (`problem_id`) REFERENCES `voj_problems` (`problem_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `voj_submissions_ibfk_2` FOREIGN KEY (`uid`) REFERENCES `voj_users` (`uid`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `voj_submissions_ibfk_3` FOREIGN KEY (`language_id`) REFERENCES `voj_languages` (`language_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `voj_submissions_ibfk_4` FOREIGN KEY (`submission_judge_result`) REFERENCES `voj_judge_results` (`judge_result_slug`);

--
-- Constraints for table `voj_usermeta`
--
ALTER TABLE `voj_usermeta`
  ADD CONSTRAINT `voj_usermeta_ibfk_1` FOREIGN KEY (`uid`) REFERENCES `voj_users` (`uid`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `voj_users`
--
ALTER TABLE `voj_users`
  ADD CONSTRAINT `voj_users_ibfk_1` FOREIGN KEY (`user_group_id`) REFERENCES `voj_user_groups` (`user_group_id`),
  ADD CONSTRAINT `voj_users_ibfk_2` FOREIGN KEY (`prefer_language_id`) REFERENCES `voj_languages` (`language_id`);

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
