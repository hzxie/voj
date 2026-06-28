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
package org.verwandlung.voj.web.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.mapper.OptionMapper;
import org.verwandlung.voj.web.model.Option;

/**
 * The test class for TurnstileService. The successful-verification path is intentionally not
 * exercised because it would require a live call to Cloudflare; the tests cover the configuration
 * gating and the fail-closed behaviour, which are the parts that protect the application.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class TurnstileServiceTest {
  /** Test case: Turnstile is disabled by default. Expected: it reports disabled and lets requests through. */
  @Test
  public void testDisabledByDefault() {
    Assertions.assertFalse(turnstileService.isEnabled());
    Assertions.assertTrue(turnstileService.verify("", "127.0.0.1"));
    Assertions.assertTrue(turnstileService.verify(null, "127.0.0.1"));
  }

  /** Test case: the option is on but no secret key is configured. Expected: it stays disabled so the site keeps working. */
  @Test
  public void testEnabledFlagWithoutSecretKeyStaysDisabled() {
    setOption("turnstileEnabled", "1");
    Assertions.assertFalse(turnstileService.isEnabled());
    Assertions.assertTrue(turnstileService.verify("", "127.0.0.1"));
  }

  /** Test case: Turnstile is fully configured and the token is empty. Expected: it is enabled and fails closed without calling Cloudflare. */
  @Test
  public void testEnabledRejectsEmptyToken() {
    setOption("turnstileEnabled", "1");
    setOption("turnstileSecretKey", "0x-test-secret");
    Assertions.assertTrue(turnstileService.isEnabled());
    Assertions.assertFalse(turnstileService.verify("", "127.0.0.1"));
    Assertions.assertFalse(turnstileService.verify(null, "127.0.0.1"));
  }

  /** Sets an option value within the current test transaction. */
  private void setOption(String name, String value) {
    Option option = optionMapper.getOption(name);
    option.setOptionValue(value);
    optionMapper.updateOption(option);
  }

  /** The TurnstileService object under test. */
  @Autowired private TurnstileService turnstileService;

  /** The Mapper used to toggle the Turnstile configuration within the test transaction. */
  @Autowired private OptionMapper optionMapper;
}
