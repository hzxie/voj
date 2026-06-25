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
package org.verwandlung.voj.judger.config;

import javax.sql.DataSource;

import com.alibaba.druid.pool.DruidDataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class for PersistenceConfig.
 *
 * @author Haozhe Xie
 */
public class PersistenceConfigTest {
  /**
   * Test case: tests the dataSource bean. Expected result: a Druid data source configured with the
   * supplied connection properties is returned. The data source is not initialized, so no actual
   * database connection is opened.
   */
  @Test
  public void testDataSource() {
    PersistenceConfig persistenceConfig = new PersistenceConfig();
    DataSource dataSource =
        persistenceConfig.dataSource(
            "com.mysql.cj.jdbc.Driver",
            "jdbc:mysql://localhost:3306/voj",
            "root",
            "",
            1,
            20,
            1,
            60000L,
            60000L,
            300000L,
            true,
            1800);

    Assertions.assertInstanceOf(DruidDataSource.class, dataSource);
    DruidDataSource druidDataSource = (DruidDataSource) dataSource;
    Assertions.assertEquals("com.mysql.cj.jdbc.Driver", druidDataSource.getDriverClassName());
    Assertions.assertEquals("jdbc:mysql://localhost:3306/voj", druidDataSource.getUrl());
    Assertions.assertEquals("root", druidDataSource.getUsername());
    Assertions.assertEquals(1, druidDataSource.getInitialSize());
    Assertions.assertEquals(20, druidDataSource.getMaxActive());
    Assertions.assertEquals("SELECT 1", druidDataSource.getValidationQuery());
  }
}
