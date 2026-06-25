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
package org.verwandlung.voj.web.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * The test class for the {@link PasswordEncoder} produced by {@link SecurityConfig}. Validates the
 * legacy-MD5 compatibility and the transparent bcrypt upgrade path, which are the security-critical
 * parts of the password migration.
 *
 * @author Haozhe Xie
 */
public class SecurityConfigTest {
  /** The bare (un-prefixed) MD5 digest of {@code "zjhzxhz"}, as stored by the legacy system. */
  private static final String LEGACY_MD5 = "785ee107c11dfe36de668b1ae7baacbb";

  /** The plaintext password whose MD5 digest is {@link #LEGACY_MD5}. */
  private static final String PLAINTEXT = "zjhzxhz";

  /** The password encoder under test. */
  private final PasswordEncoder passwordEncoder = new SecurityConfig().passwordEncoder();

  /** Test case: a legacy bare-MD5 hash is matched against the correct plaintext. Expected: matches. */
  @Test
  public void testMatchesLegacyMd5() {
    Assertions.assertTrue(passwordEncoder.matches(PLAINTEXT, LEGACY_MD5));
    Assertions.assertFalse(passwordEncoder.matches("wrong-password", LEGACY_MD5));
  }

  /** Test case: a legacy bare-MD5 hash should be flagged for upgrade. Expected: upgrade is needed. */
  @Test
  public void testLegacyMd5NeedsUpgrade() {
    Assertions.assertTrue(passwordEncoder.upgradeEncoding(LEGACY_MD5));
  }

  /** Test case: a freshly encoded password uses bcrypt and matches its plaintext. Expected: bcrypt prefix and a successful match. */
  @Test
  public void testEncodeUsesBcrypt() {
    String encoded = passwordEncoder.encode(PLAINTEXT);
    Assertions.assertTrue(encoded.startsWith("{bcrypt}"));
    Assertions.assertTrue(passwordEncoder.matches(PLAINTEXT, encoded));
    Assertions.assertFalse(passwordEncoder.upgradeEncoding(encoded));
  }
}
