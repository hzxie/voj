-- Verwandlung Online Judge - one-time upgrade for installations created with
-- version 1.0.2 or earlier. See "Upgrading" in docs/deployment.md.
--
-- 1. The foreign keys of voj_contest_contestants and voj_contest_submissions
--    had no ON DELETE rule, so deleting a user who attended a contest, a
--    contest submission, or a problem (whose submissions cascade) failed. They
--    now cascade like every other foreign key to users, problems and
--    submissions.
-- 2. voj_contests.contest_mode (ACM / OI) decided how a contest was ranked,
--    and contest_scoring (ICPC / IOI / Codeforces) only labelled it: a
--    Codeforces contest was ranked as ICPC, and contests created before the
--    scoring column existed kept its default ICPC whatever their mode. The
--    ranking is now derived from contest_scoring alone, so it is set to the
--    rule each contest was actually ranked by (OI -> IOI, anything else ->
--    ICPC) and contest_mode is dropped.
-- 3. voj_contests.contest_is_rated was stored but never used; it is dropped.
-- 4. voj_submissions.submission_judge_generation counts the rejudges of a
--    submission; every judging task carries it, so that a task a rejudge
--    superseded is told apart from the current one. It is added (0 for every
--    existing submission).
-- 5. Up to 1.0.2 the problem page wrote the description, the input and output
--    formats and the samples of a problem, and the contest page the notes of
--    a contest, as HTML (th:utext). Statements were therefore written with
--    HTML entities, e.g. a sample output "a &lt; b". Those fields are now
--    written as text (Markdown for all but the samples), so the entities would
--    show literally - in the samples, in code and in math. They are decoded
--    once: &lt; &gt; &quot; &#39; &nbsp; (to U+00A0) and, last, &amp;, so that
--    a statement that showed "&lt;" (stored "&amp;lt;") still shows "&lt;".
--    The hint was already written as text, so it is left alone.
--
-- Safe to run more than once. Steps 1 to 4 first check the information
-- schema and only change what is still in the old state (MySQL commits DDL
-- implicitly, so a transaction could not make them atomic; an interrupted run
-- is completed by running the script again). Step 5 runs in one transaction
-- with the recording of the option upgradeFrom102Applied in voj_options, and
-- only if that option was not recorded yet, so that text typed after the
-- upgrade (e.g. a statement that means "&amp;") is never decoded. New
-- installations get the option from seed.sql.
--
-- Usage: mysql -u <user> -p <database> < sql/upgrades/upgrade-from-1.0.2.sql
SET NAMES utf8mb4;

-- 1. Cascading foreign keys. Each one is dropped and added again unless its
--    delete rule is already CASCADE. The second statement also restores a key
--    that an interrupted run dropped.
SET @voj_rule = (SELECT DELETE_RULE FROM information_schema.REFERENTIAL_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'voj_contest_contestants_ibfk_1');
SET @voj_sql = IF(@voj_rule IS NOT NULL AND @voj_rule <> 'CASCADE',
  'ALTER TABLE `voj_contest_contestants` DROP FOREIGN KEY `voj_contest_contestants_ibfk_1`', 'DO 0');
PREPARE voj_stmt FROM @voj_sql;
EXECUTE voj_stmt;
DEALLOCATE PREPARE voj_stmt;
SET @voj_sql = IF(@voj_rule IS NULL OR @voj_rule <> 'CASCADE',
  'ALTER TABLE `voj_contest_contestants` ADD CONSTRAINT `voj_contest_contestants_ibfk_1` FOREIGN KEY (`contest_id`) REFERENCES `voj_contests` (`contest_id`) ON DELETE CASCADE ON UPDATE CASCADE', 'DO 0');
PREPARE voj_stmt FROM @voj_sql;
EXECUTE voj_stmt;
DEALLOCATE PREPARE voj_stmt;

SET @voj_rule = (SELECT DELETE_RULE FROM information_schema.REFERENTIAL_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'voj_contest_contestants_ibfk_2');
SET @voj_sql = IF(@voj_rule IS NOT NULL AND @voj_rule <> 'CASCADE',
  'ALTER TABLE `voj_contest_contestants` DROP FOREIGN KEY `voj_contest_contestants_ibfk_2`', 'DO 0');
PREPARE voj_stmt FROM @voj_sql;
EXECUTE voj_stmt;
DEALLOCATE PREPARE voj_stmt;
SET @voj_sql = IF(@voj_rule IS NULL OR @voj_rule <> 'CASCADE',
  'ALTER TABLE `voj_contest_contestants` ADD CONSTRAINT `voj_contest_contestants_ibfk_2` FOREIGN KEY (`contestant_uid`) REFERENCES `voj_users` (`uid`) ON DELETE CASCADE ON UPDATE CASCADE', 'DO 0');
PREPARE voj_stmt FROM @voj_sql;
EXECUTE voj_stmt;
DEALLOCATE PREPARE voj_stmt;

SET @voj_rule = (SELECT DELETE_RULE FROM information_schema.REFERENTIAL_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'voj_contest_submissions_ibfk_1');
SET @voj_sql = IF(@voj_rule IS NOT NULL AND @voj_rule <> 'CASCADE',
  'ALTER TABLE `voj_contest_submissions` DROP FOREIGN KEY `voj_contest_submissions_ibfk_1`', 'DO 0');
PREPARE voj_stmt FROM @voj_sql;
EXECUTE voj_stmt;
DEALLOCATE PREPARE voj_stmt;
SET @voj_sql = IF(@voj_rule IS NULL OR @voj_rule <> 'CASCADE',
  'ALTER TABLE `voj_contest_submissions` ADD CONSTRAINT `voj_contest_submissions_ibfk_1` FOREIGN KEY (`contest_id`) REFERENCES `voj_contests` (`contest_id`) ON DELETE CASCADE ON UPDATE CASCADE', 'DO 0');
