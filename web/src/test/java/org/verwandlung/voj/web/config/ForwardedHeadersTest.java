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

import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

/**
 * Tests which addresses the web application takes X-Forwarded-For and X-Forwarded-Proto from, as
 * application.properties configures Tomcat.
 *
 * @author Haozhe Xie
 */
public class ForwardedHeadersTest {
  /**
   * Test case: tests server.tomcat.remoteip.internal-proxies without VOJ_TRUSTED_PROXIES. Test
   * data: loopback and private addresses. Expected: only the loopback addresses are trusted, so
   * that a client on a private network cannot set its own address.
   */
  @Test
  public void testTrustsLoopbackProxiesOnly() throws Exception {
    StandardEnvironment environment = new StandardEnvironment();
    for (PropertySource<?> propertySource :
        new PropertiesPropertySourceLoader()
            .load("application", new ClassPathResource("application.properties"))) {
      environment.getPropertySources().addLast(propertySource);
    }
    Assertions.assertNull(environment.getProperty("VOJ_TRUSTED_PROXIES"));
    Pattern internalProxies =
        Pattern.compile(environment.getProperty("server.tomcat.remoteip.internal-proxies"));

    for (String address : new String[] {"127.0.0.1", "0:0:0:0:0:0:0:1", "::1"}) {
      Assertions.assertTrue(internalProxies.matcher(address).matches(), address);
    }
    for (String address :
        new String[] {"10.0.0.1", "172.17.0.1", "192.168.1.1", "127.0.0.10", "127a0b0c1"}) {
      Assertions.assertFalse(internalProxies.matcher(address).matches(), address);
    }
  }
}
