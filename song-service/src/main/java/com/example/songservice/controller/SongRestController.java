package com.example.songservice.controller;

import com.example.songservice.dto.Song;
import com.example.songservice.dto.SongDTO;
import com.example.songservice.entity.SongEntity;
import com.example.songservice.exception.ConflictDataException;
import com.example.songservice.exception.InvalidDataException;
import com.example.songservice.exception.NotFoundException;
import com.example.songservice.mapper.SongMapper;
import com.example.songservice.model.ErrorResponse;
import com.example.songservice.model.ValidationErrorResponse;
import com.example.songservice.service.SongService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;

import static com.example.songservice.service.SongService.BAD_REQUEST_RESPONSE_CODE;
import static com.example.songservice.service.SongService.NOT_FOUND_REQUEST_RESPONSE_CODE;

@RestController
@RequestMapping("/songs")
public class SongRestController {

    @Autowired
    private SongService songService;
    @Autowired
    private SongMapper songMapper;

    @PostMapping
    public ResponseEntity<Map<String, Integer>> addSongMetadata(@RequestBody @Valid SongDTO songDTO) {
        try {
            ValidationErrorResponse validationErrorResponse = songService.checkValidity(songDTO);
            if (Objects.nonNull(validationErrorResponse.getErrorMessage()) && !validationErrorResponse.getErrorMessage().isEmpty()) {
                throw new InvalidDataException(validationErrorResponse);
            }
            ErrorResponse errorResponse = songService.checkMissingFields(songDTO);
            if (Objects.nonNull(errorResponse.getErrorMessage()) && !errorResponse.getErrorMessage().isEmpty()) {
                throw new InvalidDataException(errorResponse);
            }
            if (Objects.nonNull(songDTO.getId()) && songService.existById(songDTO.getId())) {
                throw new ConflictDataException(String.format("Metadata for resource ID=%s already exists", songDTO.getId()));
            }
            final SongEntity songEntity = songService.addSong(this.songMapper.mapToEntity(songDTO));
            return ResponseEntity.ok(prepareResponseObject(songEntity));
        } catch (ConflictDataException e) {
            throw new ConflictDataException(this.songService.prepareErrorResponse(e.getMessage(), "409"));
        } catch (final InvalidDataException e) {
            if (Objects.nonNull(e.getErrorResponse())) {
                ValidationErrorResponse e1 = e.getErrorResponse();
                throw new InvalidDataException(e1);
            } else {
                ErrorResponse e2 = e.getSimpleErrorResponse();
                throw new InvalidDataException(e2);
            }
        } catch (final Exception e) {
            throw new InvalidDataException(this.songService.prepareErrorResponse(e.getMessage(), BAD_REQUEST_RESPONSE_CODE));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Song> getSongMetadata(@PathVariable Integer id) {
        final Optional<SongEntity> songEntity = songService.getSong(id);
        if (songEntity.isPresent()) {
            return ResponseEntity.ok(songMapper.mapToSong(songEntity.get()));
        }
        throw new NotFoundException(this.songService.prepareErrorResponse(String.format("Song metadata for ID=%s not found", id), NOT_FOUND_REQUEST_RESPONSE_CODE));
    }

    @GetMapping("/resource-identifiers/{resourceId}")
    public ResponseEntity<SongDTO> getSongMetadataByResourceId(@PathVariable Integer resourceId) {
        final Optional<SongEntity> songEntity = songService.getSongByResourceId(resourceId);

        if (songEntity.isPresent()) {
            return ResponseEntity.ok(songMapper.mapToDTOWithResourceId(songEntity.get()));
        }
        throw new NotFoundException(String.format("Song metadata for RESOURCE_ID=%d not found", resourceId));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, List<Integer>>> deleteSongsMetadata(@RequestParam String id) {
        return ResponseEntity.ok(this.songService.deleteSongByIds(id));
    }

    private Map<String, Integer> prepareResponseObject(final SongEntity songEntity) {
        final Map<String, Integer> responseObject = new HashMap<>();
        responseObject.put("id", songEntity.getId());
        return responseObject;
    }
}
