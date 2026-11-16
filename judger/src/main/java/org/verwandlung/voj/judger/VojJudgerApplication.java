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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
import org.verwandlung.voj.judger.application.SupportedLanguages;
import org.verwandlung.voj.judger.core.CheckpointStore;
import org.verwandlung.voj.judger.core.Dispatcher;
import org.verwandlung.voj.judger.core.SandboxAccountLock;

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
   * information, finds the languages this judger supports, and starts the heartbeats in the
   * registry of the judgers and the judging of the pending submissions.
   *
   * @param judgerAccount - the account of the judger, checked first
   * @param supportedLanguages - the languages this judger supports, probed here
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
      JudgerAccount judgerAccount,
      SupportedLanguages supportedLanguages,
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
      // The claims and the heartbeats of the judger are recorded by its account, so a judger whose
      // credentials are wrong does not start.
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

      // Only the submissions this judger can build are taken: the others wait in the database for
      // a capable judger. The heartbeats probe the languages again once they changed.
      supportedLanguages.detect();
      setupHeartBeat(heartbeat);
      new Thread(
              () -> dispatcher.judgeSubmissions(supportedLanguages::getLanguageIds), "voj-judging")
          .start();
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

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(VojJudgerApplication.class);
}
