-- Verwandlung Online Judge - one-time upgrade for installations created with
-- version 1.0.2 or earlier. See "Upgrading" in docs/deployment.md.
--
-- Up to 1.0.2, HtmlTextFilter passed user text through Jsoup (Safelist.none())
-- and stored the result entity-encoded. Jsoup's output escapes exactly four
-- characters in text: & -> &amp;  < -> &lt;  > -> &gt;  U+00A0 -> &nbsp;
-- (quotes and apostrophes were left as is; every other character, including
-- non-ASCII text, was emitted literally). The filtered columns were:
--   voj_discussion_threads.discussion_thread_name
--   voj_discussion_replies.discussion_reply_content
--   voj_usermeta.meta_value for displayName, location, website, socialLinks
--     and aboutMe
-- Since then user text is stored verbatim, so those legacy rows show the
-- entities literally. This script decodes them. &amp; is decoded last, so the
-- text a user originally typed as "&lt;" (stored as "&amp;lt;") comes back as
-- "&lt;" rather than "<". (HTML tags the old filter removed cannot be restored.)
--
-- Safe to run more than once: the first run records the option
-- legacyHtmlEntitiesDecoded in voj_options (in the same transaction as the
-- updates) and every later run is a no-op, so text typed as "&amp;" after the
-- upgrade is never decoded. New installations get the marker from seed.sql.
-- Concurrent runs serialise on the unique option_name index.
--
-- Usage: mysql -u <user> -p <database> < sql/upgrades/decode-legacy-html-entities.sql
SET NAMES utf8mb4;

START TRANSACTION;

INSERT IGNORE INTO `voj_options` (`option_name`, `option_value`, `is_autoload`)
VALUES ('legacyHtmlEntitiesDecoded', '1', 0);
SET @voj_decode_legacy_entities = (ROW_COUNT() = 1);

UPDATE `voj_discussion_threads`
SET `discussion_thread_name` =
  REPLACE(REPLACE(REPLACE(REPLACE(`discussion_thread_name`,
    '&lt;', '<'), '&gt;', '>'), '&nbsp;', _utf8mb4 X'C2A0'), '&amp;', '&')
WHERE @voj_decode_legacy_entities AND `discussion_thread_name` LIKE '%&%';

UPDATE `voj_discussion_replies`
SET `discussion_reply_content` =
  REPLACE(REPLACE(REPLACE(REPLACE(`discussion_reply_content`,
    '&lt;', '<'), '&gt;', '>'), '&nbsp;', _utf8mb4 X'C2A0'), '&amp;', '&')
WHERE @voj_decode_legacy_entities AND `discussion_reply_content` LIKE '%&%';

UPDATE `voj_usermeta`
SET `meta_value` =
  REPLACE(REPLACE(REPLACE(REPLACE(`meta_value`,
    '&lt;', '<'), '&gt;', '>'), '&nbsp;', _utf8mb4 X'C2A0'), '&amp;', '&')
WHERE @voj_decode_legacy_entities
  AND `meta_key` IN ('displayName', 'location', 'website', 'socialLinks', 'aboutMe')
  AND `meta_value` LIKE '%&%';

COMMIT;
