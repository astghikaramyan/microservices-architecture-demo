package com.example.resourceservice.controller;

import com.example.resourceservice.entity.ResourceEntity;
import com.example.resourceservice.exception.InvalidDataException;
import com.example.resourceservice.service.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.example.resourceservice.service.ResourceService.BAD_REQUEST_RESPONSE_CODE;

@RestController
@RequestMapping("/resources")
public class ResourceRestController {
    @Autowired
    private ResourceService resourceService;

    @PostMapping(consumes = "audio/mpeg")
    public ResponseEntity<Map<String, Integer>> uploadResource(@RequestBody byte[] audioData) {
        try {
            final ResourceEntity resourceEntity = resourceService.createResource(audioData);
            if (Objects.nonNull(resourceEntity)) {
                final Map<String, Integer> result = new HashMap<>();
                result.put("id", resourceEntity.getId());
                return ResponseEntity.ok(result);
            }
            throw new InvalidDataException("Validation failed");
        } catch (InvalidDataException e) {
            throw new InvalidDataException(this.resourceService.prepareErrorResponse(e.getMessage(), BAD_REQUEST_RESPONSE_CODE));
        }
    }

    @GetMapping(value = "/{id}", produces = "audio/mpeg")
    public ResponseEntity<byte[]> getResource(@PathVariable Integer id) {
        final ResourceEntity resource = resourceService.getResource(id);
        return ResponseEntity.ok(resource.getData());
    }

    @DeleteMapping
    public ResponseEntity<Map<String, List<Integer>>> deleteResource(@RequestParam String id) {
        return ResponseEntity.ok(this.resourceService.deleteResourceByIds(id));
    }

}
