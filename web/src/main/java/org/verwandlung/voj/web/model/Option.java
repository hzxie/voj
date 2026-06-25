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

/**
 * The model of a system setting option.
 *
 * @author Haozhe Xie
 */
public class Option implements Serializable {
  /** Default constructor of the Option class. */
  public Option() {}

  /**
   * Constructor of the Option class.
   *
   * @param optionName - the name of the option
   * @param optionValue - the value of the option
   * @param isAutoload - whether the option is loaded automatically
   */
  public Option(String optionName, String optionValue, boolean isAutoload) {
    this.optionName = optionName;
    this.optionValue = optionValue;
    this.isAutoload = isAutoload;
  }

  /**
   * Constructor of the Option class.
   *
   * @param optionId - the unique identifier of the option
   * @param optionName - the name of the option
   * @param optionValue - the value of the option
   * @param isAutoload - whether the option is loaded automatically
   */
  public Option(int optionId, String optionName, String optionValue, boolean isAutoload) {
    this(optionName, optionValue, isAutoload);
    this.optionId = optionId;
  }

  /**
   * Gets the unique identifier of the option.
   *
   * @return the unique identifier of the option
   */
  public int getOptionId() {
    return optionId;
  }

  /**
   * Sets the unique identifier of the option.
   *
   * @param optionId - the unique identifier of the option
   */
  public void setOptionId(int optionId) {
    this.optionId = optionId;
  }

  /**
   * Gets the name of the option.
   *
   * @return the name of the option
   */
  public String getOptionName() {
    return optionName;
  }

  /**
   * Sets the name of the option.
   *
   * @param optionName - the name of the option
   */
  public void setOptionName(String optionName) {
    this.optionName = optionName;
  }

  /**
   * Gets the value of the option.
   *
   * @return the value of the option
   */
  public String getOptionValue() {
    return optionValue;
  }

  /**
   * Sets the value of the option.
   *
   * @param optionValue - the value of the option
   */
  public void setOptionValue(String optionValue) {
    this.optionValue = optionValue;
  }

  /**
   * Gets whether the option is loaded automatically.
   *
   * @return whether the option is loaded automatically
   */
  public boolean isAutoload() {
    return isAutoload;
  }

  /**
   * Sets whether the option is loaded automatically.
   *
   * @param isAutoload - whether the option is loaded automatically
   */
  public void setAutoload(boolean isAutoload) {
    this.isAutoload = isAutoload;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return String.format(
        "Option [ID=%d, Name=%s, Value=%s, isAutoLoad=%s]",
        new Object[] {optionId, optionName, optionValue, isAutoload});
  }

  /** The unique identifier of the option. */
  private int optionId;

  /** The name of the option. */
  private String optionName;

  /** The value of the option. */
  private String optionValue;

  /** Whether the option is loaded automatically. */
  private boolean isAutoload;

  /** The unique serialization identifier. */
  private static final long serialVersionUID = -1533277464247886659L;
}
