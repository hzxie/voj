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

import java.util.Collections;
import java.util.Properties;

import javax.sql.DataSource;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

/**
 * The persistence layer configuration. Replaces the Druid data source definition in the original
 * dispatcher-servlet.xml, and exposes voj.properties as a Spring Bean, so that JSPs can read
 * information such as the build version number and CDN address via {@code
 * @propertyConfigurer.getProperty('...')}.
 *
 * @author Haozhe Xie
 */
@Configuration
public class PersistenceConfig {
  /** The Druid database connection pool. The MyBatis auto-configuration reuses this data source. */
  @Bean(initMethod = "init", destroyMethod = "close")
  public DataSource dataSource(
      @Value("${jdbc.driverClassName}") String driverClassName,
      @Value("${jdbc.url}") String url,
      @Value("${jdbc.username}") String username,
      @Value("${jdbc.password}") String password,
      @Value("${jdbc.initialSize}") int initialSize,
      @Value("${jdbc.maxActive}") int maxActive,
      @Value("${jdbc.minIdle}") int minIdle,
      @Value("${jdbc.maxWait}") long maxWait,
      @Value("${jdbc.timeBetweenEvictionRunsMillis}") long timeBetweenEvictionRunsMillis,
      @Value("${jdbc.minEvictableIdleTimeMillis}") long minEvictableIdleTimeMillis,
      @Value("${jdbc.removeAbandoned}") boolean removeAbandoned,
      @Value("${jdbc.removeAbandonedTimeout}") int removeAbandonedTimeout) {
    DruidDataSource dataSource = new DruidDataSource();
    dataSource.setDriverClassName(driverClassName);
    dataSource.setUrl(url);
    dataSource.setUsername(username);
    dataSource.setPassword(password);
    dataSource.setInitialSize(initialSize);
    dataSource.setMaxActive(maxActive);
    dataSource.setMinIdle(minIdle);
    dataSource.setMaxWait(maxWait);
    dataSource.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
    dataSource.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
    dataSource.setRemoveAbandoned(removeAbandoned);
    dataSource.setRemoveAbandonedTimeout(removeAbandonedTimeout);
    dataSource.setValidationQuery("SELECT 1");
    dataSource.setConnectionInitSqls(Collections.singletonList("SET NAMES utf8mb4;"));
    // Validate idle connections in the background only. testOnBorrow/testOnReturn would run the
    // validationQuery on every borrow/return, adding a round-trip to the hot path; testWhileIdle is
    // the recommended Druid setup.
    dataSource.setTestWhileIdle(true);
    dataSource.setTestOnBorrow(false);
    dataSource.setTestOnReturn(false);
    return dataSource;
  }

  /**
   * Exposes voj.properties (plus the build-time version.properties) as a {@link
   * java.util.Properties} Bean named {@code propertyConfigurer}, so that the SpEL expression {@code
   * @propertyConfigurer.getProperty('...')} in JSPs remains usable. version.properties carries
   * build.version / product.version, filled in by Maven resource filtering at build time.
   *
   * <p>Unlike the {@code @Value("${...}")} injection points, a raw {@link PropertiesFactoryBean}
   * does not resolve {@code ${...}} placeholders, so each value is run through the {@link
   * Environment} here. That keeps env-driven values such as {@code url.cdn = ${VOJ_BASE_URL:...}}
   * from leaking the literal placeholder string into the rendered pages.
   */
  @Bean(name = "propertyConfigurer")
  public Properties propertyConfigurer(Environment environment) throws Exception {
    PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
    propertiesFactoryBean.setLocations(
        new ClassPathResource("voj.properties"), new ClassPathResource("version.properties"));
    propertiesFactoryBean.afterPropertiesSet();
    Properties raw = propertiesFactoryBean.getObject();

    Properties resolved = new Properties();
    if (raw != null) {
      for (String name : raw.stringPropertyNames()) {
        resolved.setProperty(name, environment.resolvePlaceholders(raw.getProperty(name)));
      }
    }
    return resolved;
  }
}
