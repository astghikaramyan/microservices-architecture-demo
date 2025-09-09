package com.example.songservice.entity;

import jakarta.persistence.*;
import org.springframework.lang.NonNull;

@Entity
@Table(name = "song")
public class SongEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "song_seq")
    @SequenceGenerator(name = "song_seq", sequenceName = "song_seq", allocationSize = 1)
    private Integer id;

    @NonNull
    @Column(name = "name")
    private String name;
    @NonNull
    @Column(name = "artist")
    private String artist;
    @Column(name = "album")
    private String album;

    @Column(name = "duration")
    private String duration;

    @Column(name = "year")
    private String year;

    @Column(name = "resource_id")
    private Integer resourceId;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(final String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(final String album) {
        this.album = album;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(final String length) {
        this.duration = length;
    }

    public String getYear() {
        return year;
    }

    public void setYear(final String year) {
        this.year = year;
    }

    public Integer getResourceId() {
        return resourceId;
    }

    public void setResourceId(final Integer resourceId) {
        this.resourceId = resourceId;
    }
}
