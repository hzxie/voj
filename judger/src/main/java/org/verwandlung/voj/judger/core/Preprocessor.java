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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.exception.CreateDirectoryException;
import org.verwandlung.voj.judger.model.Language;
import org.verwandlung.voj.judger.model.Submission;

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
    File workDirFile = new File(workDirectory);
    if (!workDirFile.exists() && !workDirFile.mkdirs()) {
      throw new CreateDirectoryException("Failed to create directory: " + workDirectory);
    }
    setWorkDirectoryPermission(workDirFile);

    Language language = submission.getLanguage();
    String code = replaceClassName(language, submission.getCode(), baseFileName);
    String codeFilePath =
        String.format(
            "%s/%s.%s", new Object[] {workDirectory, baseFileName, getCodeFileSuffix(language)});

    FileOutputStream outputStream = new FileOutputStream(new File(codeFilePath));
    IOUtils.write(code, outputStream);
    IOUtils.closeQuietly(outputStream);
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
   * @param language - the programming language object
   * @param code - the code in which to perform the replacement
   * @param newClassName - the new class name
   */
  private String replaceClassName(Language language, String code, String newClassName) {
    if (!language.getLanguageName().equalsIgnoreCase("Java")) {
      return code;
    }
    return code.replaceAll("class[ \n]+Main", "class " + newClassName);
  }

  /**
   * Restricts the work directory to the unprivileged sandbox user. The directory is made private
   * (owner only), so that other accounts on the host cannot read a submission's files. When the
   * judger runs as root the directory is then handed to the sandbox user (system.username), so that
   * the compiler and the submitted program, which run with dropped privileges, can still write
   * their output here.
   *
   * @param workDirectory the directory used for producing compilation output
   */
  private void setWorkDirectoryPermission(File workDirectory) throws IOException {
    if (System.getProperty("os.name").contains("Windows")) {
      return;
    }
    Set<PosixFilePermission> permissions = new HashSet<>();
    permissions.add(PosixFilePermission.OWNER_READ);
    permissions.add(PosixFilePermission.OWNER_WRITE);
    permissions.add(PosixFilePermission.OWNER_EXECUTE);
    Files.setPosixFilePermissions(workDirectory.toPath(), permissions);

    if (isRunningAsRoot() && sandboxUsername != null && !sandboxUsername.isEmpty()) {
      try {
        UserPrincipal owner =
            FileSystems.getDefault().getUserPrincipalLookupService().lookupPrincipalByName(
                sandboxUsername);
        Files.setOwner(workDirectory.toPath(), owner);
      } catch (IOException ex) {
        LOGGER.warn(
            "Failed to hand work directory to sandbox user '" + sandboxUsername + "'", ex);
      }
    }
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
