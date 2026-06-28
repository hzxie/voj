-- Verwandlung Online Judge - optional sample content for the install wizard.
-- Loaded after schema.sql + seed.sql. Safe to edit/replace; the test suite
-- uses its own copy in test-data.sql, so changing this never breaks tests.
-- Idempotent: clears its tables first, and inserts are FK-dependency ordered.
SET NAMES utf8mb4;
SET SQL_MODE = "STRICT_TRANS_TABLES";
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM `voj_bulletin_board_messages`;
DELETE FROM `voj_discussion_reply_votes`;
DELETE FROM `voj_discussion_replies`;
DELETE FROM `voj_discussion_threads`;
DELETE FROM `voj_contest_submissions`;
DELETE FROM `voj_contest_contestants`;
DELETE FROM `voj_problem_checkpoints`;
DELETE FROM `voj_problem_tag_relationships`;
DELETE FROM `voj_problem_category_relationships`;
DELETE FROM `voj_submissions`;
DELETE FROM `voj_email_validation`;
DELETE FROM `voj_usermeta`;
DELETE FROM `voj_contests`;
DELETE FROM `voj_problems`;
DELETE FROM `voj_users`;

-- Every demo account's password equals its username (e.g. log in as root / root).
-- Stored as legacy un-prefixed MD5, which the DelegatingPasswordEncoder still verifies.
INSERT INTO `voj_users` (`uid`, `username`, `password`, `email`, `user_group_id`, `prefer_language_id`) VALUES
(1000, 'root', '63a9f0ea7bb98050796b649e85481845', 'admin@voj.local', 8, 2),
(1001, 'judge-alpha', '389bfff1a0af2473dde4c881223b44b6', 'judge-alpha@voj.local', 4, 2),
(1002, 'judge-beta', '9d905b0a407a2aac80f9f58dfaacf26c', 'judge-beta@voj.local', 4, 2),
(1003, 'alice', '6384e2b2184bcbf58eccf10ca7a6563c', 'alice@voj.local', 2, 2),
(1004, 'bob', '9f9d51bc70ef21ca5c14f307980a29d8', 'bob@voj.local', 2, 1),
(1005, 'carol', 'a9a0198010a6073db96434f6cc5f22a8', 'carol@voj.local', 2, 3),
(1006, 'dave', '1610838743cc90e3e4fdda748282d9b8', 'dave@voj.local', 2, 5),
(1007, 'erin', '5f5be3890fa875bfe8fa797b4ba6a397', 'erin@voj.local', 2, 2),
(1008, 'frank', '26253c50741faa9c2e2b836773c69fe6', 'frank@voj.local', 2, 3),
(1009, 'grace', '15e5c87b18c1289d45bb4a72961b58e8', 'grace@voj.local', 2, 2),
(1010, 'heidi', '3e41d67d5461196c3e784fb6549f7763', 'heidi@voj.local', 2, 6),
(1011, 'ivan', '2c42e5cf1cdbafea04ed267018ef1511', 'ivan@voj.local', 2, 1),
(1012, 'judy', 'c6a1ca47b645f4c4b786ce951f8d26a7', 'judy@voj.local', 2, 4),
(1013, 'mallory', '2fbeefa3a12d1bf0ce004e4af7e0cf6f', 'mallory@voj.local', 2, 2),
(1014, 'trent', '1843e51e39e7195d4ef79a58fdeea872', 'trent@voj.local', 2, 3);

INSERT INTO `voj_problems` (`problem_id`, `problem_status`, `problem_name`, `problem_time_limit`, `problem_memory_limit`, `problem_description`, `problem_input_format`, `problem_output_format`, `problem_sample_input`, `problem_sample_output`, `problem_hint`) VALUES
(1000, 'PUBLISHED', 'A+B Problem', 1000, 65536, '输入两个自然数, 输出他们的和', '两个自然数x和y (0<=x, y<=32767).', '一个数, 即x和y的和.', '123 500', '623', '## C++ Code\r\n\r\n    #include <iostream>\r\n\r\n    int main() {\r\n        int a = 0, b = 0;\r\n        std::cin >> a >> b;\r\n        std::cout << a + b << std::endl;\r\n        return 0;\r\n    }\r\n\r\n## Free Pascal Code\r\n\r\n    program Plus;\r\n    var a, b:longint;\r\n    begin\r\n        readln(a, b);\r\n        writeln(a + b);\r\n    end.\r\n\r\n## Java Code\r\n\r\n    import java.util.Scanner;\r\n\r\n    public class Main {\r\n        public static void main(String[] args) {\r\n            Scanner in = new Scanner(System.in);\r\n            int a = in.nextInt();\r\n            int b = in.nextInt();\r\n            System.out.println(a + b);\r\n        }\r\n    }\r\n'),
(1001, 'PUBLISHED', '谁拿了最多奖学金', 1000, 65536, '某校的惯例是在每学期的期末考试之后发放奖学金。发放的奖学金共有五种，获取的条件各自不同：\r\n1) 院士奖学金，每人8000元，期末平均成绩高于80分（>80），并且在本学期内发表1篇或1篇以上论文的学生均可获得；\r\n2) 五四奖学金，每人4000元，期末平均成绩高于85分（>85），并且班级评议成绩高于80分（>80）的学生均可获得；\r\n3) 成绩优秀奖，每人2000元，期末平均成绩高于90分（>90）的学生均可获得；\r\n4) 西部奖学金，每人1000元，期末平均成绩高于85分（>85）的西部省份学生均可获得；\r\n5) 班级贡献奖，每人850元，班级评议成绩高于80分（>80）的学生干部均可获得；\r\n只要符合条件就可以得奖，每项奖学金的获奖人数没有限制，每名学生也可以同时获得多项奖学金。例如姚林的期末平均成绩是87分，班级评议成绩82分，同时他还是一位学生干部，那么他可以同时获得五四奖学金和班级贡献奖，奖金总数是4850元。\r\n现在给出若干学生的相关数据，请计算哪些同学获得的奖金总数最高（假设总有同学能满足获得奖学金的条件）。', '输入的第一行是一个整数N（1 <= N <= 100），表示学生的总数。接下来的N行每行是一位学生的数据，从左向右依次是姓名，期末平均成绩，班级评议成绩，是否是学生干部，是否是西部省份学生，以及发表的论文数。姓名是由大小写英文字母组成的长度不超过20的字符串（不含空格）；期末平均成绩和班级评议成绩都是0到100之间的整数（包括0和100）；是否是学生干部和是否是西部省份学生分别用一个字符表示，Y表示是，N表示不是；发表的论文数是0到10的整数（包括0和10）。每两个相邻数据项之间用一个空格分隔。', '输出包括三行，第一行是获得最多奖金的学生的姓名，第二行是这名学生获得的奖金总数。如果有两位或两位以上的学生获得的奖金最多，输出他们之中在输入文件中出现最早的学生的姓名。第三行是这N个学生获得的奖学金的总数。', '4\r\nYaoLin 87 82 Y N 0\r\nChenRuiyi 88 78 N Y 1\r\nLiXin 92 88 N N 0\r\nZhangQin 83 87 Y N 1', 'ChenRuiyi\r\n9000\r\n28700', NULL),
(1002, 'HIDDEN', '过河', 1000, 65536, '在河上有一座独木桥, 一只青蛙想沿着独木桥从河的一侧跳到另一侧. 在桥上有一些石子, 青蛙很讨厌踩在这些石子上. 由于桥的长度和青蛙一次跳过的距离都是正整数, 我们可以把独木桥上青蛙可能到达的点看成数轴上的一串整点：0, 1, ……, L(其中L是桥的长度). 坐标为0的点表示桥的起点, 坐标为L的点表示桥的终点. 青蛙从桥的起点开始, 不停的向终点方向跳跃. 一次跳跃的距离是S到T之间的任意正整数(包括S,T). 当青蛙跳到或跳过坐标为L的点时, 就算青蛙已经跳出了独木桥. \r\n题目给出独木桥的长度L, 青蛙跳跃的距离范围S,T, 桥上石子的位置. 你的任务是确定青蛙要想过河, 最少需要踩到的石子数. \r\n对于30%的数据, L <= 10000；\r\n对于全部的数据, L <= 10^9. ', '输入的第一行有一个正整数L(1 <= L <= 10^9), 表示独木桥的长度. 第二行有三个正整数S, T, M, 分别表示青蛙一次跳跃的最小距离, 最大距离, 及桥上石子的个数, 其中1 <= S <= T <= 10, 1 <= M <= 100. 第三行有M个不同的正整数分别表示这M个石子在数轴上的位置(数据保证桥的起点和终点处没有石子). 所有相邻的整数之间用一个空格隔开. ', '输出只包括一个整数, 表示青蛙过河最少需要踩到的石子数.', '10\r\n2 3 5\r\n2 3 5 6 7', '2', NULL),
(1003, 'PUBLISHED', '等价表达式', 1000, 65536, '明明进了中学之后, 学到了代数表达式. 有一天, 他碰到一个很麻烦的选择题. 这个题目的题干中首先给出了一个代数表达式, 然后列出了若干选项, 每个选项也是一个代数表达式, 题目的要求是判断选项中哪些代数表达式是和题干中的表达式等价的. \n\n这个题目手算很麻烦, 因为明明对计算机编程很感兴趣, 所以他想是不是可以用计算机来解决这个问题. 假设你是明明, 能完成这个任务吗? \n\n这个选择题中的每个表达式都满足下面的性质: \n\n1. 表达式只可能包含一个变量''a''. \n2. 表达式中出现的数都是正整数, 而且都小于10000. \n3. 表达式中可以包括四种运算''+''(加), ''-''(减), ''\\*''(乘), ''^''(乘幂), 以及小括号''('', '')''. 小括号的优先级最高, 其次是''^'', 然后是''*'', 最后是''+''和''-''. ''+''和''-''的优先级是相同的. 相同优先级的运算从左到右进行. (注意: 运算符''+'', ''-'', ''\\*'', ''^''以及小括号''('', '')''都是英文字符)\n4. 幂指数只可能是1到10之间的正整数(包括1和10). \n5. 表达式内部, 头部或者尾部都可能有一些多余的空格. \n\n下面是一些合理的表达式的例子: \n\n    ((a^1) ^ 2)^3, a*a+a-a, ((a+a)), 9999+(a-a)*a, 1 + (a -1)^3, 1^10^9……\n\n\n- 对于30%的数据, 表达式中只可能出现两种运算符''+''和''-''；\n- 对于其它的数据, 四种运算符''+'', ''-'', ''*'', ''^''在表达式中都可能出现. \n- 对于全部的数据, 表达式中都可能出现小括号''(''和'')''. ', '输入的第一行给出的是题干中的表达式. 第二行是一个整数n(2 <= n <= 26), 表示选项的个数. 后面n行, 每行包括一个选项中的表达式. 这n个选项的标号分别是A, B, C, D……\n输入中的表达式的长度都不超过50个字符, 而且保证选项中总有表达式和题干中的表达式是等价的. ', '输出包括一行, 这一行包括一系列选项的标号, 表示哪些选项是和题干中的表达式等价的. 选项的标号按照字母顺序排列, 而且之间没有空格.', '( a + 1) ^2\n3\n(a-1)^2+4*a\na  + 1+ a\na^2 + 2 * a * 1 + 1^2 + 10 -10 +a -a', 'AC', '');

UPDATE `voj_problems` SET `problem_difficulty_id` = 1 WHERE `problem_id` IN (1000, 1001);

UPDATE `voj_problems` SET `problem_difficulty_id` = 2 WHERE `problem_id` IN (1003);

UPDATE `voj_problems` SET `problem_difficulty_id` = 3 WHERE `problem_id` IN (1002);

-- A spread of finished / running / upcoming contests in both OI and ACM modes.
-- "Running" rows straddle the seed date of 2026-06-27 so the home page shows live contests.
INSERT INTO `voj_contests` (`contest_id`, `contest_name`, `contest_notes`, `contest_start_time`, `contest_end_time`, `contest_mode`, `contest_problems`, `contest_status`, `contest_scoring`, `contest_penalty`, `contest_freeze`, `contest_is_rated`, `contest_open_registration`) VALUES
(1, 'Welcome Round 2025', 'A friendly ACM-style opener for newcomers: two beginner problems, five hours.', '2025-03-01 09:00:00', '2025-03-01 14:00:00', 'ACM', '[1000, 1001]', 'PUBLISHED', 'ICPC', 20, 0, 0, 1),
(2, 'Spring Algorithm Cup', 'An OI-scored round mixing implementation with a dynamic-programming finale.', '2025-04-15 13:00:00', '2025-04-15 18:00:00', 'OI', '[1000, 1001, 1003]', 'PUBLISHED', 'IOI', 0, 0, 1, 1),
(3, 'Summer Sprint 2025', 'A short ACM sprint where speed matters and partial scores do not.', '2025-07-12 09:00:00', '2025-07-12 14:00:00', 'ACM', '[1000, 1001]', 'PUBLISHED', 'ICPC', 20, 0, 1, 1),
(4, 'DP Masters Cup', 'For seasoned solvers: the river-crossing and equivalent-expression problems.', '2025-10-18 09:00:00', '2025-10-18 13:00:00', 'OI', '[1002, 1003]', 'PUBLISHED', 'IOI', 0, 0, 1, 0),
(5, 'Summer Showdown 2026', 'Our flagship ACM round of the season. Good luck to all contestants!', '2026-06-25 00:00:00', '2026-06-29 23:59:59', 'ACM', '[1000, 1001, 1003]', 'PUBLISHED', 'ICPC', 20, 15, 1, 1),
(6, 'Weekly Contest 42', 'The regular Saturday OI round: two problems, one day.', '2026-06-27 00:00:00', '2026-06-27 23:59:59', 'OI', '[1000, 1003]', 'PUBLISHED', 'IOI', 0, 0, 1, 1),
(7, 'Autumn Championship 2026', 'Season finale featuring all four problems. Registration is open.', '2026-09-01 09:00:00', '2026-09-01 14:00:00', 'ACM', '[1000, 1001, 1002, 1003]', 'HIDDEN', 'Codeforces', 10, 30, 1, 1),
(8, 'New Year Warmup 2027', 'Kick off the year with a relaxed OI round.', '2027-01-01 10:00:00', '2027-01-01 13:00:00', 'OI', '[1000, 1003]', 'HIDDEN', 'IOI', 0, 0, 0, 1);

-- registerTime drives the profile "member since" line and the account age.
-- A few users also carry optional profile fields (location / website / aboutMe /
-- socialLinks) so the profile pages render with real content.
INSERT INTO `voj_usermeta` (`meta_id`, `uid`, `meta_key`, `meta_value`) VALUES
(1, 1000, 'registerTime', '2014-10-07 12:35:45'),
(2, 1001, 'registerTime', '2015-01-01 08:00:00'),
(3, 1002, 'registerTime', '2015-01-02 08:00:00'),
(4, 1003, 'registerTime', '2024-09-01 10:12:00'),
(5, 1004, 'registerTime', '2024-09-15 14:03:00'),
(6, 1005, 'registerTime', '2024-10-02 09:25:00'),
(7, 1006, 'registerTime', '2024-11-20 20:40:00'),
(8, 1007, 'registerTime', '2025-01-05 08:15:00'),
(9, 1008, 'registerTime', '2025-02-18 16:30:00'),
(10, 1009, 'registerTime', '2025-03-30 11:05:00'),
(11, 1010, 'registerTime', '2025-05-11 13:20:00'),
(12, 1011, 'registerTime', '2025-07-22 19:45:00'),
(13, 1012, 'registerTime', '2025-09-08 10:00:00'),
(14, 1013, 'registerTime', '2025-11-15 21:30:00'),
(15, 1014, 'registerTime', '2026-01-20 09:10:00'),
(16, 1000, 'location', 'Shanghai, China'),
(17, 1000, 'website', 'https://voj.local'),
(18, 1000, 'aboutMe', 'Administrator of this Verwandlung Online Judge instance. Reach out via the support forum.'),
(19, 1000, 'socialLinks', '{"GitHub":"hzxie"}'),
(20, 1003, 'location', 'Hangzhou, China'),
(21, 1003, 'aboutMe', 'Competitive programmer. I enjoy dynamic programming and string algorithms.'),
(22, 1003, 'socialLinks', '{"GitHub":"alice"}'),
(23, 1009, 'location', 'Berlin, Germany'),
(24, 1009, 'website', 'https://grace.example.com'),
(25, 1009, 'aboutMe', 'Math student who codes on weekends.'),
(26, 1013, 'aboutMe', 'Algorithms enthusiast. Always up for a hard DP problem.'),
(27, 1013, 'socialLinks', '{"GitHub":"mallory"}');

INSERT INTO `voj_email_validation` (`email`, `token`, `expire_time`) VALUES
('trent@voj.local', '7c9e6679-7425-40de-944b-e07fc1f90ae7', '2026-12-31 23:59:59'),
('ivan@voj.local', 'b1d5781c-1c5f-4f3e-9a2b-3f0e5d6a7c8b', '2026-12-31 23:59:59');

