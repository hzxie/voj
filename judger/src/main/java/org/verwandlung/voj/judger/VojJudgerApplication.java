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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
import org.verwandlung.voj.judger.application.JudgerAccount;
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
 * running, records its heartbeats in the database, and sends the progress of its judgings to the
 * web application via ActiveMQ.
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
   * information, starts the heartbeats in the registry of the judgers and the judging of the
   * pending submissions.
   *
   * @param languageMapper - the data access object for programming languages
   * @param judgerAccount - the account of the judger, checked first
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
      JudgerAccount judgerAccount,
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
                + " test data download, so the judgings of this judger fail and it is soon not"
                + " ready. Set it to the judger.apiToken of the web application.");
      }
      // Fail fast: verify the judger's credentials against the shared user table before taking any
      // submission. Otherwise a misconfigured judger would accept submissions and abort them
      // mid-judging (leaving them stuck) once the heartbeat later detected the bad credentials.
      if (!judgerAccount.authenticate()) {
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
      heartbeat.setLanguageIds(supportedLanguageIds);
      setupHeartBeat(heartbeat);
      if (supportedLanguageIds.isEmpty()) {
        // No usable toolchain on this host. Rather than taking submissions we cannot build (and
        // failing them with a bogus Compile Error), stay online for telemetry but never take one.
        // The submissions in a language no judger supports are judged a System Error by the web
        // application.
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

  /**
   * Records the heartbeats of the judger in the database from now on, every {@value
   * ApplicationHeartbeat#HEARTBEAT_INTERVAL_MS} ms; the web application treats a judger as stopped
   * after a few missed heartbeats.
   *
   * @param heartbeat - the heartbeat task
   */
  private void setupHeartBeat(ApplicationHeartbeat heartbeat) {
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleWithFixedDelay(
        heartbeat, 0, ApplicationHeartbeat.HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
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
   * Probes the compiler of every language and returns those this host can compile, once, at
   * startup: a compiler installed later is found at the next start. A language counts as supported
   * when its compiler prints its version and exits with 0 (see {@link #getVersionCommand}), which
   * is logged to make bug reproduction easier.
   *
   * @param languageMapper - the data access object for programming languages
   * @return the unique identifiers of the languages this judger supports
   */
  private List<Integer> detectSupportedLanguages(LanguageMapper languageMapper) {
    LOGGER.info("Compiler Information: ");
    List<Integer> supportedLanguageIds = new ArrayList<>();
    for (Language language : languageMapper.getAllLanguages()) {
      List<String> versionCommand = getVersionCommand(language.getCompileCommand());
      String version =
          versionCommand == null ? null : getCommandOutput(versionCommand, PROBE_TIMEOUT_MS);
      LOGGER.info(
          "\t{}: {}",
          language.getLanguageName(),
          version == null ? "Not Found" : version.lines().findFirst().orElse(""));
      if (version != null) {
        supportedLanguageIds.add(language.getLanguageId());
      }
    }
    return supportedLanguageIds;
  }

  /**
   * Gets the command that prints the version of the compiler that a compile command runs (its
   * first word): {@code -version} for javac and kotlinc, {@code version} for go, {@code -iV} for
   * Free Pascal, {@code --version} for any other.
   *
   * @param compileCommand - the command line of the compile command
   * @return the command, or {@code null} if the compile command is empty
   */
  static List<String> getVersionCommand(String compileCommand) {
    if (compileCommand == null || compileCommand.isBlank()) {
      return null;
    }
    String compiler = compileCommand.trim().split("\\s+")[0];
    String name =
        compiler
            .substring(Math.max(compiler.lastIndexOf('/'), compiler.lastIndexOf('\\')) + 1)
            .toLowerCase(Locale.ROOT)
            .replaceFirst("\\.exe$", "");
    switch (name) {
      case "javac":
      case "kotlinc":
        return List.of(compiler, "-version");
      case "go":
        return List.of(compiler, "version");
      case "fpc":
        return List.of(compiler, "-iV");
      default:
        return List.of(compiler, "--version");
    }
  }

  /**
   * Runs a command and gets what it prints (its standard output and error, read as it runs, so it
   * never blocks on a full pipe). A command that does not exit in time is killed.
   *
   * @param command - the command
   * @param timeoutMs - how long (ms) it may run
   * @return what it printed, or {@code null} if it cannot be started, does not exit with 0, or
   *     does not exit in time
   */
  static String getCommandOutput(List<String> command, long timeoutMs) {
    Process process;
    try {
      process = new ProcessBuilder(command).redirectErrorStream(true).start();
    } catch (IOException ex) {
      return null;
    }
    try {
      process.getOutputStream().close();
      FutureTask<byte[]> output = new FutureTask<>(process.getInputStream()::readAllBytes);
      Thread reader = new Thread(output, "voj-compiler-probe");
      reader.setDaemon(true);
      reader.start();
      if (!process.waitFor(timeoutMs, TimeUnit.MILLISECONDS) || process.exitValue() != 0) {
        return null;
      }
      return new String(output.get(timeoutMs, TimeUnit.MILLISECONDS), StandardCharsets.UTF_8)
          .trim();
    } catch (IOException | ExecutionException | TimeoutException ex) {
      return null;
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return null;
    } finally {
      process.destroyForcibly();
    }
  }

  /** How long (ms) the compiler of a language may take to print its version. */
  static final long PROBE_TIMEOUT_MS = 30000;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(VojJudgerApplication.class);
}
