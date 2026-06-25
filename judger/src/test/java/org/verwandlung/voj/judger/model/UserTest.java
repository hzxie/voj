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
package org.verwandlung.voj.judger.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class for the User model.
 *
 * @author Haozhe Xie
 */
public class UserTest {
  /**
   * Test case: tests the five-argument constructor. Expected result: the getters return the values
   * passed to the constructor and the identifier defaults to zero.
   */
  @Test
  public void testConstructorWithoutId() {
    UserGroup userGroup = new UserGroup(4, "judgers", "Judgers");
    Language language = new Language("cpp", "C++", "g++", "./a.out");
    User user = new User("judger", "password", "judger@voj.org", userGroup, language);

    Assertions.assertEquals(0, user.getUid());
    Assertions.assertEquals("judger", user.getUsername());
    Assertions.assertEquals("password", user.getPassword());
    Assertions.assertEquals("judger@voj.org", user.getEmail());
    Assertions.assertSame(userGroup, user.getUserGroup());
    Assertions.assertSame(language, user.getPreferLanguage());
  }

  /**
   * Test case: tests the six-argument constructor. Expected result: the getters return the values
   * passed to the constructor, including the identifier.
   */
  @Test
  public void testConstructorWithId() {
    UserGroup userGroup = new UserGroup(4, "judgers", "Judgers");
    Language language = new Language("cpp", "C++", "g++", "./a.out");
    User user = new User(1, "judger", "password", "judger@voj.org", userGroup, language);

    Assertions.assertEquals(1, user.getUid());
    Assertions.assertEquals("judger", user.getUsername());
  }

  /**
   * Test case: tests the default constructor together with the setters. Expected result: the
   * getters return the values set by the setters.
   */
  @Test
  public void testSetters() {
    UserGroup userGroup = new UserGroup(1, "users", "Users");
    Language language = new Language("java", "Java", "javac", "java");
    User user = new User();
    user.setUid(2);
    user.setUsername("alice");
    user.setPassword("secret");
    user.setEmail("alice@voj.org");
    user.setUserGroup(userGroup);
    user.setPreferLanguage(language);

    Assertions.assertEquals(2, user.getUid());
    Assertions.assertEquals("alice", user.getUsername());
    Assertions.assertEquals("secret", user.getPassword());
    Assertions.assertEquals("alice@voj.org", user.getEmail());
    Assertions.assertSame(userGroup, user.getUserGroup());
    Assertions.assertSame(language, user.getPreferLanguage());
  }

  /** Test case: tests toString. Expected result: a string containing the username. */
  @Test
  public void testToString() {
    User user = new User("judger", "password", "judger@voj.org", null, null);
    Assertions.assertTrue(user.toString().contains("judger"));
  }
}
