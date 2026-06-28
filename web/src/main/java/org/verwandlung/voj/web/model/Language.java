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
package org.verwandlung.voj.web.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The model of a programming language. Maps to the voj_languages table in the database.
 *
 * @author Haozhe Xie
 */
public class Language implements Serializable {
  /** Default constructor of the programming language. */
  public Language() {}

  /**
   * Constructor of the programming language.
   *
   * @param languageSlug - the English abbreviation of the programming language
   * @param languageName - the name of the programming language
   * @param compileCommand - the compile command of the programming language
   * @param runCommand - the command used to run programs of the programming language
   */
  public Language(
      String languageSlug, String languageName, String compileCommand, String runCommand) {
    this.languageSlug = languageSlug;
    this.languageName = languageName;
    this.compileCommand = compileCommand;
    this.runCommand = runCommand;
  }

  /**
   * Constructor of the programming language.
   *
   * @param languageId - the unique identifier of the programming language
   * @param languageSlug - the English abbreviation of the programming language
   * @param languageName - the name of the programming language
   * @param compileCommand - the compile command of the programming language
   * @param runCommand - the command used to run programs of the programming language
   */
  public Language(
      int languageId,
      String languageSlug,
      String languageName,
      String compileCommand,
      String runCommand) {
    this(languageSlug, languageName, compileCommand, runCommand);
    this.languageId = languageId;
  }

  /**
   * Gets the unique identifier of the programming language.
   *
   * @return the unique identifier of the programming language
   */
  public int getLanguageId() {
    return languageId;
  }

  /**
   * Sets the unique identifier of the programming language.
   *
   * @param languageId - the unique identifier of the programming language
   */
  public void setLanguageId(int languageId) {
    this.languageId = languageId;
  }

  /**
   * Gets the English abbreviation of the programming language.
   *
   * @return the English abbreviation of the programming language
   */
  public String getLanguageSlug() {
    return languageSlug;
  }

  /**
   * Sets the English abbreviation of the programming language.
   *
   * @param languageSlug - the English abbreviation of the programming language
   */
  public void setLanguageSlug(String languageSlug) {
    this.languageSlug = languageSlug;
  }

  /**
   * Gets the name of the programming language.
   *
   * @return the name of the programming language
   */
  public String getLanguageName() {
    return languageName;
  }

  /**
   * Sets the name of the programming language.
   *
   * @param languageName - the name of the programming language
   */
  public void setLanguageName(String languageName) {
    this.languageName = languageName;
  }

  /**
   * Gets the compile command of the programming language.
   *
   * @return the compile command of the programming language
   */
  public String getCompileCommand() {
    return compileCommand;
  }

  /**
   * Sets the compile command of the programming language.
   *
   * @param compileCommand - the compile command of the programming language
   */
  public void setCompileCommand(String compileCommand) {
    this.compileCommand = compileCommand;
  }

  /**
   * Gets the run command of the programming language.
   *
   * @return the run command of the programming language
   */
  public String getRunCommand() {
    return runCommand;
  }

  /**
   * Sets the run command of the programming language.
   *
   * @param runCommand - the run command of the programming language
   */
  public void setRunCommand(String runCommand) {
    this.runCommand = runCommand;
  }

  /**
   * Gets whether the programming language is enabled (offered to submitters).
   *
   * @return whether the programming language is enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Sets whether the programming language is enabled (offered to submitters).
   *
   * @param enabled - whether the programming language is enabled
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Gets the canonical source filename used when the judger writes the submission to disk.
   *
   * @return the canonical source filename of the programming language
   */
  public String getSourceFilename() {
    return sourceFilename;
  }

  /**
   * Sets the canonical source filename used when the judger writes the submission to disk.
   *
   * @param sourceFilename - the canonical source filename of the programming language
   */
  public void setSourceFilename(String sourceFilename) {
    this.sourceFilename = sourceFilename;
  }

  /**
   * Gets the factor by which the wall-clock time limit is scaled for this programming language.
   *
   * @return the time limit multiplier of the programming language
   */
  public double getTimeMultiplier() {
    return timeMultiplier;
  }

  /**
   * Sets the factor by which the wall-clock time limit is scaled for this programming language.
   *
   * @param timeMultiplier - the time limit multiplier of the programming language
   */
  public void setTimeMultiplier(double timeMultiplier) {
    this.timeMultiplier = timeMultiplier;
  }

  /**
   * Gets the factor by which the memory limit is scaled for this programming language.
   *
   * @return the memory limit multiplier of the programming language
   */
  public double getMemoryMultiplier() {
    return memoryMultiplier;
  }

  /**
   * Sets the factor by which the memory limit is scaled for this programming language.
   *
   * @param memoryMultiplier - the memory limit multiplier of the programming language
   */
  public void setMemoryMultiplier(double memoryMultiplier) {
    this.memoryMultiplier = memoryMultiplier;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return String.format(
        "Language [ID=%d, Slug=%s, Name=%s, CompileCommand=%s, runCommand=%s]",
        new Object[] {languageId, languageSlug, languageName, compileCommand, runCommand});
  }

  /** The unique identifier of the programming language. */
  private int languageId;

  /** The alias of the programming language. */
  private String languageSlug;

  /** The name of the programming language. */
  private String languageName;

  /**
   * The compile command of the programming language. WRITE_ONLY keeps it out of serialized output
   * (the original {@code @JsonIgnore} intent) while still allowing it to be read from the admin save
   * payload.
   */
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String compileCommand;

  /**
   * The run command of the programming language. WRITE_ONLY keeps it out of serialized output while
   * still allowing it to be read from the admin save payload.
   */
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String runCommand;

  /** Whether the programming language is enabled (offered to submitters). */
  private boolean enabled = true;

  /** The canonical source filename used when the judger writes the submission to disk. */
  private String sourceFilename = "";

  /** The factor by which the wall-clock time limit is scaled for this programming language. */
  private double timeMultiplier = 1.0;

  /** The factor by which the memory limit is scaled for this programming language. */
  private double memoryMultiplier = 1.0;

  /** The unique serialization identifier. */
  private static final long serialVersionUID = 9065824880175832696L;
}
