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
package org.verwandlung.voj.judger.core;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.util.NativeLibraryLoader;

/**
 * The native {@link SandboxRunner} (JNI): on Linux, {@code fork}/{@code exec} under rlimits,
 * Landlock and seccomp filters, as {@code system.username} when the judger is root; on Windows, a
 * Job Object, a restricted Low-integrity token of {@code system.username}, a private desktop and a
 * network filter. The backend on Windows, and on Linux for a judger that is not root (development)
 * or where isolate is not available. The class name is part of the JNI symbols.
 *
 * @author Haozhe Xie
 */
@Component
public class Runner implements SandboxRunner {
  /**
   * Runs a compilation or a run of a submission in the native sandbox.
   *
   * @param command - what to run, and how
   * @return what the sandbox measured
   * @throws IOException if the sandbox could not be set up or failed
   */
  @Override
  public SandboxResult run(SandboxCommand command) throws IOException {
    if (LIBRARY_LOAD_FAILURE != null) {
      throw new IOException("The native sandbox library is not loaded", LIBRARY_LOAD_FAILURE);
    }
    Limits limits = command.limits();
    try {
      openSandboxAccount();
      int[] result =
          execute(
              command.arguments().toArray(new String[0]),
              command.environment().toArray(new String[0]),
              command.workDirectory(),
              command.inputFile(),
              command.outputFile(),
              command.compilation(),
              limits.timeLimitMs(),
              limits.wallTimeLimitMs(),
              limits.memoryLimitKb(),
              limits.memoryBackstopKb(),
              limits.outputLimitKb(),
              limits.processLimit(),
              limits.fileLimit(),
              limits.signalQueueLimit());
      return new SandboxResult(
          result[0], result[1], result[2], SandboxResult.Stop.values()[result[3]]);
    } catch (RuntimeException ex) {
      // The native sandbox throws when it cannot be set up (no sandbox account, a failed privilege
      // drop or filter, a program that cannot be started, ...).
      throw new IOException("The native sandbox failed to run " + command.arguments(), ex);
    }
  }

  /**
   * Opens the sandbox account once, before the first program runs as it: on Linux, the account a
   * judger running as root drops to; on Windows, its logon session, which every program runs in.
   */
  private synchronized void openSandboxAccount() {
    if (sandboxAccountOpen) {
      return;
    }
    if (IS_WINDOWS) {
      logOnSandboxAccount(sandboxUsername, sandboxPassword);
    } else {
      selectSandboxAccount(sandboxUsername);
    }
    sandboxAccountOpen = true;
  }

  /**
   * Runs a program in the sandbox and measures it.
   *
   * @param arguments - the program and its arguments
   * @param environment - the environment of the program ({@code NAME=value})
   * @param workDirectory - the directory the program runs in and the only one it may write to
   * @param inputFile - the file standard input is read from ({@code null}: none)
   * @param outputFile - the file standard output goes to, created by the sandbox; standard error
   *     goes there too for a compilation
   * @param compilation - whether a trusted compiler runs, rather than the submission
   * @param timeLimit - the CPU time limit (ms)
   * @param wallTimeLimit - the wall-clock limit (ms)
   * @param memoryLimit - the limit of the peak resident memory (KB)
   * @param memoryBackstop - the limit of the committed memory (KB)
   * @param outputLimit - the largest file the program may write (KB)
   * @param processLimit - the processes (on Linux, and threads) the program may have at once
   * @param fileLimit - the descriptors each process may have open at once (Linux)
   * @param signalQueueLimit - the signals and POSIX timers that may be queued at once (Linux)
   * @return the CPU time (ms), the peak memory (KB), the exit code and the limit the program was
   *     stopped for (see {@link SandboxResult.Stop})
   */
  private native int[] execute(
      String[] arguments,
      String[] environment,
      String workDirectory,
      String inputFile,
      String outputFile,
      boolean compilation,
      int timeLimit,
      int wallTimeLimit,
      int memoryLimit,
      int memoryBackstop,
      int outputLimit,
      int processLimit,
      int fileLimit,
      int signalQueueLimit);

  /**
   * Selects the account programs run as when the judger runs as root (Linux), which must exist
   * and not be root; a judger that is not root runs them as itself.
   *
   * @param username - the name of the account
   */
  private static native void selectSandboxAccount(String username);

  /**
   * Logs the sandbox account on (Windows): the logon session every program runs in, with the
   * account's profile, a private desktop and a network filter, kept until the judger exits. No
   * other judger may use the account meanwhile.
   *
   * @param username - the name of the account
   * @param password - its password
   */
  private static native void logOnSandboxAccount(String username, String password);

  /** Logs the sandbox account off (Windows), as the judger exits. */
  private static native void logOffSandboxAccount();

  /** The account programs run as (see {@code system.username}). */
  @Value("${system.username}")
  private String sandboxUsername;

  /** The password of the account, which Windows logs it on with. */
  @Value("${system.password}")
  private String sandboxPassword;

  /** Whether the sandbox account is open. */
  private boolean sandboxAccountOpen;

  /** Whether the judger runs on Windows. */
  private static final boolean IS_WINDOWS = System.getProperty("os.name").contains("Windows");

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(Runner.class);

  /**
   * Why the native library could not be loaded, or {@code null} once it is. The judger still
   * starts without it (e.g. with {@code judger.sandbox=isolate}).
   */
  private static final Throwable LIBRARY_LOAD_FAILURE = loadNativeLibrary();

  /**
   * Loads the native library.
   *
   * @return why it could not be loaded, or {@code null} if it was
   */
  private static Throwable loadNativeLibrary() {
    try {
      NativeLibraryLoader.loadLibrary("JudgerCore");
      if (IS_WINDOWS) {
        Runtime.getRuntime()
            .addShutdownHook(new Thread(Runner::logOffSandboxAccount, "voj-sandbox-logoff"));
      }
      return null;
    } catch (Throwable ex) {
      LOGGER.error("Failed to load the native sandbox library JudgerCore", ex);
      return ex;
    }
  }
}
