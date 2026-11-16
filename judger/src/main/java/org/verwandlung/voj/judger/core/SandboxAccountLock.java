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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.util.ExclusiveFileLock;

/**
 * Makes sure no two judgers on a host run their submissions as the same sandbox account.
 *
 * <p>The native sandbox on Linux runs each submission as {@code system.username}. Runs of two
 * judgers under one account could signal each other's processes, share the account's {@code
 * RLIMIT_NPROC} (which caps the threads of a run, so a busy run of one judger leaves the other's
 * without any) and read each other's work directories. So, like the box id of isolate, the
 * account must be a judger's own: a judger locks a lock file named after the account's uid, in a
 * location every judger on the host sees ({@code /run}, else the temporary directory), and holds
 * the lock while it runs; a judger that finds the account taken does not start.
 *
 * <p>This applies only where the judger drops privileges to the account: with the native sandbox,
 * on Linux, as root. isolate gives each judger a box (and uid) of its own, and a judger that does
 * not run as root runs its submissions as itself. Judgers in separate containers do not see each
 * other's lock (each container has its own {@code /run} and {@code /tmp}); their PID and mount
 * namespaces keep their runs apart anyway, but not their {@code RLIMIT_NPROC} (see
 * docs/deployment.md).
 *
 * @author Haozhe Xie
 */
@Component
public class SandboxAccountLock {
  /**
   * Takes the sandbox account for this judger, if it drops privileges to it, for as long as the
   * judger runs.
   *
   * @throws IOException if the account does not exist or another judger on the host uses it
   */
  public synchronized void acquire() throws IOException {
    if (!dropsPrivileges(sandbox)) {
      return;
    }
    acquire(lockDirectory());
  }

  /**
   * Takes the sandbox account for this judger: locks the lock file named after its uid.
   *
   * @param lockDirectory - the directory of the lock file
   * @throws IOException if the account does not exist or another judger on the host uses it
   */
  synchronized void acquire(Path lockDirectory) throws IOException {
    if (lock != null) {
      return;
    }
    String uid = uidOf(sandboxUsername);
    Path lockFile = lockDirectory.resolve("voj-sandbox-" + uid + ".lock");
    lock = ExclusiveFileLock.tryAcquire(lockFile);
    if (lock == null) {
      throw new IOException(
          String.format(
              "Another judger on this host already runs its submissions as the sandbox account %s"
                  + " (uid %s, locked in %s). Give each judger on a host an account of its own"
                  + " (system.username), as each one has an isolate box of its own.",
              sandboxUsername, uid, lockFile));
    }
    LOGGER.info(
        "Running submissions as {} (uid {}), locked in {}", sandboxUsername, uid, lockFile);
  }

  /**
   * Checks whether the judger drops privileges to the sandbox account: the native sandbox does,
   * on Linux, when the judger runs as root.
   *
   * @param sandbox - the sandbox backend ({@code native} or {@code isolate})
   * @return whether the judger runs submissions as the sandbox account
   */
  static boolean dropsPrivileges(String sandbox) {
    return !"isolate".equalsIgnoreCase(sandbox)
        && !System.getProperty("os.name").contains("Windows")
        && "root".equals(System.getProperty("user.name"));
  }

  /**
   * Gets a directory for the lock file that every judger on the host sees and that no clean-up of
   * old files touches: {@code /run} (root's, which the judger is), else the temporary directory.
   *
   * @return the directory of the lock file
   */
  private static Path lockDirectory() {
    Path run = Paths.get("/run");
    if (Files.isDirectory(run) && Files.isWritable(run)) {
      return run;
    }
    return Paths.get(System.getProperty("java.io.tmpdir"));
  }

  /**
   * Gets the uid of an account, which identifies it (several names may share one uid).
   *
   * @param username - the name of the account
   * @return the uid
   * @throws IOException if the account does not exist
   */
  static String uidOf(String username) throws IOException {
    Process process =
        new ProcessBuilder("id", "-u", "--", username)
            .redirectErrorStream(true)
            .redirectInput(ProcessBuilder.Redirect.from(new File("/dev/null")))
            .start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    try {
      if (process.waitFor() != 0 || !output.trim().matches("\\d+")) {
        throw new IOException(
            String.format(
                "The sandbox account %s (system.username) does not exist: %s",
                username, output.trim()));
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while looking up the sandbox account", ex);
    }
    return output.trim();
  }

  /** The lock on the sandbox account, held while the judger runs, once taken. */
  private ExclusiveFileLock lock;

  /** The sandbox backend ({@code native} or {@code isolate}). */
  @Value("${judger.sandbox:native}")
  private String sandbox;

  /** The account the native sandbox runs submissions as. */
  @Value("${system.username}")
  private String sandboxUsername;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(SandboxAccountLock.class);
}
