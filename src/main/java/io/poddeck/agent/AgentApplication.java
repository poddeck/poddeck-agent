package io.poddeck.agent;

import com.google.inject.Guice;
import io.poddeck.agent.communication.CommunicationClient;
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
      var client = injector.getInstance(CommunicationClient.class);
      client.connect();
      log.info("Successfully connected to core");
      log.info("Successfully booted PodDeck - Agent");
      client.awaitTermination();
    } catch (Exception exception) {
      log.processError(exception);
    }
  }
}