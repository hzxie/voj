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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.Option;

/**
 * OptionService测试类.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class OptionServiceTest {
  /** 测试用例: 测试getOptions()方法 测试数据: N/a 预期结果: 返回全部系统选项 */
  @Test
  public void testGetOptions() {
    List<Option> options = optionService.getOptions();
    Assertions.assertEquals(8, options.size());
  }

  /** 测试用例: 测试getAutoloadOptions()方法 测试数据: N/a 预期结果: 仅返回自动加载的系统选项 */
  @Test
  public void testGetAutoloadOptions() {
    List<Option> options = optionService.getAutoloadOptions();
    Assertions.assertEquals(6, options.size());
  }

  /** 测试用例: 测试getOption(String)方法 测试数据: 存在的选项名称 预期结果: 返回对应的Option对象 */
  @Test
  public void testGetOptionExists() {
    Option option = optionService.getOption("websiteName");
    Assertions.assertNotNull(option);
    Assertions.assertEquals(1, option.getOptionId());
  }

  /** 测试用例: 测试getOption(String)方法 测试数据: 不存在的选项名称 预期结果: 返回空引用 */
  @Test
  public void testGetOptionNotExists() {
    Assertions.assertNull(optionService.getOption("notExistOption"));
  }

  /** 测试用例: 测试updateOptions(Map)方法 测试数据: 合法的键值对 预期结果: 系统选项被更新 */
  @Test
  public void testUpdateOptionsUsingMap() {
    Map<String, String> optionMap = new HashMap<>();
    optionMap.put("websiteName", "New OJ Platform");
    optionService.updateOptions(optionMap);

    Assertions.assertEquals("New OJ Platform", optionService.getOption("websiteName").getOptionValue());
  }

  /** 测试用例: 测试updateOptions(...)方法 测试数据: 全部合法的系统选项 预期结果: 更新成功, 并持久化网站名称 */
  @Test
  public void testUpdateOptionsWithLegalArguments() {
    Map<String, Boolean> result =
        optionService.updateOptions(
            "Verwandlung OJ",
            "An online judge platform",
            "Copyright (C) Haozhe Xie",
            true,
            "浙ICP备15017174号",
            "浙公网安备33010202000766号",
            "<script>console.log('ga');</script>",
            "shit,fuck");
    Assertions.assertTrue(result.get("isSuccessful"));
    Assertions.assertEquals("Verwandlung OJ", optionService.getOption("websiteName").getOptionValue());
  }

  /** 测试用例: 测试updateOptions(...)方法 测试数据: 空的网站名称 预期结果: 更新失败 (isWebsiteNameEmpty) */
  @Test
  public void testUpdateOptionsWithEmptyWebsiteName() {
    Map<String, Boolean> result =
        optionService.updateOptions(
            "", "Description", "Copyright", true, "", "", "", "");
    Assertions.assertTrue(result.get("isWebsiteNameEmpty"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试updateOptions(...)方法 测试数据: 过长的网站名称 (超过32字符) 预期结果: 更新失败 (isWebisteNameLegal为false) */
  @Test
  public void testUpdateOptionsWithTooLongWebsiteName() {
    String tooLongName = "x".repeat(33);
    Map<String, Boolean> result =
        optionService.updateOptions(
            tooLongName, "Description", "Copyright", true, "", "", "", "");
    Assertions.assertFalse(result.get("isWebisteNameLegal"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试updateOptions(...)方法 测试数据: 过长的网站描述 (超过128字符) 预期结果: 更新失败 (isDescriptionLegal为false) */
  @Test
  public void testUpdateOptionsWithTooLongDescription() {
    String tooLongDescription = "x".repeat(129);
    Map<String, Boolean> result =
        optionService.updateOptions(
            "OJ", tooLongDescription, "Copyright", true, "", "", "", "");
    Assertions.assertFalse(result.get("isDescriptionLegal"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试updateOptions(...)方法 测试数据: 非法的网站备案号 预期结果: 更新失败 (isIcpNumberLegal为false) */
  @Test
  public void testUpdateOptionsWithIllegalIcpNumber() {
    Map<String, Boolean> result =
        optionService.updateOptions(
            "OJ", "Description", "Copyright", true, "Illegal ICP", "", "", "");
    Assertions.assertFalse(result.get("isIcpNumberLegal"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试updateOptions(...)方法 测试数据: 非法的Google Analytics代码 预期结果: 更新失败 (isAnalyticsCodeLegal为false) */
  @Test
  public void testUpdateOptionsWithIllegalAnalyticsCode() {
    Map<String, Boolean> result =
        optionService.updateOptions(
            "OJ", "Description", "Copyright", true, "", "", "not-a-script-tag", "");
    Assertions.assertFalse(result.get("isAnalyticsCodeLegal"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 待测试的OptionService对象. */
  @Autowired private OptionService optionService;
}
