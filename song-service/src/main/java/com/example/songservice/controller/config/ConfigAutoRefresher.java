package com.example.songservice.controller.config;

import java.util.Set;

import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ConfigAutoRefresher {

  private final ContextRefresher contextRefresher;

  public ConfigAutoRefresher(ContextRefresher contextRefresher) {
    this.contextRefresher = contextRefresher;
  }

  @Scheduled(fixedRate = 60000)
  public void refreshConfig() {
    Set<String> refreshedKeys = contextRefresher.refresh();
    if (!refreshedKeys.isEmpty()) {
      System.out.println("Refreshed keys: " + refreshedKeys);
    }
  }
}

