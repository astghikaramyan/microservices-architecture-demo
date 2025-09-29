package com.example.resourceservice.controller;

import static com.example.resourceservice.constants.Constants.BAD_REQUEST_RESPONSE_CODE;

import com.example.resourceservice.entity.ResourceEntity;
import com.example.resourceservice.exception.InvalidDataException;
import com.example.resourceservice.service.ResourceService;
import com.example.resourceservice.util.DataPreparerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/resources")
public class ResourceRestController {
    @Autowired
    private ResourceService resourceService;
    @Autowired
    private DataPreparerService dataPreparerService;

    @PostMapping(consumes = "audio/mpeg")
    public ResponseEntity<Map<String, Integer>> uploadResource(@RequestBody byte[] audioData) {
        try {
            final ResourceEntity resourceEntity = resourceService.uploadResource(audioData);
            if (Objects.nonNull(resourceEntity)) {
                final Map<String, Integer> result = new HashMap<>();
                result.put("id", resourceEntity.getId());
                return ResponseEntity.ok(result);
            }
            throw new InvalidDataException("Validation failed");
        } catch (InvalidDataException e) {
            throw new InvalidDataException(this.dataPreparerService.prepareErrorResponse(e.getMessage(), BAD_REQUEST_RESPONSE_CODE));
        }
    }

    @GetMapping(value = "/{id}", produces = "audio/mpeg")
    public ResponseEntity<byte[]> getResource(@PathVariable Integer id) {
        final byte[] resource = resourceService.getFileAsBytes(id);
        return ResponseEntity.ok(resource);
    }

    @DeleteMapping
    public ResponseEntity<Map<String, List<Integer>>> deleteResource(@RequestParam String id) {
        return ResponseEntity.ok(this.resourceService.deleteResourceByIds(id));
    }

}
