package io.poddeck.agent.event;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.poddeck.agent.application.ApplicationLaunchEvent;
import io.poddeck.common.event.Hook;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class EventHook implements Hook {
  private final EventWatcher eventWatcher;

  @io.poddeck.common.event.EventHook
  private void applicationLaunch(ApplicationLaunchEvent event) {
    new Thread(eventWatcher::watch).start();
  }
}