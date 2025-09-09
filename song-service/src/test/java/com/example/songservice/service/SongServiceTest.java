package com.example.songservice.service;

import com.example.songservice.dto.SongDTO;
import com.example.songservice.entity.SongEntity;
import com.example.songservice.exception.InvalidDataException;
import com.example.songservice.exception.NotFoundException;
import com.example.songservice.model.ErrorResponse;
import com.example.songservice.model.ValidationErrorResponse;
import com.example.songservice.repository.SongRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SongServiceTest {
    @Mock
    private SongRepository songRepository;

    @InjectMocks
    private SongService songService;

    @Test
    void addSong() {
        SongEntity song = new SongEntity();
        when(songRepository.save(any(SongEntity.class))).thenReturn(song);
        SongEntity result = songService.addSong(song);
        assertNotNull(result);
        verify(songRepository).save(song);
    }

    @Test
    void getSong_validId() {
        SongEntity song = new SongEntity();
        when(songRepository.existsById(1)).thenReturn(true);
        when(songRepository.findById(1)).thenReturn(Optional.of(song));
        Optional<SongEntity> result = songService.getSong(1);
        assertTrue(result.isPresent());
    }

    @Test
    void getSong_invalidIdFormat() {
        assertThrows(InvalidDataException.class, () -> songService.getSong(-1));
    }

    @Test
    void getSong_notFound() {
        when(songRepository.existsById(99)).thenReturn(false);
        assertThrows(NotFoundException.class, () -> songService.getSong(99));
    }

    @Test
    void getSongByResourceId_found() {
        SongEntity song = new SongEntity();
        song.setResourceId(123);
        when(songRepository.findAll()).thenReturn(Collections.singletonList(song));
        Optional<SongEntity> result = songService.getSongByResourceId(123);
        assertTrue(result.isPresent());
    }

    @Test
    void getSongByResourceId_notFound() {
        when(songRepository.findAll()).thenReturn(Collections.emptyList());
        Optional<SongEntity> result = songService.getSongByResourceId(123);
        assertFalse(result.isPresent());
    }

    @Test
    void deleteSong() {
        songService.deleteSong(1);
        verify(songRepository).deleteById(1);
    }

    @Test
    void deleteSongByIds_valid() {
        when(songRepository.existsById(anyInt())).thenReturn(true);
        doNothing().when(songRepository).deleteById(anyInt());
        Map<String, List<Integer>> result = songService.deleteSongByIds("1,2");
        assertEquals(Arrays.asList(1,2), result.get("ids"));
    }

    @Test
    void deleteSongByIds_invalidFormat() {
        assertThrows(InvalidDataException.class, () -> songService.deleteSongByIds("abc"));
    }

    @Test
    void existById() {
        when(songRepository.existsById(1)).thenReturn(true);
        assertTrue(songService.existById(1));
        when(songRepository.existsById(2)).thenReturn(false);
        assertFalse(songService.existById(2));
    }

    @Test
    void checkValidity_invalidYearAndDuration() {
        SongDTO dto = new SongDTO();
        dto.setYear("1800");
        dto.setDuration("1:2");
        ValidationErrorResponse response = songService.checkValidity(dto);
        assertEquals("400", response.getErrorCode());
    }

    @Test
    void checkMissingFields_missingName() {
        SongDTO dto = new SongDTO();
        dto.setName("");
        ErrorResponse response = songService.checkMissingFields(dto);
        assertEquals("400", response.getErrorCode());
    }

    @Test
    void isValidDuration_valid() {
        assertTrue(SongService.isValidDuration("12:34"));
    }

    @Test
    void isValidDuration_invalid() {
        assertFalse(SongService.isValidDuration("1234"));
    }

    @Test
    void isValidYear_valid() {
        assertTrue(SongService.isValidYear("2000"));
    }

    @Test
    void isValidYear_invalid() {
        assertFalse(SongService.isValidYear("1899"));
    }

    @Test
    void prepareErrorResponse() {
        ErrorResponse response = songService.prepareErrorResponse("msg", "400");
        assertEquals("msg", response.getErrorMessage());
        assertEquals("400", response.getErrorCode());
    }
}