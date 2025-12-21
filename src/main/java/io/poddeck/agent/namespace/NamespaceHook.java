package io.poddeck.agent.namespace;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.poddeck.agent.application.ApplicationLaunchEvent;
import io.poddeck.agent.communication.service.ServiceRepository;
import io.poddeck.common.NamespaceCreateRequest;
import io.poddeck.common.NamespaceDeleteRequest;
import io.poddeck.common.NamespaceListRequest;
import io.poddeck.common.event.EventHook;
import io.poddeck.common.event.Hook;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class NamespaceHook implements Hook {
  private final ServiceRepository serviceRepository;
  private final NamespaceListService namespaceListService;
  private final NamespaceCreateService namespaceCreateService;
  private final NamespaceDeleteService namespaceDeleteService;

  @EventHook
  private void applicationLaunch(ApplicationLaunchEvent event) {
    serviceRepository.register(NamespaceListRequest.class,
      namespaceListService);
    serviceRepository.register(NamespaceCreateRequest.class,
      namespaceCreateService);
    serviceRepository.register(NamespaceDeleteRequest.class,
      namespaceDeleteService);
  }
}