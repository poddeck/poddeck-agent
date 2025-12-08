package io.poddeck.agent;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.poddeck.agent.application.ApplicationLaunchEvent;
import io.poddeck.agent.application.ApplicationPostRunEvent;
import io.poddeck.agent.application.ApplicationPreRunEvent;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.deployment.DeploymentHook;
import io.poddeck.agent.event.EventHook;
import io.poddeck.agent.metric.MetricSchedule;
import io.poddeck.agent.namespace.NamespaceHook;
import io.poddeck.agent.node.NodeHook;
import io.poddeck.agent.pod.PodHook;
import io.poddeck.common.event.EventExecutor;
import io.poddeck.common.event.HookRegistry;
import io.poddeck.common.log.Log;

public class AgentApplication {
  /**
   * The starting point where the application is executed
   * @param args The arguments that are passed into the application
   */
  public static void main(String[] args) {
    var injector = Guice.createInjector(AgentModule.create());
    var log = injector.getInstance(Log.class);
    Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
      log.processError(throwable));
    try {
      log.info("Initializing PodDeck - Agent");
      registerHooks(injector);
      var eventExecutor = injector.getInstance(EventExecutor.class);
      eventExecutor.execute(ApplicationLaunchEvent.create());
      log.info("Connecting to core...");
      var client = injector.getInstance(CommunicationClient.class);
      eventExecutor.execute(ApplicationPreRunEvent.create());
      client.connect();
      log.info("Successfully connected to core");
      injector.getInstance(MetricSchedule.class).start();
      log.info("Successfully started metric schedule");
      log.info("Successfully booted PodDeck - Agent");
      eventExecutor.execute(ApplicationPostRunEvent.create());
      client.awaitTermination();
    } catch (Exception exception) {
      log.processError(exception);
    }
  }

  private static void registerHooks(Injector injector) {
    var hookRegistry = injector.getInstance(HookRegistry.class);
    hookRegistry.register(injector.getInstance(NodeHook.class));
    hookRegistry.register(injector.getInstance(NamespaceHook.class));
    hookRegistry.register(injector.getInstance(EventHook.class));
    hookRegistry.register(injector.getInstance(PodHook.class));
    hookRegistry.register(injector.getInstance(DeploymentHook.class));
  }
}