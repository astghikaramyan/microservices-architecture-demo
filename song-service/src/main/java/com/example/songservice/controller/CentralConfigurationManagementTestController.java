package com.example.songservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RefreshScope
@RestController
@RequestMapping("/config")
public class CentralConfigurationManagementTestController {

  @Value("${configuration.test.message:Hello default}")
  private String message;

  @GetMapping
  public String testConfigurationsChanges() {
    return message;
  }
}
