/* Verwandlung Online Judge - A cross-platform judge online system
 * Copyright (C) 2014-2026 Haozhe Xie <root@haozhexie.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.verwandlung.voj.web.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The test class for the message bundles (src/main/resources/localization).
 *
 * <p>Every locale must define exactly the keys of the English bundle (a missing key renders as
 * {@code ??key_locale??}), and every key a template names literally must exist.
 *
 * @author Haozhe Xie
 */
public class LocalizationFilesTest {
  /** Test case: every locale defines each key of voj_en_US once, and no other key. */
  @Test
  public void testLocalesDefineTheSameKeys() throws IOException {
    Set<String> english = keysOf("en_US");
    for (String locale : LOCALES) {
      Assertions.assertEquals(english, keysOf(locale), "keys of voj_" + locale);
    }
  }

  /** Test case: every message key named literally in a template is defined. */
  @Test
  public void testTemplatesUseDefinedKeys() throws IOException {
    Set<String> english = keysOf("en_US");
    Pattern reference =
        Pattern.compile("#\\{(voj\\.[A-Za-z0-9.-]+)[}(]|#messages\\.msg\\('(voj\\.[A-Za-z0-9.-]+)'\\)");
    List<String> missing = new ArrayList<>();
    try (Stream<Path> files = Files.walk(TEMPLATES)) {
      for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".html"))::iterator) {
        Matcher m = reference.matcher(Files.readString(file, StandardCharsets.UTF_8));
        while (m.find()) {
          String key = m.group(1) != null ? m.group(1) : m.group(2);
          if (!english.contains(key)) {
            missing.add(TEMPLATES.relativize(file) + ": " + key);
          }
        }
      }
    }
    Assertions.assertEquals(List.of(), missing);
  }

  /** Returns the keys of a locale's bundle, failing on a key defined twice. */
  private static Set<String> keysOf(String locale) throws IOException {
    Set<String> keys = new TreeSet<>();
    Path bundle = BUNDLES.resolve("voj_" + locale + ".properties");
    for (String line : Files.readAllLines(bundle, StandardCharsets.UTF_8)) {
      String trimmed = line.trim();
      if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
        continue;
      }
      String key = trimmed.substring(0, trimmed.indexOf('=')).trim();
      Assertions.assertTrue(keys.add(key), key + " is defined twice in " + bundle);
    }
    return keys;
  }

  private static final List<String> LOCALES =
      List.of("en_US", "ja_JP", "ko_KR", "ru_RU", "vi_VN", "zh_CN", "zh_TW");
  private static final Path BUNDLES = Path.of("src/main/resources/localization");
  private static final Path TEMPLATES = Path.of("src/main/resources/templates");
}
