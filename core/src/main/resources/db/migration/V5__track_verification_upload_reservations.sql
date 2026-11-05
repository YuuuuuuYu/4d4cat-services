-- Hibernate creates this legacy table after Flyway when starting with an empty schema.
ALTER TABLE IF EXISTS verification_image
ADD COLUMN IF NOT EXISTS reserved_until TIMESTAMP WITHOUT TIME ZONE;
