-- V1__initial_schema.sql
-- Initial database schema for authentication system
-- Includes: users table and verification_tokens table

-- ============================================================================
-- USERS TABLE
-- ============================================================================
-- Stores user authentication and profile information
-- Includes role-based access control (RBAC) and subscription tiers
CREATE TABLE users (
    -- Primary key and timestamps (from BaseEntity)
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Authentication fields
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,

    -- Profile information
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,

    -- Authorization (dual enum system)
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    subscription_tier VARCHAR(20) NOT NULL DEFAULT 'FREE',

    -- Account status fields
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    account_locked BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,

    -- Constraints
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT chk_users_subscription_tier CHECK (subscription_tier IN ('FREE', 'PRO', 'PREMIUM'))
);

-- Indexes for users table
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_email_verified ON users(email_verified);
CREATE INDEX idx_users_enabled ON users(enabled);
CREATE INDEX idx_users_created_at ON users(created_at);

-- Comments for users table
COMMENT ON TABLE users IS 'System users with authentication, authorization, and subscription information';
COMMENT ON COLUMN users.id IS 'Auto-generated unique identifier';
COMMENT ON COLUMN users.created_at IS 'Timestamp when user account was created';
COMMENT ON COLUMN users.updated_at IS 'Timestamp when user account was last modified';
COMMENT ON COLUMN users.email IS 'Unique email address used for login and communication';
COMMENT ON COLUMN users.password IS 'BCrypt hashed password (format: $2a$10$...)';
COMMENT ON COLUMN users.first_name IS 'User first name';
COMMENT ON COLUMN users.last_name IS 'User last name';
COMMENT ON COLUMN users.role IS 'System role (WHO the user is): USER or ADMIN';
COMMENT ON COLUMN users.subscription_tier IS 'Subscription level (WHAT features user has): FREE, PRO, or PREMIUM';
COMMENT ON COLUMN users.email_verified IS 'Whether user has verified their email address via verification token';
COMMENT ON COLUMN users.account_locked IS 'Whether account is locked due to security reasons (failed logins, suspicious activity)';
COMMENT ON COLUMN users.enabled IS 'Whether account is active (can be disabled by admin or user)';


-- ============================================================================
-- VERIFICATION_TOKENS TABLE
-- ============================================================================
-- Stores temporary tokens for email verification and password reset
-- Tokens expire after a set time period for security
CREATE TABLE verification_tokens (
    -- Primary key and timestamps (from BaseEntity)
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Token data
    token VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(30) NOT NULL,

    -- Expiration and verification
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,

    -- Foreign key to users
    user_id BIGINT NOT NULL,

    -- Constraints
    CONSTRAINT fk_verification_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_verification_tokens_type
        CHECK (type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET'))
);

-- Indexes for verification_tokens table
CREATE INDEX idx_verification_tokens_token ON verification_tokens(token);
CREATE INDEX idx_verification_tokens_user_id ON verification_tokens(user_id);
CREATE INDEX idx_verification_tokens_type ON verification_tokens(type);
CREATE INDEX idx_verification_tokens_expires_at ON verification_tokens(expires_at);
CREATE INDEX idx_verification_tokens_user_id_type ON verification_tokens(user_id, type);

-- Comments for verification_tokens table
COMMENT ON TABLE verification_tokens IS 'Temporary tokens for email verification and password reset workflows';
COMMENT ON COLUMN verification_tokens.id IS 'Auto-generated unique identifier';
COMMENT ON COLUMN verification_tokens.created_at IS 'Timestamp when token was created';
COMMENT ON COLUMN verification_tokens.updated_at IS 'Timestamp when token was last modified';
COMMENT ON COLUMN verification_tokens.token IS 'Unique UUID token sent to user (format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)';
COMMENT ON COLUMN verification_tokens.type IS 'Token purpose: EMAIL_VERIFICATION or PASSWORD_RESET';
COMMENT ON COLUMN verification_tokens.expires_at IS 'Timestamp when token becomes invalid (typically 24-48 hours)';
COMMENT ON COLUMN verification_tokens.verified_at IS 'Timestamp when token was used (NULL if not yet verified)';
COMMENT ON COLUMN verification_tokens.user_id IS 'Reference to the user this token belongs to';


-- ============================================================================
-- INITIAL DATA (Optional - for development/testing)
-- ============================================================================
-- Uncomment to create a test admin user
-- Password: 'admin123' (BCrypt hash)

INSERT INTO users (
    email,
    password,
    first_name,
    last_name,
    role,
    subscription_tier,
    email_verified,
    enabled
) VALUES (
    'admin@template.com',
    '$2a$10$rF4H7HvLvJ4xVHqYMPvLCOQmVZZhZZLQqmZZhZZhZZhZZhZZhZZhZ',  -- 'admin123'
    'Admin',
    'User',
    'ADMIN',
    'PREMIUM',
    true,
    true
);

-- Create MFA codes table
CREATE TABLE mfa_codes (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    code VARCHAR(6) NOT NULL,
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,

    CONSTRAINT fk_mfa_codes_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- Index for quick lookup
CREATE INDEX idx_mfa_codes_user_id ON mfa_codes(user_id);
CREATE INDEX idx_mfa_codes_code ON mfa_codes(code);
CREATE INDEX idx_mfa_codes_expires_at ON mfa_codes(expires_at);

COMMENT ON TABLE mfa_codes IS 'Temporary MFA codes for email-based two-factor authentication';