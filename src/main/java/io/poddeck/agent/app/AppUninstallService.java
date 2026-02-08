package io.poddeck.agent.app;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.marcnuri.helm.Helm;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.AppUninstallRequest;
import io.poddeck.common.AppUninstallResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class AppUninstallService implements Service<AppUninstallRequest> {
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId, AppUninstallRequest request
  ) throws Exception {
    try {
      Helm.uninstall(request.getName()).withNamespace(request.getNamespace())
        .call();
      client.send(requestId, AppUninstallResponse.newBuilder()
        .setSuccess(true)
        .build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, AppUninstallResponse.newBuilder()
        .setSuccess(false)
        .build());
    }
  }
}