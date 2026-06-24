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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.Language;

/**
 * LanguageService测试类.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class LanguageServiceTest {
  /** 测试用例: 测试getAllLanguages()方法 测试数据: N/a 预期结果: 返回全部支持的编程语言 */
  @Test
  public void testGetAllLanguages() {
    List<Language> languages = languageService.getAllLanguages();
    Assertions.assertEquals(6, languages.size());
  }

  /** 测试用例: 测试getLanguageUsingSlug(String)方法 测试数据: 存在的编程语言别名 预期结果: 返回对应的编程语言对象 */
  @Test
  public void testGetLanguageUsingSlugExists() {
    Language language = languageService.getLanguageUsingSlug("text/x-java");
    Assertions.assertNotNull(language);
    Assertions.assertEquals("Java", language.getLanguageName());
  }

  /** 测试用例: 测试getLanguageUsingSlug(String)方法 测试数据: 不存在的编程语言别名 预期结果: 返回空引用 */
  @Test
  public void testGetLanguageUsingSlugNotExists() {
    Assertions.assertNull(languageService.getLanguageUsingSlug("text/x-not-exist"));
  }

  /** 测试用例: 测试updateLanguageSettings(List)方法 测试数据: 在现有语言基础上新增一门合法语言 预期结果: 更新成功, 语言总数加一 */
  @Test
  @SuppressWarnings("unchecked")
  public void testUpdateLanguageSettingsCreateLanguage() {
    List<Language> newLanguages = currentLanguages();
    newLanguages.add(new Language("text/x-go", "Go", "go build {filename}.go", "{filename}"));

    Map<String, Object> result = languageService.updateLanguageSettings(newLanguages);
    Assertions.assertTrue((Boolean) result.get("isSuccessful"));
    Assertions.assertEquals(1, ((List<Language>) result.get("languageCreated")).size());
    Assertions.assertEquals(7, languageService.getAllLanguages().size());
  }

  /** 测试用例: 测试updateLanguageSettings(List)方法 测试数据: 新增语言的别名为空 预期结果: 更新失败 */
  @Test
  public void testUpdateLanguageSettingsCreateLanguageWithEmptySlug() {
    List<Language> newLanguages = currentLanguages();
    newLanguages.add(new Language("", "Go", "go build {filename}.go", "{filename}"));

    Map<String, Object> result = languageService.updateLanguageSettings(newLanguages);
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
    Assertions.assertEquals(6, languageService.getAllLanguages().size());
  }

  /** 测试用例: 测试updateLanguageSettings(List)方法 测试数据: 新增语言的别名已存在 预期结果: 更新失败 */
  @Test
  public void testUpdateLanguageSettingsCreateLanguageWithDuplicatedSlug() {
    List<Language> newLanguages = currentLanguages();
    newLanguages.add(new Language("text/x-java", "Java Clone", "javac {filename}.java", "java -cp {filename}"));

    Map<String, Object> result = languageService.updateLanguageSettings(newLanguages);
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** 测试用例: 测试updateLanguageSettings(List)方法 测试数据: 新增语言的编译命令缺少{filename}占位符 预期结果: 更新失败 */
  @Test
  public void testUpdateLanguageSettingsCreateLanguageWithIllegalCompileCommand() {
    List<Language> newLanguages = currentLanguages();
    newLanguages.add(new Language("text/x-go", "Go", "go build main.go", "{filename}"));

    Map<String, Object> result = languageService.updateLanguageSettings(newLanguages);
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** 测试用例: 测试updateLanguageSettings(List)方法 测试数据: 修改一门现有语言的名称 预期结果: 更新成功, 语言名称被持久化 */
  @Test
  @SuppressWarnings("unchecked")
  public void testUpdateLanguageSettingsUpdateLanguage() {
    List<Language> newLanguages = currentLanguages();
    // 修改 C 语言 (language_id = 1) 的名称.
    for (Language language : newLanguages) {
      if (language.getLanguageId() == 1) {
        language.setLanguageName("C11");
      }
    }

    Map<String, Object> result = languageService.updateLanguageSettings(newLanguages);
    Assertions.assertTrue((Boolean) result.get("isSuccessful"));
    Assertions.assertEquals(1, ((List<Language>) result.get("languageUpdated")).size());
    Assertions.assertEquals("C11", languageService.getLanguageUsingSlug("text/x-csrc").getLanguageName());
  }

  /** 测试用例: 测试updateLanguageSettings(List)方法 测试数据: 删除一门未被使用的语言 预期结果: 更新成功, 语言总数减一 */
  @Test
  @SuppressWarnings("unchecked")
  public void testUpdateLanguageSettingsDeleteUnusedLanguage() {
    List<Language> newLanguages = currentLanguages();
    // Pascal (language_id = 4) 未被任何提交记录或用户使用.
    newLanguages.removeIf(language -> language.getLanguageId() == 4);

    Map<String, Object> result = languageService.updateLanguageSettings(newLanguages);
    Assertions.assertTrue((Boolean) result.get("isSuccessful"));
    Assertions.assertEquals(1, ((List<Language>) result.get("languageDeleted")).size());
    Assertions.assertEquals(5, languageService.getAllLanguages().size());
  }

  /** 测试用例: 测试updateLanguageSettings(List)方法 测试数据: 删除一门正在被使用的语言 预期结果: 更新失败, 语言不被删除 */
  @Test
  public void testUpdateLanguageSettingsDeleteLanguageInUse() {
    List<Language> newLanguages = currentLanguages();
    // C++ (language_id = 2) 被提交记录和用户偏好使用, 不允许删除.
    newLanguages.removeIf(language -> language.getLanguageId() == 2);

    Map<String, Object> result = languageService.updateLanguageSettings(newLanguages);
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
    Assertions.assertEquals(6, languageService.getAllLanguages().size());
  }

  /** 返回当前全部语言的可变副本, 用于构造updateLanguageSettings的输入. */
  private List<Language> currentLanguages() {
    List<Language> languages = new ArrayList<>();
    for (Language language : languageService.getAllLanguages()) {
      languages.add(
          new Language(
              language.getLanguageId(),
              language.getLanguageSlug(),
              language.getLanguageName(),
              language.getCompileCommand(),
              language.getRunCommand()));
    }
    return languages;
  }

  /** 待测试的LanguageService对象. */
  @Autowired private LanguageService languageService;
}
