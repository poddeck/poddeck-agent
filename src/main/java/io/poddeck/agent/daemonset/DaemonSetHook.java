package io.poddeck.agent.daemonset;

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
public final class DaemonSetHook implements Hook {
  private final ServiceRepository serviceRepository;
  private final DaemonSetListService daemonSetListService;
  private final DaemonSetFindService daemonSetFindService;
  private final DaemonSetCreateService daemonsetCreateService;
  private final DaemonSetDeleteService daemonSetDeleteService;
  private final DaemonSetRestartService daemonSetRestartService;
  private final DaemonSetEditService daemonSetEditService;

  @EventHook
  private void applicationLaunch(ApplicationLaunchEvent event) {
    serviceRepository.register(DaemonSetListRequest.class,
      daemonSetListService);
    serviceRepository.register(DaemonSetFindRequest.class,
      daemonSetFindService);
    serviceRepository.register(DaemonSetCreateRequest.class,
      daemonsetCreateService);
    serviceRepository.register(DaemonSetDeleteRequest.class,
      daemonSetDeleteService);
    serviceRepository.register(DaemonSetRestartRequest.class,
      daemonSetRestartService);
    serviceRepository.register(DaemonSetEditRequest.class,
      daemonSetEditService);
  }
}