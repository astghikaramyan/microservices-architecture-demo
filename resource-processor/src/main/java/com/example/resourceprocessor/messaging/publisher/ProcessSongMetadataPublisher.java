package com.example.resourceprocessor.messaging.publisher;

import static com.example.resourceprocessor.constants.Constants.INTERNAL_SERVER_ERROR_RESPONSE_CODE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.example.resourceprocessor.exception.StreamBridgeException;
import com.example.resourceprocessor.util.DataPreparerService;

@Service
public class ProcessSongMetadataPublisher {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProcessSongMetadataPublisher.class);
  private final StreamBridge streamBridge;
  private final DataPreparerService dataPreparerService;
  public ProcessSongMetadataPublisher(StreamBridge streamBridge, DataPreparerService dataPreparerService) {
    this.dataPreparerService = dataPreparerService;
    this.streamBridge = streamBridge;
  }
  @Retryable(
      retryFor = {Exception.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000, multiplier = 2)
  )
  public void sendProcessedSongMetadataEvent(String outBindingName, Message<String> message) {
    try {
      streamBridge.send(outBindingName, message);
    } catch (Exception e){
      throw  new StreamBridgeException(dataPreparerService.prepareErrorResponse("Failed to send message through StreamBridge", INTERNAL_SERVER_ERROR_RESPONSE_CODE));
    }
  }

  @Recover
  public void recoverSendProcessedSongMetadataEvent(Exception e, String outBindingName, Message<String> message) {
    LOGGER.error("Failed to send message through StreamBridge after retries. Error: {}", e.getMessage(), e);
    throw new StreamBridgeException(dataPreparerService.prepareErrorResponse("Failed to send message through StreamBridge", INTERNAL_SERVER_ERROR_RESPONSE_CODE));
  }
}