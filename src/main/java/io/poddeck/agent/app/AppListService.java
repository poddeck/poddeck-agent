package io.poddeck.agent.app;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.marcnuri.helm.Helm;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.AppListRequest;
import io.poddeck.common.AppListResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class AppListService implements Service<AppListRequest> {
  private final AppFactory appFactory;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId, AppListRequest request
  ) throws Exception {
    try {
      var results = Helm.search().repo().call();
      var releases = Helm.list().call();
      var apps = appFactory.assembleApps(results, releases);
      client.send(requestId, AppListResponse.newBuilder()
        .setSuccess(true)
        .addAllApps(apps)
        .build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, AppListResponse.newBuilder()
        .setSuccess(false)
        .build());
    }
  }
}