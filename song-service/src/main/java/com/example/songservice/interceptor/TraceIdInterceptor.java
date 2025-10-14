package com.example.songservice.interceptor;

import java.util.UUID;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class TraceIdInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    String traceId = request.getHeader("X-Trace-Id");
    if (traceId == null) {
      traceId = UUID.randomUUID().toString();
    }
    ThreadContext.put("traceId", traceId);
    return true;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    ThreadContext.clearAll();
  }
}

