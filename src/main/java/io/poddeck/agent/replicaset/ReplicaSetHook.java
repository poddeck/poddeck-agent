package io.poddeck.agent.replicaset;

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
public final class ReplicaSetHook implements Hook {
  private final ServiceRepository serviceRepository;
  private final ReplicaSetListService replicaSetListService;
  private final ReplicaSetFindService replicaSetFindService;
  private final ReplicaSetCreateService replicaSetCreateService;
  private final ReplicaSetDeleteService replicaSetDeleteService;
  private final ReplicaSetScaleService replicaSetScaleService;
  private final ReplicaSetEditService replicaSetEditService;

  @EventHook
  private void applicationLaunch(ApplicationLaunchEvent event) {
    serviceRepository.register(ReplicaSetListRequest.class,
      replicaSetListService);
    serviceRepository.register(ReplicaSetFindRequest.class,
      replicaSetFindService);
    serviceRepository.register(ReplicaSetCreateRequest.class,
      replicaSetCreateService);
    serviceRepository.register(ReplicaSetDeleteRequest.class,
      replicaSetDeleteService);
    serviceRepository.register(ReplicaSetScaleRequest.class,
      replicaSetScaleService);
    serviceRepository.register(ReplicaSetEditRequest.class,
      replicaSetEditService);
  }
}