-- Submissions. Judge logs reproduce the judger's real output (see
-- ApplicationDispatcher#getJudgeLog): "Compile Successfully." then one
-- "- Test Point #n: <Result>, Time = .. ms, Memory = .. KB, Score = .." line per
-- checkpoint, then a summary with SUMMED time, MAX memory and AC-only score.
-- Practice ids 1000-1023; contest ids 1030-1048 (linked in voj_contest_submissions).
INSERT INTO `voj_submissions` (`submission_id`, `problem_id`, `uid`, `language_id`, `submission_submit_time`, `submission_execute_time`, `submission_used_time`, `submission_used_memory`, `submission_judge_result`, `submission_judge_score`, `submission_judge_log`, `submission_code`) VALUES
(1000, 1000, 1003, 2, '2024-09-02 10:15:00', '2024-09-02 10:15:05', 10, 308, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #1: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #2: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #3: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #4: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #5: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #6: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #7: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #8: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #9: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10

Accepted, Time = 10 ms, Memory = 308 KB, Score = 100
', '#include <iostream>
int main() {
    int a, b;
    std::cin >> a >> b;
    std::cout << a + b << std::endl;
    return 0;
}'),
(1001, 1000, 1004, 1, '2024-09-16 14:20:00', '2024-09-16 14:20:05', 10, 256, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #1: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #2: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #3: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #4: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #5: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #6: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #7: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #8: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #9: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10

Accepted, Time = 10 ms, Memory = 256 KB, Score = 100
', '#include <stdio.h>
int main() {
    int a, b;
    scanf("%d %d", &a, &b);
    printf("%d", a + b);
    return 0;
}'),
(1002, 1000, 1005, 3, '2024-10-03 09:05:00', '2024-10-03 09:05:05', 90, 15800, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #1: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #2: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #3: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #4: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #5: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #6: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #7: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #8: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #9: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10

Accepted, Time = 90 ms, Memory = 15800 KB, Score = 100
', 'import java.util.Scanner;
public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        System.out.println(in.nextInt() + in.nextInt());
    }
}'),
(1003, 1000, 1006, 5, '2024-11-21 20:11:00', '2024-11-21 20:11:05', 120, 4200, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 12 ms, Memory = 4200 KB, Score = 10
- Test Point #1: Accepted, Time = 12 ms, Memory = 4200 KB, Score = 10
- Test Point #2: Accepted, Time = 12 ms, Memory = 4200 KB, Score = 10
- Test Point #3: Accepted, Time = 12 ms, Memory = 4200 KB, Score = 10
- Test Point #4: Accepted, Time = 12 ms, Memory = 4200 KB, Score = 10
- Test Point #5: Accepted, Time = 12 ms, Memory = 4200 KB, Score = 10
- Test Point #6: Accepted, Time = 12 ms, Memory = 4200 KB, Score = 10
- Test Point #7: Accepted, Time = 12 ms, Memory = 4200 KB, Score = 10
- Test Point #8: Accepted, Time = 12 ms, Memory = 4200 KB, Score = 10
- Test Point #9: Accepted, Time = 12 ms, Memory = 4200 KB, Score = 10

Accepted, Time = 120 ms, Memory = 4200 KB, Score = 100
', 'a, b = map(int, raw_input().split())
print a + b'),
(1004, 1000, 1008, 3, '2025-02-19 16:45:00', '2025-02-19 16:45:05', 0, 0, 'CE', 0, 'Compile Error.

    Main.java:3: error: cannot find symbol
            System.out.println(a + b);
                               ^
      symbol:   variable a
      location: class Main
    1 error

Compile Error, Time = 0 ms, Memory = 0 KB, Score = 0.
', 'public class Main {
    public static void main(String[] args) {
        System.out.println(a + b);
    }
}'),
(1005, 1000, 1008, 3, '2025-02-19 16:52:00', '2025-02-19 16:52:05', 90, 15800, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #1: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #2: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #3: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #4: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #5: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #6: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #7: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #8: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #9: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10

Accepted, Time = 90 ms, Memory = 15800 KB, Score = 100
', 'import java.util.Scanner;
public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        System.out.println(in.nextInt() + in.nextInt());
    }
}'),
(1006, 1000, 1010, 5, '2025-05-12 13:13:00', '2025-05-12 13:13:05', 130, 8200, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 13 ms, Memory = 8200 KB, Score = 10
- Test Point #1: Accepted, Time = 13 ms, Memory = 8200 KB, Score = 10
- Test Point #2: Accepted, Time = 13 ms, Memory = 8200 KB, Score = 10
- Test Point #3: Accepted, Time = 13 ms, Memory = 8200 KB, Score = 10
- Test Point #4: Accepted, Time = 13 ms, Memory = 8200 KB, Score = 10
- Test Point #5: Accepted, Time = 13 ms, Memory = 8200 KB, Score = 10
- Test Point #6: Accepted, Time = 13 ms, Memory = 8200 KB, Score = 10
- Test Point #7: Accepted, Time = 13 ms, Memory = 8200 KB, Score = 10
- Test Point #8: Accepted, Time = 13 ms, Memory = 8200 KB, Score = 10
- Test Point #9: Accepted, Time = 13 ms, Memory = 8200 KB, Score = 10

Accepted, Time = 130 ms, Memory = 8200 KB, Score = 100
', 'a, b = map(int, input().split())
print(a + b)'),
(1007, 1000, 1011, 1, '2025-07-23 19:00:00', '2025-07-23 19:00:05', 10, 256, 'WA', 30, 'Compile Successfully.

- Test Point #0: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #1: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #2: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #3: Wrong Answer, Time = 1 ms, Memory = 256 KB, Score = 0
- Test Point #4: Wrong Answer, Time = 1 ms, Memory = 256 KB, Score = 0
- Test Point #5: Wrong Answer, Time = 1 ms, Memory = 256 KB, Score = 0
- Test Point #6: Wrong Answer, Time = 1 ms, Memory = 256 KB, Score = 0
- Test Point #7: Wrong Answer, Time = 1 ms, Memory = 256 KB, Score = 0
- Test Point #8: Wrong Answer, Time = 1 ms, Memory = 256 KB, Score = 0
- Test Point #9: Wrong Answer, Time = 1 ms, Memory = 256 KB, Score = 0

Wrong Answer, Time = 10 ms, Memory = 256 KB, Score = 30
', '#include <stdio.h>
int main() {
    int a, b;
    scanf("%d %d", &a, &b);
    printf("%d", a - b);
    return 0;
}'),
(1008, 1000, 1012, 4, '2025-09-09 10:30:00', '2025-09-09 10:30:05', 20, 290, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 2 ms, Memory = 290 KB, Score = 10
- Test Point #1: Accepted, Time = 2 ms, Memory = 290 KB, Score = 10
- Test Point #2: Accepted, Time = 2 ms, Memory = 290 KB, Score = 10
- Test Point #3: Accepted, Time = 2 ms, Memory = 290 KB, Score = 10
- Test Point #4: Accepted, Time = 2 ms, Memory = 290 KB, Score = 10
- Test Point #5: Accepted, Time = 2 ms, Memory = 290 KB, Score = 10
- Test Point #6: Accepted, Time = 2 ms, Memory = 290 KB, Score = 10
- Test Point #7: Accepted, Time = 2 ms, Memory = 290 KB, Score = 10
- Test Point #8: Accepted, Time = 2 ms, Memory = 290 KB, Score = 10
- Test Point #9: Accepted, Time = 2 ms, Memory = 290 KB, Score = 10

Accepted, Time = 20 ms, Memory = 290 KB, Score = 100
', 'var a, b: longint;
begin
    readln(a, b);
    writeln(a + b);
end.'),
(1009, 1000, 1014, 3, '2026-01-21 09:15:00', '2026-01-21 09:15:05', 90, 15800, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #1: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #2: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #3: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #4: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #5: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #6: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #7: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #8: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #9: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10

Accepted, Time = 90 ms, Memory = 15800 KB, Score = 100
', 'import java.util.Scanner;
public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        System.out.println(in.nextInt() + in.nextInt());
    }
}'),
(1010, 1001, 1003, 2, '2024-09-05 10:00:00', '2024-09-05 10:00:05', 10, 308, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #1: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #2: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #3: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #4: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #5: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #6: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #7: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #8: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #9: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10

Accepted, Time = 10 ms, Memory = 308 KB, Score = 100
', '#include <iostream>
#include <string>
using namespace std;
int main() {
    int n;
    cin >> n;
    string best, name;
    long bestAmt = -1, total = 0;
    int avg, cls, papers;
    char leader, west;
    for (int i = 0; i < n; ++i) {
        cin >> name >> avg >> cls >> leader >> west >> papers;
        long amt = 0;
        if (avg > 80 && papers >= 1) amt += 8000;
        if (avg > 85 && cls > 80) amt += 4000;
        if (avg > 90) amt += 2000;
        if (avg > 85 && west == (char)89) amt += 1000;
        if (cls > 80 && leader == (char)89) amt += 850;
        total += amt;
        if (amt > bestAmt) { bestAmt = amt; best = name; }
    }
    cout << best << endl << bestAmt << endl << total << endl;
    return 0;
}'),
(1011, 1001, 1004, 1, '2024-09-20 14:00:00', '2024-09-20 14:00:05', 10, 256, 'WA', 40, 'Compile Successfully.

- Test Point #0: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #1: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #2: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #3: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #4: Wrong Answer, Time = 1 ms, Memory = 256 KB, Score = 0
- Test Point #5: Wrong Answer, Time = 1 ms, Memory = 256 KB, Score = 0
- Test Point #6: Wrong Answer, Time = 1 ms, Memory = 256 KB, Score = 0
- Test Point #7: Wrong Answer, Time = 1 ms, Memory = 256 KB, Score = 0
- Test Point #8: Wrong Answer, Time = 1 ms, Memory = 256 KB, Score = 0
- Test Point #9: Wrong Answer, Time = 1 ms, Memory = 256 KB, Score = 0

Wrong Answer, Time = 10 ms, Memory = 256 KB, Score = 40
', '#include <stdio.h>
int main() {
    int n;
    scanf("%d", &n);
    /* incomplete: ignores the western-province bonus */
    puts("ChenRuiyi");
    puts("9000");
    puts("27700");
    return 0;
}'),
(1012, 1001, 1005, 3, '2024-10-10 09:30:00', '2024-10-10 09:30:05', 90, 15800, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #1: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #2: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #3: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #4: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #5: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #6: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #7: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #8: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #9: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10

Accepted, Time = 90 ms, Memory = 15800 KB, Score = 100
', 'import java.util.Scanner;
public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int n = in.nextInt();
        String best = "";
        long bestAmt = -1, total = 0;
        for (int i = 0; i < n; i++) {
            String name = in.next();
            int avg = in.nextInt(), cls = in.nextInt();
            String leader = in.next(), west = in.next();
            int papers = in.nextInt();
            long amt = 0;
            if (avg > 80 && papers >= 1) amt += 8000;
            if (avg > 85 && cls > 80) amt += 4000;
            if (avg > 90) amt += 2000;
            if (avg > 85 && west.equals("Y")) amt += 1000;
            if (cls > 80 && leader.equals("Y")) amt += 850;
            total += amt;
            if (amt > bestAmt) { bestAmt = amt; best = name; }
        }
        System.out.println(best);
        System.out.println(bestAmt);
        System.out.println(total);
    }
}'),
(1013, 1001, 1007, 2, '2025-01-10 08:45:00', '2025-01-10 08:45:05', 10, 308, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #1: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #2: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #3: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #4: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #5: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #6: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #7: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #8: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #9: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10

Accepted, Time = 10 ms, Memory = 308 KB, Score = 100
', '#include <iostream>
#include <string>
using namespace std;
int main() {
    int n;
    cin >> n;
    string best, name;
    long bestAmt = -1, total = 0;
    int avg, cls, papers;
    char leader, west;
    for (int i = 0; i < n; ++i) {
        cin >> name >> avg >> cls >> leader >> west >> papers;
        long amt = 0;
        if (avg > 80 && papers >= 1) amt += 8000;
        if (avg > 85 && cls > 80) amt += 4000;
        if (avg > 90) amt += 2000;
        if (avg > 85 && west == (char)89) amt += 1000;
        if (cls > 80 && leader == (char)89) amt += 850;
        total += amt;
        if (amt > bestAmt) { bestAmt = amt; best = name; }
    }
    cout << best << endl << bestAmt << endl << total << endl;
    return 0;
}'),
(1014, 1001, 1006, 5, '2025-04-20 20:00:00', '2025-04-20 20:00:05', 8024, 4200, 'TLE', 20, 'Compile Successfully.

- Test Point #0: Accepted, Time = 12 ms, Memory = 4200 KB, Score = 10
- Test Point #1: Accepted, Time = 12 ms, Memory = 4200 KB, Score = 10
- Test Point #2: Time Limit Exceed, Time = 1000 ms, Memory = 4200 KB, Score = 0
- Test Point #3: Time Limit Exceed, Time = 1000 ms, Memory = 4200 KB, Score = 0
- Test Point #4: Time Limit Exceed, Time = 1000 ms, Memory = 4200 KB, Score = 0
- Test Point #5: Time Limit Exceed, Time = 1000 ms, Memory = 4200 KB, Score = 0
- Test Point #6: Time Limit Exceed, Time = 1000 ms, Memory = 4200 KB, Score = 0
- Test Point #7: Time Limit Exceed, Time = 1000 ms, Memory = 4200 KB, Score = 0
- Test Point #8: Time Limit Exceed, Time = 1000 ms, Memory = 4200 KB, Score = 0
- Test Point #9: Time Limit Exceed, Time = 1000 ms, Memory = 4200 KB, Score = 0

Time Limit Exceed, Time = 8024 ms, Memory = 4200 KB, Score = 20
', 'n = int(raw_input())
for _ in range(n):
    data = raw_input().split()
    # an accidental quadratic loop times out on large inputs
    for i in range(n * n):
        pass
print "ChenRuiyi"'),
(1015, 1001, 1013, 2, '2025-11-18 21:00:00', '2025-11-18 21:00:05', 10, 308, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #1: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #2: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #3: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #4: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #5: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #6: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #7: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #8: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #9: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10

Accepted, Time = 10 ms, Memory = 308 KB, Score = 100
', '#include <iostream>
#include <string>
using namespace std;
int main() {
    int n;
    cin >> n;
    string best, name;
    long bestAmt = -1, total = 0;
    int avg, cls, papers;
    char leader, west;
    for (int i = 0; i < n; ++i) {
        cin >> name >> avg >> cls >> leader >> west >> papers;
        long amt = 0;
        if (avg > 80 && papers >= 1) amt += 8000;
        if (avg > 85 && cls > 80) amt += 4000;
        if (avg > 90) amt += 2000;
        if (avg > 85 && west == (char)89) amt += 1000;
        if (cls > 80 && leader == (char)89) amt += 850;
        total += amt;
        if (amt > bestAmt) { bestAmt = amt; best = name; }
    }
    cout << best << endl << bestAmt << endl << total << endl;
    return 0;
}'),
(1016, 1003, 1003, 2, '2024-10-01 10:00:00', '2024-10-01 10:00:05', 5, 308, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #1: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #2: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #3: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #4: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20

Accepted, Time = 5 ms, Memory = 308 KB, Score = 100
', '#include <bits/stdc++.h>
using namespace std;
// Evaluate every expression at a few random points modulo a large prime
// and compare the resulting fingerprints to detect equivalence.
int main() {
    // expression parser and modular evaluation omitted for brevity
    return 0;
}'),
(1017, 1003, 1013, 2, '2025-11-20 21:30:00', '2025-11-20 21:30:05', 5, 308, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #1: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #2: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #3: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #4: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20

Accepted, Time = 5 ms, Memory = 308 KB, Score = 100
', '#include <bits/stdc++.h>
using namespace std;
// Evaluate every expression at a few random points modulo a large prime
// and compare the resulting fingerprints to detect equivalence.
int main() {
    // expression parser and modular evaluation omitted for brevity
    return 0;
}'),
(1018, 1003, 1005, 3, '2025-06-01 09:00:00', '2025-06-01 09:00:05', 45, 15800, 'WA', 40, 'Compile Successfully.

- Test Point #0: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 20
- Test Point #1: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 20
- Test Point #2: Wrong Answer, Time = 9 ms, Memory = 15800 KB, Score = 0
- Test Point #3: Wrong Answer, Time = 9 ms, Memory = 15800 KB, Score = 0
- Test Point #4: Wrong Answer, Time = 9 ms, Memory = 15800 KB, Score = 0

Wrong Answer, Time = 45 ms, Memory = 15800 KB, Score = 40
', 'public class Main {
    public static void main(String[] args) {
        // single-point hashing collides on a few adversarial cases
        System.out.println("AC");
    }
}'),
(1019, 1003, 1008, 3, '2025-06-15 16:30:00', '2025-06-15 16:30:05', 45, 15800, 'RE', 0, 'Compile Successfully.

- Test Point #0: Runtime Error, Time = 9 ms, Memory = 15800 KB, Score = 0
- Test Point #1: Runtime Error, Time = 9 ms, Memory = 15800 KB, Score = 0
- Test Point #2: Runtime Error, Time = 9 ms, Memory = 15800 KB, Score = 0
- Test Point #3: Runtime Error, Time = 9 ms, Memory = 15800 KB, Score = 0
- Test Point #4: Runtime Error, Time = 9 ms, Memory = 15800 KB, Score = 0

Runtime Error, Time = 45 ms, Memory = 15800 KB, Score = 0
', 'public class Main {
    public static void main(String[] args) {
        int[] a = new int[1];
        System.out.println(a[5]);
    }
}'),
(1020, 1003, 1009, 2, '2025-08-10 11:00:00', '2025-08-10 11:00:05', 5, 308, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #1: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #2: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #3: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #4: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20

Accepted, Time = 5 ms, Memory = 308 KB, Score = 100
', '#include <bits/stdc++.h>
using namespace std;
// Evaluate every expression at a few random points modulo a large prime
// and compare the resulting fingerprints to detect equivalence.
int main() {
    // expression parser and modular evaluation omitted for brevity
    return 0;
}'),
(1021, 1003, 1007, 2, '2025-12-05 08:00:00', '2025-12-05 08:00:05', 4001, 308, 'TLE', 20, 'Compile Successfully.

- Test Point #0: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #1: Time Limit Exceed, Time = 1000 ms, Memory = 308 KB, Score = 0
- Test Point #2: Time Limit Exceed, Time = 1000 ms, Memory = 308 KB, Score = 0
- Test Point #3: Time Limit Exceed, Time = 1000 ms, Memory = 308 KB, Score = 0
- Test Point #4: Time Limit Exceed, Time = 1000 ms, Memory = 308 KB, Score = 0

Time Limit Exceed, Time = 4001 ms, Memory = 308 KB, Score = 20
', '#include <bits/stdc++.h>
using namespace std;
int main() {
    // exponential brute force over all sub-expressions
    volatile long x = 0;
    while (true) x++;
    return 0;
}'),
(1022, 1003, 1010, 2, '2026-03-01 13:00:00', '2026-03-01 13:00:05', 65, 65540, 'MLE', 20, 'Compile Successfully.

- Test Point #0: Accepted, Time = 13 ms, Memory = 8200 KB, Score = 20
- Test Point #1: Memory Limit Exceed, Time = 13 ms, Memory = 65540 KB, Score = 0
- Test Point #2: Memory Limit Exceed, Time = 13 ms, Memory = 65540 KB, Score = 0
- Test Point #3: Memory Limit Exceed, Time = 13 ms, Memory = 65540 KB, Score = 0
- Test Point #4: Memory Limit Exceed, Time = 13 ms, Memory = 65540 KB, Score = 0

Memory Limit Exceed, Time = 65 ms, Memory = 65540 KB, Score = 20
', '# builds a huge memo table that exceeds the memory limit
memo = Array.new(100000000, 0)
puts memo.size'),
(1023, 1000, 1009, 2, '2025-03-31 11:00:00', '2025-03-31 11:00:05', 10, 308, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #1: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #2: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #3: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #4: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #5: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #6: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #7: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #8: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #9: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10

Accepted, Time = 10 ms, Memory = 308 KB, Score = 100
', '#include <iostream>
int main() {
    int a, b;
    std::cin >> a >> b;
    std::cout << a + b << std::endl;
    return 0;
}'),
(1030, 1000, 1003, 2, '2025-03-01 09:12:00', '2025-03-01 09:12:05', 10, 308, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #1: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #2: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #3: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #4: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #5: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #6: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #7: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #8: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #9: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10

Accepted, Time = 10 ms, Memory = 308 KB, Score = 100
', '#include <iostream>
int main() {
    int a, b;
    std::cin >> a >> b;
    std::cout << a + b << std::endl;
    return 0;
}'),
(1031, 1001, 1003, 2, '2025-03-01 09:45:00', '2025-03-01 09:45:05', 10, 308, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #1: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #2: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #3: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #4: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #5: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #6: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #7: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #8: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #9: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10

Accepted, Time = 10 ms, Memory = 308 KB, Score = 100
', '#include <iostream>
#include <string>
using namespace std;
int main() {
    int n;
    cin >> n;
    string best, name;
    long bestAmt = -1, total = 0;
    int avg, cls, papers;
    char leader, west;
    for (int i = 0; i < n; ++i) {
        cin >> name >> avg >> cls >> leader >> west >> papers;
        long amt = 0;
        if (avg > 80 && papers >= 1) amt += 8000;
        if (avg > 85 && cls > 80) amt += 4000;
        if (avg > 90) amt += 2000;
        if (avg > 85 && west == (char)89) amt += 1000;
        if (cls > 80 && leader == (char)89) amt += 850;
        total += amt;
        if (amt > bestAmt) { bestAmt = amt; best = name; }
    }
    cout << best << endl << bestAmt << endl << total << endl;
    return 0;
}'),
(1032, 1000, 1004, 1, '2025-03-01 09:20:00', '2025-03-01 09:20:05', 10, 256, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #1: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #2: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #3: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #4: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #5: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #6: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #7: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #8: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #9: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10

Accepted, Time = 10 ms, Memory = 256 KB, Score = 100
', '#include <stdio.h>
int main() {
    int a, b;
    scanf("%d %d", &a, &b);
    printf("%d", a + b);
    return 0;
}'),
(1033, 1001, 1004, 1, '2025-03-01 10:05:00', '2025-03-01 10:05:05', 10, 256, 'WA', 40, 'Compile Successfully.

- Test Point #0: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #1: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #2: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #3: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #4: Wrong Answer, Time = 1 ms, Memory = 256 KB, Score = 0
- Test Point #5: Wrong Answer, Time = 1 ms, Memory = 256 KB, Score = 0
- Test Point #6: Wrong Answer, Time = 1 ms, Memory = 256 KB, Score = 0
- Test Point #7: Wrong Answer, Time = 1 ms, Memory = 256 KB, Score = 0
- Test Point #8: Wrong Answer, Time = 1 ms, Memory = 256 KB, Score = 0
- Test Point #9: Wrong Answer, Time = 1 ms, Memory = 256 KB, Score = 0

Wrong Answer, Time = 10 ms, Memory = 256 KB, Score = 40
', '#include <stdio.h>
int main() {
    int n;
    scanf("%d", &n);
    /* incomplete: ignores the western-province bonus */
    puts("ChenRuiyi");
    puts("9000");
    puts("27700");
    return 0;
}'),
(1034, 1000, 1005, 3, '2025-03-01 09:35:00', '2025-03-01 09:35:05', 90, 15800, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #1: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #2: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #3: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #4: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #5: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #6: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #7: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #8: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #9: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10

Accepted, Time = 90 ms, Memory = 15800 KB, Score = 100
', 'import java.util.Scanner;
public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        System.out.println(in.nextInt() + in.nextInt());
    }
}'),
(1035, 1003, 1003, 2, '2025-04-15 13:30:00', '2025-04-15 13:30:05', 5, 308, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #1: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #2: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #3: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #4: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20

Accepted, Time = 5 ms, Memory = 308 KB, Score = 100
', '#include <bits/stdc++.h>
using namespace std;
// Evaluate every expression at a few random points modulo a large prime
// and compare the resulting fingerprints to detect equivalence.
int main() {
    // expression parser and modular evaluation omitted for brevity
    return 0;
}'),
(1036, 1003, 1013, 2, '2025-04-15 14:10:00', '2025-04-15 14:10:05', 5, 308, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #1: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #2: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #3: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #4: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20

Accepted, Time = 5 ms, Memory = 308 KB, Score = 100
', '#include <bits/stdc++.h>
using namespace std;
// Evaluate every expression at a few random points modulo a large prime
// and compare the resulting fingerprints to detect equivalence.
int main() {
    // expression parser and modular evaluation omitted for brevity
    return 0;
}'),
(1037, 1001, 1009, 2, '2025-04-15 13:50:00', '2025-04-15 13:50:05', 10, 308, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #1: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #2: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #3: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #4: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #5: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #6: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #7: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #8: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #9: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10

Accepted, Time = 10 ms, Memory = 308 KB, Score = 100
', '#include <iostream>
#include <string>
using namespace std;
int main() {
    int n;
    cin >> n;
    string best, name;
    long bestAmt = -1, total = 0;
    int avg, cls, papers;
    char leader, west;
    for (int i = 0; i < n; ++i) {
        cin >> name >> avg >> cls >> leader >> west >> papers;
        long amt = 0;
        if (avg > 80 && papers >= 1) amt += 8000;
        if (avg > 85 && cls > 80) amt += 4000;
        if (avg > 90) amt += 2000;
        if (avg > 85 && west == (char)89) amt += 1000;
        if (cls > 80 && leader == (char)89) amt += 850;
        total += amt;
        if (amt > bestAmt) { bestAmt = amt; best = name; }
    }
    cout << best << endl << bestAmt << endl << total << endl;
    return 0;
}'),
(1038, 1000, 1004, 1, '2025-07-12 09:15:00', '2025-07-12 09:15:05', 10, 256, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #1: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #2: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #3: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #4: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #5: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #6: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #7: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #8: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10
- Test Point #9: Accepted, Time = 1 ms, Memory = 256 KB, Score = 10

Accepted, Time = 10 ms, Memory = 256 KB, Score = 100
', '#include <stdio.h>
int main() {
    int a, b;
    scanf("%d %d", &a, &b);
    printf("%d", a + b);
    return 0;
}'),
(1039, 1001, 1005, 3, '2025-07-12 09:50:00', '2025-07-12 09:50:05', 90, 15800, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #1: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #2: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #3: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #4: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #5: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #6: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #7: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #8: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10
- Test Point #9: Accepted, Time = 9 ms, Memory = 15800 KB, Score = 10

Accepted, Time = 90 ms, Memory = 15800 KB, Score = 100
', 'import java.util.Scanner;
public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int n = in.nextInt();
        String best = "";
        long bestAmt = -1, total = 0;
        for (int i = 0; i < n; i++) {
            String name = in.next();
            int avg = in.nextInt(), cls = in.nextInt();
            String leader = in.next(), west = in.next();
            int papers = in.nextInt();
            long amt = 0;
            if (avg > 80 && papers >= 1) amt += 8000;
            if (avg > 85 && cls > 80) amt += 4000;
            if (avg > 90) amt += 2000;
            if (avg > 85 && west.equals("Y")) amt += 1000;
            if (cls > 80 && leader.equals("Y")) amt += 850;
            total += amt;
            if (amt > bestAmt) { bestAmt = amt; best = name; }
        }
        System.out.println(best);
        System.out.println(bestAmt);
        System.out.println(total);
    }
}'),
(1040, 1000, 1010, 5, '2025-07-12 10:20:00', '2025-07-12 10:20:05', 130, 8200, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 13 ms, Memory = 8200 KB, Score = 10
- Test Point #1: Accepted, Time = 13 ms, Memory = 8200 KB, Score = 10
- Test Point #2: Accepted, Time = 13 ms, Memory = 8200 KB, Score = 10
- Test Point #3: Accepted, Time = 13 ms, Memory = 8200 KB, Score = 10
- Test Point #4: Accepted, Time = 13 ms, Memory = 8200 KB, Score = 10
- Test Point #5: Accepted, Time = 13 ms, Memory = 8200 KB, Score = 10
- Test Point #6: Accepted, Time = 13 ms, Memory = 8200 KB, Score = 10
- Test Point #7: Accepted, Time = 13 ms, Memory = 8200 KB, Score = 10
- Test Point #8: Accepted, Time = 13 ms, Memory = 8200 KB, Score = 10
- Test Point #9: Accepted, Time = 13 ms, Memory = 8200 KB, Score = 10

Accepted, Time = 130 ms, Memory = 8200 KB, Score = 100
', 'a, b = map(int, input().split())
print(a + b)'),
(1041, 1002, 1003, 2, '2025-10-18 09:40:00', '2025-10-18 09:40:05', 5, 308, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #1: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #2: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #3: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #4: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20

Accepted, Time = 5 ms, Memory = 308 KB, Score = 100
', '#include <bits/stdc++.h>
using namespace std;
// Cap each gap at lcm(1..10) = 2520 to compress the bridge,
// then DP over reachable positions counting stepped-on stones.
int main() {
    // input parsing and DP omitted for brevity
    return 0;
}'),
(1042, 1003, 1003, 2, '2025-10-18 11:00:00', '2025-10-18 11:00:05', 5, 308, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #1: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #2: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #3: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #4: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20

Accepted, Time = 5 ms, Memory = 308 KB, Score = 100
', '#include <bits/stdc++.h>
using namespace std;
// Evaluate every expression at a few random points modulo a large prime
// and compare the resulting fingerprints to detect equivalence.
int main() {
    // expression parser and modular evaluation omitted for brevity
    return 0;
}'),
(1043, 1002, 1013, 2, '2025-10-18 10:30:00', '2025-10-18 10:30:05', 5, 308, 'WA', 20, 'Compile Successfully.

- Test Point #0: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #1: Wrong Answer, Time = 1 ms, Memory = 308 KB, Score = 0
- Test Point #2: Wrong Answer, Time = 1 ms, Memory = 308 KB, Score = 0
- Test Point #3: Wrong Answer, Time = 1 ms, Memory = 308 KB, Score = 0
- Test Point #4: Wrong Answer, Time = 1 ms, Memory = 308 KB, Score = 0

Wrong Answer, Time = 5 ms, Memory = 308 KB, Score = 20
', '#include <bits/stdc++.h>
using namespace std;
// gap cap set to 100, which is too small for the worst case
int main() {
    return 0;
}'),
(1044, 1000, 1003, 2, '2026-06-25 10:00:00', '2026-06-25 10:00:05', 10, 308, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #1: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #2: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #3: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #4: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #5: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #6: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #7: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #8: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #9: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10

Accepted, Time = 10 ms, Memory = 308 KB, Score = 100
', '#include <iostream>
int main() {
    int a, b;
    std::cin >> a >> b;
    std::cout << a + b << std::endl;
    return 0;
}'),
(1045, 1003, 1013, 2, '2026-06-26 20:00:00', '2026-06-26 20:00:05', 5, 308, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #1: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #2: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #3: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #4: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20

Accepted, Time = 5 ms, Memory = 308 KB, Score = 100
', '#include <bits/stdc++.h>
using namespace std;
// Evaluate every expression at a few random points modulo a large prime
// and compare the resulting fingerprints to detect equivalence.
int main() {
    // expression parser and modular evaluation omitted for brevity
    return 0;
}'),
(1046, 1000, 1009, 2, '2026-06-27 09:30:00', '2026-06-27 09:30:05', 10, 308, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #1: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #2: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #3: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #4: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #5: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #6: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #7: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #8: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #9: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10

Accepted, Time = 10 ms, Memory = 308 KB, Score = 100
', '#include <iostream>
int main() {
    int a, b;
    std::cin >> a >> b;
    std::cout << a + b << std::endl;
    return 0;
}'),
(1047, 1001, 1007, 2, '2026-06-26 14:00:00', '2026-06-26 14:00:05', 10, 308, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #1: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #2: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #3: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #4: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #5: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #6: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #7: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #8: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10
- Test Point #9: Accepted, Time = 1 ms, Memory = 308 KB, Score = 10

Accepted, Time = 10 ms, Memory = 308 KB, Score = 100
', '#include <iostream>
#include <string>
using namespace std;
int main() {
    int n;
    cin >> n;
    string best, name;
    long bestAmt = -1, total = 0;
    int avg, cls, papers;
    char leader, west;
    for (int i = 0; i < n; ++i) {
        cin >> name >> avg >> cls >> leader >> west >> papers;
        long amt = 0;
        if (avg > 80 && papers >= 1) amt += 8000;
        if (avg > 85 && cls > 80) amt += 4000;
        if (avg > 90) amt += 2000;
        if (avg > 85 && west == (char)89) amt += 1000;
        if (cls > 80 && leader == (char)89) amt += 850;
        total += amt;
        if (amt > bestAmt) { bestAmt = amt; best = name; }
    }
    cout << best << endl << bestAmt << endl << total << endl;
    return 0;
}'),
(1048, 1003, 1003, 2, '2026-06-27 10:15:00', '2026-06-27 10:15:05', 5, 308, 'AC', 100, 'Compile Successfully.

- Test Point #0: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #1: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #2: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #3: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20
- Test Point #4: Accepted, Time = 1 ms, Memory = 308 KB, Score = 20

Accepted, Time = 5 ms, Memory = 308 KB, Score = 100
', '#include <bits/stdc++.h>
using namespace std;
// Evaluate every expression at a few random points modulo a large prime
// and compare the resulting fingerprints to detect equivalence.
int main() {
    // expression parser and modular evaluation omitted for brevity
    return 0;
}');

-- Attribute each judged submission to one of the demo judgers (judge-alpha / judge-beta),
-- so the submission detail page shows a real judger name. Pending submissions stay unassigned.
UPDATE `voj_submissions`
SET `judger_uid` = IF(MOD(`submission_id`, 2) = 0, 1001, 1002)
WHERE `submission_judge_result` <> 'PD';

INSERT INTO `voj_problem_category_relationships` (`problem_id`, `problem_category_id`) VALUES
(1000, 1),
(1001, 1),
(1002, 1),
(1002, 2),
(1003, 1),
(1003, 2);

INSERT INTO `voj_problem_tag_relationships` (`problem_id`, `problem_tag_id`) VALUES
(1001, 1),
(1002, 2),
(1003, 2);

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

-- Checkpoints for the medium/hard problems, so 1002 and 1003 are judgeable too
-- (inputs/outputs hand-verified; 5 points x 20 = 100).
INSERT INTO `voj_problem_checkpoints` (`problem_id`, `checkpoint_id`, `checkpoint_exactly_match`, `checkpoint_score`, `checkpoint_input`, `checkpoint_output`) VALUES
(1002, 0, 0, 20, '10
2 3 5
2 3 5 6 7
', '2
'),
(1002, 1, 0, 20, '5
2 2 1
3
', '0
'),
(1002, 2, 0, 20, '6
2 2 1
4
', '1
'),
(1002, 3, 0, 20, '4
1 1 2
1 2
', '2
'),
(1002, 4, 0, 20, '8
2 3 2
4 5
', '0
'),
(1003, 0, 0, 20, '( a + 1) ^2
3
(a-1)^2+4*a
a  + 1+ a
a^2 + 2 * a * 1 + 1^2 + 10 -10 +a -a
', 'AC
'),
(1003, 1, 0, 20, 'a
2
a
a+a
', 'A
'),
(1003, 2, 0, 20, 'a+a
3
2*a
a
a*2
', 'AC
'),
(1003, 3, 0, 20, 'a^2
2
a*a
a+a
', 'A
'),
(1003, 4, 0, 20, '2*a+3
2
a+a+3
a+3
', 'A
');

-- Contest rosters. Contests 7 and 8 are upcoming, so contestants are pre-registered
-- but have no submissions yet.
INSERT INTO `voj_contest_contestants` (`contest_id`, `contestant_uid`) VALUES
(1, 1003), (1, 1004), (1, 1005), (1, 1006), (1, 1007), (1, 1008),
(2, 1003), (2, 1005), (2, 1008), (2, 1009), (2, 1013),
(3, 1004), (3, 1005), (3, 1010), (3, 1011), (3, 1012),
(4, 1003), (4, 1009), (4, 1013),
(5, 1003), (5, 1004), (5, 1007), (5, 1008), (5, 1009), (5, 1013), (5, 1014),
(6, 1003), (6, 1005), (6, 1008), (6, 1009),
(7, 1003), (7, 1004), (7, 1005), (7, 1009), (7, 1013),
(8, 1003), (8, 1009), (8, 1014);

INSERT INTO `voj_contest_submissions` (`contest_id`, `submission_id`) VALUES
(1, 1030), (1, 1031), (1, 1032), (1, 1033), (1, 1034),
(2, 1035), (2, 1036), (2, 1037),
(3, 1038), (3, 1039), (3, 1040),
(4, 1041), (4, 1042), (4, 1043),
(5, 1044), (5, 1045), (5, 1047),
(6, 1046), (6, 1048);

INSERT INTO `voj_discussion_threads` (`discussion_thread_id`, `discussion_thread_creator_uid`, `discussion_thread_create_time`, `problem_id`, `discussion_topic_id`, `discussion_thread_name`) VALUES
(1, 1003, '2025-03-05 19:20:00', 1000, 1, 'A clean solution to the A+B Problem'),
(2, 1005, '2025-04-10 21:05:00', 1001, 2, 'How are ties broken in the scholarship ranking?'),
(3, 1013, '2025-10-20 22:40:00', 1002, 1, 'Crossing the River: gap compression with lcm(1..10)'),
(4, 1003, '2025-05-01 09:15:00', 1003, 1, 'Polynomial hashing for Equivalent Expressions'),
(5, 1004, '2025-02-20 11:30:00', NULL, 4, 'Which C++ standard does the judge use?'),
(6, 1009, '2025-06-12 14:00:00', NULL, 3, 'Introduce yourself! New members welcome'),
(7, 1006, '2025-04-22 20:10:00', 1001, 2, 'Getting TLE with Python on the scholarship problem'),
(8, 1000, '2026-06-20 10:00:00', NULL, 3, 'Scheduled maintenance window this weekend'),
(9, 1008, '2026-03-02 16:45:00', 1003, 2, 'Runtime error on test 3 - any hints?');

INSERT INTO `voj_discussion_replies` (`discussion_reply_id`, `discussion_thread_id`, `discussion_reply_uid`, `discussion_reply_time`, `discussion_reply_content`) VALUES
(1, 1, 1007, '2025-03-05 20:01:00', 'Nice write-up! Reading the two integers and printing the sum really is all it takes.'),
(2, 1, 1011, '2025-03-06 08:12:00', 'Watch the input range though - it still fits in a 32-bit int here, so no need for long long.'),
(3, 2, 1003, '2025-04-10 21:40:00', 'Ties are broken by input order: the earliest student with the maximum amount wins.'),
(4, 2, 1009, '2025-04-11 10:05:00', 'Thanks, that matches the sample where ChenRuiyi is printed.'),
(5, 3, 1003, '2025-10-21 09:30:00', 'Great trick. lcm(1..10) = 2520, so any gap larger than that can be capped safely.'),
(6, 4, 1013, '2025-05-01 12:00:00', 'Evaluating at a few random points modulo a big prime is enough to avoid collisions in practice.'),
(7, 5, 1000, '2025-02-20 13:00:00', 'The judge compiles C++ with -std=c++20 by default. See the Languages page for the exact command.'),
(8, 6, 1005, '2025-06-12 15:20:00', 'Hi everyone, Carol here. Happy to be solving problems with you all!'),
(9, 6, 1014, '2025-06-13 09:00:00', 'Welcome! Trent from the data-structures study group.'),
(10, 7, 1003, '2025-04-22 21:00:00', 'Read all input at once and avoid recomputing inside the loop - that is usually the culprit.'),
(11, 7, 1006, '2025-04-23 07:45:00', 'That fixed it, dropped from TLE to 0.3s. Thank you!'),
(12, 9, 1003, '2026-03-02 17:30:00', 'Check for an empty option list - test 3 has a tricky edge case there.');

INSERT INTO `voj_discussion_reply_votes` (`discussion_reply_id`, `voter_uid`, `vote_type`, `vote_time`) VALUES
(1, 1003, 1, '2025-03-05 21:00:00'),
(1, 1005, 1, '2025-03-06 09:00:00'),
(1, 1009, 1, '2025-03-06 10:30:00'),
(3, 1005, 1, '2025-04-10 22:00:00'),
(3, 1007, 1, '2025-04-11 08:00:00'),
(5, 1009, 1, '2025-10-21 10:00:00'),
(5, 1003, 1, '2025-10-21 11:00:00'),
(6, 1003, 1, '2025-05-01 12:30:00'),
(7, 1004, 1, '2025-02-20 14:00:00'),
(10, 1006, 1, '2025-04-22 21:30:00'),
(11, 1003, 1, '2025-04-23 08:00:00'),
(2, 1009, -1, '2025-03-06 12:00:00'),
(8, 1011, -1, '2025-06-12 16:00:00'),
(4, 1011, 2, '2025-04-11 11:00:00');

INSERT INTO `voj_bulletin_board_messages` (`message_id`, `message_author_id`, `message_title`, `message_body`, `message_is_pinned`, `message_status`, `message_create_time`) VALUES
(1, 1000, 'Welcome to Verwandlung Online Judge', 'Thanks for installing VOJ! Browse the problem set, join a contest, or say hello in the discussion forum. Sign in as the administrator with the credentials you set during installation.', 1, 'PUBLISHED', '2026-06-01 09:00:00'),
(2, 1000, 'Judge cluster upgraded', 'The judging nodes have been upgraded to faster hardware. Expect noticeably quicker verdicts on heavy submissions.', 0, 'PUBLISHED', '2026-04-15 12:00:00'),
(3, 1000, 'Summer Showdown 2026 is live', 'Our flagship ACM round runs June 25-29. Head over to the Contests page to register and compete.', 1, 'PUBLISHED', '2026-06-25 00:05:00'),
(4, 1000, 'Scheduled maintenance', 'The judge will pause for maintenance on June 29, 02:00-04:00 UTC. Submissions made during this window are queued and judged afterwards.', 0, 'DRAFT', '2026-06-20 18:00:00'),
(5, 1000, 'New to competitive programming?', 'Start with the A+B Problem, then work through the Easy tier. The discussion forum has editorials and hints for every public problem.', 0, 'HIDDEN', '2026-05-10 10:00:00');



-- >>> CODECONTESTS BEGIN (generated by scripts/import_codecontests.py; do not edit)
-- Problems from DeepMind CodeContests (https://huggingface.co/datasets/deepmind/code_contests),
-- licensed CC BY 4.0. Statements (c) their original authors; attributed per problem.
INSERT INTO `voj_problems` (`problem_id`, `problem_status`, `problem_name`, `problem_time_limit`, `problem_memory_limit`, `problem_description`, `problem_input_format`, `problem_output_format`, `problem_sample_input`, `problem_sample_output`, `problem_hint`) VALUES
(2000, 'PUBLISHED', 'CQXYM Count Permutations', 1000, 250000, 'CQXYM is counting permutations length of 2n.

A permutation is an array consisting of n distinct integers from 1 to n in arbitrary order. For example, [2,3,1,5,4] is a permutation, but [1,2,2] is not a permutation (2 appears twice in the array) and [1,3,4] is also not a permutation (n=3 but there is 4 in the array).

A permutation p(length of 2n) will be counted only if the number of i satisfying p_i&lt;p_{i+1} is no less than n. For example:

  * Permutation [1, 2, 3, 4] will count, because the number of such i that p_i&lt;p_{i+1} equals 3 (i = 1, i = 2, i = 3).
  * Permutation [3, 2, 1, 4] won''t count, because the number of such i that p_i&lt;p_{i+1} equals 1 (i = 3). 



CQXYM wants you to help him to count the number of such permutations modulo 1000000007 (10^9+7).

In addition, [modulo operation](https://en.wikipedia.org/wiki/Modulo_operation) is to get the remainder. For example:

  * 7 mod 3=1, because 7 = 3 ⋅ 2 + 1, 
  * 15 mod 4=3, because 15 = 4 ⋅ 3 + 3.

---
*Source: Codeforces problem, via DeepMind CodeContests (CC BY 4.0).*', 'The input consists of multiple test cases. 

The first line contains an integer t (t ≥ 1) — the number of test cases. The description of the test cases follows.

Only one line of each test case contains an integer n(1 ≤ n ≤ 10^5).

It is guaranteed that the sum of n over all test cases does not exceed 10^5', 'For each test case, print the answer in a single line.', '4
1
2
9
91234
', '1
12
830455698
890287984
', NULL),
(2001, 'PUBLISHED', 'Equal or Not Equal', 2000, 250000, 'You had n positive integers a_1, a_2, ..., a_n arranged in a circle. For each pair of neighboring numbers (a_1 and a_2, a_2 and a_3, ..., a_{n - 1} and a_n, and a_n and a_1), you wrote down: are the numbers in the pair equal or not.

Unfortunately, you''ve lost a piece of paper with the array a. Moreover, you are afraid that even information about equality of neighboring elements may be inconsistent. So, you are wondering: is there any array a which is consistent with information you have about equality or non-equality of corresponding pairs?

---
*Source: Codeforces problem, via DeepMind CodeContests (CC BY 4.0).*', 'The first line contains a single integer t (1 ≤ t ≤ 1000) — the number of test cases. Next t cases follow.

The first and only line of each test case contains a non-empty string s consisting of characters E and/or N. The length of s is equal to the size of array n and 2 ≤ n ≤ 50. For each i from 1 to n: 

  * if s_i = E then a_i is equal to a_{i + 1} (a_n = a_1 for i = n); 
  * if s_i = N then a_i is not equal to a_{i + 1} (a_n ≠ a_1 for i = n).', 'For each test case, print YES if it''s possible to choose array a that are consistent with information from s you know. Otherwise, print NO.

It can be proved, that if there exists some array a, then there exists an array a of positive integers with values less or equal to 10^9.', '4
EEE
EN
ENNEENE
NENN
', 'YES
NO
YES
YES
', NULL),
(2002, 'PUBLISHED', 'Linear Keyboard', 1000, 250000, 'You are given a keyboard that consists of 26 keys. The keys are arranged sequentially in one row in a certain order. Each key corresponds to a unique lowercase Latin letter.

You have to type the word s on this keyboard. It also consists only of lowercase Latin letters.

To type a word, you need to type all its letters consecutively one by one. To type each letter you must position your hand exactly over the corresponding key and press it.

Moving the hand between the keys takes time which is equal to the absolute value of the difference between positions of these keys (the keys are numbered from left to right). No time is spent on pressing the keys and on placing your hand over the first letter of the word.

For example, consider a keyboard where the letters from ''a'' to ''z'' are arranged in consecutive alphabetical order. The letters ''h'', ''e'', ''l'' and ''o'' then are on the positions 8, 5, 12 and 15, respectively. Therefore, it will take |5 - 8| + |12 - 5| + |12 - 12| + |15 - 12| = 13 units of time to type the word "hello". 

Determine how long it will take to print the word s.

---
*Source: Codeforces problem, via DeepMind CodeContests (CC BY 4.0).*', 'The first line contains an integer t (1 ≤ t ≤ 1000) — the number of test cases.

The next 2t lines contain descriptions of the test cases.

The first line of a description contains a keyboard — a string of length 26, which consists only of lowercase Latin letters. Each of the letters from ''a'' to ''z'' appears exactly once on the keyboard.

The second line of the description contains the word s. The word has a length from 1 to 50 letters inclusive and consists of lowercase Latin letters.', 'Print t lines, each line containing the answer to the corresponding test case. The answer to the test case is the minimal time it takes to type the word s on the given keyboard.', '5
abcdefghijklmnopqrstuvwxyz
hello
abcdefghijklmnopqrstuvwxyz
i
abcdefghijklmnopqrstuvwxyz
codeforces
qwertyuiopasdfghjklzxcvbnm
qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq
qwertyuiopasdfghjklzxcvbnm
abacaba
', '13
0
68
0
74
', NULL),
(2003, 'PUBLISHED', 'Square String?', 1000, 250000, 'A string is called square if it is some string written twice in a row. For example, the strings "aa", "abcabc", "abab" and "baabaa" are square. But the strings "aaa", "abaaab" and "abcdabc" are not square.

For a given string s determine if it is square.

---
*Source: Codeforces problem, via DeepMind CodeContests (CC BY 4.0).*', 'The first line of input data contains an integer t (1 ≤ t ≤ 100) —the number of test cases.

This is followed by t lines, each containing a description of one test case. The given strings consist only of lowercase Latin letters and have lengths between 1 and 100 inclusive.', 'For each test case, output on a separate line:

  * YES if the string in the corresponding test case is square, 
  * NO otherwise. 



You can output YES and NO in any case (for example, strings yEs, yes, Yes and YES will be recognized as a positive response).', '10
a
aa
aaa
aaaa
abab
abcabc
abacaba
xxyy
xyyx
xyxy
', 'NO
YES
NO
YES
YES
YES
NO
NO
NO
YES
', NULL),
(2004, 'PUBLISHED', 'Long Comparison', 2000, 250000, 'Monocarp wrote down two numbers on a whiteboard. Both numbers follow a specific format: a positive integer x with p zeros appended to its end.

Now Monocarp asks you to compare these two numbers. Can you help him?

---
*Source: Codeforces problem, via DeepMind CodeContests (CC BY 4.0).*', 'The first line contains a single integer t (1 ≤ t ≤ 10^4) — the number of testcases.

The first line of each testcase contains two integers x_1 and p_1 (1 ≤ x_1 ≤ 10^6; 0 ≤ p_1 ≤ 10^6) — the description of the first number.

The second line of each testcase contains two integers x_2 and p_2 (1 ≤ x_2 ≤ 10^6; 0 ≤ p_2 ≤ 10^6) — the description of the second number.', 'For each testcase print the result of the comparison of the given two numbers. If the first number is smaller than the second one, print ''&lt;''. If the first number is greater than the second one, print ''&gt;''. If they are equal, print ''=''.', '5
2 1
19 0
10 2
100 1
1999 0
2 3
1 0
1 0
99 0
1 2
', '&gt;
=
&lt;
=
&lt;
', NULL),
(2005, 'PUBLISHED', 'Groups', 4000, 250000, 'n students attended the first meeting of the Berland SU programming course (n is even). All students will be divided into two groups. Each group will be attending exactly one lesson each week during one of the five working days (Monday, Tuesday, Wednesday, Thursday and Friday), and the days chosen for the groups must be different. Furthermore, both groups should contain the same number of students.

Each student has filled a survey in which they told which days of the week are convenient for them to attend a lesson, and which are not. 

Your task is to determine if it is possible to choose two different week days to schedule the lessons for the group (the first group will attend the lesson on the first chosen day, the second group will attend the lesson on the second chosen day), and divide the students into two groups, so the groups have equal sizes, and for each student, the chosen lesson day for their group is convenient.

---
*Source: Codeforces problem, via DeepMind CodeContests (CC BY 4.0).*', 'The first line contains a single integer t (1 ≤ t ≤ 10^4) — the number of testcases.

Then the descriptions of t testcases follow.

The first line of each testcase contains one integer n (2 ≤ n ≤ 1 000) — the number of students.

The i-th of the next n lines contains 5 integers, each of them is 0 or 1. If the j-th integer is 1, then the i-th student can attend the lessons on the j-th day of the week. If the j-th integer is 0, then the i-th student cannot attend the lessons on the j-th day of the week. 

Additional constraints on the input: for each student, at least one of the days of the week is convenient, the total number of students over all testcases doesn''t exceed 10^5.', 'For each testcase print an answer. If it''s possible to divide the students into two groups of equal sizes and choose different days for the groups so each student can attend the lesson in the chosen day of their group, print "YES" (without quotes). Otherwise, print "NO" (without quotes).', '2
4
1 0 0 1 0
0 1 0 0 1
0 0 0 1 0
0 1 0 1 0
2
0 0 0 1 0
0 0 0 1 0
', 'YES
NO
', NULL),
(2006, 'PUBLISHED', 'Delete Two Elements', 2000, 250000, 'Monocarp has got an array a consisting of n integers. Let''s denote k as the mathematic mean of these elements (note that it''s possible that k is not an integer). 

The mathematic mean of an array of n elements is the sum of elements divided by the number of these elements (i. e. sum divided by n).

Monocarp wants to delete exactly two elements from a so that the mathematic mean of the remaining (n - 2) elements is still equal to k.

Your task is to calculate the number of pairs of positions [i, j] (i &lt; j) such that if the elements on these positions are deleted, the mathematic mean of (n - 2) remaining elements is equal to k (that is, it is equal to the mathematic mean of n elements of the original array a).

---
*Source: Codeforces problem, via DeepMind CodeContests (CC BY 4.0).*', 'The first line contains a single integer t (1 ≤ t ≤ 10^4) — the number of testcases.

The first line of each testcase contains one integer n (3 ≤ n ≤ 2 ⋅ 10^5) — the number of elements in the array.

The second line contains a sequence of integers a_1, a_2, ..., a_n (0 ≤ a_i ≤ 10^{9}), where a_i is the i-th element of the array.

The sum of n over all testcases doesn''t exceed 2 ⋅ 10^5.', 'Print one integer — the number of pairs of positions [i, j] (i &lt; j) such that if the elements on these positions are deleted, the mathematic mean of (n - 2) remaining elements is equal to k (that is, it is equal to the mathematic mean of n elements of the original array a).', '4
4
8 8 8 8
3
50 20 10
5
1 4 7 3 5
7
1 2 3 4 5 6 7
', '6
0
2
3
', NULL),
(2007, 'PUBLISHED', 'Poisoned Dagger', 2000, 250000, 'Monocarp is playing yet another computer game. In this game, his character has to kill a dragon. The battle with the dragon lasts 100^{500} seconds, during which Monocarp attacks the dragon with a poisoned dagger. The i-th attack is performed at the beginning of the a_i-th second from the battle start. The dagger itself does not deal damage, but it applies a poison effect on the dragon, which deals 1 damage during each of the next k seconds (starting with the same second when the dragon was stabbed by the dagger). However, if the dragon has already been poisoned, then the dagger updates the poison effect (i.e. cancels the current poison effect and applies a new one).

For example, suppose k = 4, and Monocarp stabs the dragon during the seconds 2, 4 and 10. Then the poison effect is applied at the start of the 2-nd second and deals 1 damage during the 2-nd and 3-rd seconds; then, at the beginning of the 4-th second, the poison effect is reapplied, so it deals exactly 1 damage during the seconds 4, 5, 6 and 7; then, during the 10-th second, the poison effect is applied again, and it deals 1 damage during the seconds 10, 11, 12 and 13. In total, the dragon receives 10 damage.

Monocarp knows that the dragon has h hit points, and if he deals at least h damage to the dragon during the battle — he slays the dragon. Monocarp has not decided on the strength of the poison he will use during the battle, so he wants to find the minimum possible value of k (the number of seconds the poison effect lasts) that is enough to deal at least h damage to the dragon.

---
*Source: Codeforces problem, via DeepMind CodeContests (CC BY 4.0).*', 'The first line contains a single integer t (1 ≤ t ≤ 1000) — the number of test cases.

The first line of the test case contains two integers n and h (1 ≤ n ≤ 100; 1 ≤ h ≤ 10^{18}) — the number of Monocarp''s attacks and the amount of damage that needs to be dealt.

The second line contains n integers a_1, a_2, ..., a_n (1 ≤ a_i ≤ 10^9; a_i &lt; a_{i + 1}), where a_i is the second when the i-th attack is performed.', 'For each test case, print a single integer — the minimum value of the parameter k, such that Monocarp will cause at least h damage to the dragon.', '4
2 5
1 5
3 10
2 4 10
5 3
1 2 4 5 7
4 1000
3 25 64 1337
', '3
4
1
470
', NULL),
(2008, 'PUBLISHED', 'And It''s Non-Zero', 2000, 250000, 'You are given an array consisting of all integers from [l, r] inclusive. For example, if l = 2 and r = 5, the array would be [2, 3, 4, 5]. What''s the minimum number of elements you can delete to make the [bitwise AND](https://en.wikipedia.org/wiki/Bitwise_operation#AND) of the array non-zero?

A bitwise AND is a binary operation that takes two equal-length binary representations and performs the AND operation on each pair of the corresponding bits.

---
*Source: Codeforces problem, via DeepMind CodeContests (CC BY 4.0).*', 'The first line contains one integer t (1 ≤ t ≤ 10^4) — the number of test cases. Then t cases follow.

The first line of each test case contains two integers l and r (1 ≤ l ≤ r ≤ 2 ⋅ 10^5) — the description of the array.', 'For each test case, output a single integer — the answer to the problem.', '5
1 2
2 8
4 5
1 5
100000 200000
', '1
3
0
2
31072
', NULL),
(2009, 'PUBLISHED', 'Minimize Distance', 1000, 250000, 'A total of n depots are located on a number line. Depot i lies at the point x_i for 1 ≤ i ≤ n.

You are a salesman with n bags of goods, attempting to deliver one bag to each of the n depots. You and the n bags are initially at the origin 0. You can carry up to k bags at a time. You must collect the required number of goods from the origin, deliver them to the respective depots, and then return to the origin to collect your next batch of goods.

Calculate the minimum distance you need to cover to deliver all the bags of goods to the depots. You do not have to return to the origin after you have delivered all the bags.

---
*Source: Codeforces problem, via DeepMind CodeContests (CC BY 4.0).*', 'Each test contains multiple test cases. The first line contains the number of test cases t (1 ≤ t ≤ 10 500). Description of the test cases follows.

The first line of each test case contains two integers n and k (1 ≤ k ≤ n ≤ 2 ⋅ 10^5).

The second line of each test case contains n integers x_1, x_2, …, x_n (-10^9 ≤ x_i ≤ 10^9). It is possible that some depots share the same position.

It is guaranteed that the sum of n over all test cases does not exceed 2 ⋅ 10^5.', 'For each test case, output a single integer denoting the minimum distance you need to cover to deliver all the bags of goods to the depots.', '4
5 1
1 2 3 4 5
9 3
-5 -10 -15 6 5 8 3 7 4
5 3
2 2 3 3 3
4 2
1000000000 1000000000 1000000000 1000000000
', '25
41
7
3000000000
', NULL),
(2010, 'PUBLISHED', 'Rubik''s Cube Coloring (easy version)', 2000, 250000, 'It is the easy version of the problem. The difference is that in this version, there are no nodes with already chosen colors.

Theofanis is starving, and he wants to eat his favorite food, sheftalia. However, he should first finish his homework. Can you help him with this problem?

You have a perfect binary tree of 2^k - 1 nodes — a binary tree where all vertices i from 1 to 2^{k - 1} - 1 have exactly two children: vertices 2i and 2i + 1. Vertices from 2^{k - 1} to 2^k - 1 don''t have any children. You want to color its vertices with the 6 Rubik''s cube colors (White, Green, Red, Blue, Orange and Yellow).

Let''s call a coloring good when all edges connect nodes with colors that are neighboring sides in the Rubik''s cube.

(figure omitted)| (figure omitted)  
---|---  
A picture of Rubik''s cube and its 2D map.

More formally: 

  * a white node can not be neighboring with white and yellow nodes; 
  * a yellow node can not be neighboring with white and yellow nodes; 
  * a green node can not be neighboring with green and blue nodes; 
  * a blue node can not be neighboring with green and blue nodes; 
  * a red node can not be neighboring with red and orange nodes; 
  * an orange node can not be neighboring with red and orange nodes; 



You want to calculate the number of the good colorings of the binary tree. Two colorings are considered different if at least one node is colored with a different color.

The answer may be too large, so output the answer modulo 10^9+7.

---
*Source: Codeforces problem, via DeepMind CodeContests (CC BY 4.0).*', 'The first and only line contains the integers k (1 ≤ k ≤ 60) — the number of levels in the perfect binary tree you need to color.', 'Print one integer — the number of the different colorings modulo 10^9+7.', '14
', '934234
', NULL),
(2011, 'PUBLISHED', 'Jeopardy of Dropped Balls', 2000, 500000, 'Mr. Chanek has a new game called Dropping Balls. Initially, Mr. Chanek has a grid a of size n × m

Each cell (x,y) contains an integer a_{x,y} denoting the direction of how the ball will move.

  * a_{x,y}=1 — the ball will move to the right (the next cell is (x, y + 1)); 
  * a_{x,y}=2 — the ball will move to the bottom (the next cell is (x + 1, y)); 
  * a_{x,y}=3 — the ball will move to the left (the next cell is (x, y - 1)). 



Every time a ball leaves a cell (x,y), the integer a_{x,y} will change to 2. Mr. Chanek will drop k balls sequentially, each starting from the first row, and on the c_1, c_2, ..., c_k-th (1 ≤ c_i ≤ m) columns.

Determine in which column each ball will end up in (position of the ball after leaving the grid).

---
*Source: Codeforces problem, via DeepMind CodeContests (CC BY 4.0).*', 'The first line contains three integers n, m, and k (1 ≤ n, m ≤ 1000, 1 ≤ k ≤ 10^5) — the size of the grid and the number of balls dropped by Mr. Chanek.

The i-th of the next n lines contains m integers a_{i,1},a_{i,2},…,a_{i,m} (1 ≤ a_{i,j} ≤ 3). It will satisfy a_{i, 1} ≠ 3 and a_{i, m} ≠ 1.

The next line contains k integers c_1, c_2, …, c_k (1 ≤ c_i ≤ m) — the balls'' column positions dropped by Mr. Chanek sequentially.', 'Output k integers — the i-th integer denoting the column where the i-th ball will end.', '5 5 3
1 2 3 3 3
2 2 2 2 2
2 2 2 2 2
2 2 2 2 2
2 2 2 2 2
1 2 1
', '2 2 1 
', NULL),
(2012, 'PUBLISHED', 'Set or Decrease', 2000, 250000, 'You are given an integer array a_1, a_2, ..., a_n and integer k.

In one step you can 

  * either choose some index i and decrease a_i by one (make a_i = a_i - 1); 
  * or choose two indices i and j and set a_i equal to a_j (make a_i = a_j). 



What is the minimum number of steps you need to make the sum of array ∑_{i=1}^{n}{a_i} ≤ k? (You are allowed to make values of array negative).

---
*Source: Codeforces problem, via DeepMind CodeContests (CC BY 4.0).*', 'The first line contains a single integer t (1 ≤ t ≤ 10^4) — the number of test cases.

The first line of each test case contains two integers n and k (1 ≤ n ≤ 2 ⋅ 10^5; 1 ≤ k ≤ 10^{15}) — the size of array a and upper bound on its sum.

The second line of each test case contains n integers a_1, a_2, ..., a_n (1 ≤ a_i ≤ 10^9) — the array itself.

It''s guaranteed that the sum of n over all test cases doesn''t exceed 2 ⋅ 10^5.', 'For each test case, print one integer — the minimum number of steps to make ∑_{i=1}^{n}{a_i} ≤ k.', '4
1 10
20
2 69
6 9
7 8
1 2 1 3 1 2 1
10 1
1 2 3 1 2 6 1 6 8 10
', '10
0
2
7
', NULL),
(2013, 'PUBLISHED', 'Array Stabilization (AND version)', 2000, 250000, 'You are given an array a[0 … n - 1] = [a_0, a_1, …, a_{n - 1}] of zeroes and ones only. Note that in this problem, unlike the others, the array indexes are numbered from zero, not from one.

In one step, the array a is replaced by another array of length n according to the following rules: 

  1. First, a new array a^{→ d} is defined as a cyclic shift of the array a to the right by d cells. The elements of this array can be defined as a^{→ d}_i = a_{(i + n - d) mod n}, where (i + n - d) mod n is the remainder of integer division of i + n - d by n. 

It means that the whole array a^{→ d} can be represented as a sequence $$$a^{→ d} = [a_{n - d}, a_{n - d + 1}, …, a_{n - 1}, a_0, a_1, …, a_{n - d - 1}]$$$

  2. Then each element of the array a_i is replaced by a_i  \\&amp;  a^{→ d}_i, where \\&amp; is a logical "AND" operator. 



For example, if a = [0, 0, 1, 1] and d = 1, then a^{→ d} = [1, 0, 0, 1] and the value of a after the first step will be [0  \\&amp;  1, 0  \\&amp;  0, 1  \\&amp;  0, 1  \\&amp;  1], that is [0, 0, 0, 1].

The process ends when the array stops changing. For a given array a, determine whether it will consist of only zeros at the end of the process. If yes, also find the number of steps the process will take before it finishes.

---
*Source: Codeforces problem, via DeepMind CodeContests (CC BY 4.0).*', 'The first line contains an integer t (1 ≤ t ≤ 1000) — the number of test cases.

The next 2t lines contain descriptions of the test cases. 

The first line of each test case description contains two integers: n (1 ≤ n ≤ 10^6) — array size and d (1 ≤ d ≤ n) — cyclic shift offset. The second line of the description contains n space-separated integers a_i (0 ≤ a_i ≤ 1) — elements of the array.

It is guaranteed that the sum of n over all test cases does not exceed 10^6.', 'Print t lines, each line containing the answer to the corresponding test case. The answer to a test case should be a single integer — the number of steps after which the array will contain only zeros for the first time. If there are still elements equal to 1 in the array after the end of the process, print -1.', '5
2 1
0 1
3 2
0 1 0
5 2
1 1 0 1 0
4 2
0 1 0 1
1 1
0
', '1
1
3
-1
0
', NULL),
(2014, 'PUBLISHED', 'MEX and Increments', 2000, 250000, 'Dmitry has an array of n non-negative integers a_1, a_2, ..., a_n.

In one operation, Dmitry can choose any index j (1 ≤ j ≤ n) and increase the value of the element a_j by 1. He can choose the same index j multiple times.

For each i from 0 to n, determine whether Dmitry can make the MEX of the array equal to exactly i. If it is possible, then determine the minimum number of operations to do it.

The MEX of the array is equal to the minimum non-negative integer that is not in the array. For example, the MEX of the array [3, 1, 0] is equal to 2, and the array [3, 3, 1, 4] is equal to 0.

---
*Source: Codeforces problem, via DeepMind CodeContests (CC BY 4.0).*', 'The first line of input data contains a single integer t (1 ≤ t ≤ 10^4) — the number of test cases in the input. 

The descriptions of the test cases follow.

The first line of the description of each test case contains a single integer n (1 ≤ n ≤ 2 ⋅ 10^5) — the length of the array a.

The second line of the description of each test case contains n integers a_1, a_2, ..., a_n (0 ≤ a_i ≤ n) — elements of the array a.

It is guaranteed that the sum of the values n over all test cases in the test does not exceed 2⋅10^5.', 'For each test case, output n + 1 integer — i-th number is equal to the minimum number of operations for which you can make the array MEX equal to i (0 ≤ i ≤ n), or -1 if this cannot be done.', '5
3
0 1 3
7
0 1 2 3 4 3 2
4
3 0 0 0
7
4 6 2 3 5 0 5
5
4 0 1 0 4
', '1 1 0 -1 
1 1 2 2 1 0 2 6 
3 0 1 4 3 
1 0 -1 -1 -1 -1 -1 -1 
2 1 0 2 -1 -1 
', NULL),
(2015, 'PUBLISHED', 'The Number of Imposters', 3000, 250000, 'Theofanis started playing the new online game called "Among them". However, he always plays with Cypriot players, and they all have the same name: "Andreas" (the most common name in Cyprus).

In each game, Theofanis plays with n other players. Since they all have the same name, they are numbered from 1 to n.

The players write m comments in the chat. A comment has the structure of "i j c" where i and j are two distinct integers and c is a string (1 ≤ i, j ≤ n; i ≠ j; c is either imposter or crewmate). The comment means that player i said that player j has the role c.

An imposter always lies, and a crewmate always tells the truth. 

Help Theofanis find the maximum possible number of imposters among all the other Cypriot players, or determine that the comments contradict each other (see the notes for further explanation).

Note that each player has exactly one role: either imposter or crewmate.

---
*Source: Codeforces problem, via DeepMind CodeContests (CC BY 4.0).*', 'The first line contains a single integer t (1 ≤ t ≤ 10^4) — the number of test cases. Description of each test case follows.

The first line of each test case contains two integers n and m (1 ≤ n ≤ 2 ⋅ 10^5; 0 ≤ m ≤ 5 ⋅ 10^5) — the number of players except Theofanis and the number of comments.

Each of the next m lines contains a comment made by the players of the structure "i j c" where i and j are two distinct integers and c is a string (1 ≤ i, j ≤ n; i ≠ j; c is either imposter or crewmate).

There can be multiple comments for the same pair of (i, j).

It is guaranteed that the sum of all n does not exceed 2 ⋅ 10^5 and the sum of all m does not exceed 5 ⋅ 10^5.', 'For each test case, print one integer — the maximum possible number of imposters. If the comments contradict each other, print -1.', '5
3 2
1 2 imposter
2 3 crewmate
5 4
1 3 crewmate
2 5 crewmate
2 4 imposter
3 4 imposter
2 2
1 2 imposter
2 1 crewmate
3 5
1 2 imposter
1 2 imposter
3 2 crewmate
3 2 crewmate
1 3 imposter
5 0
', '2
4
-1
2
5
', NULL),
(2016, 'PUBLISHED', 'BA-String', 2000, 250000, 'You are given an integer k and a string s that consists only of characters ''a'' (a lowercase Latin letter) and ''*'' (an asterisk).

Each asterisk should be replaced with several (from 0 to k inclusive) lowercase Latin letters ''b''. Different asterisk can be replaced with different counts of letter ''b''.

The result of the replacement is called a BA-string.

Two strings a and b are different if they either have different lengths or there exists such a position i that a_i ≠ b_i.

A string a is lexicographically smaller than a string b if and only if one of the following holds: 

  * a is a prefix of b, but a ≠ b; 
  * in the first position where a and b differ, the string a has a letter that appears earlier in the alphabet than the corresponding letter in b. 



Now consider all different BA-strings and find the x-th lexicographically smallest of them.

---
*Source: Codeforces problem, via DeepMind CodeContests (CC BY 4.0).*', 'The first line contains a single integer t (1 ≤ t ≤ 2000) — the number of testcases.

The first line of each testcase contains three integers n, k and x (1 ≤ n ≤ 2000; 0 ≤ k ≤ 2000; 1 ≤ x ≤ 10^{18}). n is the length of string s.

The second line of each testcase is a string s. It consists of n characters, each of them is either ''a'' (a lowercase Latin letter) or ''*'' (an asterisk).

The sum of n over all testcases doesn''t exceed 2000. For each testcase x doesn''t exceed the total number of different BA-strings. String s contains at least one character ''a''.', 'For each testcase, print a single string, consisting only of characters ''b'' and ''a'' (lowercase Latin letters) — the x-th lexicographically smallest BA-string.', '3
2 4 3
a*
4 1 3
a**a
6 3 20
**a***
', 'abb
abba
babbbbbbbbb
', NULL),
(2017, 'PUBLISHED', 'Crazy Robot', 2000, 500000, 'There is a grid, consisting of n rows and m columns. Each cell of the grid is either free or blocked. One of the free cells contains a lab. All the cells beyond the borders of the grid are also blocked.

A crazy robot has escaped from this lab. It is currently in some free cell of the grid. You can send one of the following commands to the robot: "move right", "move down", "move left" or "move up". Each command means moving to a neighbouring cell in the corresponding direction.

However, as the robot is crazy, it will do anything except following the command. Upon receiving a command, it will choose a direction such that it differs from the one in command and the cell in that direction is not blocked. If there is such a direction, then it will move to a neighbouring cell in that direction. Otherwise, it will do nothing.

We want to get the robot to the lab to get it fixed. For each free cell, determine if the robot can be forced to reach the lab starting in this cell. That is, after each step of the robot a command can be sent to a robot such that no matter what different directions the robot chooses, it will end up in a lab.

---
*Source: Codeforces problem, via DeepMind CodeContests (CC BY 4.0).*', 'The first line contains a single integer t (1 ≤ t ≤ 1000) — the number of testcases.

The first line of each testcase contains two integers n and m (1 ≤ n, m ≤ 10^6; n ⋅ m ≤ 10^6) — the number of rows and the number of columns in the grid.

The i-th of the next n lines provides a description of the i-th row of the grid. It consists of m elements of one of three types: 

  * ''.'' — the cell is free; 
  * ''#'' — the cell is blocked; 
  * ''L'' — the cell contains a lab. 



The grid contains exactly one lab. The sum of n ⋅ m over all testcases doesn''t exceed 10^6.', 'For each testcase find the free cells that the robot can be forced to reach the lab from. Given the grid, replace the free cells (marked with a dot) with a plus sign (''+'') for the cells that the robot can be forced to reach the lab from. Print the resulting grid.', '4
3 3
...
.L.
...
4 5
#....
..##L
...#.
.....
1 1
L
1 9
....L..#.
', '...
.L.
...
#++++
..##L
...#+
...++
L
++++L++#.
', NULL),
(2018, 'PUBLISHED', 'Arena', 3000, 250000, 'There are n heroes fighting in the arena. Initially, the i-th hero has a_i health points.

The fight in the arena takes place in several rounds. At the beginning of each round, each alive hero deals 1 damage to all other heroes. Hits of all heroes occur simultaneously. Heroes whose health is less than 1 at the end of the round are considered killed.

If exactly 1 hero remains alive after a certain round, then he is declared the winner. Otherwise, there is no winner.

Your task is to calculate the number of ways to choose the initial health points for each hero a_i, where 1 ≤ a_i ≤ x, so that there is no winner of the fight. The number of ways can be very large, so print it modulo 998244353. Two ways are considered different if at least one hero has a different amount of health. For example, [1, 2, 1] and [2, 1, 1] are different.

---
*Source: Codeforces problem, via DeepMind CodeContests (CC BY 4.0).*', 'The only line contains two integers n and x (2 ≤ n ≤ 500; 1 ≤ x ≤ 500).', 'Print one integer — the number of ways to choose the initial health points for each hero a_i, where 1 ≤ a_i ≤ x, so that there is no winner of the fight, taken modulo 998244353.', '5 4
', '1024
', NULL),
(2019, 'PUBLISHED', 'Minimal Coverage', 1000, 250000, 'You are given n lengths of segments that need to be placed on an infinite axis with coordinates.

The first segment is placed on the axis so that one of its endpoints lies at the point with coordinate 0. Let''s call this endpoint the "start" of the first segment and let''s call its "end" as that endpoint that is not the start. 

The "start" of each following segment must coincide with the "end" of the previous one. Thus, if the length of the next segment is d and the "end" of the previous one has the coordinate x, the segment can be placed either on the coordinates [x-d, x], and then the coordinate of its "end" is x - d, or on the coordinates [x, x+d], in which case its "end" coordinate is x + d.

The total coverage of the axis by these segments is defined as their overall union which is basically the set of points covered by at least one of the segments. It''s easy to show that the coverage will also be a segment on the axis. Determine the minimal possible length of the coverage that can be obtained by placing all the segments on the axis without changing their order.

---
*Source: Codeforces problem, via DeepMind CodeContests (CC BY 4.0).*', 'The first line contains an integer t (1 ≤ t ≤ 1000) — the number of test cases.

The next 2t lines contain descriptions of the test cases. 

The first line of each test case description contains an integer n (1 ≤ n ≤ 10^4) — the number of segments. The second line of the description contains n space-separated integers a_i (1 ≤ a_i ≤ 1000) — lengths of the segments in the same order they should be placed on the axis.

It is guaranteed that the sum of n over all test cases does not exceed 10^4.', 'Print t lines, each line containing the answer to the corresponding test case. The answer to a test case should be a single integer — the minimal possible length of the axis coverage.', '6
2
1 3
3
1 2 3
4
6 2 3 9
4
6 8 4 5
7
1 2 4 6 7 7 3
8
8 6 5 1 2 2 3 6
', '3
3
9
9
7
8
', NULL),
(2020, 'PUBLISHED', 'Illusions of the Desert', 3000, 500000, 'Chanek Jones is back, helping his long-lost relative Indiana Jones, to find a secret treasure in a maze buried below a desert full of illusions.

The map of the labyrinth forms a tree with n rooms numbered from 1 to n and n - 1 tunnels connecting them such that it is possible to travel between each pair of rooms through several tunnels.

The i-th room (1 ≤ i ≤ n) has a_i illusion rate. To go from the x-th room to the y-th room, there must exist a tunnel between x and y, and it takes max(|a_x + a_y|, |a_x - a_y|) energy. |z| denotes the absolute value of z.

To prevent grave robbers, the maze can change the illusion rate of any room in it. Chanek and Indiana would ask q queries.

There are two types of queries to be done:

  * 1\\ u\\ c — The illusion rate of the x-th room is changed to c (1 ≤ u ≤ n, 0 ≤ |c| ≤ 10^9). 
  * 2\\ u\\ v — Chanek and Indiana ask you the minimum sum of energy needed to take the secret treasure at room v if they are initially at room u (1 ≤ u, v ≤ n). 



Help them, so you can get a portion of the treasure!

---
*Source: Codeforces problem, via DeepMind CodeContests (CC BY 4.0).*', 'The first line contains two integers n and q (2 ≤ n ≤ 10^5, 1 ≤ q ≤ 10^5) — the number of rooms in the maze and the number of queries.

The second line contains n integers a_1, a_2, …, a_n (0 ≤ |a_i| ≤ 10^9) — inital illusion rate of each room.

The i-th of the next n-1 lines contains two integers s_i and t_i (1 ≤ s_i, t_i ≤ n), meaning there is a tunnel connecting s_i-th room and t_i-th room. The given edges form a tree.

The next q lines contain the query as described. The given queries are valid.', 'For each type 2 query, output a line containing an integer — the minimum sum of energy needed for Chanek and Indiana to take the secret treasure.', '6 4
10 -9 2 -1 4 -6
1 5
5 4
5 6
6 2
6 3
2 1 2
1 1 -3
2 1 2
2 3 3
', '39
32
0
', NULL),
(2021, 'PUBLISHED', 'Non-equal Neighbours', 3000, 500000, 'You are given an array of n positive integers a_1, a_2, …, a_n. Your task is to calculate the number of arrays of n positive integers b_1, b_2, …, b_n such that: 

  * 1 ≤ b_i ≤ a_i for every i (1 ≤ i ≤ n), and 
  * b_i ≠ b_{i+1} for every i (1 ≤ i ≤ n - 1). 



The number of such arrays can be very large, so print it modulo 998 244 353.

---
*Source: Codeforces problem, via DeepMind CodeContests (CC BY 4.0).*', 'The first line contains a single integer n (1 ≤ n ≤ 2 ⋅ 10^5) — the length of the array a.

The second line contains n integers a_1, a_2, …, a_n (1 ≤ a_i ≤ 10^9).', 'Print the answer modulo 998 244 353 in a single line.', '3
1 1 1
', '0', NULL),
(2022, 'PUBLISHED', 'Middle Duplication', 1000, 250000, 'A binary tree of n nodes is given. Nodes of the tree are numbered from 1 to n and the root is the node 1. Each node can have no child, only one left child, only one right child, or both children. For convenience, let''s denote l_u and r_u as the left and the right child of the node u respectively, l_u = 0 if u does not have the left child, and r_u = 0 if the node u does not have the right child.

Each node has a string label, initially is a single character c_u. Let''s define the string representation of the binary tree as the concatenation of the labels of the nodes in the in-order. Formally, let f(u) be the string representation of the tree rooted at the node u. f(u) is defined as follows: $$$ f(u) = \\begin{cases} &lt;empty string&gt;, &amp; if u = 0; \\\\\\ f(l_u) + c_u + f(r_u) &amp; otherwise, \\end{cases}  where +$$$ denotes the string concatenation operation.

This way, the string representation of the tree is f(1).

For each node, we can duplicate its label at most once, that is, assign c_u with c_u + c_u, but only if u is the root of the tree, or if its parent also has its label duplicated.

You are given the tree and an integer k. What is the lexicographically smallest string representation of the tree, if we can duplicate labels of at most k nodes?

A string a is lexicographically smaller than a string b if and only if one of the following holds: 

  * a is a prefix of b, but a ≠ b; 
  * in the first position where a and b differ, the string a has a letter that appears earlier in the alphabet than the corresponding letter in b.

---
*Source: Codeforces problem, via DeepMind CodeContests (CC BY 4.0).*', 'The first line contains two integers n and k (1 ≤ k ≤ n ≤ 2 ⋅ 10^5).

The second line contains a string c of n lower-case English letters, where c_i is the initial label of the node i for 1 ≤ i ≤ n. Note that the given string c is not the initial string representation of the tree.

The i-th of the next n lines contains two integers l_i and r_i (0 ≤ l_i, r_i ≤ n). If the node i does not have the left child, l_i = 0, and if the node i does not have the right child, r_i = 0.

It is guaranteed that the given input forms a binary tree, rooted at 1.', 'Print a single line, containing the lexicographically smallest string representation of the tree if at most k nodes have their labels duplicated.', '4 3
abab
2 3
0 0
0 4
0 0
', 'baaaab', NULL),
(2023, 'PUBLISHED', 'Weights', 1000, 250000, 'You are given an array A of length N weights of masses A_1, A_2...A_N. No two weights have the same mass. You can put every weight on one side of the balance (left or right). You don''t have to put weights in order A_1,...,A_N. There is also a string S consisting of characters "L" and "R", meaning that after putting the i-th weight (not A_i, but i-th weight of your choice) left or right side of the balance should be heavier. Find the order of putting the weights on the balance such that rules of string S are satisfied.

---
*Source: Codeforces problem, via DeepMind CodeContests (CC BY 4.0).*', 'The first line contains one integer N (1 ≤ N ≤ 2*10^5) - the length of the array A The second line contains N distinct integers: A_1, A_2,...,A_N (1 ≤ A_i ≤ 10^9) - the weights given The third line contains string S of length N consisting only of letters "L" and "R" - string determining which side of the balance should be heavier after putting the i-th weight of your choice', 'The output contains N lines. In every line, you should print one integer and one letter - integer representing the weight you are putting on the balance in that move and the letter representing the side of the balance where you are putting the weight. If there is no solution, print -1.', '5
3 8 2 13 7
LLRLL
', '7 L
3 R
8 R
13 L
2 L
', NULL);
UPDATE `voj_problems` SET `problem_difficulty_id` = 1 WHERE `problem_id` IN (2000, 2001, 2002, 2003, 2004, 2005, 2006, 2007);
UPDATE `voj_problems` SET `problem_difficulty_id` = 2 WHERE `problem_id` IN (2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015);
UPDATE `voj_problems` SET `problem_difficulty_id` = 3 WHERE `problem_id` IN (2016, 2017, 2018, 2019, 2020, 2021, 2022, 2023);
INSERT INTO `voj_problem_checkpoints` (`problem_id`, `checkpoint_id`, `checkpoint_exactly_match`, `checkpoint_score`, `checkpoint_input`, `checkpoint_output`) VALUES
(2000, 0, 0, 10, '4
1
2
9
91234
', '1
12
830455698
890287984
'),
(2000, 1, 0, 10, '1
100000
', '553573229
'),
(2000, 2, 0, 10, '2
99997
3
', '979296788
360
'),
(2000, 3, 0, 10, '2
99997
4
', '979296788
20160
'),
(2000, 4, 0, 10, '4
1
2
3
91234
', '1
12
360
890287984
'),
(2000, 5, 0, 10, '4
1
2
15
91234
', '1
12
554680740
890287984
'),
(2000, 6, 0, 10, '4
2
2
9
91234
', '12
12
830455698
890287984
'),
(2000, 7, 0, 10, '8
48
2
44
409
1
53950
103
1
', '882862985
12
593679722
685602584
1
77761031
346887392
1
'),
(2000, 8, 0, 10, '8
48
2
44
212
1
98869
822
1
', '882862985
12
593679722
904840727
1
299753863
710075077
1
'),
(2000, 9, 0, 10, '8
48
2
44
212
1
98869
103
1
', '882862985
12
593679722
904840727
1
299753863
346887392
1
'),
(2001, 0, 0, 10, '4
EEE
EN
ENNEENE
NENN
', 'YES
NO
YES
YES
'),
(2001, 1, 0, 10, '2
EEEEEN
EE
', 'NO
YES
'),
(2001, 2, 0, 10, '2
EENEEE
EE
', 'NO
YES
'),
(2001, 3, 0, 10, '2
EEEEEN
EEEEEN
', 'NO
NO
'),
(2001, 4, 0, 10, '2
NEEEEE
EEEEEN
', 'NO
NO
'),
(2001, 5, 0, 10, '2
EEEEEEN
EEEEEEEN
', 'NO
NO
'),
(2001, 6, 0, 10, '2
NEEEEEE
EEEEEEEN
', 'NO
NO
'),
(2001, 7, 0, 10, '2
NEEEEEE
NEEEEEEE
', 'NO
NO
'),
(2001, 8, 0, 10, '2
EEEEENE
NEEEEEEE
', 'NO
NO
'),
(2001, 9, 0, 10, '2
EEEEENE
EEEEEEEN
', 'NO
NO
'),
(2002, 0, 0, 10, '5
abcdefghijklmnopqrstuvwxyz
hello
abcdefghijklmnopqrstuvwxyz
i
abcdefghijklmnopqrstuvwxyz
codeforces
qwertyuiopasdfghjklzxcvbnm
qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq
qwertyuiopasdfghjklzxcvbnm
abacaba
', '13
0
68
0
74
'),
(2002, 1, 0, 10, '1
abcdefghilkjmnopqrstuvwxyz
abdes
', '18
'),
(2002, 2, 0, 10, '1
abcdefyhilkjmnopqrstuvwxgz
abdes
', '18
'),
(2002, 3, 0, 10, '1
zhxwvutsrqponmjklihyfedcba
asdeb
', '37
'),
(2002, 4, 0, 10, '1
zhxwvutsrqponmjklihyfedcba
aseeb
', '35
'),
(2002, 5, 0, 10, '1
zhxwvutsrqponmjklihyfedcba
bbdes
', '17
'),
(2002, 6, 0, 10, '1
zhxwvutsrqponmjklihyfedcba
saeeb
', '25
'),
(2002, 7, 0, 10, '1
abcdefjhilkglnopqrstuvwxyz
abdet
', '19
'),
(2002, 8, 0, 10, '1
aycdefbhilkjmnopqrstuvwxgz
sfdba
', '24
'),
(2002, 9, 0, 10, '1
zhxwvutsrqponmjklihyfedcba
sadfb
', '27
'),
(2003, 0, 0, 10, '10
a
aa
aaa
aaaa
abab
abcabc
abacaba
xxyy
xyyx
xyxy
', 'NO
YES
NO
YES
YES
YES
NO
NO
NO
YES
'),
(2003, 1, 0, 10, '1
z{
', 'NO
'),
(2003, 2, 0, 10, '1
{|
', 'NO
'),
(2003, 3, 0, 10, '1
|{
', 'NO
'),
(2003, 4, 0, 10, '1
z|
', 'NO
'),
(2003, 5, 0, 10, '1
|z
', 'NO
'),
(2003, 6, 0, 10, '1
z}
', 'NO
'),
(2003, 7, 0, 10, '1
}z
', 'NO
'),
(2003, 8, 0, 10, '1
{}
', 'NO
'),
(2003, 9, 0, 10, '1
{z
', 'NO
'),
(2004, 0, 0, 10, '5
2 1
19 0
10 2
100 1
1999 0
2 3
1 0
1 0
99 0
1 2
', '>
=
<
=
<
'),
(2004, 1, 0, 10, '1
7 0
1 1
', '<
'),
(2004, 2, 0, 10, '1
7 0
1 2
', '<
'),
(2004, 3, 0, 10, '1
7 0
2 2
', '<
'),
(2004, 4, 0, 10, '1
15 0
1 1
', '>
'),
(2004, 5, 0, 10, '1
1 5
101 3
', '<
'),
(2004, 6, 0, 10, '1
105 0
1 2
', '>
'),
(2004, 7, 0, 10, '1
402 2
4 4
', '>
'),
(2004, 8, 0, 10, '1
4 4
402 2
', '<
'),
(2004, 9, 0, 10, '1
2 5
101 3
', '>
'),
(2005, 0, 0, 10, '2
4
1 0 0 1 0
0 1 0 0 1
0 0 0 1 0
0 1 0 1 0
2
0 0 0 1 0
0 0 0 1 0
', 'YES
NO
'),
(2005, 1, 0, 10, '2
4
1 0 0 1 0
1 1 0 0 1
0 0 0 0 0
0 1 0 1 0
2
0 0 0 0 0
0 0 0 1 1
', 'NO
NO
'),
(2005, 2, 0, 10, '2
4
1 0 0 1 0
1 1 0 0 1
0 0 0 0 0
0 1 0 1 0
2
0 0 0 0 0
0 0 0 1 0
', 'NO
NO
'),
(2005, 3, 0, 10, '2
4
0 0 0 1 0
1 1 0 0 1
0 0 0 0 0
0 1 0 1 0
2
0 0 0 0 0
0 0 0 1 1
', 'NO
NO
'),
(2005, 4, 0, 10, '2
4
1 0 0 1 0
0 0 0 0 1
0 1 0 1 0
0 1 0 0 0
2
0 0 0 0 0
0 0 1 1 1
', 'NO
NO
'),
(2005, 5, 0, 10, '2
4
1 0 0 1 0
1 0 0 0 1
0 0 0 0 0
0 1 0 1 0
2
0 0 0 0 0
0 0 0 1 0
', 'NO
NO
'),
(2005, 6, 0, 10, '2
4
0 0 0 1 0
1 1 0 0 1
0 0 0 0 0
0 0 0 1 0
2
0 0 0 0 0
0 0 0 1 1
', 'NO
NO
'),
(2005, 7, 0, 10, '2
4
1 0 0 1 0
0 0 0 0 1
0 1 0 1 0
0 0 0 0 0
2
0 0 0 0 0
0 0 1 1 1
', 'NO
NO
'),
(2005, 8, 0, 10, '2
4
1 0 0 0 0
0 1 0 0 1
0 0 0 1 0
0 1 0 1 0
2
0 0 0 0 0
0 0 0 1 0
', 'NO
NO
'),
(2005, 9, 0, 10, '2
4
1 0 0 1 0
0 1 1 0 1
0 0 0 1 0
1 0 0 1 0
2
0 0 0 0 0
0 0 0 1 0
', 'NO
NO
'),
(2006, 0, 0, 10, '4
4
8 8 8 8
3
50 20 10
5
1 4 7 3 5
7
1 2 3 4 5 6 7
', '6
0
2
3
'),
(2006, 1, 0, 10, '1
1
228 1337 3
', '0
'),
(2006, 2, 0, 10, '1
1
228 1512 3
', '0
'),
(2006, 3, 0, 10, '1
1
228 2063 3
', '0
'),
(2006, 4, 0, 10, '1
1
228 2063 2
', '0
'),
(2006, 5, 0, 10, '1
1
342 1512 3
', '0
'),
(2006, 6, 0, 10, '1
1
228 2063 0
', '0
'),
(2006, 7, 0, 10, '1
1
228 4021 2
', '0
'),
(2006, 8, 0, 10, '1
3
40 270 228
', '0
'),
(2006, 9, 0, 10, '1
1
228 4021 1
', '0
'),
(2007, 0, 0, 10, '4
2 5
1 5
3 10
2 4 10
5 3
1 2 4 5 7
4 1000
3 25 64 1337
', '3
4
1
470
'),
(2007, 1, 0, 10, '1
2 0000000000000000010
1000100 1001110000
', '5
'),
(2007, 2, 0, 10, '1
2 0000000000000001010
1000100 1001110000
', '505
'),
(2007, 3, 0, 10, '1
2 0000000000000100000
1000100 1000110000
', '50000
'),
(2007, 4, 0, 10, '1
2 0000000000000100010
1000100 1001110000
', '50005
'),
(2007, 5, 0, 10, '1
2 0000000000000101010
1000100 1001110000
', '50505
'),
(2007, 6, 0, 10, '1
2 0000000000000101000
1100100 1000000000
', '50500
'),
(2007, 7, 0, 10, '1
1 1000000000000000000
1000000
', '1000000000000000000
'),
(2007, 8, 0, 10, '1
2 0000000001000100000
1000100 1001110000
', '500050000
'),
(2007, 9, 0, 10, '1
1 1000000000000000000
1000000000
', '1000000000000000000
'),
(2008, 0, 0, 10, '5
1 2
2 8
4 5
1 5
100000 200000
', '1
3
0
2
31072
'),
(2008, 1, 0, 10, '5
1 2
2 8
4 5
4 5
100000 104325
', '1
3
0
0
0
'),
(2008, 2, 0, 10, '5
2 4
2 5
4 5
2 7
100100 123776
', '1
2
0
2
0
'),
(2008, 3, 0, 10, '5
2 2
2 8
4 5
2 5
100000 101573
', '0
3
0
2
0
'),
(2008, 4, 0, 10, '5
4 4
2 5
4 5
2 7
100100 123776
', '0
2
0
2
0
'),
(2008, 5, 0, 10, '5
2 2
2 5
3 6
2 4
100101 108244
', '0
2
1
1
0
'),
(2008, 6, 0, 10, '5
2 2
2 8
3 5
2 5
100000 101573
', '0
3
1
2
0
'),
(2008, 7, 0, 10, '5
2 2
2 5
3 5
2 5
101000 129464
', '0
2
1
2
0
'),
(2008, 8, 0, 10, '5
2 2
2 5
3 9
2 5
101000 129464
', '0
2
3
2
0
'),
(2008, 9, 0, 10, '5
1 2
2 10
4 5
1 5
100000 121445
', '1
4
0
2
0
'),
(2009, 0, 0, 10, '4
5 1
1 2 3 4 5
9 3
-5 -10 -15 6 5 8 3 7 4
5 3
2 2 3 3 3
4 2
1000000000 1000000000 1000000000 1000000000
', '25
41
7
3000000000
'),
(2009, 1, 0, 10, '4
5 1
1 1 3 4 1
9 3
-9 -6 -6 6 5 8 3 7 4
5 3
1 1 6 3 4
4 2
1000000000 1000000000 1000000000 1010100000
', '16
35
8
3010100000
'),
(2009, 2, 0, 10, '4
5 2
1 1 3 1 1
9 3
-9 -22 -6 6 6 0 3 7 4
5 3
1 3 6 0 4
4 2
1000000010 1000000000 1000000000 1000000000
', '7
44
8
3000000010
'),
(2009, 3, 0, 10, '4
5 1
1 1 3 4 1
9 3
-9 -18 -6 6 6 8 3 7 4
5 3
1 1 6 3 4
4 2
1000000000 1000000000 1000000000 1000000000
', '16
46
8
3000000000
'),
(2009, 4, 0, 10, '4
5 1
1 2 3 4 5
9 3
-9 -4 -15 6 5 8 3 7 4
5 3
2 2 5 3 3
4 2
1000000000 1000100000 1000000000 1000000000
', '25
41
9
3000100000
'),
(2009, 5, 0, 10, '4
5 1
1 0 3 4 5
9 3
-9 -18 -1 6 6 8 3 7 4
5 3
1 2 5 3 2
4 2
1000000000 1000000000 1000000000 1000000000
', '21
46
9
3000000000
'),
(2009, 6, 0, 10, '4
5 1
1 1 3 4 1
9 3
-9 -18 -6 6 5 8 3 7 4
5 3
1 1 6 3 4
4 2
1000000000 1000000000 1000000000 1000000000
', '16
44
8
3000000000
'),
(2009, 7, 0, 10, '4
5 1
1 1 3 4 1
9 3
-9 -22 -6 6 6 8 3 7 4
5 3
1 3 6 0 4
4 2
1000000010 1000000000 1000000000 1000000000
', '16
50
8
3000000010
'),
(2009, 8, 0, 10, '4
5 1
1 1 3 4 1
9 3
-9 -18 -6 6 5 8 3 7 4
5 3
1 1 6 3 4
4 2
1000000000 1000000000 1000000000 1010000000
', '16
44
8
3010000000
'),
(2009, 9, 0, 10, '4
5 1
1 1 3 4 1
9 3
-9 -22 -6 6 6 0 3 7 4
5 3
1 3 6 0 4
4 2
1000000010 1000000000 1000000000 1000000000
', '16
44
8
3000000010
'),
(2010, 0, 0, 10, '14
', '934234
'),
(2010, 1, 0, 10, '1
', '6
'),
(2010, 2, 0, 10, '2
', '96
'),
(2010, 3, 0, 10, '001
', '6
'),
(2010, 4, 0, 10, '3
', '24576
'),
(2010, 5, 0, 10, '7
', '47316973
'),
(2010, 6, 0, 10, '20
', '61556388
'),
(2010, 7, 0, 10, '4
', '610612729
'),
(2010, 8, 0, 10, '5
', '218379003
'),
(2010, 9, 0, 10, '6
', '979862110
'),
(2011, 0, 0, 10, '5 5 3
1 2 3 3 3
2 2 2 2 2
2 2 2 2 2
2 2 2 2 2
2 2 2 2 2
1 2 1
', '2 2 1 
'),
(2011, 1, 0, 10, '1 2 2
2 3
1 2
', '1 1
'),
(2011, 2, 0, 10, '1 2 2
1 3
1 2
', '1 2 
'),
(2011, 3, 0, 10, '5 5 3
1 2 3 3 3
2 2 2 2 2
2 2 3 2 2
2 2 2 2 2
2 2 2 2 2
1 2 1
', '2 2 1
'),
(2011, 4, 0, 10, '5 5 3
1 2 3 3 3
2 2 2 2 2
1 2 3 2 2
2 2 2 2 2
2 2 2 2 2
1 2 1
', '2 2 2
'),
(2011, 5, 0, 10, '5 5 3
1 2 3 3 3
1 2 2 2 2
1 2 3 1 2
2 1 2 2 2
2 2 2 2 2
1 2 1
', '3 2 2
'),
(2011, 6, 0, 10, '5 5 3
1 2 3 3 4
2 2 2 2 2
1 2 3 2 2
2 2 2 3 2
2 3 2 1 2
1 2 1
', '1 2 2
'),
(2011, 7, 0, 10, '5 5 3
1 1 3 3 3
2 2 2 2 2
2 2 3 2 2
2 1 2 2 2
2 2 2 2 2
1 2 1
', '3 2 1
'),
(2011, 8, 0, 10, '5 5 3
2 2 3 3 3
1 2 2 2 2
1 2 3 1 2
2 1 1 2 2
2 2 2 2 2
1 2 1
', '4 2 2
'),
(2011, 9, 0, 10, '5 5 3
1 3 3 3 4
2 2 2 2 2
1 2 3 2 2
2 2 2 3 2
2 3 2 1 2
1 3 1
', '1 2 1
'),
(2012, 0, 0, 10, '4
1 10
20
2 69
6 9
7 8
1 2 1 3 1 2 1
10 1
1 2 3 1 2 6 1 6 8 10
', '10
0
2
7
'),
(2012, 1, 0, 10, '1
15 11
1 1 4 4 4 4 2 2 5 4 4 4 1 5 1
', '8
'),
(2012, 2, 0, 10, '1
15 11
1 1 4 4 4 4 2 0 5 4 4 4 1 5 1
', '7
'),
(2012, 3, 0, 10, '1
15 11
1 1 4 4 4 8 2 0 5 4 4 4 1 5 1
', '7
'),
(2012, 4, 0, 10, '1
15 11
1 1 4 4 4 8 2 0 5 4 4 4 1 7 1
', '7
'),
(2012, 5, 0, 10, '1
15 11
1 1 4 3 4 8 2 0 5 4 4 4 1 7 1
', '7
'),
(2012, 6, 0, 10, '1
15 11
1 1 4 3 4 8 2 0 5 4 4 4 2 7 1
', '7
'),
(2012, 7, 0, 10, '1
15 11
1 1 4 3 4 9 2 0 5 4 4 4 2 7 1
', '7
'),
(2012, 8, 0, 10, '1
15 11
1 1 4 3 6 9 2 0 5 4 4 4 2 7 1
', '7
'),
(2012, 9, 0, 10, '4
1 8
38
2 69
7 9
7 8
3 4 1 1 1 0 1
10 1
0 2 1 1 2 6 1 9 8 3
', '30
0
1
6
'),
(2013, 0, 0, 10, '5
2 1
0 1
3 2
0 1 0
5 2
1 1 0 1 0
4 2
0 1 0 1
1 1
0
', '1
1
3
-1
0
'),
(2013, 1, 0, 10, '5
2 1
0 1
3 2
0 1 0
5 2
1 1 0 0 0
4 3
0 1 0 1
1 1
0
', '1
1
1
1
0
'),
(2013, 2, 0, 10, '5
2 1
0 1
3 2
0 1 1
5 2
1 1 0 0 0
4 3
0 1 0 1
1 1
0
', '1
2
1
1
0
'),
(2013, 3, 0, 10, '5
2 1
0 1
3 2
0 1 0
5 2
1 1 1 0 0
4 3
0 1 0 0
1 1
0
', '1
1
2
1
0
'),
(2013, 4, 0, 10, '5
2 1
0 0
3 2
0 1 0
5 2
1 1 1 1 0
4 1
0 1 0 1
1 1
0
', '0
1
4
1
0
'),
(2013, 5, 0, 10, '5
2 1
0 1
3 2
0 1 1
5 2
1 1 0 0 0
4 3
0 1 1 0
1 1
0
', '1
2
1
2
0
'),
(2013, 6, 0, 10, '5
2 1
0 1
3 2
0 1 0
5 3
1 1 1 1 0
4 1
0 1 0 1
1 1
0
', '1
1
4
1
0
'),
(2013, 7, 0, 10, '5
2 1
0 1
3 1
0 0 0
5 2
1 1 0 0 0
4 3
0 1 0 0
1 1
0
', '1
0
1
1
0
'),
(2013, 8, 0, 10, '5
2 1
0 1
3 2
0 1 1
5 3
1 1 0 0 1
4 3
0 1 0 1
1 1
0
', '1
2
2
1
0
'),
(2013, 9, 0, 10, '5
2 1
0 0
3 1
0 1 0
5 2
1 1 0 0 0
4 3
0 1 0 0
1 1
0
', '0
1
1
1
0
'),
(2014, 0, 0, 10, '5
3
0 1 3
7
0 1 2 3 4 3 2
4
3 0 0 0
7
4 6 2 3 5 0 5
5
4 0 1 0 4
', '1 1 0 -1 
1 1 2 2 1 0 2 6 
3 0 1 4 3 
1 0 -1 -1 -1 -1 -1 -1 
2 1 0 2 -1 -1 
'),
(2014, 1, 0, 10, '5
3
0 1 0
7
0 1 2 3 4 3 2
4
3 0 0 0
7
4 0 2 3 5 0 5
5
2 0 0 0 4
', '2 1 0 2 
1 1 2 2 1 0 2 6 
3 0 1 4 3 
2 0 2 2 2 3 1 2 
3 0 2 1 5 4 
'),
(2014, 2, 0, 10, '5
3
0 1 3
7
0 1 2 3 4 3 2
4
3 0 0 0
7
4 0 2 3 5 0 5
5
2 0 1 0 4
', '1 1 0 -1 
1 1 2 2 1 0 2 6 
3 0 1 4 3 
2 0 2 2 2 3 1 2 
2 1 1 0 4 3 
'),
(2014, 3, 0, 10, '5
3
0 1 3
7
0 1 2 3 4 3 2
4
3 0 0 0
7
4 0 2 3 5 0 5
5
2 0 0 0 4
', '1 1 0 -1 
1 1 2 2 1 0 2 6 
3 0 1 4 3 
2 0 2 2 2 3 1 2 
3 0 2 1 5 4 
'),
(2014, 4, 0, 10, '5
3
0 1 3
7
0 1 2 3 4 3 2
4
3 0 0 0
7
4 0 2 1 5 0 5
5
2 0 0 0 4
', '1 1 0 -1 
1 1 2 2 1 0 2 6 
3 0 1 4 3 
2 1 1 0 4 5 3 4 
3 0 2 1 5 4 
'),
(2014, 5, 0, 10, '5
3
0 1 3
7
0 1 2 3 4 3 2
4
3 0 0 0
7
4 0 2 1 5 0 4
5
2 0 0 0 4
', '1 1 0 -1 
1 1 2 2 1 0 2 6 
3 0 1 4 3 
2 1 1 0 5 4 3 5 
3 0 2 1 5 4 
'),
(2014, 6, 0, 10, '5
3
0 1 3
7
0 1 2 3 4 5 2
4
3 0 0 0
7
4 0 2 3 5 0 5
5
2 0 1 0 4
', '1 1 0 -1 
1 1 2 1 1 1 0 4 
3 0 1 4 3 
2 0 2 2 2 3 1 2 
2 1 1 0 4 3 
'),
(2014, 7, 0, 10, '5
3
0 1 3
7
0 1 2 3 4 3 2
4
3 0 0 0
7
4 0 2 1 5 0 5
5
2 0 0 1 4
', '1 1 0 -1 
1 1 2 2 1 0 2 6 
3 0 1 4 3 
2 1 1 0 4 5 3 4 
2 1 1 0 4 3 
'),
(2014, 8, 0, 10, '5
3
0 0 3
7
0 1 2 3 4 3 2
4
3 0 0 0
7
4 0 2 1 5 0 4
5
2 0 0 0 4
', '2 0 1 -1 
1 1 2 2 1 0 2 6 
3 0 1 4 3 
2 1 1 0 5 4 3 5 
3 0 2 1 5 4 
'),
(2014, 9, 0, 10, '5
3
0 0 3
7
0 1 2 3 4 3 2
4
3 0 0 0
7
4 0 2 1 5 0 5
5
2 0 0 1 4
', '2 0 1 -1 
1 1 2 2 1 0 2 6 
3 0 1 4 3 
2 1 1 0 4 5 3 4 
2 1 1 0 4 3 
'),
(2015, 0, 0, 10, '5
3 2
1 2 imposter
2 3 crewmate
5 4
1 3 crewmate
2 5 crewmate
2 4 imposter
3 4 imposter
2 2
1 2 imposter
2 1 crewmate
3 5
1 2 imposter
1 2 imposter
3 2 crewmate
3 2 crewmate
1 3 imposter
5 0
', '2
4
-1
2
5
'),
(2015, 1, 0, 10, '5
3 2
1 2 imposter
2 3 crewmate
5 4
1 3 crewmate
2 5 crewmate
2 4 imposter
3 4 imposter
2 2
1 2 imposter
2 2 crewmate
3 5
1 2 imposter
1 2 imposter
3 2 crewmate
3 2 crewmate
1 3 imposter
5 0
', '2
4
1
2
5
'),
(2015, 2, 0, 10, '5
3 2
1 2 imposter
2 3 crewmate
5 4
1 3 crewmate
2 5 crewmate
2 4 imposter
3 4 imposter
3 2
1 2 imposter
2 2 crewmate
3 5
1 2 imposter
1 2 imposter
3 2 crewmate
3 2 crewmate
1 3 imposter
5 0
', '2
4
2
2
5
'),
(2015, 3, 0, 10, '5
3 2
1 2 imposter
2 3 crewmate
5 4
1 5 crewmate
2 5 crewmate
2 4 imposter
3 4 imposter
5 2
1 2 imposter
1 1 crewmate
3 5
1 2 imposter
1 2 imposter
3 2 crewmate
3 2 crewmate
1 3 imposter
5 0
', '2
4
4
2
5
'),
(2015, 4, 0, 10, '5
3 2
1 2 imposter
2 3 crewmate
5 4
1 5 crewmate
2 5 crewmate
2 4 imposter
3 4 imposter
2 2
1 2 imposter
2 2 crewmate
3 5
1 2 imposter
1 2 imposter
3 2 crewmate
3 2 crewmate
1 3 imposter
4 0
', '2
4
1
2
4
'),
(2015, 5, 0, 10, '5
3 2
1 2 imposter
2 3 crewmate
5 4
1 3 crewmate
2 5 crewmate
2 4 imposter
3 4 imposter
3 2
1 2 imposter
2 2 crewmate
3 5
1 2 imposter
1 2 imposter
3 2 crewmate
3 2 crewmate
1 3 imposter
4 0
', '2
4
2
2
4
'),
(2015, 6, 0, 10, '5
5 2
1 2 imposter
2 3 crewmate
5 4
1 4 crewmate
2 5 crewmate
2 4 imposter
3 4 imposter
2 2
1 2 imposter
2 2 crewmate
3 5
1 2 imposter
1 2 imposter
3 2 crewmate
3 2 crewmate
1 3 imposter
5 0
', '4
3
1
2
5
'),
(2015, 7, 0, 10, '5
3 2
1 2 imposter
2 3 crewmate
5 4
1 3 crewmate
2 5 crewmate
4 5 imposter
3 5 imposter
2 2
1 2 imposter
2 2 crewmate
3 5
1 3 imposter
1 2 imposter
2 2 crewmate
3 2 crewmate
1 3 imposter
5 0
', '2
3
1
2
5
'),
(2015, 8, 0, 10, '5
3 2
1 2 imposter
2 3 crewmate
5 4
1 3 crewmate
2 5 crewmate
2 4 imposter
3 4 imposter
2 2
1 2 imposter
2 2 crewmate
3 5
1 2 imposter
1 2 imposter
2 2 crewmate
2 2 crewmate
1 2 imposter
2 0
', '2
4
1
2
2
'),
(2015, 9, 0, 10, '5
3 2
1 2 imposter
2 3 crewmate
5 4
1 5 crewmate
2 5 crewmate
2 4 imposter
3 4 imposter
4 2
1 2 imposter
2 2 crewmate
3 5
1 2 imposter
1 2 imposter
3 2 crewmate
3 2 crewmate
1 3 imposter
5 0
', '2
4
3
2
5
'),
(2016, 0, 0, 10, '3
2 4 3
a*
4 1 3
a**a
6 3 20
**a***
', 'abb
abba
babbbbbbbbb
'),
(2016, 1, 0, 10, '3
2 4 3
*a
4 1 3
a**a
6 3 29
***a**
', 'bba
abba
bbbba
'),
(2016, 2, 0, 10, '3
2 4 3
a*
4 1 3
a**a
6 3 51
**a***
', 'abb
abba
bbbbba
'),
(2016, 3, 0, 10, '3
2 4 3
a*
4 1 3
a**a
6 3 35
**a***
', 'abb
abba
bbbabbbb
'),
(2016, 4, 0, 10, '3
2 4 3
*a
4 1 3
a**a
6 3 20
***a**
', 'bba
abba
bbabbbbb
'),
(2016, 5, 0, 10, '3
2 4 3
a*
4 1 3
a**a
6 3 54
**a***
', 'abb
abba
bbbbbabbb
'),
(2016, 6, 0, 10, '3
2 4 3
*a
4 1 3
a**a
6 3 20
**a***
', 'bba
abba
babbbbbbbbb
'),
(2016, 7, 0, 10, '2
55 001 31372
a*a*a*a*aa*aaaaa*aaaaa*aa*a*aaaaa*aaa*aa*aa*a*aa*aa*a*a
63 15 36
*aaaaa*aa*a*a*aa*aaaa*a*aa*a*a*aaaa*aaaaa*a*a*aaa*aa*aaa*a*a*aa
', 'aaababaabaaaaabaaaaaaabaaaaaabaaaaaaaabaaaababa
aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaabbabbbaa
'),
(2016, 8, 0, 10, '2
55 001 54062
a*a*a*a*aa*aaaaa*aaaaa*aa*a*aaaaa*aaa*aa*aa*a*aa*aa*a*a
63 15 105
*aaaaa*aa*a*a*aa*aaaa*a*aa*a*a*aaaa*aaaaa*a*a*aaa*aa*aaa*a*a*aa
', 'aababaaabaaaaaaaaaaaababaaaaaaaaaabaaabaabaaaba
aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaabbbbbbabbbbbbbbaa
'),
(2016, 9, 0, 10, '2
55 001 31372
a*a*a*a*aa*aaaaa*aaaaa*aa*a*aaaaa*aaa*aa*aa*a*aa*aa*a*a
63 15 64
aa*a*a*aaa*aa*aaa*a*a*aaaaa*aaaa*a*a*aa*a*aaaa*aa*a*a*aa*aaaaa*
', 'aaababaabaaaaabaaaaaaabaaaaaabaaaaaaaabaaaababa
aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaabbbaaaaabbbbbbbbbbbbbbb
'),
(2017, 0, 0, 10, '4
3 3
...
.L.
...
4 5
#....
..##L
...#.
.....
1 1
L
1 9
....L..#.
', '...
.L.
...
#++++
..##L
...#+
...++
L
++++L++#.
'),
(2017, 1, 0, 10, '4
3 3
...
.L.
...
4 5
#....
..##L
...#.
.....
1 1
L
1 9
.#..L....
', '...
.L.
...
#++++
..##L
...#+
...++
L
.#++L++++
'),
(2017, 2, 0, 10, '4
3 3
...
.L.
...
4 5
#....
..##L
.#...
.....
1 1
L
1 9
.#..L....
', '...
.L.
...
#++++
++##L
+#...
++...
L
.#++L++++
'),
(2017, 3, 0, 10, '4
3 3
...
..L
...
4 5
#....
..##L
.#...
.....
1 1
L
1 9
.#..L....
', '..+
..L
..+
#++++
++##L
+#...
++...
L
.#++L++++
'),
(2017, 4, 0, 10, '4
3 3
...
..L
...
4 5
#....
..##L
..#..
.....
1 1
L
1 9
.#..L....
', '..+
..L
..+
#++++
..##L
..#..
.....
L
.#++L++++
'),
(2017, 5, 0, 10, '4
3 3
...
.L.
...
4 5
#....
..##L
.#...
.....
1 1
L
1 9
....L..#.
', '...
.L.
...
#++++
++##L
+#...
++...
L
++++L++#.
'),
(2017, 6, 0, 10, '4
3 3
...
.L.
...
4 5
#....
L.##.
...#.
.....
1 1
L
1 9
....L..#.
', '...
.L.
...
#....
L.##.
...#.
.....
L
++++L++#.
'),
(2017, 7, 0, 10, '4
3 3
...
..L
...
4 5
#....
..##L
...#.
.....
1 1
L
1 9
.#..L....
', '..+
..L
..+
#++++
..##L
...#+
...++
L
.#++L++++
'),
(2017, 8, 0, 10, '1
2 25
######################..#
.......................L.
######################..#
', '######################++#
+++++++++++++++++++++++L+
'),
(2017, 9, 0, 10, '1
2 25
######################..#
.L.......................
#..######################
', '######################..#
+L++++++++++++++++++++...
'),
(2018, 0, 0, 10, '5 4
', '1024
'),
(2018, 1, 0, 10, '3 1
', '1'),
(2018, 2, 0, 10, '2 8
', '8'),
(2018, 3, 0, 10, '2 5
', '5
'),
(2018, 4, 0, 10, '3 3
', '15
'),
(2018, 5, 0, 10, '4 5
', '301'),
(2018, 6, 0, 10, '4 4
', '148'),
(2018, 7, 0, 10, '4 7
', '781'),
(2018, 8, 0, 10, '500 1
', '1
'),
(2018, 9, 0, 10, '6 4
', '4096'),
(2019, 0, 0, 10, '6
2
1 3
3
1 2 3
4
6 2 3 9
4
6 8 4 5
7
1 2 4 6 7 7 3
8
8 6 5 1 2 2 3 6
', '3
3
9
9
7
8
'),
(2019, 1, 0, 10, '6
2
1 3
3
1 2 3
4
2 2 3 9
4
6 8 4 5
7
1 2 4 6 7 7 3
8
8 6 5 1 2 2 3 6
', '3
3
9
9
7
8
'),
(2019, 2, 0, 10, '6
2
1 3
3
1 2 6
4
6 2 3 9
4
6 8 4 5
7
1 2 4 6 7 7 3
8
8 6 5 1 2 2 3 6
', '3
6
9
9
7
8
'),
(2019, 3, 0, 10, '6
2
1 3
3
1 2 3
4
2 2 3 9
4
6 8 4 5
7
1 2 4 6 7 7 3
8
7 6 5 1 2 2 3 6
', '3
3
9
9
7
7
'),
(2019, 4, 0, 10, '6
2
1 3
3
1 2 0
4
2 2 3 9
4
6 8 4 5
7
1 2 4 6 7 7 3
8
7 6 5 1 2 2 3 6
', '3
2
9
9
7
7
'),
(2019, 5, 0, 10, '6
2
1 4
3
1 2 3
4
2 2 3 9
4
6 8 4 5
7
1 0 4 6 4 7 3
8
8 2 5 1 2 4 6 6
', '4
3
9
9
9
8
'),
(2019, 6, 0, 10, '6
2
1 5
3
1 2 6
4
6 2 3 9
4
6 8 4 5
7
1 2 4 6 7 7 3
8
8 6 5 1 2 2 3 6
', '5
6
9
9
7
8
'),
(2019, 7, 0, 10, '6
2
1 5
3
1 2 0
4
2 2 3 9
4
6 8 4 5
7
1 2 4 6 7 7 3
8
7 6 5 1 2 2 3 6
', '5
2
9
9
7
7
'),
(2019, 8, 0, 10, '6
2
1 8
3
1 2 3
4
4 2 3 9
4
6 8 4 5
7
1 0 4 6 4 7 3
8
8 2 5 1 2 4 6 6
', '8
3
9
9
9
8
'),
(2019, 9, 0, 10, '6
2
1 4
3
1 2 3
4
4 4 3 2
4
6 8 4 5
7
1 0 4 6 4 7 3
8
8 2 5 1 2 4 6 6
', '4
3
4
9
9
8
'),
(2020, 0, 0, 10, '6 4
10 -9 2 -1 4 -6
1 5
5 4
5 6
6 2
6 3
2 1 2
1 1 -3
2 1 2
2 3 3
', '39
32
0
'),
(2020, 1, 0, 10, '2 1
-651044801 1010000000
2 1
2 1 2
', '1661044801
'),
(2020, 2, 0, 10, '2 1
-651044801 1010000100
2 1
2 1 2
', '1661044901
'),
(2020, 3, 0, 10, '2 1
-1000000000 1000000000
2 1
2 1 2
', '2000000000
'),
(2020, 4, 0, 10, '2 1
-1000000000 1010000000
2 1
2 1 2
', '2010000000
'),
(2020, 5, 0, 10, '2 1
-1000000000 1000000010
2 1
2 1 2
', '2000000010
'),
(2020, 6, 0, 10, '2 1
-1000000000 1010001000
2 1
2 1 2
', '2010001000
'),
(2020, 7, 0, 10, '6 3
0 -5 2 -1 4 -5
1 5
5 4
5 6
6 2
6 3
2 1 4
1 1 -3
1 1 4
2 2 3
', '9
'),
(2020, 8, 0, 10, '6 3
0 -9 2 0 4 -5
1 5
5 4
5 6
6 2
6 3
2 1 4
1 1 -4
2 1 5
2 2 3
', '8
8
'),
(2020, 9, 0, 10, '6 3
0 -9 4 0 8 -6
1 5
5 4
5 6
6 2
6 3
2 1 1
1 1 -3
2 2 2
2 2 3
', '0
0
'),
(2021, 0, 0, 10, '3
1 1 1
', '0'),
(2021, 1, 0, 10, '2
2 3
', '4'),
(2021, 2, 0, 10, '2
2 5
', '8
'),
(2021, 3, 0, 10, '2
4 3
', '9
'),
(2021, 4, 0, 10, '3
2 2 2
', '2'),
(2021, 5, 0, 10, '2
2 6
', '10
'),
(2021, 6, 0, 10, '2
7 6
', '36
'),
(2021, 7, 0, 10, '3
2 1 1
', '0
'),
(2021, 8, 0, 10, '3
4 2 2
', '6
'),
(2021, 9, 0, 10, '3
2 2 1
', '1
'),
(2022, 0, 0, 10, '4 3
abab
2 3
0 0
0 4
0 0
', 'baaaab'),
(2022, 1, 0, 10, '1 1
z
0 0
', 'z'),
(2022, 2, 0, 10, '1 2
z
0 0
', 'z
'),
(2022, 3, 0, 10, '1 1
y
0 0
', 'y
'),
(2022, 4, 0, 10, '1 0
x
0 0
', 'x
'),
(2022, 5, 0, 10, '1 0
z
0 0
', 'z
'),
(2022, 6, 0, 10, '1 0
y
0 0
', 'y
'),
(2022, 7, 0, 10, '1 2
y
0 0
', 'y
'),
(2022, 8, 0, 10, '1 4
z
0 0
', 'z
'),
(2022, 9, 0, 10, '1 4
y
0 0
', 'y
'),
(2023, 0, 0, 10, '5
3 8 2 13 7
LLRLL
', '7 L
3 R
8 R
13 L
2 L
'),
(2023, 1, 0, 10, '5
3 8 2 13 7
LLRLL
', '7 L
3 R
8 R
13 L
2 L
'),
(2023, 2, 0, 10, '5
3 4 2 2 7
LLRLL
', '3 L
2  R
4  R
7 L
2 L
'),
(2023, 3, 0, 10, '5
6 4 2 2 7
LLRLL
', '4 L
2  R
6  R
7 L
2 L
'),
(2023, 4, 0, 10, '5
6 4 3 2 7
LLRLL
', '4 L
3  R
6  R
7 L
2 L
'),
(2023, 5, 0, 10, '5
3 8 2 9 7
LLRLL
', '7 L
3  R
8  R
9 L
2 L
'),
(2023, 6, 0, 10, '5
4 4 2 2 7
LLRLL
', '4 L
2  R
4  R
7 L
2 L
'),
(2023, 7, 0, 10, '5
6 4 1 2 7
LLRLL
', '4 L
2  R
6  R
7 L
1 L
'),
(2023, 8, 0, 10, '5
6 4 3 2 8
LLRLL
', '4 L
3  R
6  R
8 L
2 L
'),
(2023, 9, 0, 10, '5
6 4 3 2 9
LLRLL
', '4 L
3  R
6  R
9 L
2 L
');
INSERT INTO `voj_problem_category_relationships` (`problem_id`, `problem_category_id`) VALUES
(2000, 1),
(2001, 1),
(2002, 1),
(2003, 1),
(2004, 1),
(2005, 1),
(2006, 1),
(2006, 2),
(2007, 1),
(2008, 1),
(2009, 1),
(2010, 1),
(2011, 1),
(2012, 1),
(2013, 1),
(2014, 1),
(2014, 2),
(2015, 1),
(2015, 2),
(2016, 1),
(2016, 2),
(2017, 1),
(2018, 1),
(2018, 2),
(2019, 1),
(2019, 2),
(2020, 1),
(2021, 1),
(2021, 2),
(2022, 1),
(2023, 1);
INSERT INTO `voj_problem_tag_relationships` (`problem_id`, `problem_tag_id`) VALUES
(2000, 3),
(2000, 4),
(2000, 5),
(2001, 6),
(2001, 7),
(2001, 8),
(2002, 8),
(2002, 9),
(2003, 8),
(2003, 9),
(2004, 8),
(2004, 4),
(2005, 10),
(2005, 8),
(2006, 11),
(2006, 2),
(2006, 8),
(2006, 4),
(2006, 12),
(2007, 13),
(2008, 14),
(2008, 1),
(2008, 4),
(2009, 1),
(2009, 4),
(2010, 3),
(2010, 4),
(2011, 13),
(2011, 10),
(2011, 7),
(2011, 8),
(2012, 13),
(2012, 10),
(2012, 1),
(2012, 15),
(2013, 10),
(2013, 16),
(2013, 4),
(2013, 5),
(2013, 17),
(2014, 6),
(2014, 11),
(2014, 2),
(2014, 1),
(2014, 8),
(2014, 4),
(2014, 15),
(2015, 6),
(2015, 18),
(2015, 2),
(2015, 7),
(2015, 16),
(2016, 10),
(2016, 2),
(2016, 1),
(2016, 8),
(2016, 4),
(2017, 18),
(2017, 16),
(2018, 3),
(2018, 2),
(2018, 4),
(2019, 2),
(2020, 11),
(2020, 19),
(2021, 3),
(2021, 11),
(2021, 2),
(2021, 4),
(2022, 11),
(2022, 18),
(2022, 1),
(2022, 9),
(2022, 19),
(2023, 6),
(2023, 1),
(2023, 12);
-- <<< CODECONTESTS END

-- Keep the demo fresh over time. The data above is authored around the anchor date
-- 2026-06-27 (contest 5 is "running", 6 is "today", 7/8 are "upcoming"). On every
-- import we shift the whole contest timeline by the number of days between that
-- anchor and today, so the live/upcoming contests never rot into the past. The
-- shift is a whole number of days, so contests and their linked submissions move
-- together and standings/penalties stay intact. Discussion and registration times
-- are intentionally left as historical literals.
SET @voj_demo_shift = DATEDIFF(CURDATE(), '2026-06-27');

UPDATE `voj_contests`
   SET `contest_start_time` = `contest_start_time` + INTERVAL @voj_demo_shift DAY,
       `contest_end_time`   = `contest_end_time`   + INTERVAL @voj_demo_shift DAY;

UPDATE `voj_submissions`
   SET `submission_submit_time`  = `submission_submit_time`  + INTERVAL @voj_demo_shift DAY,
       `submission_execute_time` = `submission_execute_time` + INTERVAL @voj_demo_shift DAY;

SET FOREIGN_KEY_CHECKS = 1;
