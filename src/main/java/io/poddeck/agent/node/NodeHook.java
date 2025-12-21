package io.poddeck.agent.node;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.poddeck.agent.application.ApplicationLaunchEvent;
import io.poddeck.agent.communication.service.ServiceRepository;
import io.poddeck.common.NodeFindRequest;
import io.poddeck.common.NodeListRequest;
import io.poddeck.common.event.EventHook;
import io.poddeck.common.event.Hook;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class NodeHook implements Hook {
  private final ServiceRepository serviceRepository;
  private final NodeListService nodeListService;
  private final NodeFindService nodeFindService;

  @EventHook
  private void applicationLaunch(ApplicationLaunchEvent event) {
    serviceRepository.register(NodeListRequest.class, nodeListService);
    serviceRepository.register(NodeFindRequest.class, nodeFindService);
  }
}