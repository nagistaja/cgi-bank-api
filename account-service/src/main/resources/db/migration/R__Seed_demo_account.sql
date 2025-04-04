-- Repeatable migration: Ensures a specific demo account exists.
-- This runs after versioned migrations if its checksum changes.
-- WARNING: Do not rely on this account existing with specific balances in automated tests.
INSERT INTO accounts (account_id, version)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 0)
ON CONFLICT (account_id) DO NOTHING; -- Avoid errors if run multiple times or account exists

-- COMMENT ON TABLE accounts IS 'Ensures demo account a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11 exists.'; -- Removed misplaced comment