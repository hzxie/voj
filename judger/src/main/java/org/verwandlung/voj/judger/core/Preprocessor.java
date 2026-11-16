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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
 * <p>This now covers only the submission's source code and the directories it is judged in; a
 * problem's checkpoints (test data) are managed by {@link CheckpointStore}, which caches them
 * locally and downloads them on demand.
 *
 * <p>A judging has a directory of its own (the work directory the dispatcher gives it), which
 * belongs to the judger and which no compilation or run may write to: the compile log and the
 * output of every run are created there by the judger. In it:
 *
 * <ul>
 *   <li>{@value #PROGRAM_DIRECTORY}: the program directory, which holds the source and, once
 *       compiled, the program. The compilation runs there and may write there (and nowhere else);
 *       afterwards the judger takes the directory and what is in it, which the runs may then only
 *       read (see {@link #sealProgramDirectory}), so that no run can change, remove or add to what
 *       a later run of the program uses.
 *   <li>{@value #RUN_DIRECTORY}: the work directory of a run, created empty for each run (see
 *       {@link #createRunDirectory}) and removed after it: the only directory a run may write to,
 *       its current, home and temporary directory.
 * </ul>
 *
 * @author Haozhe Xie
 */
@Component
public class Preprocessor {
  /**
   * Creates the directory of a judging and its program directory, and writes the code to be tested
   * into the program directory.
   *
   * @param submission - the submission object
   * @param workDirectory - the directory of the judging
   * @param baseFileName - the random file name (without suffix)
   * @throws Exception
   */
  public void createTestCode(Submission submission, String workDirectory, String baseFileName)
      throws Exception {
    createJudgingDirectory(Paths.get(workDirectory));
    Path workDirPath = Paths.get(getProgramDirectory(workDirectory));
    try {
      if (isPosix()) {
        CheckpointStore.createDirectory(workDirPath, CheckpointStore.PRIVATE);
      } else {
        // With the access to it the judging directory passes on to the sandbox account.
        Files.createDirectory(workDirPath);
      }
    } catch (IOException ex) {
      throw new CreateDirectoryException(
          "Failed to create directory: " + workDirPath + " (" + ex + ")");
    }

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
    List<Path> sandboxWritablePaths = new ArrayList<>();
    sandboxWritablePaths.add(workDirPath);
    if (usesGoToolchain(language)) {
      sandboxWritablePaths.addAll(seedGoBuildCache(workDirPath));
    }
    // Only hand the directories to the sandbox user once the judger has finished writing into them.
    for (Path path : sandboxWritablePaths) {
      setWorkDirectoryOwner(path);
    }
  }

  /**
   * Checks whether a language is compiled by the Go toolchain ({@code go build ...}).
   *
   * @param language - the programming language object
   * @return whether the compile command runs {@code go}
   */
  private boolean usesGoToolchain(Language language) {
    String compileCommand = language.getCompileCommand();
    if (compileCommand == null || compileCommand.isBlank()) {
      return false;
    }
    String program = compileCommand.trim().split("\\s+")[0];
    return program.equals("go") || program.endsWith("/go");
  }

  /**
   * Seeds the submission's own Go build cache ({@value #GO_BUILD_CACHE_DIRECTORY} in the work
   * directory, where both the native sandbox and IsolateRunner point GOCACHE) from the read-only
   * cache at {@code judger.goBuildCacheSeed}, so that the standard library does not have to be
   * rebuilt for every submission. A cache shared between submissions
   * would let one poison the Go builds of all later ones, so every submission gets its own: the
   * seed's files are hard-linked (or, across file systems, copied) into it, and stay owned by the
   * judger and read-only, while the cache's directories are handed to the sandbox user so the build
   * can add its own entries. Linking a file the sandbox user cannot write gives it no way to modify
   * the seed: it can only remove its own link. Only files that are owned by the judger and not
   * writable by anybody else are taken from the seed. A judger that does not run as root may not
   * link files it does not own (protected_hardlinks), so it copies them.
   *
   * @param workDirectory - the submission's work directory
   * @return the directories (and the trim file) created, which the sandbox user must be able to
   *     write
   */
  private List<Path> seedGoBuildCache(Path workDirectory) {
    List<Path> directories = new ArrayList<>();
    if (goBuildCacheSeed == null || goBuildCacheSeed.isBlank() || !isPosix()) {
      return directories;
    }
    Path seed = Paths.get(goBuildCacheSeed);
    Path cache = workDirectory.resolve(GO_BUILD_CACHE_DIRECTORY);
    try {
      PosixFileAttributes seedAttributes =
          Files.readAttributes(seed, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      if (!seedAttributes.isDirectory() || !isTrusted(seedAttributes)) {
        LOGGER.warn("Ignoring the Go build cache seed " + seed + ": not a root-owned directory.");
        return directories;
      }
      directories.add(Files.createDirectory(cache.getParent()));
      Files.walkFileTree(
          seed,
          new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                throws IOException {
              directories.add(Files.createDirectory(cache.resolve(seed.relativize(dir))));
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
              PosixFileAttributes fileAttributes =
                  Files.readAttributes(file, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
              if (fileAttributes.isRegularFile() && isTrusted(fileAttributes)) {
                Path target = cache.resolve(seed.relativize(file));
                try {
                  Files.createLink(target, file);
                } catch (IOException | UnsupportedOperationException ex) {
                  Files.copy(file, target, StandardCopyOption.COPY_ATTRIBUTES);
                }
              }
              return FileVisitResult.CONTINUE;
            }
          });
      // Go trims entries unused for five days whenever the last trim (trim.txt) is a day old. The
      // seed is as old as the image, so mark this cache as just trimmed.
      Path trimFile = cache.resolve("trim.txt");
      Files.deleteIfExists(trimFile);
      Files.writeString(
          trimFile,
          String.valueOf(System.currentTimeMillis() / 1000),
          StandardOpenOption.CREATE_NEW,
          StandardOpenOption.WRITE);
      directories.add(trimFile);
    } catch (IOException ex) {
      LOGGER.warn("Failed to seed the Go build cache from " + seed, ex);
    }
    return directories;
  }

  /**
   * Checks that a file of the Go build cache seed is owned by the judger (root) and cannot be
   * written by anybody else, so that linking it into a submission's cache cannot expose it.
   *
   * @param attributes - the file's attributes
   * @return whether the file can be trusted
   */
  private boolean isTrusted(PosixFileAttributes attributes) {
    Set<PosixFilePermission> permissions = attributes.permissions();
    return "root".equals(attributes.owner().getName())
        && !permissions.contains(PosixFilePermission.GROUP_WRITE)
        && !permissions.contains(PosixFilePermission.OTHERS_WRITE);
  }

  /**
   * Gets the program directory of a judging (see {@link #PROGRAM_DIRECTORY}).
   *
   * @param workDirectory - the directory of the judging
   * @return the program directory
   */
  public static String getProgramDirectory(String workDirectory) {
    return workDirectory + "/" + PROGRAM_DIRECTORY;
  }

  /**
   * Hands the program directory of a judging, once compiled, to the judger: from now on, the
   * sandbox may only read it and what is in it (see {@link FileSystemUtils#makeTreeReadOnly}), so
   * that a run of the program can neither change nor remove the program (or anything else a later
   * run reads, such as the class files, the jar or the script), nor create anything next to it.
   *
   * @param workDirectory - the directory of the judging
   * @throws IOException if the directory cannot be made read-only
   */
  public void sealProgramDirectory(String workDirectory) throws IOException {
    FileSystemUtils.makeTreeReadOnly(
        Paths.get(getProgramDirectory(workDirectory)), isPosix() ? null : getSandboxAccount());
  }

  /**
   * Creates the work directory of a run (see {@link #RUN_DIRECTORY}), empty: whatever an earlier
   * run left at its path is removed first. The run may write there, and nowhere else; when the
   * judger runs as root, the directory is handed to the sandbox user.
   *
   * @param workDirectory - the directory of the judging
   * @return the work directory of the run
   * @throws IOException if the directory cannot be created
   */
  public String createRunDirectory(String workDirectory) throws IOException {
    Path runDirectory = Paths.get(workDirectory, RUN_DIRECTORY);
    FileSystemUtils.deleteRecursively(runDirectory);
    if (isPosix()) {
      CheckpointStore.createDirectory(runDirectory, CheckpointStore.PRIVATE);
      setWorkDirectoryOwner(runDirectory);
    } else {
      // With the access to it the judging directory passes on to the sandbox account; the sandbox
      // labels it so that a run (at Low integrity) may write to it.
      Files.createDirectory(runDirectory);
    }
    return runDirectory.toString();
  }

  /**
   * Creates the directory of a judging, empty, for the judger alone: no compilation or run may
   * write to it. Any existing directory at that path (left over from a crashed run, or pre-created
   * to plant files or symbolic links such as {@code output#1.txt -> /etc/...}) is removed first,
   * without following symbolic links. The directory is then created atomically with its
   * permissions; if something re-appears at the path in between, judging is aborted rather than
   * reusing a directory the judger did not create. Others may pass through it (on Linux, {@code
   * rwx--x--x}; on Windows, the sandbox account may read its attributes, as Node.js needs to),
   * to the program and run directories in it, which are the sandbox account's (on Windows, by the
   * access it passes on to what is created in it).
   *
   * @param workDirectory - the directory of the judging
   * @throws CreateDirectoryException if a fresh directory cannot be created
   */
  private void createJudgingDirectory(Path workDirectory) throws CreateDirectoryException {
    try {
      FileSystemUtils.deleteRecursively(workDirectory);
      Path parent = workDirectory.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      if (isPosix()) {
        CheckpointStore.createDirectory(workDirectory, CheckpointStore.PASSABLE);
      } else if (sandboxUsername == null || sandboxUsername.isEmpty()) {
        CheckpointStore.createWindowsDirectory(workDirectory, List.of());
      } else {
        UserPrincipal sandboxAccount = getSandboxAccount();
        CheckpointStore.createWindowsDirectory(
            workDirectory,
            List.of(
                CheckpointStore.passable(sandboxAccount),
                CheckpointStore.inheritedOnly(sandboxAccount, CheckpointStore.SANDBOX_ACCESS)));
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
   * Gets the sandbox account (Windows).
   *
   * @return the account system.username names
   * @throws IOException if there is no such account
   */
  private UserPrincipal getSandboxAccount() throws IOException {
    return FileSystems.getDefault()
        .getUserPrincipalLookupService()
        .lookupPrincipalByName(sandboxUsername);
  }

  /**
   * Hands a program or run directory to the unprivileged sandbox user. The directory is private
   * (owner only), so that other accounts on the host cannot read a submission's files. When the
   * judger runs as root the directory is then handed to the sandbox user, so that the compiler and
   * the submitted program, which run with dropped privileges, can write there: the native sandbox
   * runs them as system.username, isolate as the box's own user, whose UID is isolate's first_uid
   * plus the box id (see {@link #isolateUid()}).
   *
   * @param workDirectory the directory the sandbox writes in
   */
  private void setWorkDirectoryOwner(Path workDirectory) {
    if (!isPosix()) {
      return;
    }
    if (isRunningAsRoot() && "isolate".equalsIgnoreCase(sandbox)) {
      try {
        Files.setAttribute(workDirectory, "unix:uid", isolateUid(), LinkOption.NOFOLLOW_LINKS);
      } catch (IOException ex) {
        LOGGER.warn("Failed to hand work directory to isolate's box user " + isolateUid(), ex);
      }
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
   * Gets the UID isolate runs the sandboxed program as: its first_uid (judger.isolate.firstUid,
   * which must match isolate's configuration) plus the box id.
   *
   * @return the UID of the isolate box's user
   */
  private int isolateUid() {
    return isolateFirstUid + isolateBoxId;
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

  /** The sandbox backend ({@code native} or {@code isolate}), see SandboxConfiguration. */
  @Value("${judger.sandbox:native}")
  private String sandbox;

  /** isolate's first_uid: the box with id n runs as UID first_uid + n. */
  @Value("${judger.isolate.firstUid:60000}")
  private int isolateFirstUid;

  /** The isolate box id used for judging. */
  @Value("${judger.isolate.boxId:0}")
  private int isolateBoxId;

  /**
   * A read-only Go build cache (owned by root, e.g. built into the Docker image) that each Go
   * submission's own build cache is seeded from; empty to start every Go build from an empty cache.
   */
  @Value("${judger.goBuildCacheSeed:}")
  private String goBuildCacheSeed;

  /**
   * The submission's Go build cache, relative to its work directory. Must match GOCACHE as set by
   * the native sandbox (GO_BUILD_CACHE_DIRECTORY in Judger.Core.Runner.cpp) and by IsolateRunner.
   */
  static final String GO_BUILD_CACHE_DIRECTORY = ".cache/go-build";

  /** The program directory of a judging, in its directory (see the class comment). */
  static final String PROGRAM_DIRECTORY = "program";

  /** The work directory of a run, in the directory of its judging (see the class comment). */
  static final String RUN_DIRECTORY = "run";

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(Preprocessor.class);
}
