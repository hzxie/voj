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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The test class for JsonUtils.
 *
 * @author Haozhe Xie
 */
public class JsonUtilsTest {
  /** Test case: tests the toList(String, Class) method. Test data: a JSON array of integers (the way contest problem ids are stored). Expected: the corresponding list of integers. */
  @Test
  public void testToListOfIntegers() {
    List<Integer> problemIds = JsonUtils.toList("[1000, 1001, 1002]", Integer.class);
    Assertions.assertEquals(3, problemIds.size());
    Assertions.assertEquals(Integer.valueOf(1000), problemIds.get(0));
    Assertions.assertEquals(Integer.valueOf(1002), problemIds.get(2));
  }

  /** Test case: tests the toList(String, Class) method. Test data: an empty JSON array. Expected: an empty list. */
  @Test
  public void testToListOfEmptyArray() {
    List<String> values = JsonUtils.toList("[]", String.class);
    Assertions.assertTrue(values.isEmpty());
  }

  /** Test case: tests the toList(String, Class) method. Test data: a malformed JSON string. Expected: an IllegalArgumentException is thrown. */
  @Test
  public void testToListWithMalformedJson() {
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> JsonUtils.toList("[1000,", Integer.class));
  }

  /** Test case: tests the toMap(String) method. Test data: a JSON object string. Expected: the corresponding map of key-value pairs. */
  @Test
  public void testToMap() {
    Map<String, Object> map = JsonUtils.toMap("{\"name\": \"voj\", \"count\": 7}");
    Assertions.assertEquals("voj", map.get("name"));
    Assertions.assertEquals(7, map.get("count"));
  }

  /** Test case: tests the toJsonString(Object) method. Test data: a map of key-value pairs. Expected: the corresponding JSON object string. */
  @Test
  public void testToJsonString() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("name", "voj");
    map.put("count", 7);

    Assertions.assertEquals("{\"name\":\"voj\",\"count\":7}", JsonUtils.toJsonString(map));
  }

  /** Test case: tests the toMap(String) followed by toJsonString(Object) round trip. Test data: a JSON object string. Expected: an equivalent JSON object string. */
  @Test
  public void testRoundTrip() {
    String json = "{\"count\":7,\"name\":\"voj\"}";
    Assertions.assertEquals(json, JsonUtils.toJsonString(JsonUtils.toMap(json)));
  }
}
