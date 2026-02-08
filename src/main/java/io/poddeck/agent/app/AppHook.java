package io.poddeck.agent.app;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.marcnuri.helm.Helm;
import io.poddeck.agent.application.ApplicationLaunchEvent;
import io.poddeck.agent.communication.service.ServiceRepository;
import io.poddeck.common.AppListRequest;
import io.poddeck.common.event.EventHook;
import io.poddeck.common.event.Hook;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class AppHook implements Hook {
  private final ServiceRepository serviceRepository;
  private final AppListService appListService;
  private final Log log;

  @EventHook
  private void applicationLaunch(ApplicationLaunchEvent event) {
    initializeHelmConfig();
    updateHelmRepositories();
    serviceRepository.register(AppListRequest.class, appListService);
  }

  private static final String DEFAULT_REPOSITORY_CONFIG = """
    apiVersion: ""
    generated: "0001-01-01T00:00:00Z"
    repositories:
    - caFile: ""
      certFile: ""
      insecure_skip_tls_verify: false
      keyFile: ""
      name: bitnami
      pass_credentials_all: false
      password: ""
      url: https://charts.bitnami.com/bitnami
      username: ""
  """;

  private void initializeHelmConfig() {
    try {
      var repositoriesFile = Paths.get(System.getenv("HELM_REPOSITORY_CONFIG"));
      var configDirectory = repositoriesFile.getParent();
      Files.createDirectories(configDirectory);
      if (!Files.exists(repositoriesFile)) {
        Files.writeString(repositoriesFile, DEFAULT_REPOSITORY_CONFIG);
      }
    } catch (Exception exception) {
      log.processError(exception);
    }
  }

  private void updateHelmRepositories() {
    CompletableFuture.runAsync(() -> {
      try {
        log.info("Updating Helm repositories...");
        Helm.repo().update().call();
        log.info("Successfully updated Helm repositories");
      } catch (Exception exception) {
        log.processError(exception);
      }
    });
  }
}