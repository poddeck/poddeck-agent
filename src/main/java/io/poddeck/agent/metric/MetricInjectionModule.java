package io.poddeck.agent.metric;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.apache.commons.configuration2.AbstractConfiguration;

@RequiredArgsConstructor(staticName = "create")
public final class MetricInjectionModule extends AbstractModule {
  @Provides
  @Singleton
  MetricConfiguration metricConfiguration(AbstractConfiguration file) {
    var configuration = MetricConfiguration.create();
    configuration.load(file);
    return configuration;
  }
}