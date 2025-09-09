-- Create sequence explicitly
CREATE SEQUENCE IF NOT EXISTS resource_seq START WITH 1 INCREMENT BY 1;

-- Create table using the sequence
CREATE TABLE IF NOT EXISTS resource (
  id BIGINT PRIMARY KEY DEFAULT nextval('resource_seq'),
  file_name VARCHAR(255) NOT NULL,
  s3_key VARCHAR(255) NOT NULL,
  uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
