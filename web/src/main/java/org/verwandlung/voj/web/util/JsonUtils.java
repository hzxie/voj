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
 * 基于Jackson的JSON序列化与反序列化工具类.
 *
 * @author Haozhe Xie
 */
public class JsonUtils {
  /** Utility classes should not have a public constructor. */
  private JsonUtils() {}

  /**
   * 将JSON数组字符串反序列化为指定元素类型的List对象.
   *
   * @param json - JSON数组字符串
   * @param elementType - List中元素的类型
   * @return 反序列化后的List对象
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
   * 将JSON对象字符串反序列化为Map对象.
   *
   * @param json - JSON对象字符串
   * @return 反序列化后的Map对象
   */
  public static Map<String, Object> toMap(String json) {
    return toObject(json, new TypeReference<Map<String, Object>>() {});
  }

  /**
   * 将JSON字符串反序列化为指定泛型类型的对象.
   *
   * @param json - JSON字符串
   * @param typeReference - 目标类型的TypeReference
   * @return 反序列化后的对象
   */
  public static <T> T toObject(String json, TypeReference<T> typeReference) {
    try {
      return MAPPER.readValue(json, typeReference);
    } catch (Exception ex) {
      throw new IllegalArgumentException("Failed to parse JSON: " + json, ex);
    }
  }

  /**
   * 将对象序列化为JSON字符串.
   *
   * @param object - 待序列化的对象
   * @return 序列化后的JSON字符串
   */
  public static String toJsonString(Object object) {
    try {
      return MAPPER.writeValueAsString(object);
    } catch (Exception ex) {
      throw new IllegalArgumentException("Failed to serialize object to JSON", ex);
    }
  }

  /** 共享的Jackson ObjectMapper对象, 线程安全. */
  private static final ObjectMapper MAPPER = new ObjectMapper();
}
