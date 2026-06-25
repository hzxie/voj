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
package org.verwandlung.voj.judger.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Provides the password encoder used to verify the judger's identity against the shared user table.
 * The scheme mirrors the web module: passwords are stored as {@code {bcrypt}...}, while legacy
 * un-prefixed MD5 digests are still matched for backward compatibility.
 *
 * @author Haozhe Xie
 */
@Configuration
public class SecurityConfig {
  /**
   * The password encoder. Matches both {@code {bcrypt}}-prefixed values and legacy bare MD5 digests.
   *
   * @return the delegating password encoder
   */
  @Bean
  @SuppressWarnings("deprecation")
  public PasswordEncoder passwordEncoder() {
    MessageDigestPasswordEncoder md5Encoder = new MessageDigestPasswordEncoder("MD5");
    Map<String, PasswordEncoder> encoders = new HashMap<>();
    encoders.put("bcrypt", new BCryptPasswordEncoder());
    encoders.put("MD5", md5Encoder);

    DelegatingPasswordEncoder passwordEncoder = new DelegatingPasswordEncoder("bcrypt", encoders);
    // Existing rows store a bare 32-character MD5 digest without an {id} prefix.
    passwordEncoder.setDefaultPasswordEncoderForMatches(md5Encoder);
    return passwordEncoder;
  }
}
