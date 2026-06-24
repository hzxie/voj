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
package org.verwandlung.voj.judger;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

import org.verwandlung.voj.judger.application.ApplicationHeartbeat;
import org.verwandlung.voj.judger.mapper.LanguageMapper;
import org.verwandlung.voj.judger.model.Language;

/**
 * Verwandlung Online Judge 评测机的应用程序入口.
 *
 * <p>评测机是一个非 Web 的 Spring Boot 应用, 启动后通过 ActiveMQ 监听评测任务. 由于 JMS
 * 监听容器持有非守护线程, JVM 在监听期间不会退出.
 *
 * @author Haozhe Xie
 */
@SpringBootApplication
@MapperScan("org.verwandlung.voj.judger.mapper")
@PropertySource("classpath:voj.properties")
public class VojJudgerApplication {
  /** 应用程序入口. */
  public static void main(String[] args) {
    LOGGER.info("Starting Verwandlung Online Judge Judger...");
    new SpringApplicationBuilder(VojJudgerApplication.class)
        .web(WebApplicationType.NONE)
        .run(args);
    LOGGER.info("Verwandlung Online Judge Judger started.");
  }

  /**
   * 应用启动后执行的初始化任务: 输出运行环境信息并启动与 Web 模块的心跳.
   *
   * @param languageMapper - 编程语言数据访问对象
   * @param heartbeat - 心跳任务
   * @return 一个 ApplicationRunner 实例
   */
  @Bean
  public ApplicationRunner startupRunner(
      LanguageMapper languageMapper, ApplicationHeartbeat heartbeat) {
    return args -> {
      logSystemEnvironment(languageMapper);
      setupHeartBeat(heartbeat);
    };
  }

  /** 配置与 Web 模块的心跳连接. 定时向 Web 模块发送 Keep-Alive 信号. */
  private void setupHeartBeat(ApplicationHeartbeat heartbeat) {
    final int INITIAL_DELAY = 0;
    final int PERIOD = 25;

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    scheduler.scheduleAtFixedRate(heartbeat, INITIAL_DELAY, PERIOD, TimeUnit.MINUTES);
  }

  /** 获取系统环境变量. 以便进行 Bug 的复现. */
  private void logSystemEnvironment(LanguageMapper languageMapper) {
    LOGGER.info("System Information: ");
    LOGGER.info("\tOperating System Name: " + System.getProperty("os.name"));
    LOGGER.info("\tOperating System Version: " + System.getProperty("os.version"));
    LOGGER.info("\tJava VM Name: " + System.getProperty("java.vm.name"));
    LOGGER.info("\tJava Runtime Version: " + System.getProperty("java.runtime.version"));

    LOGGER.info("Compiler Information: ");
    List<Language> languages = languageMapper.getAllLanguages();
    for (Language language : languages) {
      String languageName = language.getLanguageName();
      String compileProgram = getCompileProgram(language.getCompileCommand());
      LOGGER.info("\t" + languageName + ": " + getCompilerVersion(languageName, compileProgram));
    }
  }

  /**
   * 获取编译程序的命令行.
   *
   * @param compileCommand - 编译命令的命令行
   * @return 编译程序的命令行
   */
  private String getCompileProgram(String compileCommand) {
    int firstSpaceIndex = compileCommand.indexOf(" ");
    String compileProgram = compileCommand.substring(0, firstSpaceIndex);

    if ("javac".equalsIgnoreCase(compileProgram)) {
      return "java";
    }
    return compileProgram;
  }

  /**
   * 获取编译器的版本信息.
   *
   * @param languageName - 编程语言名称
   * @param compileProgram - 编译所使用的命令
   * @return 编译器的版本信息
   */
  private String getCompilerVersion(String languageName, String compileProgram) {
    String versionCommand = getVersionCommand(languageName);
    StringBuilder compilerVersion = new StringBuilder();

    try {
      String command = compileProgram + versionCommand;
      Process process = Runtime.getRuntime().exec(command);

      compilerVersion.append("Command Line: " + command + "\n");
      compilerVersion.append(IOUtils.toString(process.getInputStream()));
      compilerVersion.append(IOUtils.toString(process.getErrorStream()));
    } catch (Exception ex) {
      return "Not Found";
    }
    return compilerVersion.toString();
  }

  /**
   * 获取编译器版本的命令行参数.
   *
   * @param languageName - 编程语言名称
   * @return 获取编译器版本的命令行参数
   */
  private String getVersionCommand(String languageName) {
    if ("Java".equalsIgnoreCase(languageName)) {
      return " -version";
    }
    return " --version";
  }

  /** 日志记录器. */
  private static final Logger LOGGER = LogManager.getLogger(VojJudgerApplication.class);
}
