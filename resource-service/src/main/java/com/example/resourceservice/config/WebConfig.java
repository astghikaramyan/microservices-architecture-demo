package com.example.resourceservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.resourceservice.interceptor.TraceIdInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final TraceIdInterceptor traceIdInterceptor;

  public WebConfig(TraceIdInterceptor traceIdInterceptor) {
    this.traceIdInterceptor = traceIdInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(traceIdInterceptor);
  }
}
