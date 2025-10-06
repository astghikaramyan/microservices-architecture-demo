package com.example.storageservice.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.storageservice.dto.StorageRequest;
import com.example.storageservice.dto.StorageResponse;
import com.example.storageservice.entity.StorageEntity;
import com.example.storageservice.mapper.StorageMapper;
import com.example.storageservice.service.StorageMetadataService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/storages")
public class StorageController {

  private final StorageMetadataService service;
  private final StorageMapper storageMapper;
  @Autowired
  public StorageController(StorageMetadataService service, StorageMapper storageMapper) {
    this.storageMapper = storageMapper;
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<Map<String, Integer>> createStorage(@Valid @RequestBody StorageRequest request) {
    StorageEntity storageEntity = service.addStorageMetadata(storageMapper.toEntity(request));
    final Map<String, Integer> result = new HashMap<>();
    result.put("id", storageEntity.getId());
    return ResponseEntity.ok(result);
  }

  @GetMapping
  public ResponseEntity<List<StorageResponse>> getStoragesMetadata() {
    return ResponseEntity.ok(storageMapper.toResponseList(service.findStoragesMetadata()));
  }

  @DeleteMapping
  public ResponseEntity<Map<String, List<Integer>>> deleteStoragesByIds(@RequestParam String ids) {
    Map<String, List<Integer>> deletedStoragesIds = service.deleteStorageMetadataByIds(ids);
    return ResponseEntity.ok(deletedStoragesIds);
  }
}
