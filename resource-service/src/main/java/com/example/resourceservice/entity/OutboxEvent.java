package com.example.resourceservice.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "outbox_event_seq_gen")
  @SequenceGenerator(name = "outbox_event_seq_gen", sequenceName = "outbox_event_seq", allocationSize = 1)
  private Integer id;

  @Column(name = "resource_id", nullable = false)
  private Integer resourceId;

  @Column(name = "processed", nullable = false)
  private boolean processed = false;

  // --- Constructors ---
  public OutboxEvent() {
  }

  public OutboxEvent(Integer resourceId, boolean processed) {
    this.resourceId = resourceId;
    this.processed = processed;
  }

  // --- Getters and Setters ---
  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getResourceId() {
    return resourceId;
  }

  public void setResourceId(Integer resourceId) {
    this.resourceId = resourceId;
  }

  public boolean isProcessed() {
    return processed;
  }

  public void setProcessed(boolean processed) {
    this.processed = processed;
  }

  // --- Optional toString ---
  @Override
  public String toString() {
    return "OutboxEvent{" +
        "id=" + id +
        ", resourceId=" + resourceId +
        ", processed=" + processed +
        '}';
  }
}