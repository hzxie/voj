-- Verwandlung Online Judge - one-time upgrade for installations created with
-- version 1.0.2 or earlier. See "Upgrading" in docs/deployment.md.
--
-- 1. The foreign keys of voj_contest_contestants and voj_contest_submissions
--    had no ON DELETE rule, so deleting a user who attended a contest, a
--    contest submission, or a problem (whose submissions cascade) failed. They
--    now cascade like every other foreign key to users, problems and
--    submissions.
--
-- Safe to run more than once: every step first checks the information schema
-- and only changes what is still in the old state (MySQL commits DDL
-- implicitly, so a transaction could not make the steps atomic; an interrupted
-- run is completed by running the script again). The last statement records
-- the option contestTablesUpgraded in voj_options; new installations get it
-- from seed.sql.
--
-- Usage: mysql -u <user> -p <database> < sql/upgrades/contest-tables.sql
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

INSERT IGNORE INTO `voj_options` (`option_name`, `option_value`, `is_autoload`)
VALUES ('contestTablesUpgraded', '1', 0);
