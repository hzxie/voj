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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

/**
 * The JSON serialization and deserialization utility class based on Jackson.
 *
 * @author Haozhe Xie
 */
public class JsonUtils {
  /** Utility classes should not have a public constructor. */
  private JsonUtils() {}

  /**
   * Deserializes a JSON array string into a List object of the specified element type.
   *
   * @param json - the JSON array string
   * @param elementType - the type of the elements in the List
   * @return the deserialized List object
   */
  public static <T> List<T> toList(String json, Class<T> elementType) {
    try {
      return MAPPER.readValue(
          json, MAPPER.getTypeFactory().constructCollectionType(List.class, elementType));
    } catch (Exception ex) {
      throw new IllegalArgumentException("Failed to parse JSON array: " + json, ex);
    }
  }

  /**
   * Deserializes a JSON object string into a Map object.
   *
   * @param json - the JSON object string
   * @return the deserialized Map object
   */
  public static Map<String, Object> toMap(String json) {
    return toObject(json, new TypeReference<Map<String, Object>>() {});
  }

  /**
   * Deserializes a JSON string into an object of the specified generic type.
   *
   * @param json - the JSON string
   * @param typeReference - the TypeReference of the target type
   * @return the deserialized object
   */
  public static <T> T toObject(String json, TypeReference<T> typeReference) {
    try {
      return MAPPER.readValue(json, typeReference);
    } catch (Exception ex) {
      throw new IllegalArgumentException("Failed to parse JSON: " + json, ex);
    }
  }

  /**
   * Serializes an object into a JSON string.
   *
   * @param object - the object to serialize
   * @return the serialized JSON string
   */
  public static String toJsonString(Object object) {
    try {
      return MAPPER.writeValueAsString(object);
    } catch (Exception ex) {
      throw new IllegalArgumentException("Failed to serialize object to JSON", ex);
    }
  }

  /** The shared Jackson ObjectMapper object, which is thread-safe. */
  private static final ObjectMapper MAPPER = new ObjectMapper();
}
