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
import java.util.Date;

/**
 * The model of a bulletin board message. Maps to the voj_bulletin_board_messages table in the
 * database.
 *
 * @author Haozhe Xie
 */
public class BulletinBoardMessage implements Serializable {
  /** Default constructor of the bulletin board message. */
  public BulletinBoardMessage() {}

  /**
   * Constructor of the bulletin board message.
   *
   * @param messageTitle - the title of the bulletin board message
   * @param messageBody - the content of the bulletin board message
   * @param messageCreateTime - the creation time of the bulletin board message
   */
  public BulletinBoardMessage(String messageTitle, String messageBody, Date messageCreateTime) {
    this.messageTitle = messageTitle;
    this.messageBody = messageBody;
    this.messageCreateTime = messageCreateTime;
  }

  /**
   * Constructor of the bulletin board message.
   *
   * @param messageId - the unique identifier of the bulletin board message
   * @param messageTitle - the title of the bulletin board message
   * @param messageBody - the content of the bulletin board message
   * @param messageCreateTime - the creation time of the bulletin board message
   */
  public BulletinBoardMessage(
      long messageId, String messageTitle, String messageBody, Date messageCreateTime) {
    this(messageTitle, messageBody, messageCreateTime);
    this.messageId = messageId;
  }

  /**
   * Gets the unique identifier of the bulletin board message.
   *
   * @return the unique identifier of the bulletin board message
   */
  public long getMessageId() {
    return messageId;
  }

  /**
   * Sets the unique identifier of the bulletin board message.
   *
   * @param messageId - the unique identifier of the bulletin board message
   */
  public void setMessageId(long messageId) {
    this.messageId = messageId;
  }

  /**
   * Gets the title of the bulletin board message.
   *
   * @return the title of the bulletin board message
   */
  public String getMessageTitle() {
    return messageTitle;
  }

  /**
   * Sets the title of the bulletin board message.
   *
   * @param messageTitle - the title of the bulletin board message
   */
  public void setMessageTitle(String messageTitle) {
    this.messageTitle = messageTitle;
  }

  /**
   * Gets the content of the bulletin board message.
   *
   * @return the content of the bulletin board message
   */
  public String getMessageBody() {
    return messageBody;
  }

  /**
   * Sets the content of the bulletin board message.
   *
   * @param messageBody - the content of the bulletin board message
   */
  public void setMessageBody(String messageBody) {
    this.messageBody = messageBody;
  }

  /**
   * Gets the creation time of the bulletin board message.
   *
   * @return the creation time of the bulletin board message
   */
  public Date getMessageCreateTime() {
    return messageCreateTime;
  }

  /**
   * Sets the creation time of the bulletin board message.
   *
   * @param messageCreateTime - the creation time of the bulletin board message
   */
  public void setMessageCreateTime(Date messageCreateTime) {
    this.messageCreateTime = messageCreateTime;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return String.format(
        "BulletinBoardMessage [ID=%d, Title=%s, Body=%s, CreateTime=%s]",
        new Object[] {messageId, messageTitle, messageBody, messageCreateTime});
  }

  /** The unique identifier of the bulletin board message. */
  private long messageId;

  /** The title of the bulletin board message. */
  private String messageTitle;

  /** The content of the bulletin board message. */
  private String messageBody;

  /** The creation time of the bulletin board message. */
  private Date messageCreateTime;
}
