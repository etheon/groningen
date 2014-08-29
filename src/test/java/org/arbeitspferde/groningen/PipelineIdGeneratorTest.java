/* Copyright 2012 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.arbeitspferde.groningen;

import com.google.common.hash.Hashing;

import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.config.StubConfigManager;
import org.arbeitspferde.groningen.proto.GroningenConfigProto.ProgramConfiguration;
import org.arbeitspferde.groningen.proto.Params.GroningenParams;
import org.arbeitspferde.groningen.utility.Clock;

import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.joda.time.Instant;

/**
 * Test for {@link PipelineIdGenerator}
 */
public class PipelineIdGeneratorTest extends TestCase {
  private static final int SHARD_INDEX = 0;
  private static final int NUM_SHARDS = 1;
  private static final String SERVING_ADDRESS = "myservingaddress:31337";

  private final Clock mockClock = EasyMock.createNiceMock(Clock.class);
  private final PipelineIdGenerator pipelineIdGenerator =
      new PipelineIdGenerator(SHARD_INDEX, NUM_SHARDS, SERVING_ADDRESS, mockClock, Hashing.md5());
  private final GroningenConfig stubConfig = new StubConfigManager.StubConfig() {
    @Override
    public ProgramConfiguration getProtoConfig() {
      return ProgramConfiguration.getDefaultInstance();
    }
  };

  public void testGeneratesPipelineId() {
    Instant now = Instant.now();
    EasyMock.expect(mockClock.now()).andReturn(now);
    EasyMock.replay(mockClock);
    assertNotNull(pipelineIdGenerator.generatePipelineId(stubConfig));
  }

  public void testGenerateRequestedPipelineId() {
    GroningenConfig configMock = EasyMock.createNiceMock(GroningenConfig.class);
    GroningenParams paramBlock = GroningenParams.newBuilder()
        .setRequestedPipelineId("fakeId")
        .build();
    EasyMock.expect(configMock.getParamBlock()).andReturn(paramBlock).anyTimes();
    EasyMock.replay(configMock);
    PipelineId id = pipelineIdGenerator.generatePipelineId(configMock);
    assertEquals(new PipelineId("fakeId"), id);
  }

  public void testPipelineIdDependsOnCurrentTime() {
    Instant now = Instant.now();
    Instant nowPlusOne = now.plus(1);
    EasyMock.expect(mockClock.now()).andReturn(now);
    EasyMock.expect(mockClock.now()).andReturn(nowPlusOne);
    EasyMock.replay(mockClock);

    PipelineId id1 = pipelineIdGenerator.generatePipelineId(stubConfig);
    PipelineId id2 = pipelineIdGenerator.generatePipelineId(stubConfig);

    assertTrue(!id1.equals(id2));
  }
}
