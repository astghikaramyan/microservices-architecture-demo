package com.example.resourceservice.model;

public class SongMetadata {
    private String name;
    private String artist;
    private String album;
    private String duration;
    private String year;
    private Integer resourceId;

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

    public void setDuration(final String duration) {
        this.duration = duration;
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
