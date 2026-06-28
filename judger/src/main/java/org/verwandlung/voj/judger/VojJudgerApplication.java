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
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jms.listener.SimpleMessageListenerContainer;

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
   * @param submissionTaskListenerContainer - the listener for the judging-task queue, started here
   *     only after the identity check passes
   * @return an ApplicationRunner instance
   */
  @Bean
  public ApplicationRunner startupRunner(
      LanguageMapper languageMapper,
      ApplicationHeartbeat heartbeat,
      SimpleMessageListenerContainer submissionTaskListenerContainer) {
    return args -> {
      // Fail fast: verify the judger's credentials against the shared user table before consuming
      // any task. Otherwise a misconfigured judger would accept submissions and abort them
      // mid-judging (leaving them stuck) once the heartbeat later detected the bad credentials.
      if (!heartbeat.isIdentityValid()) {
        LOGGER.error("Unauthorized: please check judger.username / judger.password. Shutting down.");
        System.exit(-1);
      }
      logSystemEnvironment();

      List<String> supportedLanguageSlugs = detectSupportedLanguages(languageMapper);
      if (supportedLanguageSlugs.isEmpty()) {
        // No usable toolchain on this host. Rather than accepting tasks we cannot build (and then
        // either failing them with a bogus Compile Error or bouncing them around the queue), stay
        // online for telemetry but never consume a submission. The web side's task TTL eventually
        // fails any submission that no judger can handle.
        LOGGER.error(
            "No supported language compilers were detected; this judger will not consume any "
                + "submission. Check the toolchain installation.");
        setupHeartBeat(heartbeat);
        return;
      }
      // Only claim tasks this judger can actually build. The broker evaluates this selector, so
      // unsupported tasks are never delivered here (no take-and-requeue loop, hence no busy loop
      // when nobody supports a language); they simply wait in the queue for a capable judger.
      String messageSelector = buildLanguageSelector(supportedLanguageSlugs);
      LOGGER.info("Listening for submission tasks with selector: " + messageSelector);
      submissionTaskListenerContainer.setMessageSelector(messageSelector);
      // The listener is configured with autoStartup=false; only now, with a valid identity and a
      // known set of supported languages, do we begin receiving submissions.
      submissionTaskListenerContainer.start();
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
  private void logSystemEnvironment() {
    LOGGER.info("System Information: ");
    LOGGER.info("\tOperating System Name: " + System.getProperty("os.name"));
    LOGGER.info("\tOperating System Version: " + System.getProperty("os.version"));
    LOGGER.info("\tJava VM Name: " + System.getProperty("java.vm.name"));
    LOGGER.info("\tJava Runtime Version: " + System.getProperty("java.runtime.version"));
  }

  /**
   * Probes the toolchain for every language known to the system and returns the slugs of those this
   * host can actually compile. A language counts as supported when its compiler can be launched; the
   * compiler version is logged along the way to make bug reproduction easier.
   *
   * @param languageMapper - the data access object for programming languages
   * @return the slugs of the languages this judger supports
   */
  private List<String> detectSupportedLanguages(LanguageMapper languageMapper) {
    LOGGER.info("Compiler Information: ");
    List<String> supportedLanguageSlugs = new ArrayList<>();
    List<Language> languages = languageMapper.getAllLanguages();
    for (Language language : languages) {
      String languageName = language.getLanguageName();
      String compileProgram = getCompileProgram(language.getCompileCommand());
      String compilerVersion = getCompilerVersion(languageName, compileProgram);
      LOGGER.info(
          "\t" + languageName + ": " + (compilerVersion == null ? "Not Found" : compilerVersion));
      if (compilerVersion != null) {
        supportedLanguageSlugs.add(language.getLanguageSlug());
      }
    }
    return supportedLanguageSlugs;
  }

  /**
   * Builds the JMS message selector that limits this judger to submission tasks it can build. Tasks
   * that carry no {@code languageSlug} property (e.g. produced by an older web build) are also
   * accepted so they are never stranded in the queue.
   *
   * @param supportedLanguageSlugs - the slugs of the languages this judger supports
   * @return the JMS message selector expression
   */
  private String buildLanguageSelector(List<String> supportedLanguageSlugs) {
    StringBuilder quotedSlugs = new StringBuilder();
    for (int i = 0; i < supportedLanguageSlugs.size(); ++i) {
      if (i > 0) {
        quotedSlugs.append(", ");
      }
      // Language slugs are CodeMirror MIME types (e.g. text/x-csrc); none contains a single quote,
      // so they are safe to embed directly in the selector literal.
      quotedSlugs.append('\'').append(supportedLanguageSlugs.get(i)).append('\'');
    }
    return String.format("languageSlug IS NULL OR languageSlug IN (%s)", quotedSlugs);
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
