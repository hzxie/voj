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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

import org.verwandlung.voj.judger.application.ApplicationHeartbeat;
import org.verwandlung.voj.judger.core.CheckpointStore;
import org.verwandlung.voj.judger.core.Dispatcher;
import org.verwandlung.voj.judger.core.SandboxAccountLock;
import org.verwandlung.voj.judger.mapper.LanguageMapper;
import org.verwandlung.voj.judger.model.Language;

/**
 * The application entry point of the Verwandlung Online Judge judger.
 *
 * <p>The judger is a non-web Spring Boot application that, once started, takes the pending
 * submissions from the database and judges them on a thread of its own, which keeps the JVM
 * running, and sends their progress and its heartbeats to the web application via ActiveMQ.
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
   * Initialization task executed after the application starts: logs runtime environment
   * information, starts the heartbeat with the web module and the judging of the pending
   * submissions.
   *
   * @param languageMapper - the data access object for programming languages
   * @param heartbeat - the heartbeat task
   * @param sandboxAccountLock - the lock that keeps other judgers off this judger's sandbox account
   * @param checkpointStore - the local cache of test data, opened (and cleaned up) here
   * @param dispatcher - the judger dispatcher, which judges the pending submissions once the
   *     identity check passes
   * @param apiToken - the shared secret for downloading test data from the web application
   * @return an ApplicationRunner instance
   */
  @Bean
  public ApplicationRunner startupRunner(
      LanguageMapper languageMapper,
      ApplicationHeartbeat heartbeat,
      SandboxAccountLock sandboxAccountLock,
      CheckpointStore checkpointStore,
      Dispatcher dispatcher,
      @Value("${judger.apiToken:}") String apiToken) {
    return args -> {
      if (apiToken == null || apiToken.isBlank()) {
        // The web application refuses to hand out test data without a shared secret.
        LOGGER.error(
            "judger.apiToken (VOJ_JUDGER_API_TOKEN) is empty: the web application refuses every"
                + " test data download, so every submission will be judged a System Error. Set it"
                + " to the judger.apiToken of the web application.");
      }
      // Fail fast: verify the judger's credentials against the shared user table before taking any
      // submission. Otherwise a misconfigured judger would accept submissions and abort them
      // mid-judging (leaving them stuck) once the heartbeat later detected the bad credentials.
      if (!heartbeat.isIdentityValid()) {
        LOGGER.error("Unauthorized: please check judger.username / judger.password. Shutting down.");
        System.exit(-1);
      }
      logSystemEnvironment();
      // Two judgers running submissions as one account could interfere with each other's runs.
      sandboxAccountLock.acquire();
      // Claims this judger's test data cache and work directory, and removes the downloads and the
      // judgings a previous run left unfinished. A checkpoint or work directory that cannot be
      // used fails every judging, so it fails the startup.
      checkpointStore.open();

      List<Integer> supportedLanguageIds = detectSupportedLanguages(languageMapper);
      setupHeartBeat(heartbeat);
      if (supportedLanguageIds.isEmpty()) {
        // No usable toolchain on this host. Rather than taking submissions we cannot build (and
        // failing them with a bogus Compile Error), stay online for telemetry but never take one.
        // The submissions no judger takes are judged a System Error by the web application once
        // they waited too long (judging.maxWait).
        LOGGER.error(
            "No supported language compilers were detected; this judger will not take any "
                + "submission. Check the toolchain installation.");
        return;
      }
      // Only take the submissions this judger can actually build: the others wait in the database
      // for a capable judger.
      LOGGER.info("Judging the submissions in the languages " + supportedLanguageIds);
      Thread judging =
          new Thread(() -> dispatcher.judgeSubmissions(supportedLanguageIds), "voj-judging");
      judging.start();
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
  private void logSystemEnvironment() {
    LOGGER.info("System Information: ");
    LOGGER.info("\tOperating System Name: " + System.getProperty("os.name"));
    LOGGER.info("\tOperating System Version: " + System.getProperty("os.version"));
    LOGGER.info("\tJava VM Name: " + System.getProperty("java.vm.name"));
    LOGGER.info("\tJava Runtime Version: " + System.getProperty("java.runtime.version"));
  }

  /**
   * Probes the toolchain for every language known to the system and returns those this host can
   * actually compile. A language counts as supported when its compiler can be launched; the
   * compiler version is logged along the way to make bug reproduction easier.
   *
   * @param languageMapper - the data access object for programming languages
   * @return the unique identifiers of the languages this judger supports
   */
  private List<Integer> detectSupportedLanguages(LanguageMapper languageMapper) {
    LOGGER.info("Compiler Information: ");
    List<Integer> supportedLanguageIds = new ArrayList<>();
    List<Language> languages = languageMapper.getAllLanguages();
    for (Language language : languages) {
      String languageName = language.getLanguageName();
      String compileProgram = getCompileProgram(language.getCompileCommand());
      String compilerVersion =
          compileProgram == null ? null : getCompilerVersion(languageName, compileProgram);
      LOGGER.info(
          "\t" + languageName + ": " + (compilerVersion == null ? "Not Found" : compilerVersion));
      if (compilerVersion != null) {
        supportedLanguageIds.add(language.getLanguageId());
      }
    }
    return supportedLanguageIds;
  }

  /**
   * Gets the compiler program from the compile command line: its first word (the whole command when
   * it has no arguments).
   *
   * @param compileCommand - the command line of the compile command
   * @return the compiler program, or {@code null} if the command is empty
   */
  static String getCompileProgram(String compileCommand) {
    if (compileCommand == null || compileCommand.isBlank()) {
      return null;
    }
    String compileProgram = compileCommand.trim().split("\\s+")[0];

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
   * @return the compiler's version information, or null when the compiler cannot be launched
   *     (i.e. this host does not support the language)
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
      return null;
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
