package io.poddeck.agent.telegraf;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.apache.commons.configuration2.AbstractConfiguration;

@RequiredArgsConstructor(staticName = "create")
public final class TelegrafInjectionModule extends AbstractModule {
  @Provides
  @Singleton
  TelegrafConfiguration telegrafConfiguration(AbstractConfiguration file) {
    var configuration = TelegrafConfiguration.create();
    configuration.load(file);
    return configuration;
  }
}