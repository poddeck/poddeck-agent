package io.poddeck.agent.cronjob;

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
public final class CronJobHook implements Hook {
  private final ServiceRepository serviceRepository;
  private final CronJobListService cronJobListService;
  private final CronJobFindService cronJobFindService;
  private final CronJobCreateService cronJobCreateService;
  private final CronJobDeleteService cronJobDeleteService;
  private final CronJobSuspendService cronJobSuspendService;
  private final CronJobEditService cronJobEditService;

  @EventHook
  private void applicationLaunch(ApplicationLaunchEvent event) {
    serviceRepository.register(CronJobListRequest.class,
      cronJobListService);
    serviceRepository.register(CronJobFindRequest.class,
      cronJobFindService);
    serviceRepository.register(CronJobCreateRequest.class,
      cronJobCreateService);
    serviceRepository.register(CronJobDeleteRequest.class,
      cronJobDeleteService);
    serviceRepository.register(CronJobSuspendRequest.class,
      cronJobSuspendService);
    serviceRepository.register(CronJobEditRequest.class,
      cronJobEditService);
  }
}