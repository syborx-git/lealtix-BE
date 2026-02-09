-- Flyway Schema History Repair Script
-- Execute this script directly in your PostgreSQL database to fix checksum mismatches

-- Step 1: Check current state of Flyway schema history
SELECT version, description, type, installed_by, installed_on, execution_time, success, checksum
FROM flyway_schema_history
WHERE version = 4
ORDER BY version;

-- Step 2: Mark migration v4 as failed (so Flyway can reprocess it)
-- WARNING: Only run this if you understand the consequences
-- This tells Flyway that version 4 needs to be re-validated
UPDATE flyway_schema_history 
SET success = false 
WHERE version = 4;

-- Step 3: Delete the failed record so Flyway can re-apply it
-- WARNING: Only run this if the migration is idempotent (safe to run again)
DELETE FROM flyway_schema_history 
WHERE version = 4;

-- Step 4: Verify the records were removed
SELECT version, description, type, installed_by, installed_on, execution_time, success, checksum
FROM flyway_schema_history
ORDER BY version DESC;

-- After executing this script:
-- 1. Restart your Spring Boot application
-- 2. Flyway will detect missing version 4 and re-apply it with the new checksum
-- 3. The application should start successfully
