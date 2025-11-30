package io.poddeck.agent;

import com.google.inject.Guice;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.ServiceRepository;
import io.poddeck.agent.metric.MetricSchedule;
import io.poddeck.agent.namespace.NamespaceCreateService;
import io.poddeck.agent.namespace.NamespaceDeleteService;
import io.poddeck.agent.namespace.NamespaceListService;
import io.poddeck.agent.node.NodeListService;
import io.poddeck.agent.pod.PodDeleteService;
import io.poddeck.agent.pod.PodFindService;
import io.poddeck.agent.pod.PodListService;
import io.poddeck.agent.pod.PodLogService;
import io.poddeck.common.*;
import io.poddeck.common.event.EventExecutor;
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
      var eventExecutor = injector.getInstance(EventExecutor.class);
      log.info("Connecting to core...");
      var serviceRepository = injector.getInstance(ServiceRepository.class);
      serviceRepository.register(NodeListRequest.class,
        injector.getInstance(NodeListService.class));
      serviceRepository.register(NamespaceCreateRequest.class,
        injector.getInstance(NamespaceCreateService.class));
      serviceRepository.register(NamespaceDeleteRequest.class,
        injector.getInstance(NamespaceDeleteService.class));
      serviceRepository.register(NamespaceListRequest.class,
        injector.getInstance(NamespaceListService.class));
      serviceRepository.register(PodListRequest.class,
        injector.getInstance(PodListService.class));
      serviceRepository.register(PodFindRequest.class,
        injector.getInstance(PodFindService.class));
      serviceRepository.register(PodDeleteRequest.class,
        injector.getInstance(PodDeleteService.class));
      serviceRepository.register(PodLogRequest.class,
        injector.getInstance(PodLogService.class));
      var client = injector.getInstance(CommunicationClient.class);
      client.connect();
      log.info("Successfully connected to core");
      injector.getInstance(MetricSchedule.class).start();
      log.info("Successfully started metric schedule");
      log.info("Successfully booted PodDeck - Agent");
      client.awaitTermination();
    } catch (Exception exception) {
      log.processError(exception);
    }
  }
}