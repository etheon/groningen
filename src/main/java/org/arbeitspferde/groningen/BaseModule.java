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

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import org.arbeitspferde.groningen.common.BlockScope;
import org.arbeitspferde.groningen.common.Settings;
import org.arbeitspferde.groningen.common.SettingsProvider;
import org.arbeitspferde.groningen.common.SimpleScope;
import org.arbeitspferde.groningen.common.SystemAdapter;
import org.arbeitspferde.groningen.common.SystemAdapterImpl;
import org.arbeitspferde.groningen.config.ConfigManager;
import org.arbeitspferde.groningen.config.PipelineIterationScoped;
import org.arbeitspferde.groningen.config.PipelineScoped;
import org.arbeitspferde.groningen.config.ProtoBufConfigManager;
import org.arbeitspferde.groningen.config.ProtoBufConfigManagerFactory;
import org.arbeitspferde.groningen.display.DisplayMediator;
import org.arbeitspferde.groningen.display.Displayable;
import org.arbeitspferde.groningen.display.MonitorGroningen;
import org.arbeitspferde.groningen.eventlog.SubjectEventLogger;
import org.arbeitspferde.groningen.eventlog.SubjectEventProtoLogger;
import org.arbeitspferde.groningen.experimentdb.ExperimentDb;
import org.arbeitspferde.groningen.externalprocess.CmdProcessInvoker;
import org.arbeitspferde.groningen.externalprocess.ProcessInvoker;
import org.arbeitspferde.groningen.generator.Generator;
import org.arbeitspferde.groningen.hypothesizer.Hypothesizer;
import org.arbeitspferde.groningen.proto.Params.GroningenParams;
import org.arbeitspferde.groningen.scorer.GenerationNumberWeightedBestPerformerScorer;
import org.arbeitspferde.groningen.scorer.HistoricalBestPerformerScorer;
import org.arbeitspferde.groningen.scorer.IterationScorer;
import org.arbeitspferde.groningen.scorer.LinearCombinationScorer;
import org.arbeitspferde.groningen.scorer.SubjectScorer;
import org.arbeitspferde.groningen.utility.Clock;
import org.arbeitspferde.groningen.utility.SystemClock;
import org.arbeitspferde.groningen.validator.Validator;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Map;
import java.util.Timer;

/**
 * The main Guice module for the Groningen server.
 */
public class BaseModule extends AbstractModule {
  private final String[] args;

  public BaseModule(final String[] args) {
    this.args = args;
  }

  @Override
  protected void configure() {
    // Scopes bindings
    SimpleScope pipelineScope = new SimpleScope();
    bindScope(PipelineScoped.class, pipelineScope);
    bind(BlockScope.class)
      .annotatedWith(Names.named(PipelineScoped.SCOPE_NAME))
      .toInstance(pipelineScope);

    SimpleScope pipelineIterationScope = new SimpleScope();
    bindScope(PipelineIterationScoped.class, pipelineIterationScope);
    bind(BlockScope.class)
      .annotatedWith(Names.named(PipelineIterationScoped.SCOPE_NAME))
      .toInstance(pipelineIterationScope);

    // Singleton bindings
    bind(Clock.class).to(SystemClock.class);
    bind(PipelineIdGenerator.class).in(Singleton.class);
    bind(Gson.class).in(Singleton.class);

    // Pipeline-scoped bindings
    bind(Pipeline.class).in(PipelineScoped.class);
    bind(PipelineId.class)
        .toProvider(SimpleScope.<PipelineId>seededKeyProvider())
        .in(PipelineScoped.class);
    bind(Displayable.class).to(DisplayMediator.class).in(PipelineScoped.class);
    bind(MonitorGroningen.class).to(DisplayMediator.class).in(PipelineScoped.class);
    bind(Hypothesizer.class).in(PipelineScoped.class);
    bind(ExperimentDb.class).in(PipelineScoped.class);
    bind(ConfigManager.class)
        .toProvider(SimpleScope.<ConfigManager>seededKeyProvider())
        .in(PipelineScoped.class);
    bind(PipelineStageInfo.class)
        .toProvider(SimpleScope.<PipelineStageInfo>seededKeyProvider())
        .in(PipelineScoped.class);

    // PipelineIteration-scoped bindings
    bind(Generator.class).in(PipelineIterationScoped.class);
    bind(Validator.class).in(PipelineIterationScoped.class);
    bind(IterationScorer.class).in(PipelineIterationScoped.class);
    bind(ProcessInvoker.class).to(CmdProcessInvoker.class).in(PipelineIterationScoped.class);

    // General bindings
    bind(SystemAdapter.class).to(SystemAdapterImpl.class);
    bind(Timer.class).toProvider(DaemonTimerProvider.class);
    bind(Settings.class).toProvider(SettingsProvider.class).asEagerSingleton();
    bind(SubjectEventLogger.class).to(SubjectEventProtoLogger.class);
    bind(SubjectScorer.class).to(LinearCombinationScorer.class);
    bind(HistoricalBestPerformerScorer.class)
        .to(GenerationNumberWeightedBestPerformerScorer.class);

    install(new FactoryModuleBuilder()
        .implement(ConfigManager.class, ProtoBufConfigManager.class)
        .build(ProtoBufConfigManagerFactory.class));

    bind(PipelineSynchronizer.class).to(EmptyPipelineSynchronizer.class);
    MapBinder<GroningenParams.PipelineSynchMode, PipelineSynchronizer> pipelineSycMapBinder =
        MapBinder.newMapBinder(
            binder(), GroningenParams.PipelineSynchMode.class, PipelineSynchronizer.class);
    pipelineSycMapBinder.addBinding(GroningenParams.PipelineSynchMode.NONE)
        .to(EmptyPipelineSynchronizer.class);
    pipelineSycMapBinder.addBinding(GroningenParams.PipelineSynchMode.ITERATION_FINALIZATION_ONLY)
        .to(IterationFinalizationSynchronizer.class);
  }

  @Provides
  @Named("numShards")
  public Integer produceNumShards(Settings settings) {
    return settings.getNumShards();
  }

  @Provides
  @Named("shardIndex")
  public Integer produceShardIndex(Settings settings) {
    return settings.getShardIndex();
  }

  @Provides
  @Singleton
  public Datastore produceDatastore(Settings settings,
      Map<String, Provider<Datastore>> datastorePlugins) {
    return datastorePlugins.get(settings.getDatastore()).get();
  }

  @Provides
  @Singleton
  public HistoryDatastore produceHistoryDatastore(Settings settings,
      Map<String, Provider<HistoryDatastore>> historyDatastorePlugins) {
    return historyDatastorePlugins.get(settings.getHistoryDatastore()).get();
  }

  /**
   * Dispenses {@link Timer} for testability.
   *
   */
  @Singleton
  public static class DaemonTimerProvider implements Provider<Timer> {

    @Override
    public Timer get() {
      return new Timer(true);
    }
  }

  @Provides
  @Singleton
  @Named("startTime")
  public Long produceStartTime() {
    final RuntimeMXBean runtimeInformation = ManagementFactory.getRuntimeMXBean();

    return runtimeInformation.getStartTime();
  }

  @Provides
  @Singleton
  public String[] getArguments() {
    return args;
  }
}
