package io.poddeck.agent.communication;

import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.poddeck.agent.communication.service.ServiceRepository;
import io.poddeck.common.TunnelMessage;
import io.poddeck.common.log.Log;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName = "create")
public final class TunnelService implements StreamObserver<TunnelMessage> {
  private final Log log;
  private final CommunicationClient client;
  private final ServiceRepository serviceRepository;

  @Override
  public void onNext(TunnelMessage message) {
    serviceRepository.dispatch(client, message.getRequestId(), message);
  }

  @Override
  public void onError(Throwable throwable) {
    if (throwable instanceof StatusRuntimeException) {
      processDisconnect();
      return;
    }
    log.processError(throwable);
  }

  @Override
  public void onCompleted() {
    processDisconnect();
  }

  private void processDisconnect() {
    log.warning("Connection lost");
    log.info("Shutting down...");
    System.exit(0);
  }
}
