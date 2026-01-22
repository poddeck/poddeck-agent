package io.poddeck.agent.audit;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.poddeck.agent.application.ApplicationLaunchEvent;
import io.poddeck.agent.communication.service.ServiceRepository;
import io.poddeck.common.AuditPerformRequest;
import io.poddeck.common.AuditFindRequest;
import io.poddeck.common.event.EventHook;
import io.poddeck.common.event.Hook;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class AuditHook implements Hook {
  private final ServiceRepository serviceRepository;
  private final AuditPerformService auditPerformService;
  private final AuditFindService auditFindService;

  @EventHook
  private void applicationLaunch(ApplicationLaunchEvent event) {
    serviceRepository.register(AuditPerformRequest.class, auditPerformService);
    serviceRepository.register(AuditFindRequest.class, auditFindService);
  }
}