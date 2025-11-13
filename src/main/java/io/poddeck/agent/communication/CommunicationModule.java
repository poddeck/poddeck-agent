package io.poddeck.agent.communication;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.apache.commons.configuration2.AbstractConfiguration;

@RequiredArgsConstructor(staticName = "create")
public final class CommunicationModule extends AbstractModule {
  @Provides
  @Singleton
  CommunicationConfiguration communicationConfiguration(AbstractConfiguration file) {
    var configuration = CommunicationConfiguration.create();
    configuration.load(file);
    return configuration;
  }
}
