package org.arbeitspferde.groningen.historydatastore;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.arbeitspferde.groningen.HistoryDatastore;
import org.arbeitspferde.groningen.PipelineHistoryState;
import org.arbeitspferde.groningen.PipelineId;

import org.joda.time.Instant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Memory-only implementation of {@link HistoryDatastore}.
 */
public class MemoryHistoryDatastore implements HistoryDatastore {

  private final Map<PipelineId, List<PipelineHistoryState>> data =
      new HashMap<>();

  @Override
  public void writeState(PipelineHistoryState state) {
    List<PipelineHistoryState> states = data.get(state.pipelineId());
    if (states == null) {
      states = new ArrayList<>();
      data.put(state.pipelineId(), states);
    }
    states.add(state);
    Collections.sort(states, new Comparator<PipelineHistoryState>() {
      @Override
      public int compare(PipelineHistoryState s1, PipelineHistoryState s2) {
        return s1.endTimestamp().compareTo(s2.endTimestamp());
      }
    });
  }

  @Override
  public List<PipelineId> listPipelinesIds() {
    return Lists.newArrayList(data.keySet());
  }

  private List<PipelineHistoryState> readStates(PipelineId pipelineId) {
    List<PipelineHistoryState> states = data.get(pipelineId);
    if (states == null) {
      states = new ArrayList<>();
    }
    return states;
  }

  @Override
  public List<PipelineHistoryState> getStatesForPipelineId(PipelineId pipelineId) {
    return ImmutableList.copyOf(readStates(pipelineId));
  }

  @Override
  public List<PipelineHistoryState> getStatesForPipelineId(
      PipelineId pipelineId, Instant afterTimestamp) {
    List<PipelineHistoryState> results = new ArrayList<>();
    for (PipelineHistoryState state : readStates(pipelineId)) {
      if (state.endTimestamp().isAfter(afterTimestamp)) {
        results.add(state);
      }
    }
    return ImmutableList.copyOf(results);
  }
}
