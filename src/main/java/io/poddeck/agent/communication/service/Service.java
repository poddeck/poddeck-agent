package io.poddeck.agent.communication.service;

import com.google.protobuf.Message;
import io.poddeck.agent.communication.CommunicationClient;

public interface Service<T extends Message> {
  void process(CommunicationClient client, String requestId, T message)
    throws Exception;
}
