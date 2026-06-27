-- Verwandlung Online Judge - reference data required by every install.
-- Loaded after schema.sql; shared by the installer and the test suite.
SET NAMES utf8mb4;
SET SQL_MODE = "STRICT_TRANS_TABLES";
SET FOREIGN_KEY_CHECKS = 0;

INSERT INTO `voj_discussion_topics` (`discussion_topic_id`, `discussion_topic_slug`, `discussion_topic_name`, `discussion_parent_topic_id`) VALUES
(1, 'solutions', 'Solutions', 0),
(2, 'qa', 'Q & A', 0),
(3, 'general', 'General', 0),
(4, 'support', 'Help & Support', 0);

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

INSERT INTO `voj_languages` (`language_id`, `language_slug`, `language_name`, `language_compile_command`, `language_run_command`) VALUES
(1, 'text/x-csrc', 'C', 'gcc -O2 -s -Wall -o {filename}.exe {filename}.c -lm', '{filename}.exe'),
(2, 'text/x-c++src', 'C++', 'g++ -O2 -s -Wall -std=c++11 -o {filename}.exe {filename}.cpp -lm', '{filename}.exe'),
(3, 'text/x-java', 'Java', 'javac {filename}.java', 'java -cp {filename}'),
(4, 'text/x-pascal', 'Pascal', 'fpc -O2 -Xs -Sgic -vw -o{filename}.exe {filename}.pas', '{filename}.exe'),
(5, 'text/x-python', 'Python 2', 'python -m py_compile {filename}.py', 'python {filename}.py'),
(6, 'text/x-ruby', 'Ruby', 'rbx compile {filename}.rb', 'ruby {filename}.rb ');

INSERT INTO `voj_options` (`option_id`, `option_name`, `option_value`, `is_autoload`) VALUES
(1, 'websiteName', 'Verwandlung Online Judge', 1),
(2, 'description', 'Verwandlung Online Judge is a cross-platform online judge system based on Spring MVC Framework.', 1),
(3, 'copyright', '<a href="https://haozhexie.com/" target="_blank">Infinite Script</a>', 1),
(4, 'googleAnalyticsCode', '', 1),
(5, 'icpNumber', '', 1),
(6, 'policeIcpNumber', '', 1),
(7, 'allowUserRegister', '1', 0),
(8, 'offensiveWords', '["法轮","中央","六四","军区","共产党","国民党"]', 0),
(9, 'discussionReportHideThreshold', '5', 0),
(10, 'discussionMinSolvedToVote', '1', 0),
(11, 'discussionMinSolvedToReport', '3', 0);

INSERT INTO `voj_problem_difficulty` (`problem_difficulty_id`, `problem_difficulty_slug`, `problem_difficulty_name`) VALUES
(1, 'easy', 'Easy'),
(2, 'medium', 'Medium'),
(3, 'hard', 'Hard');

INSERT INTO `voj_problem_categories` (`problem_category_id`, `problem_category_slug`, `problem_category_name`, `problem_category_parent_id`) VALUES
(1, 'uncategorized', 'Uncategorized', 0),
(2, 'dynamic-programming', 'Dynamic Programming', 0);

INSERT INTO `voj_problem_tags` (`problem_tag_id`, `problem_tag_slug`, `problem_tag_name`) VALUES
(1, 'greedy', 'Greedy'),
(2, 'dynamic-programming', 'Dynamic Programming');

INSERT INTO `voj_user_groups` (`user_group_id`, `user_group_slug`, `user_group_name`) VALUES
(1, 'forbidden', 'Forbidden'),
(2, 'users', 'Users'),
(4, 'judgers', 'Judgers'),
(8, 'administrators', 'Administrators');

SET FOREIGN_KEY_CHECKS = 1;