PREPARE voj_stmt FROM @voj_sql;
EXECUTE voj_stmt;
DEALLOCATE PREPARE voj_stmt;

SET @voj_rule = (SELECT DELETE_RULE FROM information_schema.REFERENTIAL_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'voj_contest_submissions_ibfk_2');
SET @voj_sql = IF(@voj_rule IS NOT NULL AND @voj_rule <> 'CASCADE',
  'ALTER TABLE `voj_contest_submissions` DROP FOREIGN KEY `voj_contest_submissions_ibfk_2`', 'DO 0');
PREPARE voj_stmt FROM @voj_sql;
EXECUTE voj_stmt;
DEALLOCATE PREPARE voj_stmt;
SET @voj_sql = IF(@voj_rule IS NULL OR @voj_rule <> 'CASCADE',
  'ALTER TABLE `voj_contest_submissions` ADD CONSTRAINT `voj_contest_submissions_ibfk_2` FOREIGN KEY (`submission_id`) REFERENCES `voj_submissions` (`submission_id`) ON DELETE CASCADE ON UPDATE CASCADE', 'DO 0');
PREPARE voj_stmt FROM @voj_sql;
EXECUTE voj_stmt;
DEALLOCATE PREPARE voj_stmt;

-- 2. One scoring column. The update runs only while contest_mode still exists,
--    so a later run never overwrites a scoring an administrator chose since.
SET @voj_has_column = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'voj_contests' AND COLUMN_NAME = 'contest_mode');
SET @voj_sql = IF(@voj_has_column > 0,
  'UPDATE `voj_contests` SET `contest_scoring` = IF(`contest_mode` = ''OI'', ''IOI'', ''ICPC'')', 'DO 0');
PREPARE voj_stmt FROM @voj_sql;
EXECUTE voj_stmt;
DEALLOCATE PREPARE voj_stmt;
SET @voj_sql = IF(@voj_has_column > 0,
  'ALTER TABLE `voj_contests` DROP COLUMN `contest_mode`', 'DO 0');
PREPARE voj_stmt FROM @voj_sql;
EXECUTE voj_stmt;
DEALLOCATE PREPARE voj_stmt;

-- 3. The unused rated flag.
SET @voj_has_column = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'voj_contests' AND COLUMN_NAME = 'contest_is_rated');
SET @voj_sql = IF(@voj_has_column > 0,
  'ALTER TABLE `voj_contests` DROP COLUMN `contest_is_rated`', 'DO 0');
PREPARE voj_stmt FROM @voj_sql;
EXECUTE voj_stmt;
DEALLOCATE PREPARE voj_stmt;

-- 4. The judge generation of the submissions.
SET @voj_has_column = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'voj_submissions'
  AND COLUMN_NAME = 'submission_judge_generation');
SET @voj_sql = IF(@voj_has_column = 0,
  'ALTER TABLE `voj_submissions` ADD COLUMN `submission_judge_generation` int(8) NOT NULL DEFAULT 0 AFTER `submission_execute_time`', 'DO 0');
PREPARE voj_stmt FROM @voj_sql;
EXECUTE voj_stmt;
DEALLOCATE PREPARE voj_stmt;

-- 5. The HTML entities of the statements and contest notes, decoded on the
--    first run only: the marker is recorded in the same transaction, and a
--    later run (or concurrent one, which waits on the unique option_name
--    index) finds it and decodes nothing.
START TRANSACTION;

INSERT IGNORE INTO `voj_options` (`option_name`, `option_value`, `is_autoload`)
VALUES ('upgradeFrom102Applied', '1', 0);
SET @voj_first_run = (ROW_COUNT() = 1);

UPDATE `voj_problems` SET
  `problem_description` = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`problem_description`,
    '&lt;', '<'), '&gt;', '>'), '&quot;', '"'), '&#39;', ''''),
    '&nbsp;', _utf8mb4 X'C2A0'), '&amp;', '&'),
  `problem_input_format` = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`problem_input_format`,
    '&lt;', '<'), '&gt;', '>'), '&quot;', '"'), '&#39;', ''''),
    '&nbsp;', _utf8mb4 X'C2A0'), '&amp;', '&'),
  `problem_output_format` = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`problem_output_format`,
    '&lt;', '<'), '&gt;', '>'), '&quot;', '"'), '&#39;', ''''),
    '&nbsp;', _utf8mb4 X'C2A0'), '&amp;', '&'),
  `problem_sample_input` = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`problem_sample_input`,
    '&lt;', '<'), '&gt;', '>'), '&quot;', '"'), '&#39;', ''''),
    '&nbsp;', _utf8mb4 X'C2A0'), '&amp;', '&'),
  `problem_sample_output` = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`problem_sample_output`,
    '&lt;', '<'), '&gt;', '>'), '&quot;', '"'), '&#39;', ''''),
    '&nbsp;', _utf8mb4 X'C2A0'), '&amp;', '&')
WHERE @voj_first_run AND (`problem_description` LIKE '%&%'
    OR `problem_input_format` LIKE '%&%'
    OR `problem_output_format` LIKE '%&%'
    OR `problem_sample_input` LIKE '%&%'
    OR `problem_sample_output` LIKE '%&%');

UPDATE `voj_contests` SET
  `contest_notes` = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`contest_notes`,
    '&lt;', '<'), '&gt;', '>'), '&quot;', '"'), '&#39;', ''''),
    '&nbsp;', _utf8mb4 X'C2A0'), '&amp;', '&')
WHERE @voj_first_run AND `contest_notes` LIKE '%&%';

COMMIT;
