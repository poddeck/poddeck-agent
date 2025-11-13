package io.poddeck.agent.communication;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class CommunicationClient {
  private final Log log;
  private final CommunicationConfiguration configuration;
  @Getter
  private ManagedChannel channel;

  public void connect() {
    try {
      channel = ManagedChannelBuilder
        .forAddress(configuration.hostname(), configuration.port())
        .usePlaintext()
        .build();
    } catch (Exception exception) {
      log.processError(exception);
    }
  }

  public void shutdown() throws Exception {
    if (channel == null) {
      throw new Exception("Client never connected");
    }
    channel.shutdown();
  }
}
