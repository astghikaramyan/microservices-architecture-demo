CREATE TABLE IF NOT EXISTS song(
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    artist VARCHAR(255) NOT NULL,
    album VARCHAR(255),
    duration VARCHAR(255),
    year VARCHAR(255),
    resource_id INTEGER
    );