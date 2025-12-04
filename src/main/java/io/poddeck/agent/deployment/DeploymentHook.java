package io.poddeck.agent.deployment;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.poddeck.agent.application.ApplicationLaunchEvent;
import io.poddeck.agent.communication.service.ServiceRepository;
import io.poddeck.common.*;
import io.poddeck.common.event.EventHook;
import io.poddeck.common.event.Hook;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class DeploymentHook implements Hook {
  private final ServiceRepository serviceRepository;
  private final DeploymentListService deploymentListService;
  private final DeploymentFindService deploymentFindService;
  private final DeploymentDeleteService deploymentDeleteService;
  private final DeploymentScaleService deploymentScaleService;
  private final DeploymentEditService deploymentEditService;

  @EventHook
  private void applicationLaunch(ApplicationLaunchEvent event) {
    serviceRepository.register(DeploymentListRequest.class,
      deploymentListService);
    serviceRepository.register(DeploymentFindRequest.class,
      deploymentFindService);
    serviceRepository.register(DeploymentDeleteRequest.class,
      deploymentDeleteService);
    serviceRepository.register(DeploymentScaleRequest.class,
      deploymentScaleService);
    serviceRepository.register(DeploymentEditRequest.class,
      deploymentEditService);
  }
}