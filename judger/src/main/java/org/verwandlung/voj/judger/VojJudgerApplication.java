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
 * The application entry point of the Verwandlung Online Judge judger.
 *
 * <p>The judger is a non-web Spring Boot application that, once started, listens for judging tasks
 * via ActiveMQ. Because the JMS listener container holds non-daemon threads, the JVM does not exit
 * while it is listening.
 *
 * @author Haozhe Xie
 */
@SpringBootApplication
@MapperScan("org.verwandlung.voj.judger.mapper")
@PropertySource("classpath:voj.properties")
public class VojJudgerApplication {
  /** The application entry point. */
  public static void main(String[] args) {
    LOGGER.info("Starting Verwandlung Online Judge Judger...");
    new SpringApplicationBuilder(VojJudgerApplication.class)
        .web(WebApplicationType.NONE)
        .run(args);
    LOGGER.info("Verwandlung Online Judge Judger started.");
  }

  /**
   * Initialization task executed after the application starts: logs runtime environment information
   * and starts the heartbeat with the web module.
   *
   * @param languageMapper - the data access object for programming languages
   * @param heartbeat - the heartbeat task
   * @return an ApplicationRunner instance
   */
  @Bean
  public ApplicationRunner startupRunner(
      LanguageMapper languageMapper, ApplicationHeartbeat heartbeat) {
    return args -> {
      logSystemEnvironment(languageMapper);
      setupHeartBeat(heartbeat);
    };
  }

  /** Configures the heartbeat connection with the web module, periodically sending Keep-Alive signals to it. */
  private void setupHeartBeat(ApplicationHeartbeat heartbeat) {
    final int INITIAL_DELAY = 0;
    // A short interval keeps the web dashboard's telemetry (CPU/memory/uptime) and online status
    // live; the web side treats a judger as offline after a few missed heartbeats.
    final int PERIOD = 30;

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    scheduler.scheduleAtFixedRate(heartbeat, INITIAL_DELAY, PERIOD, TimeUnit.SECONDS);
  }

  /** Logs the system environment variables to make bug reproduction easier. */
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
   * Gets the compiler program from the compile command line.
   *
   * @param compileCommand - the command line of the compile command
   * @return the compiler program
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
   * Gets the compiler's version information.
   *
   * @param languageName - the name of the programming language
   * @param compileProgram - the command used for compilation
   * @return the compiler's version information
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
   * Gets the command-line argument used to obtain the compiler version.
   *
   * @param languageName - the name of the programming language
   * @return the command-line argument used to obtain the compiler version
   */
  private String getVersionCommand(String languageName) {
    if ("Java".equalsIgnoreCase(languageName)) {
      return " -version";
    }
    return " --version";
  }

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(VojJudgerApplication.class);
}
