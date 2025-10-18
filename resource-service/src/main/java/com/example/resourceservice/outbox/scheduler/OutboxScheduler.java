package com.example.resourceservice.outbox.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.resourceservice.outbox.service.OutboxProcessorService;

@Component
public class OutboxScheduler {

  private final OutboxProcessorService processorService;

  public OutboxScheduler(OutboxProcessorService processorService) {
    this.processorService = processorService;
  }

  // Runs every 30 seconds
  @Scheduled(fixedRate = 30000)
  public void runOutboxProcessor() {
    processorService.processPendingEvents();
  }
}
