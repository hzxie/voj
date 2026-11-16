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
package org.verwandlung.voj.judger.application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.core.Preprocessor;
import org.verwandlung.voj.judger.mapper.LanguageMapper;
import org.verwandlung.voj.judger.model.Language;

/**
 * The languages this judger supports: those whose compiler it finds on its host (see {@link
 * #getVersionCommand}) and whose source file name is valid (see {@link
 * Preprocessor#isValidSourceFilename}). It probes them at startup (see {@link #detect}), and again
 * whenever an administrator changed the languages (see {@link #checkForChanges}), so that a
 * language added in the web application is judged without a restart. A compiler installed on the
 * host later is found at the next change of the languages, or the next start.
 *
 * @author Haozhe Xie
 */
@Component
public class SupportedLanguages {
  /**
   * Creates the supported languages of the judger, which probes the compilers again on a thread of
   * its own, never on the judging thread.
   *
   * @param languageMapper - the languages
   */
  @Autowired
  public SupportedLanguages(LanguageMapper languageMapper) {
    this(
        languageMapper,
        Executors.newSingleThreadExecutor(
            runnable -> {
              Thread thread = new Thread(runnable, "voj-language-probe");
              thread.setDaemon(true);
              return thread;
            }));
  }

  /**
   * Creates the supported languages.
   *
   * @param languageMapper - the languages
   * @param prober - runs a probe of the compilers again once the languages changed
   */
  SupportedLanguages(LanguageMapper languageMapper, Executor prober) {
    this.languageMapper = languageMapper;
    this.prober = prober;
  }

  /**
   * Gets the languages this judger supports now.
   *
   * @return the unique identifiers of the languages, empty until they were probed
   */
  public List<Integer> getLanguageIds() {
    return languageIds;
  }

  /**
   * Probes the compilers of the languages, at startup, on the calling thread.
   *
   * @throws org.springframework.dao.DataAccessException if the languages cannot be read
   */
  public void detect() {
    probe(languageMapper.getAllLanguages());
  }

  /**
   * Probes the compilers of the languages again, in the background, if the languages changed (one
   * was added or removed, or its commands or source file name changed) since they were last
   * probed. Cheap otherwise: one read of the languages, as the heartbeat does it. One probe runs at
   * a time; a change made while one runs is found at a later check.
   *
   * @throws org.springframework.dao.DataAccessException if the languages cannot be read
   */
  public void checkForChanges() {
    if (isProbing.get()) {
      return;
    }
    List<Language> languages = languageMapper.getAllLanguages();
    if (getFingerprint(languages).equals(probedFingerprint)
        || !isProbing.compareAndSet(false, true)) {
      return;
    }
    LOGGER.info("The languages changed: probing their compilers again.");
    try {
      prober.execute(
          () -> {
            try {
              probe(languages);
            } catch (RuntimeException ex) {
              LOGGER.error("Failed to probe the compilers of the languages.", ex);
            } finally {
              isProbing.set(false);
            }
          });
    } catch (RuntimeException ex) {
      isProbing.set(false);
      throw ex;
    }
  }

  /**
   * Probes the compiler of every language and takes those this host can compile, whose source file
   * name is valid, as the languages this judger supports. A language counts as supported when its
   * compiler prints its version and exits with 0 (see {@link #getVersionCommand}), which is logged
   * to make bug reproduction easier.
   *
   * @param languages - the languages
   */
  private void probe(List<Language> languages) {
    LOGGER.info("Compiler Information: ");
    List<Integer> supportedLanguageIds = new ArrayList<>();
    for (Language language : languages) {
      if (!Preprocessor.isValidSourceFilename(language.getSourceFilename())) {
        LOGGER.error(
            "\t{}: its source file name \"{}\" is not a plain file name, so it is not judged: set"
                + " one (e.g. Main.cpp) in the administration of the languages.",
            language.getLanguageName(),
            language.getSourceFilename());
        continue;
      }
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
    languageIds = List.copyOf(supportedLanguageIds);
    probedFingerprint = getFingerprint(languages);
    if (languageIds.isEmpty()) {
      LOGGER.error(
          "No language this judger can compile was found: it takes no submission until one is"
              + " (check the toolchain installation, then restart it, or change the languages).");
    } else {
      LOGGER.info("Judging the submissions in the languages " + languageIds);
    }
  }

  /**
   * Gets what a probe of some languages depends on: their identifiers, commands and source file
   * names, by identifier.
   *
   * @param languages - the languages
   * @return the fingerprint of the languages
   */
  static List<List<Object>> getFingerprint(List<Language> languages) {
    return languages.stream()
        .sorted(Comparator.comparingInt(Language::getLanguageId))
        .map(
            language ->
                Arrays.<Object>asList(
                    language.getLanguageId(),
                    language.getCompileCommand(),
                    language.getRunCommand(),
                    language.getSourceFilename()))
        .toList();
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

  /** The languages. */
  private final LanguageMapper languageMapper;

  /** Runs a probe of the compilers again once the languages changed. */
  private final Executor prober;

  /** Whether a probe of the compilers runs in the background. */
  private final AtomicBoolean isProbing = new AtomicBoolean();

  /** The unique identifiers of the languages this judger supports. */
  private volatile List<Integer> languageIds = List.of();

  /** The fingerprint of the languages as last probed (see {@link #getFingerprint}). */
  private volatile List<List<Object>> probedFingerprint;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(SupportedLanguages.class);
}
