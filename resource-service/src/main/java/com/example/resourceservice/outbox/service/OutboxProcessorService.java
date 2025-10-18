package com.example.resourceservice.outbox.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import com.example.resourceservice.entity.OutboxEvent;
import com.example.resourceservice.messaging.producer.CreateResourceMetadataPublisher;
import com.example.resourceservice.repository.OutboxEventRepository;

import jakarta.transaction.Transactional;

@Service
public class OutboxProcessorService {

  private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(OutboxProcessorService.class);
  public static final String CREATE_RESOURCE_METADATA_OUT = "createResourceMetadata-out-0";
  private final OutboxEventRepository repository;
  private final CreateResourceMetadataPublisher createResourceMetadataPublisher;

  public OutboxProcessorService(OutboxEventRepository repository,
      CreateResourceMetadataPublisher createResourceMetadataPublisher) {
    this.repository = repository;
    this.createResourceMetadataPublisher = createResourceMetadataPublisher;
  }

  @Transactional
  public void processPendingEvents() {
    List<OutboxEvent> pending = repository.findByProcessedFalse();
    if (pending.isEmpty()) {
      LOGGER.info("No pending outbox events to process.");
      return;
    }
    LOGGER.info("Processing {} outbox events...", pending.size());
    for (OutboxEvent event : pending) {
      try {
        createResourceMetadataPublisher.sendCreateResourceMetadataEvent(CREATE_RESOURCE_METADATA_OUT,
            prepareMessage(event.getResourceId()));
        LOGGER.info("Successfully sent resourceId={} to external service.", event.getResourceId());
        event.setProcessed(true);
      } catch (Exception e) {
        LOGGER.error("Failed to process outbox event id={}, resourceId={}", event.getId(), event.getResourceId(), e);
      }
    }

    // Bulk mark processed ones
    List<Integer> processedIds = pending.stream()
        .filter(OutboxEvent::isProcessed)
        .map(OutboxEvent::getId)
        .collect(Collectors.toList());

    if (!processedIds.isEmpty()) {
      repository.markAsProcessed(processedIds);
      LOGGER.info("Marked {} events as processed.", processedIds.size());
    }
  }

  private Message<Integer> prepareMessage(Integer resourceId) {
    // Get traceId from MDC (ThreadContext)
    String traceId = ThreadContext.get("traceId");
    if (traceId == null) {
      traceId = UUID.randomUUID().toString();
      ThreadContext.put("traceId", traceId);
    }
    LOGGER.info("Preparing message for resource ID={}", resourceId);

    // Build a new message with the traceId header
    return MessageBuilder
        .withPayload(resourceId)
        .setHeader("X-Trace-Id", traceId)
        .build();
  }
}
