package com.example.resourceservice.messaging.producer;

import static com.example.resourceservice.constants.Constants.INTERNAL_SERVER_ERROR_RESPONSE_CODE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.example.resourceservice.exception.StreamBridgeException;
import com.example.resourceservice.util.DataPreparerService;

@Service
public class CreateResourceMetadataPublisher {
  private static final Logger LOGGER = LoggerFactory.getLogger(CreateResourceMetadataPublisher.class);
  private final StreamBridge streamBridge;
  private final DataPreparerService dataPreparerService;
  public CreateResourceMetadataPublisher(StreamBridge streamBridge, DataPreparerService dataPreparerService) {
    this.dataPreparerService = dataPreparerService;
    this.streamBridge = streamBridge;
  }
  @Retryable(
      retryFor = {Exception.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000, multiplier = 2)
  )
  public void sendCreateResourceMetadataEvent(String outBindingName, Message<Integer> message) {
    try {
      streamBridge.send(outBindingName, message);
    } catch (Exception e){
      throw  new StreamBridgeException(dataPreparerService.prepareErrorResponse("Failed to send message through StreamBridge", INTERNAL_SERVER_ERROR_RESPONSE_CODE));
    }
  }

  @Recover
  public void recoverSendCreateResourceMetadataEvent(Exception e, String outBindingName, Message<Integer> message) {
    LOGGER.error("Failed to send message through StreamBridge after retries. Error: {}", e.getMessage(), e);
    throw new StreamBridgeException(dataPreparerService.prepareErrorResponse("Failed to send message through StreamBridge", INTERNAL_SERVER_ERROR_RESPONSE_CODE));
  }

}
