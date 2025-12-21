package io.poddeck.agent.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.ServiceFindRequest;
import io.poddeck.common.ServiceFindResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class ServiceFindService implements Service<ServiceFindRequest> {
  private final CoreV1Api coreApi;
  private final ServiceFactory serviceFactory;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId, ServiceFindRequest request
  ) throws Exception {
    try {
      var service = coreApi.readNamespacedService(request.getService(),
        request.getNamespace()).execute();
      client.send(requestId, ServiceFindResponse.newBuilder()
        .setSuccess(true)
        .setService(serviceFactory.assembleService(service))
        .build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, ServiceFindResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}
