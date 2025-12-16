package io.poddeck.agent.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.ServiceListRequest;
import io.poddeck.common.ServiceListResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class ServiceListService implements Service<ServiceListRequest> {
  private final CoreV1Api coreApi;
  private final ServiceFactory serviceFactory;

  @Override
  public void process(
    CommunicationClient client, String requestId, ServiceListRequest request
  ) throws Exception {
    var serviceList = coreApi.listServiceForAllNamespaces().execute().getItems();
    var services = serviceList.stream()
      .map(serviceFactory::assembleService).toList();
    client.send(requestId, ServiceListResponse.newBuilder()
      .addAllItems(services)
      .build());
  }
}
