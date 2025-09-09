package com.example.songservice.mapper;

import com.example.songservice.dto.Song;
import com.example.songservice.dto.SongDTO;
import com.example.songservice.entity.SongEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SongMapperTest {

    private final SongMapper mapper = new SongMapper();

    @Test
    void mapToEntity() {
        SongDTO dto = new SongDTO();
        dto.setName("Test Song");
        dto.setArtist("Test Artist");
        dto.setAlbum("Test Album");
        dto.setDuration("03:30");
        dto.setYear("2020");
        dto.setResourceId(42);

        SongEntity entity = mapper.mapToEntity(dto);

        assertEquals(dto.getName(), entity.getName());
        assertEquals(dto.getArtist(), entity.getArtist());
        assertEquals(dto.getAlbum(), entity.getAlbum());
        assertEquals(dto.getDuration(), entity.getDuration());
        assertEquals(dto.getYear(), entity.getYear());
        assertEquals(dto.getResourceId(), entity.getResourceId());
    }

    @Test
    void mapToDTO() {
        SongEntity entity = new SongEntity();
        entity.setId(1);
        entity.setName("Test Song");
        entity.setArtist("Test Artist");
        entity.setAlbum("Test Album");
        entity.setDuration("03:30");
        entity.setYear("2020");

        SongDTO dto = mapper.mapToDTO(entity);

        assertEquals(entity.getId(), dto.getId());
        assertEquals(entity.getName(), dto.getName());
        assertEquals(entity.getArtist(), dto.getArtist());
        assertEquals(entity.getAlbum(), dto.getAlbum());
        assertEquals(entity.getDuration(), dto.getDuration());
        assertEquals(entity.getYear(), dto.getYear());
    }

    @Test
    void mapToSong() {
        SongEntity entity = new SongEntity();
        entity.setId(2);
        entity.setName("Another Song");
        entity.setArtist("Another Artist");
        entity.setAlbum("Another Album");
        entity.setDuration("04:00");
        entity.setYear("2021");

        Song song = mapper.mapToSong(entity);

        assertEquals(entity.getId(), song.getId());
        assertEquals(entity.getName(), song.getName());
        assertEquals(entity.getArtist(), song.getArtist());
        assertEquals(entity.getAlbum(), song.getAlbum());
        assertEquals(entity.getDuration(), song.getDuration());
        assertEquals(entity.getYear(), song.getYear());
    }

    @Test
    void mapToDTOWithResourceId() {
        SongEntity entity = new SongEntity();
        entity.setId(3);
        entity.setName("Resource Song");
        entity.setArtist("Resource Artist");
        entity.setAlbum("Resource Album");
        entity.setDuration("05:00");
        entity.setYear("2022");
        entity.setResourceId(99);

        SongDTO dto = mapper.mapToDTOWithResourceId(entity);

        assertEquals(entity.getId(), dto.getId());
        assertEquals(entity.getName(), dto.getName());
        assertEquals(entity.getArtist(), dto.getArtist());
        assertEquals(entity.getAlbum(), dto.getAlbum());
        assertEquals(entity.getDuration(), dto.getDuration());
        assertEquals(entity.getYear(), dto.getYear());
        assertEquals(entity.getResourceId(), dto.getResourceId());
    }
}