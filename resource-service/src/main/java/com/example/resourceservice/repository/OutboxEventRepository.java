package com.example.resourceservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.resourceservice.entity.OutboxEvent;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Integer> {
  // Find all unprocessed events
  List<OutboxEvent> findByProcessedFalse();

  // Mark specific events as processed
  @Modifying
  @Query("UPDATE OutboxEvent e SET e.processed = true WHERE e.id IN :ids")
  void markAsProcessed(List<Integer> ids);

}
