package io.poddeck.agent.communication.service;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.Message;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.common.TunnelMessage;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class ServiceRepository {
  private final Log log;
  private final Map<Class<? extends Message>, Service<?>> services = Maps.newConcurrentMap();

  /**
   * Registers a new service
   * @param messageClass The class of the message of the service
   * @param service The service that is to be registered
   */
  public <T extends Message> void register(
    Class<T> messageClass, Service<T> service
  ) {
    services.put(messageClass, service);
  }

  /**
   * Unregisters a service
   * @param messageClass The class of the message of the service that is to be unregistered
   */
  public <T extends Message> void unregister(Class<T> messageClass) {
    services.remove(messageClass);
  }

  /**
   * Is used to dispatch a message to a service
   * @param client The client
   * @param requestId The id of the request
   * @param message The message to be dispatched
   */
  public void dispatch(
    CommunicationClient client, String requestId, TunnelMessage message
  ) {
    try {
      var payload = message.getPayload();
      for (var messageClass : services.keySet()) {
        if (!payload.is(messageClass)) {
          continue;
        }
        var unpacked = payload.unpack(messageClass);
        var service = services.get(messageClass);
        new Thread(() -> callService((Service<Message>) service, client,
          requestId, unpacked)).start();
      }
    } catch (Exception exception) {
      log.processError(exception);
    }
  }

  private void callService(
    Service<Message> service, CommunicationClient client, String requestId,
    Message message
  ) {
    try {
      service.process(client, requestId, message);
    } catch (Exception exception) {
      log.processError(exception);
    }
  }
}
