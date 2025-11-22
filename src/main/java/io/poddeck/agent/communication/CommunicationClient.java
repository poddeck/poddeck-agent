package io.poddeck.agent.communication;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.poddeck.agent.communication.service.ServiceRepository;
import io.poddeck.common.HandshakeRequest;
import io.poddeck.common.TunnelMessage;
import io.poddeck.common.TunnelServiceGrpc;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class CommunicationClient {
  private final Log log;
  private final CommunicationConfiguration configuration;
  private final ServiceRepository serviceRepository;
  private ManagedChannel channel;
  private StreamObserver<TunnelMessage> stream;

  private static final int RETRY_ATTEMPTS = 10;

  public void connect() {
    try {
      channel = ManagedChannelBuilder
        .forAddress(configuration.hostname(), configuration.port())
        .usePlaintext()
        .enableRetry()
        .maxRetryAttempts(RETRY_ATTEMPTS)
        .build();
      var stub = TunnelServiceGrpc.newStub(channel);
      stream = stub.connect(TunnelService.create(log, this, serviceRepository));
      var handshake = HandshakeRequest.newBuilder()
        .setCluster(UUID.randomUUID().toString())
        .setKey("secret")
        .build();
      send(handshake);
    } catch (Exception exception) {
      log.processError(exception);
    }
  }

  public void awaitTermination() {
    try {
      channel.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    } catch (Exception exception) {
      log.processError(exception);
    }
  }

  public void send(Message message) {
    send("", message);
  }

  public void send(String requestId, Message message) {
    var tunnelMessage = TunnelMessage.newBuilder()
      .setPayload(Any.pack(message));
    if (!requestId.isEmpty()) {
      tunnelMessage.setRequestId(requestId);
    }
    stream.onNext(tunnelMessage.build());
  }

  public void shutdown() throws Exception {
    if (channel == null) {
      throw new Exception("Client never connected");
    }
    channel.shutdown();
  }
}
