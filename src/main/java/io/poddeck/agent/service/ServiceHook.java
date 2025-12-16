package io.poddeck.agent.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.poddeck.agent.application.ApplicationLaunchEvent;
import io.poddeck.agent.communication.service.ServiceRepository;
import io.poddeck.common.ServiceCreateRequest;
import io.poddeck.common.ServiceDeleteRequest;
import io.poddeck.common.ServiceFindRequest;
import io.poddeck.common.ServiceListRequest;
import io.poddeck.common.event.EventHook;
import io.poddeck.common.event.Hook;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class ServiceHook implements Hook {
  private final ServiceRepository serviceRepository;
  private final ServiceListService serviceListService;
  private final ServiceFindService serviceFindService;
  private final ServiceCreateService serviceCreateService;
  private final ServiceDeleteService serviceDeleteService;

  @EventHook
  private void applicationLaunch(ApplicationLaunchEvent event) {
    serviceRepository.register(ServiceListRequest.class,
      serviceListService);
    serviceRepository.register(ServiceFindRequest.class,
      serviceFindService);
    serviceRepository.register(ServiceCreateRequest.class,
      serviceCreateService);
    serviceRepository.register(ServiceDeleteRequest.class,
      serviceDeleteService);
  }
}