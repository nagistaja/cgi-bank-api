-- Initialize PostgreSQL for test database
-- This script configures the database for parallel test execution

-- Create schema if not exists (safer than relying on default)
CREATE SCHEMA IF NOT EXISTS public;

-- Create a function to reset the database between tests
CREATE OR REPLACE FUNCTION reset_test_data() RETURNS void AS $$
BEGIN
    -- Delete all data from common test tables
    DELETE FROM transactions;
    DELETE FROM balances;
    DELETE FROM accounts;
    -- Add more tables as needed
END;
$$ LANGUAGE plpgsql;

-- Increase max connections for parallel tests
ALTER SYSTEM SET max_connections = '100';

-- Optimize for test environment
ALTER SYSTEM SET shared_buffers = '128MB';
ALTER SYSTEM SET effective_cache_size = '256MB';
ALTER SYSTEM SET work_mem = '16MB';
ALTER SYSTEM SET maintenance_work_mem = '64MB';

-- Reduce checkpoint frequency for better performance during tests
ALTER SYSTEM SET checkpoint_timeout = '30min';
ALTER SYSTEM SET checkpoint_completion_target = '0.9';

-- Apply changes
SELECT pg_reload_conf(); 