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
-- 4. voj_email_validation, which holds the token and the daily email counters
--    of an email address, referenced voj_users.email (ON UPDATE / DELETE
--    CASCADE), so its counters followed an account to its new address, and
--    several accounts taking turns on one address could each have it mailed
--    up to the daily limit. The foreign key is dropped: the counters stay with
--    the address.
-- 5. Up to 1.0.2 the problem page wrote the description, the input and output
--    formats and the samples of a problem, and the contest page the notes of
--    a contest, as HTML (th:utext). Statements were therefore written with
--    HTML entities, e.g. a sample output "a &lt; b". Those fields are now
--    written as text (Markdown for all but the samples), so the entities would
--    show literally - in the samples, in code and in math. They are decoded
--    once: &lt; &gt; &quot; &#39; &nbsp; (to U+00A0) and, last, &amp;, so that
--    a statement that showed "&lt;" (stored "&amp;lt;") still shows "&lt;".
--    The hint was already written as text, so it is left alone.
-- 6. voj_contest_contestants.code_snippet was never written; it is dropped.
-- 7. The native sandbox lets a compiler write only inside its work
--    directory, so kotlinc's default compile command, whose terminal library
--    unpacks itself into /tmp, now points the compiler's temporary directory
--    there (the compile succeeded either way, with a warning in its log). A
--    compile command an administrator changed is left as it is.
-- 8. The judgers take the pending submissions from voj_submissions itself
--    rather than from a message queue. Four columns are added:
--    submission_queued_time, when a submission was queued for judging (on
--    submission and on every rejudge; the judgers take the oldest first),
--    submission_judge_attempts, how many judgings of it were interrupted
--    since (their judger stopped, or lost the database, and their lease
--    expired), submission_claim_token, which identifies the judging that
--    took it, and submission_lease_expiry_time, until when that judging
--    holds it. The existing submissions get the time of the upgrade as their
--    queued time (only that of a pending one is read) and no judging: a
--    pending one is judged by the first judger of the new version that
--    supports its language. The index submission_judge_queue (judge result,
--    queued time, language, lease) serves the judgers' search for the oldest
--    pending submission they may take from the index alone.
-- 9. submission_runtime_result, the index of the judge result alone, is
--    dropped: submission_judge_queue starts with the judge result, so it
--    serves every query (and the foreign key) that used it.
-- 10. voj_judgers is created: every running judger (every process, by a
--    random identifier, with its account) records there, every few seconds by
--    the database's clock, the languages it supports, whether it takes
--    submissions and its load. The web application shows the judgers
--    from it (rather than from the judgers' heartbeats over the message
--    queue), and marks as a System Error the pending submissions in a
--    language that no judger taking submissions has supported for a while.
--
-- Safe to run more than once. Steps 1 to 4, 6 and 8 to 10 first check the
-- information schema (or create a table only if it is missing) and only change
-- what is still in the old state (MySQL commits DDL implicitly, so a
-- transaction could not make them atomic; an interrupted run is completed by
-- running the script again). Step 7 changes the old command only. Step 5 runs in
-- one transaction with the recording of the option upgradeFrom102Applied in
-- voj_options, and only if that option was not recorded yet, so that text
-- typed after the upgrade (e.g. a statement that means "&amp;") is never
-- decoded. New installations get the option from seed.sql.
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

-- 4. The address-keyed email counters.
SET @voj_has_key = (SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'voj_email_validation_ibfk_1');
SET @voj_sql = IF(@voj_has_key > 0,
  'ALTER TABLE `voj_email_validation` DROP FOREIGN KEY `voj_email_validation_ibfk_1`', 'DO 0');
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

-- 6. The never-written code snippets of contestants.
SET @voj_has_column = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'voj_contest_contestants'
  AND COLUMN_NAME = 'code_snippet');
SET @voj_sql = IF(@voj_has_column > 0,
  'ALTER TABLE `voj_contest_contestants` DROP COLUMN `code_snippet`', 'DO 0');
PREPARE voj_stmt FROM @voj_sql;
EXECUTE voj_stmt;
DEALLOCATE PREPARE voj_stmt;

-- 7. Kotlin's temporary directory, in the default compile command only.
UPDATE `voj_languages`
SET `language_compile_command` = 'kotlinc -J-Djava.io.tmpdir=. {filename}.kt -include-runtime -d {filename}.jar'
WHERE `language_compile_command` = 'kotlinc {filename}.kt -include-runtime -d {filename}.jar';

-- 8. The columns of the judging queue, added in one statement.
SET @voj_has_column = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'voj_submissions'
  AND COLUMN_NAME = 'submission_queued_time');
SET @voj_sql = IF(@voj_has_column = 0,
  'ALTER TABLE `voj_submissions` ADD COLUMN `submission_queued_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER `submission_submit_time`, ADD COLUMN `submission_judge_attempts` int(4) NOT NULL DEFAULT 0 AFTER `submission_execute_time`, ADD COLUMN `submission_claim_token` bigint(20) DEFAULT NULL AFTER `submission_judge_attempts`, ADD COLUMN `submission_lease_expiry_time` timestamp NULL DEFAULT NULL AFTER `submission_claim_token`, ADD KEY `submission_judge_queue` (`submission_judge_result`,`submission_queued_time`,`language_id`,`submission_lease_expiry_time`)', 'DO 0');
PREPARE voj_stmt FROM @voj_sql;
EXECUTE voj_stmt;
DEALLOCATE PREPARE voj_stmt;

-- 9. The redundant index of the judge result, dropped once the index of the
--    judging queue (step 8), which starts with the judge result, exists.
SET @voj_has_key = (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'voj_submissions'
  AND INDEX_NAME = 'submission_runtime_result');
SET @voj_sql = IF(@voj_has_key > 0,
  'ALTER TABLE `voj_submissions` DROP KEY `submission_runtime_result`', 'DO 0');
PREPARE voj_stmt FROM @voj_sql;
EXECUTE voj_stmt;
DEALLOCATE PREPARE voj_stmt;

-- 10. The registry of the running judgers.
CREATE TABLE IF NOT EXISTS `voj_judgers` (
  `judger_instance` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `judger_uid` bigint(20) NOT NULL,
  `judger_description` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `judger_language_ids` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `judger_is_ready` tinyint(1) NOT NULL DEFAULT 0,
  `judger_start_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `judger_heartbeat_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `judger_cpu_load` int(4) NOT NULL DEFAULT -1,
  `judger_memory_usage` int(4) NOT NULL DEFAULT -1,
  PRIMARY KEY (`judger_instance`),
  KEY `judger_uid` (`judger_uid`),
  CONSTRAINT `voj_judgers_ibfk_1` FOREIGN KEY (`judger_uid`) REFERENCES `voj_users` (`uid`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
