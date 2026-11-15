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
package org.verwandlung.voj.web.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

/**
 * The test class for the JSON exposure of {@link User}.
 *
 * @author Haozhe Xie
 */
public class UserTest {
  /** Test case: serializes a User the way the JSON action endpoints do. Test data: a user with a password and an email address. Expected: only the public fields (uid, username) are written. */
  @Test
  public void testJsonOmitsPrivateFields() {
    User user =
        new User(
            1000,
            "zjhzxhz",
            "secret-hash",
            "cshzxie@gmail.com",
            new UserGroup(2, "users", "Users"),
            null);

    String json = JsonMapper.builder().build().writeValueAsString(user);

    Assertions.assertTrue(json.contains("\"username\":\"zjhzxhz\""), json);
    Assertions.assertFalse(json.contains("cshzxie@gmail.com"), json);
    Assertions.assertFalse(json.contains("\"email\""), json);
    Assertions.assertFalse(json.contains("secret-hash"), json);
  }
}
