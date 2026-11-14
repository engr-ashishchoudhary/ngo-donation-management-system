-- =============================================================================
-- FILE: 01_create_database.sql
-- PROJECT: NGO Donation Management System
-- PURPOSE: Create the application database with correct character set
-- EXECUTE: Run this file first, before all other SQL scripts
-- =============================================================================

-- Drop the database if it already exists (use carefully in production)
DROP DATABASE IF EXISTS ngo_donation_db;

-- Create the database
-- utf8mb4 supports full Unicode including emoji; utf8mb4_unicode_ci is a
-- case-insensitive collation suitable for names and email addresses.
CREATE DATABASE ngo_donation_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Switch to the new database for all subsequent statements
USE ngo_donation_db;

-- Confirm creation
SELECT 'Database ngo_donation_db created successfully.' AS status;
