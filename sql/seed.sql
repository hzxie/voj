-- Verwandlung Online Judge - reference data required by every install.
-- Loaded after schema.sql; shared by the installer and the test suite.
-- Idempotent: clears its tables first, and inserts are FK-dependency ordered.
SET NAMES utf8mb4;
SET SQL_MODE = "STRICT_TRANS_TABLES";
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM `voj_options`;
DELETE FROM `voj_offensive_words`;
DELETE FROM `voj_discussion_topics`;
DELETE FROM `voj_problem_tags`;
DELETE FROM `voj_problem_categories`;
DELETE FROM `voj_problem_difficulty`;
DELETE FROM `voj_judge_results`;
DELETE FROM `voj_languages`;
DELETE FROM `voj_user_groups`;

INSERT INTO `voj_user_groups` (`user_group_id`, `user_group_slug`, `user_group_name`) VALUES
(1, 'forbidden', 'Forbidden'),
(2, 'users', 'Users'),
(4, 'judgers', 'Judgers'),
(8, 'administrators', 'Administrators');

INSERT INTO `voj_languages` (`language_id`, `language_slug`, `language_name`, `language_compile_command`, `language_run_command`, `language_enabled`, `language_source_filename`, `language_time_multiplier`, `language_memory_multiplier`) VALUES
(1, 'text/x-csrc', 'C', 'gcc -O2 -s -Wall -o {filename}.exe {filename}.c -lm', '{filename}.exe', 1, 'Main.c', 1.0, 1.0),
(2, 'text/x-c++src', 'C++', 'g++ -O2 -s -Wall -std=c++11 -o {filename}.exe {filename}.cpp -lm', '{filename}.exe', 1, 'Main.cpp', 1.0, 1.0),
(3, 'text/x-java', 'Java', 'javac {filename}.java', 'java -cp {filename}', 1, 'Main.java', 2.0, 2.0),
(4, 'text/x-pascal', 'Pascal', 'fpc -O2 -Xs -Sgic -vw -o{filename}.exe {filename}.pas', '{filename}.exe', 1, 'Main.pas', 1.0, 1.0),
(5, 'text/x-python', 'Python 2', 'python -m py_compile {filename}.py', 'python {filename}.py', 1, 'Main.py', 3.0, 2.0),
(6, 'text/x-ruby', 'Ruby', 'rbx compile {filename}.rb', 'ruby {filename}.rb ', 1, 'Main.rb', 2.0, 1.0);

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

INSERT INTO `voj_problem_difficulty` (`problem_difficulty_id`, `problem_difficulty_slug`, `problem_difficulty_name`) VALUES
(1, 'easy', 'Easy'),
(2, 'medium', 'Medium'),
(3, 'hard', 'Hard');

INSERT INTO `voj_problem_categories` (`problem_category_id`, `problem_category_slug`, `problem_category_name`, `problem_category_parent_id`) VALUES
(1, 'uncategorized', 'Uncategorized', 0),
(2, 'dynamic-programming', 'Dynamic Programming', 0);

INSERT INTO `voj_problem_tags` (`problem_tag_id`, `problem_tag_slug`, `problem_tag_name`) VALUES
(1, 'greedy', 'Greedy'),
(2, 'dynamic-programming', 'Dynamic Programming'),
(3, 'combinatorics', 'Combinatorics'),
(4, 'math', 'Math'),
(5, 'number-theory', 'Number Theory'),
(6, 'constructive-algorithms', 'Constructive Algorithms'),
(7, 'dsu', 'Dsu'),
(8, 'implementation', 'Implementation'),
(9, 'strings', 'Strings'),
(10, 'brute-force', 'Brute Force'),
(11, 'data-structures', 'Data Structures'),
(12, 'two-pointers', 'Two Pointers'),
(13, 'binary-search', 'Binary Search'),
(14, 'bitmasks', 'Bitmasks'),
(15, 'sortings', 'Sortings'),
(16, 'graphs', 'Graphs'),
(17, 'shortest-paths', 'Shortest Paths'),
(18, 'dfs-and-similar', 'Dfs And Similar'),
(19, 'trees', 'Trees');

INSERT INTO `voj_discussion_topics` (`discussion_topic_id`, `discussion_topic_slug`, `discussion_topic_name`, `discussion_parent_topic_id`) VALUES
(1, 'solutions', 'Solutions', 0),
(2, 'qa', 'Q & A', 0),
(3, 'general', 'General', 0),
(4, 'support', 'Help & Support', 0);

INSERT INTO `voj_options` (`option_id`, `option_name`, `option_value`, `is_autoload`) VALUES
(1, 'websiteName', 'Verwandlung Online Judge', 1),
(2, 'description', 'Verwandlung Online Judge is an open-source, cross-platform online judge system built on Spring Boot.', 1),
(3, 'copyright', '<a href="https://haozhexie.com/" target="_blank">Infinite Script</a>', 1),
(4, 'googleAnalyticsCode', '', 1),
(5, 'icpNumber', '', 1),
(6, 'policeIcpNumber', '', 1),
(7, 'allowUserRegister', '1', 0),
(8, 'offensiveWordSources', '', 0),
(9, 'discussionReportHideThreshold', '5', 0),
(10, 'discussionMinSolvedToVote', '1', 0),
(11, 'discussionMinSolvedToReport', '3', 0),
(12, 'offensiveWordsImportedAt', '', 0),
(13, 'problemsPerPage', '100', 0),
(14, 'usersPerPage', '100', 0),
(15, 'submissionsPerPage', '100', 0),
(16, 'contestsPerPage', '50', 0),
(17, 'bulletinsPerPage', '50', 0),
(18, 'discussionsPerPage', '50', 0),
(19, 'contactEmail', '', 1),
(20, 'requireEmailVerification', '0', 0),
(21, 'publicSubmissions', '1', 0),
(22, 'maintenanceMode', '0', 0),
(23, 'autoHideOnReports', '1', 0),
(24, 'newUserPostDelay', '0', 0),
(25, 'autoCensorOffensiveWords', '1', 0),
(26, 'turnstileEnabled', '0', 1),
(27, 'turnstileSiteKey', '', 1),
(28, 'turnstileSecretKey', '', 0),
(29, 'emailDailyLimit', '3', 0);

-- Baseline offensive-word library. Administrators normally replace this by
-- importing external word lists (see voj_options.offensiveWordSources); the
-- handful of words below give a fresh install a working default.
INSERT INTO `voj_offensive_words` (`offensive_word_id`, `word`) VALUES
(1, '法轮'),
(2, '中央'),
(3, '六四'),
(4, '军区'),
(5, '共产党'),
(6, '国民党');

SET FOREIGN_KEY_CHECKS = 1;
