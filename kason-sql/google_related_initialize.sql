-- Spring Security OAuth2 Required Tables
-- These tables are needed for OAuth2 token storage and client management

-- 1. OAuth Client Details Table
-- Stores OAuth2 client configurations
CREATE TABLE oauth_client_details (
                                      client_id VARCHAR(256) PRIMARY KEY,
                                      resource_ids VARCHAR(256),
                                      client_secret VARCHAR(256),
                                      scope VARCHAR(256),
                                      authorized_grant_types VARCHAR(256),
                                      web_server_redirect_uri VARCHAR(256),
                                      authorities VARCHAR(256),
                                      access_token_validity INTEGER,
                                      refresh_token_validity INTEGER,
                                      additional_information VARCHAR(4096),
                                      autoapprove VARCHAR(256)
);

-- 2. OAuth Access Token Table
-- Stores OAuth2 access tokens
CREATE TABLE oauth_access_token (
                                    token_id VARCHAR(256),
                                    token LONGBLOB,
                                    authentication_id VARCHAR(256) PRIMARY KEY,
                                    user_name VARCHAR(256),
                                    client_id VARCHAR(256),
                                    authentication LONGBLOB,
                                    refresh_token VARCHAR(256)
);

-- 3. OAuth Refresh Token Table
-- Stores OAuth2 refresh tokens
CREATE TABLE oauth_refresh_token (
                                     token_id VARCHAR(256),
                                     token LONGBLOB,
                                     authentication LONGBLOB
);

-- 4. OAuth Code Table
-- Stores authorization codes for OAuth2 authorization code flow
CREATE TABLE oauth_code (
                            code VARCHAR(256),
                            authentication LONGBLOB
);

-- 5. OAuth Approvals Table
-- Stores user approvals for OAuth2 scopes
CREATE TABLE oauth_approvals (
                                 userId VARCHAR(256),
                                 clientId VARCHAR(256),
                                 scope VARCHAR(256),
                                 status VARCHAR(10),
                                 expiresAt TIMESTAMP,
                                 lastModifiedAt TIMESTAMP
);

