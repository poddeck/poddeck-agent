package io.poddeck.agent;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.poddeck.agent.communication.CommunicationModule;
import io.poddeck.common.event.EventExecutor;
import io.poddeck.common.event.HookRegistry;
import io.poddeck.common.log.Log;
import lombok.RequiredArgsConstructor;
import org.apache.commons.configuration2.AbstractConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;

import java.io.File;

@RequiredArgsConstructor(staticName = "create")
public class AgentModule extends AbstractModule {
  @Override
  protected void configure() {
    install(CommunicationModule.create());
  }

  @Provides
  @Singleton
  Log agentLog() throws Exception {
    return Log.create("Agent");
  }

  @Provides
  @Singleton
  AbstractConfiguration configurationFile() throws Exception {
    return new Configurations().ini(new File("config.ini"));
  }

  @Provides
  @Singleton
  HookRegistry hookRegistry() {
    return HookRegistry.create();
  }

  @Provides
  @Singleton
  EventExecutor eventExecutor(HookRegistry registry, Log log) {
    return EventExecutor.create(registry, log);
  }
}
