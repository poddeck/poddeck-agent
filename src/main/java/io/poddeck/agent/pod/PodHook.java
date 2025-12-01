package io.poddeck.agent.pod;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.poddeck.agent.application.ApplicationLaunchEvent;
import io.poddeck.agent.communication.service.ServiceRepository;
import io.poddeck.common.PodDeleteRequest;
import io.poddeck.common.PodFindRequest;
import io.poddeck.common.PodListRequest;
import io.poddeck.common.PodLogRequest;
import io.poddeck.common.event.EventHook;
import io.poddeck.common.event.Hook;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class PodHook implements Hook {
  private final ServiceRepository serviceRepository;
  private final PodListService podListService;
  private final PodFindService podFindService;
  private final PodDeleteService podDeleteService;
  private final PodLogService podLogService;

  @EventHook
  private void applicationLaunch(ApplicationLaunchEvent event) {
    serviceRepository.register(PodListRequest.class, podListService);
    serviceRepository.register(PodFindRequest.class, podFindService);
    serviceRepository.register(PodDeleteRequest.class, podDeleteService);
    serviceRepository.register(PodLogRequest.class, podLogService);
  }
}