-- 6. Client Details Table (Alternative simpler version)
-- If you prefer a simpler client configuration
CREATE TABLE oauth_client_registration (
                                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           client_id VARCHAR(256) NOT NULL UNIQUE,
                                           client_name VARCHAR(256) NOT NULL,
                                           client_secret VARCHAR(256),
                                           redirect_uris TEXT,
                                           scopes VARCHAR(1000),
                                           grant_types VARCHAR(256),
                                           client_authentication_methods VARCHAR(256),
                                           authorization_grant_type VARCHAR(50),
                                           created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                           updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Insert default OAuth2 clients
-- 1. Default Kason client for traditional authentication
INSERT INTO oauth_client_details (
    client_id,
    resource_ids,
    client_secret,
    scope,
    authorized_grant_types,
    web_server_redirect_uri,
    authorities,
    access_token_validity,
    refresh_token_validity,
    additional_information,
    autoapprove
) VALUES (
             'kason-client',
             '',
             '{bcrypt}$2a$10$IB1qgbTHYhgLp5.8LJhD6OiNBNFWxdLK7VG.9yzCdOL.tUDzGJL5C', -- password: secret
             'read,write',
             'password,refresh_token,authorization_code,client_credentials',
             'http://localhost:3000/callback',
             'ROLE_CLIENT',
             43200, -- 12 hours
             2592000, -- 30 days
             '{}',
             'true'
         );

-- 2. Google SSO client
INSERT INTO oauth_client_details (
    client_id,
    resource_ids,
    client_secret,
    scope,
    authorized_grant_types,
    web_server_redirect_uri,
    authorities,
    access_token_validity,
    refresh_token_validity,
    additional_information,
    autoapprove
) VALUES (
             'google-sso-client',
             '',
             '{bcrypt}$2a$10$IB1qgbTHYhgLp5.8LJhD6OiNBNFWxdLK7VG.9yzCdOL.tUDzGJL5C', -- password: secret
             'read,write,profile,email',
             'authorization_code,refresh_token,google_sso',
             'http://localhost:3001/oauth/google/callback,http://localhost:3000/auth/callback',
             'ROLE_CLIENT',
             2592000, -- 30 days
             2592000, -- 30 days
             '{"description": "Google SSO client for Kason authentication"}',
             'true'
         );

-- 3. Mobile app client (optional)
INSERT INTO oauth_client_details (
    client_id,
    resource_ids,
    client_secret,
    scope,
    authorized_grant_types,
    web_server_redirect_uri,
    authorities,
    access_token_validity,
    refresh_token_validity,
    additional_information,
    autoapprove
) VALUES (
             'kason-mobile-client',
             '',
             '', -- No secret for mobile apps
             'read,write',
             'authorization_code,refresh_token',
             'kasonapp://oauth/callback',
             'ROLE_CLIENT',
             43200, -- 12 hours
             2592000, -- 30 days
             '{"platform": "mobile"}',
             'false'
         );

-- Create indexes for better performance
CREATE INDEX idx_oauth_access_token_client_id ON oauth_access_token(client_id);
CREATE INDEX idx_oauth_access_token_user_name ON oauth_access_token(user_name);
CREATE INDEX idx_oauth_access_token_refresh_token ON oauth_access_token(refresh_token);

CREATE INDEX idx_oauth_refresh_token_token_id ON oauth_refresh_token(token_id);

CREATE INDEX idx_oauth_approvals_user_client ON oauth_approvals(userId, clientId);
CREATE INDEX idx_oauth_approvals_expires ON oauth_approvals(expiresAt);

-- Add comments for better documentation
ALTER TABLE oauth_client_details COMMENT = 'OAuth2 client configuration table';
ALTER TABLE oauth_access_token COMMENT = 'OAuth2 access tokens storage';
ALTER TABLE oauth_refresh_token COMMENT = 'OAuth2 refresh tokens storage';
ALTER TABLE oauth_code COMMENT = 'OAuth2 authorization codes storage';
ALTER TABLE oauth_approvals COMMENT = 'OAuth2 user approvals storage';

-- Create a view for client information (without secrets)
CREATE VIEW v_oauth_client_info AS
SELECT
    client_id,
    scope,
    authorized_grant_types,
    web_server_redirect_uri,
    authorities,
    access_token_validity,
    refresh_token_validity,
    autoapprove,
    JSON_EXTRACT(additional_information, '$.description') as description
FROM oauth_client_details;

-- Create stored procedures for token cleanup (optional)
DELIMITER //

-- Procedure to clean up expired tokens
CREATE PROCEDURE CleanupExpiredTokens()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE token_count INT DEFAULT 0;

    -- Delete expired access tokens
    DELETE FROM oauth_access_token
    WHERE authentication_id IN (
        SELECT authentication_id FROM (
                                          SELECT oa.authentication_id
                                          FROM oauth_access_token oa
                                                   JOIN oauth_client_details ocd ON oa.client_id = ocd.client_id
                                          WHERE ocd.access_token_validity > 0
                                            AND oa.token_id IS NOT NULL
                                          -- This is a simplified check; in practice, you'd need to decode the token to check expiration
                                      ) AS expired_tokens
    );

    SELECT ROW_COUNT() AS tokens_deleted;
END//

-- Procedure to get client statistics
CREATE PROCEDURE GetClientStatistics(IN client_id_param VARCHAR(256))
BEGIN
    SELECT
        cd.client_id,
        cd.client_secret IS NOT NULL as has_secret,
        cd.scope,
        cd.authorized_grant_types,
        COUNT(DISTINCT at.user_name) as active_users,
        COUNT(at.authentication_id) as active_tokens,
        cd.access_token_validity,
        cd.refresh_token_validity
    FROM oauth_client_details cd
             LEFT JOIN oauth_access_token at ON cd.client_id = at.client_id
    WHERE cd.client_id = client_id_param OR client_id_param IS NULL
    GROUP BY cd.client_id;
END//

DELIMITER ;

-- Security: Create a user specifically for OAuth2 operations (optional)
-- CREATE USER 'oauth2_user'@'localhost' IDENTIFIED BY 'secure_password_here';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON kason_db.oauth_* TO 'oauth2_user'@'localhost';
-- GRANT SELECT ON kason_db.sys_user TO 'oauth2_user'@'localhost';

-- Sample queries to verify the setup

-- Check if all tables are created
SELECT
    TABLE_NAME,
    TABLE_ROWS,
    CREATE_TIME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'kason_db'
  AND TABLE_NAME LIKE 'oauth_%';

-- Check client configurations
SELECT
    client_id,
    scope,
    authorized_grant_types,
    access_token_validity
FROM oauth_client_details;

-- Note: After creating these tables, you may need to restart your Spring Boot application
-- The OAuth2 configuration should automatically detect these tables

-- Additional configuration for production:
-- 1. Change default client secrets to secure random values
-- 2. Configure proper redirect URIs for your domain
-- 3. Set appropriate token validity periods
-- 4. Enable SSL/TLS for production deployments
-- 5. Consider using external token store like Redis for better performance