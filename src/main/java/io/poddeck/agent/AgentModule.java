package io.poddeck.agent;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.Config;
import io.poddeck.agent.communication.CommunicationModule;
import io.poddeck.agent.metric.MetricInjectionModule;
import io.poddeck.agent.telegraf.TelegrafInjectionModule;
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
    install(TelegrafInjectionModule.create());
    install(MetricInjectionModule.create());
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

  @Provides
  @Singleton
  ApiClient provideKubernetesApiClient() throws Exception {
    var client = Config.defaultClient();
    Configuration.setDefaultApiClient(client);
    return client;
  }

  @Provides
  @Singleton
  CoreV1Api provideKubernetesCoreApi(ApiClient client) {
    var coreApi = new CoreV1Api();
    coreApi.setApiClient(client);
    return coreApi;
  }

  @Provides
  @Singleton
  AppsV1Api provideKubernetesAppsApi(ApiClient client) {
    var appsApi = new AppsV1Api();
    appsApi.setApiClient(client);
    return appsApi;
  }
}
