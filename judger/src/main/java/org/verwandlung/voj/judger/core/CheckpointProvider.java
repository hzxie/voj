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

/**
 * Fetches a problem's checkpoints (test data) from wherever they are stored.
 *
 * <p>This is the pluggable seam of the file-based checkpoint storage: {@link CheckpointStore} owns
 * the local cache (versioning, atomic replacement) and delegates the actual transfer to an
 * implementation of this interface. The default {@link HttpCheckpointProvider} downloads the data
 * from the web application; a future implementation could read it from object storage without any
 * change to the caching logic or the rest of the judger.
 *
 * @author Haozhe Xie
 */
public interface CheckpointProvider {
  /**
   * Downloads a problem's checkpoints into the given directory.
   *
   * <p>On return the directory holds a {@code manifest.properties} describing the checkpoints plus,
   * per checkpoint, an {@code input#<id>.txt} and an {@code output#<id>.txt}. The directory should be
   * treated as a scratch location: {@link CheckpointStore} downloads into a temporary directory and
   * only then swaps it into place, so a failed download never corrupts a usable cache.
   *
   * @param problemId - the unique identifier of the problem
   * @param destinationDirectory - the directory the checkpoints are written into
   * @throws IOException if the checkpoints cannot be downloaded
   */
  void download(long problemId, File destinationDirectory) throws IOException;
}
