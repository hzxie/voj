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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.exception.CreateDirectoryException;
import org.verwandlung.voj.judger.model.Language;
import org.verwandlung.voj.judger.model.Submission;
import org.verwandlung.voj.judger.util.FileSystemUtils;

/**
 * The preprocessor, used to perform preparation work before judging.
 *
 * <p>This now covers only the submission's source code; a problem's checkpoints (test data) are
 * managed by {@link CheckpointStore}, which caches them locally and downloads them on demand.
 *
 * @author Haozhe Xie
 */
@Component
public class Preprocessor {
  /**
   * Writes the code to be tested to the local disk.
   *
   * @param submission - the submission object
   * @param workDirectory - the directory used for producing compilation output
   * @param baseFileName - the random file name (without suffix)
   * @throws Exception
   */
  public void createTestCode(Submission submission, String workDirectory, String baseFileName)
      throws Exception {
    Path workDirPath = Paths.get(workDirectory);
    createFreshWorkDirectory(workDirPath);

    Language language = submission.getLanguage();
    String code = replaceClassName(language, submission.getCode(), baseFileName);
    Path codeFilePath = workDirPath.resolve(baseFileName + "." + getCodeFileSuffix(language));

    // CREATE_NEW + NOFOLLOW_LINKS: the directory was just created empty, so anything already at
    // this path (e.g. a planted symbolic link) is an attack and must not be written through.
    try (OutputStream outputStream =
        Files.newOutputStream(
            codeFilePath,
            StandardOpenOption.CREATE_NEW,
            StandardOpenOption.WRITE,
            LinkOption.NOFOLLOW_LINKS)) {
      outputStream.write(code.getBytes(StandardCharsets.UTF_8));
    }
    // Only hand the directory to the sandbox user once the judger has finished writing into it.
    setWorkDirectoryOwner(workDirPath);
  }

  /**
   * Creates an empty, private work directory for a submission. Any existing directory at that path
   * (left over from a crashed run, or pre-created by an earlier submission running as the same
   * sandbox user to plant files or symbolic links such as {@code output#1.txt -> /etc/...}) is
   * removed first, without following symbolic links. The directory is then created atomically with
   * owner-only permissions; if something re-appears at the path in between, judging is aborted
   * rather than reusing a directory the judger did not create.
   *
   * @param workDirectory - the directory used for producing compilation output
   * @throws CreateDirectoryException if a fresh directory cannot be created
   */
  private void createFreshWorkDirectory(Path workDirectory) throws CreateDirectoryException {
    try {
      FileSystemUtils.deleteRecursively(workDirectory);
      Path parent = workDirectory.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      if (isPosix()) {
        Files.createDirectory(
            workDirectory,
            PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
      } else {
        Files.createDirectory(workDirectory);
      }
    } catch (FileAlreadyExistsException ex) {
      throw new CreateDirectoryException(
          "Work directory re-appeared while being recreated: " + workDirectory);
    } catch (IOException ex) {
      throw new CreateDirectoryException(
          "Failed to create directory: " + workDirectory + " (" + ex + ")");
    }
  }

  /**
   * Gets the suffix of the code file.
   *
   * @param language - the programming language object
   * @return the suffix of the code file
   */
  private String getCodeFileSuffix(Language language) {
    String compileCommand = language.getCompileCommand();

    Pattern pattern = Pattern.compile("\\{filename\\}\\.((?!exe| ).)+");
    Matcher matcher = pattern.matcher(compileCommand);

    if (matcher.find()) {
      String sourceFileName = matcher.group();
      return sourceFileName.replaceAll("\\{filename\\}\\.", "");
    }
    return "";
  }

  /**
   * Replaces the class name in certain languages (such as Java) to ensure compilation succeeds.
   *
   * <p>Only the declaration {@code class Main} is renamed: the word boundaries keep e.g. {@code
   * class MainHelper} or {@code subclass Main} intact, and {@code \s+} accepts any whitespace
   * (tabs, CRLF line breaks) between the keyword and the name. This is a plain text substitution,
   * so a {@code "class Main"} inside a string literal or comment is rewritten as well.
   *
   * @param language - the programming language object
   * @param code - the code in which to perform the replacement
   * @param newClassName - the new class name
   */
  private String replaceClassName(Language language, String code, String newClassName) {
    if (!language.getLanguageName().equalsIgnoreCase("Java")) {
      return code;
    }
    return JAVA_MAIN_CLASS_DECLARATION
        .matcher(code)
        .replaceAll(Matcher.quoteReplacement("class " + newClassName));
  }

  /** Matches the declaration of the Java class {@code Main}, which is renamed before compiling. */
  private static final Pattern JAVA_MAIN_CLASS_DECLARATION = Pattern.compile("\\bclass\\s+Main\\b");

  /**
   * Hands the work directory to the unprivileged sandbox user. The directory is private (owner
   * only, see {@link #createFreshWorkDirectory}), so that other accounts on the host cannot read a
   * submission's files. When the judger runs as root the directory is then handed to the sandbox
   * user (system.username), so that the compiler and the submitted program, which run with dropped
   * privileges, can still write their output here.
   *
   * @param workDirectory the directory used for producing compilation output
   */
  private void setWorkDirectoryOwner(Path workDirectory) {
    if (!isPosix()) {
      return;
    }
    if (isRunningAsRoot() && sandboxUsername != null && !sandboxUsername.isEmpty()) {
      try {
        UserPrincipal owner =
            FileSystems.getDefault().getUserPrincipalLookupService().lookupPrincipalByName(
                sandboxUsername);
        Files.setOwner(workDirectory, owner);
      } catch (IOException ex) {
        LOGGER.warn(
            "Failed to hand work directory to sandbox user '" + sandboxUsername + "'", ex);
      }
    }
  }

  /**
   * Checks whether the host file system supports POSIX permissions (i.e. is not Windows).
   *
   * @return whether POSIX permissions are available
   */
  private boolean isPosix() {
    return !System.getProperty("os.name").contains("Windows");
  }

  /**
   * Checks whether the judger is running as the root user.
   *
   * @return whether the current process is running as root
   */
  private boolean isRunningAsRoot() {
    return "root".equals(System.getProperty("user.name"));
  }

  /** The unprivileged user the native sandbox drops to; the work directory is handed to it. */
  @Value("${system.username}")
  private String sandboxUsername;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(Preprocessor.class);
}
