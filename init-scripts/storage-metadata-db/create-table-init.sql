-- Create sequence explicitly
CREATE SEQUENCE IF NOT EXISTS storage_seq START WITH 1 INCREMENT BY 1;

-- Create table using the sequence
CREATE TABLE IF NOT EXISTS storage (
  id BIGINT PRIMARY KEY DEFAULT nextval('storage_seq'),
  storage_type VARCHAR(255) NOT NULL,
  bucket VARCHAR(255) NOT NULL,
  path VARCHAR(255) NOT NULL
);

INSERT INTO storage (storage_type, bucket, path)
VALUES ('PERMANENT', 'permanent-resource-files', '/permanent-resource-files');

INSERT INTO storage (storage_type, bucket, path)
VALUES ('STAGING', 'staging-resource-files', '/staging-resource-files')
