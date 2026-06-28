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
package org.verwandlung.voj.web.mapper;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.Option;

/**
 * The test class for OptionMapper.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class OptionMapperTest {
  /** Test case: tests the getAutoloadOptions() method. Test data: N/a. Expected: the list of auto-loaded system options. */
  @Test
  public void testGetAutoloadOptions() {
    List<Option> options = optionMapper.getAutoloadOptions();
    Assertions.assertEquals(7, options.size());

    Option firstOption = options.get(0);
    int optionId = firstOption.getOptionId();
    Assertions.assertEquals(1, optionId);

    String optionName = firstOption.getOptionName();
    Assertions.assertEquals("websiteName", optionName);
  }

  /** Test case: tests the getOption(String) method. Test data: an existing option name. Expected: the expected Option object. */
  @Test
  public void testGetOptionExists() {
    Option option = optionMapper.getOption("websiteName");
    Assertions.assertNotNull(option);

    int optionId = option.getOptionId();
    Assertions.assertEquals(1, optionId);
  }

  /** Test case: tests the getOption(String) method. Test data: a non-existing option name. Expected: a null reference. */
  @Test
  public void testGetOptionNotExists() {
    Option option = optionMapper.getOption("notExistOption");
    Assertions.assertNull(option);
  }

  /** Test case: tests the updateOption(Option) method. Test data: an existing OptionId. Expected: the data update operation completes successfully. */
  @Test
  public void testUpdateOptionExists() {
    Option option = optionMapper.getOption("websiteName");
    option.setOptionValue("New OJ Platform");
    int numberOfRowsAffected = optionMapper.updateOption(option);
    Assertions.assertEquals(1, numberOfRowsAffected);

    Option newOption = optionMapper.getOption("websiteName");
    String optionValue = newOption.getOptionValue();
    Assertions.assertEquals("New OJ Platform", optionValue);
  }

  /** Test case: tests the updateOption(Option) method. Test data: a non-existing OptionId. Expected: the method executes normally without affecting the data in the table. */
  @Test
  public void testUpdateOptionNotExists() {
    Option option = optionMapper.getOption("websiteName");
    option.setOptionId(0);
    int numberOfRowsAffected = optionMapper.updateOption(option);
    Assertions.assertEquals(0, numberOfRowsAffected);
  }

  /** The OptionMapper object under test. */
  @Autowired private OptionMapper optionMapper;
}
