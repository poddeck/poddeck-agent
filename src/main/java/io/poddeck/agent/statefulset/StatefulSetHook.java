package io.poddeck.agent.statefulset;

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
public final class StatefulSetHook implements Hook {
  private final ServiceRepository serviceRepository;
  private final StatefulSetListService statefulSetListService;
  private final StatefulSetFindService statefulSetFindService;
  private final StatefulSetCreateService statefulSetCreateService;
  private final StatefulSetDeleteService statefulSetDeleteService;
  private final StatefulSetScaleService statefulSetScaleService;
  private final StatefulSetRestartService statefulSetRestartService;
  private final StatefulSetEditService statefulSetEditService;

  @EventHook
  private void applicationLaunch(ApplicationLaunchEvent event) {
    serviceRepository.register(StatefulSetListRequest.class,
      statefulSetListService);
    serviceRepository.register(StatefulSetFindRequest.class,
      statefulSetFindService);
    serviceRepository.register(StatefulSetCreateRequest.class,
      statefulSetCreateService);
    serviceRepository.register(StatefulSetDeleteRequest.class,
      statefulSetDeleteService);
    serviceRepository.register(StatefulSetScaleRequest.class,
      statefulSetScaleService);
    serviceRepository.register(StatefulSetRestartRequest.class,
      statefulSetRestartService);
    serviceRepository.register(StatefulSetEditRequest.class,
      statefulSetEditService);
  }
}