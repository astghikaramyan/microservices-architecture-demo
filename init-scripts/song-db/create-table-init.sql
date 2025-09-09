-- Explicit sequence
CREATE SEQUENCE IF NOT EXISTS song_seq START WITH 1 INCREMENT BY 1;

-- Table definition using the explicit sequence
CREATE TABLE IF NOT EXISTS song (
    id BIGINT PRIMARY KEY DEFAULT nextval('song_seq'),
    name VARCHAR(255) NOT NULL,
    artist VARCHAR(255) NOT NULL,
    album VARCHAR(255),
    duration VARCHAR(255),
    year VARCHAR(255),
    resource_id INTEGER
);