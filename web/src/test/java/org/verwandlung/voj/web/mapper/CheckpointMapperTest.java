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
package org.verwandlung.voj.web.mapper;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.Checkpoint;

/**
 * The test class for CheckpointMapper.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class CheckpointMapperTest {
  /**
   * Test case: tests the getCheckpointsUsingProblemId(long) method. Test data: an existing problem
   * unique identifier (1000). Expected: the corresponding checkpoint list (10 items).
   */
  @Test
  public void testGetCheckpointsUsingProblemIdExists() {
    List<Checkpoint> checkpoints = checkpointMapper.getCheckpointsUsingProblemId(1000);
    Assertions.assertEquals(10, checkpoints.size());

    Checkpoint firstCheckpoint = checkpoints.get(0);
    String output = firstCheckpoint.getOutput();
    Assertions.assertEquals("45652\r\n", output);
  }

  /** Test case: tests the getCheckpointsUsingProblemId(long) method. Test data: a non-existing problem unique identifier (0). Expected: the corresponding checkpoint list (0 items). */
  @Test
  public void testGetCheckpointsUsingProblemIdNotExists() {
    List<Checkpoint> checkpoints = checkpointMapper.getCheckpointsUsingProblemId(0);
    Assertions.assertEquals(0, checkpoints.size());
  }

  /** Test case: tests the createCheckpoint(Checkpoint) method. Test data: creates a checkpoint with a normal data set. Expected: the checkpoint is created successfully. */
  @Test
  public void testCreateCheckpointNormally() {
    Checkpoint checkpoint = new Checkpoint(1000, 100, false, 10, "input", "output");
    int numberOfRowsAffected = checkpointMapper.createCheckpoint(checkpoint);
    Assertions.assertEquals(1, numberOfRowsAffected);
  }

  /** Test case: tests the createCheckpoint(Checkpoint) method. Test data: creates a checkpoint with a duplicate primary key. Expected: a DuplicateKeyException is thrown. */
  @Test
  public void testCreateCheckpointUsingExistingProblemIdAndCheckpointId() {
    Checkpoint checkpoint = new Checkpoint(1000, 0, false, 10, "input", "output");
    Executable e =
        () -> {
          checkpointMapper.createCheckpoint(checkpoint);
        };
    Assertions.assertThrows(org.springframework.dao.DuplicateKeyException.class, e);
  }

  /**
   * Test case: tests the createCheckpoint(Checkpoint) method. Test data: creates a checkpoint with a
   * non-existing problem ID. Expected: a DataIntegrityViolationException is thrown.
   */
  @Test
  public void testCreateCheckpointUsingNotExistingProblemId() {
    Checkpoint checkpoint = new Checkpoint(0, 100, false, 10, "input", "output");
    Executable e =
        () -> {
          checkpointMapper.createCheckpoint(checkpoint);
        };
    Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
  }

  /** Test case: tests the deleteCheckpoint(long) method. Test data: deletes checkpoints with an existing problem ID. Expected: all checkpoints of the specified problem are deleted. */
  @Test
  public void testDeleteCheckpointUsingExistsingProblemId() {
    List<Checkpoint> checkpoints = checkpointMapper.getCheckpointsUsingProblemId(1000);
    Assertions.assertEquals(10, checkpoints.size());

    checkpointMapper.deleteCheckpoint(1000);
    checkpoints = checkpointMapper.getCheckpointsUsingProblemId(1000);
    Assertions.assertEquals(0, checkpoints.size());
  }

  /** The CheckpointMapper object under test. */
  @Autowired private CheckpointMapper checkpointMapper;
}
