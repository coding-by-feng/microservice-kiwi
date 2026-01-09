-- Kiwi Database Initialization Script
-- This script runs automatically when MySQL container starts for the first time

-- Ensure proper character set
ALTER DATABASE kiwi_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant permissions (if needed for additional users)
-- CREATE USER IF NOT EXISTS 'kiwi'@'%' IDENTIFIED BY 'kiwi123';
-- GRANT ALL PRIVILEGES ON kiwi_db.* TO 'kiwi'@'%';
-- FLUSH PRIVILEGES;

-- Note: Actual table creation is handled by Liquibase migrations
-- This file is for any additional database initialization that needs
-- to happen before the application starts
