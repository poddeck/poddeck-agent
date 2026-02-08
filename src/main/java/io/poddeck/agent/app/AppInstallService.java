package io.poddeck.agent.app;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.marcnuri.helm.Helm;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.AppInstallRequest;
import io.poddeck.common.AppInstallResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class AppInstallService implements Service<AppInstallRequest> {
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId, AppInstallRequest request
  ) throws Exception {
    try {
      var release = Helm.install(request.getChart()).withName(request.getName())
        .withNamespace(request.getNamespace())
        .withVersion(request.getVersion()).call();
      client.send(requestId, AppInstallResponse.newBuilder()
        .setSuccess(true).setStatus(release.getStatus())
        .setOutput(release.getOutput())
        .build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, AppInstallResponse.newBuilder()
        .setSuccess(false)
        .build());
    }
  }
}