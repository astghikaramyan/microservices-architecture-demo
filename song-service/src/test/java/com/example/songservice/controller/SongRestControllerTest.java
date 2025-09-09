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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class SongRestControllerTest {

    @Mock
    private SongService songService;

    @Mock
    private SongMapper songMapper;

    @InjectMocks
    private SongRestController controller;

    @Test
    void addSongMetadata_success() {
        SongDTO dto = new SongDTO();
        SongEntity entity = new SongEntity();
        entity.setId(1);

        when(songService.checkValidity(dto)).thenReturn(new ValidationErrorResponse());
        when(songService.checkMissingFields(dto)).thenReturn(new ErrorResponse());
        when(songMapper.mapToEntity(dto)).thenReturn(entity);
        when(songService.addSong(entity)).thenReturn(entity);

        ResponseEntity<Map<String, Integer>> response = controller.addSongMetadata(dto);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().get("id"));
    }

    @Test
    void addSongMetadata_conflict() {
        SongDTO dto = new SongDTO();
        dto.setId(1);

        when(songService.checkValidity(dto)).thenReturn(new ValidationErrorResponse());
        when(songService.checkMissingFields(dto)).thenReturn(new ErrorResponse());
        when(songService.existById(1)).thenReturn(true);

        ConflictDataException ex = assertThrows(ConflictDataException.class, () -> controller.addSongMetadata(dto));
        assertNotNull(ex);
    }

    @Test
    void addSongMetadata_invalidData() {
        SongDTO dto = new SongDTO();
        ValidationErrorResponse validationError = new ValidationErrorResponse();
        validationError.setErrorMessage("Invalid");
        when(songService.checkValidity(dto)).thenReturn(validationError);

        InvalidDataException ex = assertThrows(InvalidDataException.class, () -> controller.addSongMetadata(dto));
        assertEquals("Invalid", ex.getErrorResponse().getErrorMessage());
    }

    @Test
    void getSongMetadata_success() {
        SongEntity entity = new SongEntity();
        entity.setId(1);
        Song song = new Song();
        song.setId(1);

        when(songService.getSong(1)).thenReturn(Optional.of(entity));
        when(songMapper.mapToSong(entity)).thenReturn(song);

        ResponseEntity<Song> response = controller.getSongMetadata(1);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().getId());
    }

    @Test
    void getSongMetadata_notFound() {
        when(songService.getSong(99)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> controller.getSongMetadata(99));
        assertNotNull(ex);
    }

    @Test
    void getSongMetadataByResourceId_success() {
        SongEntity entity = new SongEntity();
        entity.setResourceId(123);
        SongDTO dto = new SongDTO();
        dto.setResourceId(123);

        when(songService.getSongByResourceId(123)).thenReturn(Optional.of(entity));
        when(songMapper.mapToDTOWithResourceId(entity)).thenReturn(dto);

        ResponseEntity<SongDTO> response = controller.getSongMetadataByResourceId(123);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(123, response.getBody().getResourceId());
    }

    @Test
    void getSongMetadataByResourceId_notFound() {
        when(songService.getSongByResourceId(999)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> controller.getSongMetadataByResourceId(999));
        assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    void deleteSongsMetadata_success() {
        Map<String, List<Integer>> result = new HashMap<>();
        result.put("ids", Arrays.asList(1, 2));
        when(songService.deleteSongByIds("1,2")).thenReturn(result);

        ResponseEntity<Map<String, List<Integer>>> response = controller.deleteSongsMetadata("1,2");

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(Arrays.asList(1, 2), response.getBody().get("ids"));
    }
}