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
package org.verwandlung.voj.web.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.verwandlung.voj.web.mapper.BulletinBoardMessageMapper;
import org.verwandlung.voj.web.model.BulletinBoardMessage;

import java.util.List;

/**
 * The business logic layer of the bulletin board.
 *
 * @author Haozhe Xie
 */
@Service
@Transactional
public class BulletinBoardService {
  /**
   * Gets the number of bulletin board messages.
   *
   * @return the number of bulletin board messages
   */
  public long getNumberOfBulletinBoardMessages() {
    return bulletinBoardMessageMapper.getNumberOfBulletinBoardMessages();
  }

  /**
   * Gets bulletin board messages.
   *
   * @param offset - the starting number of the bulletin board message identifier
   * @param limit - the number of bulletin board messages to fetch
   * @return a list containing bulletin board messages
   */
  public List<BulletinBoardMessage> getBulletinBoardMessages(long offset, int limit) {
    return bulletinBoardMessageMapper.getBulletinBoardMessages(offset, limit);
  }

  /**
   * Gets a bulletin board message by its unique identifier.
   *
   * @param bulletinBoardMessageId - the unique identifier of the bulletin board message
   * @return the specific bulletin board message
   */
  public BulletinBoardMessage getBulletinBoardMessage(long bulletinBoardMessageId) {
    return bulletinBoardMessageMapper.getBulletinBoardMessageUsingId(bulletinBoardMessageId);
  }

  /** The autowired BulletinBoardMessageMapper, used to obtain bulletin board messages. */
  @Autowired private BulletinBoardMessageMapper bulletinBoardMessageMapper;
}
