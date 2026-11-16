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
-- 5. voj_email_validation, which holds the token and the daily email counters
--    of an email address, referenced voj_users.email (ON UPDATE / DELETE
--    CASCADE), so its counters followed an account to its new address, and
--    several accounts taking turns on one address could each have it mailed
--    up to the daily limit. The foreign key is dropped: the counters stay with
--    the address.
-- 6. Up to 1.0.2 the problem page wrote the description, the input and output
--    formats and the samples of a problem, and the contest page the notes of
--    a contest, as HTML (th:utext). Statements were therefore written with
--    HTML entities, e.g. a sample output "a &lt; b". Those fields are now
--    written as text (Markdown for all but the samples), so the entities would
--    show literally - in the samples, in code and in math. They are decoded
--    once: &lt; &gt; &quot; &#39; &nbsp; (to U+00A0) and, last, &amp;, so that
--    a statement that showed "&lt;" (stored "&amp;lt;") still shows "&lt;".
--    The hint was already written as text, so it is left alone.
-- 7. voj_contest_contestants.code_snippet was never written; it is dropped.
-- 8. The native sandbox lets a compiler write only inside its work
--    directory, so kotlinc's default compile command, whose terminal library
--    unpacks itself into /tmp, now points the compiler's temporary directory
--    there (the compile succeeded either way, with a warning in its log). A
--    compile command an administrator changed is left as it is.
-- 9. voj_submissions.submission_queued_time is when the current judging task
--    of a submission was queued (on submission and on every rejudge); the web
--    application judges a submission a System Error that no judger took, or
--    finished judging, in time. It is added, with the submit time for every
--    existing submission.
--
-- Safe to run more than once. Steps 1 to 5 and 7 to 9 first check the information
-- schema and only change what is still in the old state (MySQL commits DDL
-- implicitly, so a transaction could not make them atomic; an interrupted run
-- is completed by running the script again). Step 6 runs in one transaction
-- with the recording of the option upgradeFrom102Applied in voj_options, and
-- only if that option was not recorded yet, so that text typed after the
-- upgrade (e.g. a statement that means "&amp;") is never decoded. New
-- installations get the option from seed.sql.
--
-- Usage: mysql -u <user> -p <database> < sql/upgrades/upgrade-from-1.0.2.sql
SET NAMES utf8mb4;
-- The SQL mode schema.sql creates the tables in. The server's default may
-- include NO_ZERO_DATE (MySQL 5.7 and later do), which makes every ALTER TABLE
-- of voj_contests fail on the '0000-00-00 00:00:00' defaults of its times.
SET SQL_MODE = "STRICT_TRANS_TABLES";

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

-- 5. The address-keyed email counters.
SET @voj_has_key = (SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'voj_email_validation_ibfk_1');
SET @voj_sql = IF(@voj_has_key > 0,
  'ALTER TABLE `voj_email_validation` DROP FOREIGN KEY `voj_email_validation_ibfk_1`', 'DO 0');
PREPARE voj_stmt FROM @voj_sql;
EXECUTE voj_stmt;
DEALLOCATE PREPARE voj_stmt;

-- 6. The HTML entities of the statements and contest notes, decoded on the
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

-- 7. The never-written code snippets of contestants.
SET @voj_has_column = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'voj_contest_contestants'
  AND COLUMN_NAME = 'code_snippet');
SET @voj_sql = IF(@voj_has_column > 0,
  'ALTER TABLE `voj_contest_contestants` DROP COLUMN `code_snippet`', 'DO 0');
PREPARE voj_stmt FROM @voj_sql;
EXECUTE voj_stmt;
DEALLOCATE PREPARE voj_stmt;

-- 8. Kotlin's temporary directory, in the default compile command only.
UPDATE `voj_languages`
SET `language_compile_command` = 'kotlinc -J-Djava.io.tmpdir=. {filename}.kt -include-runtime -d {filename}.jar'
WHERE `language_compile_command` = 'kotlinc {filename}.kt -include-runtime -d {filename}.jar';

-- 9. The time the current judging task of a submission was queued. The submit
--    time is filled in only when the column is added, so that a later run
--    keeps the time of a rejudge.
SET @voj_has_column = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'voj_submissions'
  AND COLUMN_NAME = 'submission_queued_time');
SET @voj_sql = IF(@voj_has_column = 0,
  'ALTER TABLE `voj_submissions` ADD COLUMN `submission_queued_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER `submission_submit_time`', 'DO 0');
PREPARE voj_stmt FROM @voj_sql;
EXECUTE voj_stmt;
DEALLOCATE PREPARE voj_stmt;
SET @voj_sql = IF(@voj_has_column = 0,
  'UPDATE `voj_submissions` SET `submission_queued_time` = `submission_submit_time`', 'DO 0');
PREPARE voj_stmt FROM @voj_sql;
EXECUTE voj_stmt;
DEALLOCATE PREPARE voj_stmt;
