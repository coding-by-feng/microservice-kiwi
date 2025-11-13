-- Database migration script for Google OAuth integration
-- Add new columns to sys_user table to support Google authentication

-- 1. Add Google OpenID column
ALTER TABLE sys_user
    ADD COLUMN google_openid VARCHAR(255) COMMENT 'Google OpenID' AFTER qq_openid;

-- 2. Add email column if not exists
ALTER TABLE sys_user
    ADD COLUMN email VARCHAR(255) COMMENT '邮箱地址' AFTER google_openid;

-- 3. Add real name column if not exists
ALTER TABLE sys_user
    ADD COLUMN real_name VARCHAR(100) COMMENT '用户真实姓名' AFTER email;

-- 4. Add register source column to track user registration method
ALTER TABLE sys_user
    ADD COLUMN register_source VARCHAR(20) DEFAULT 'local' COMMENT '注册来源：local, google, wechat, qq' AFTER real_name;

-- 5. Create unique index on google_openid to prevent duplicate bindings
CREATE UNIQUE INDEX idx_sys_user_google_openid ON sys_user(google_openid);

-- 6. Create index on email for faster lookups
CREATE INDEX idx_sys_user_email ON sys_user(email);

-- 7. Create index on register_source for analytics
CREATE INDEX idx_sys_user_register_source ON sys_user(register_source);

-- 8. Update existing users to set register_source
UPDATE sys_user
SET register_source = CASE
                          WHEN wx_openid IS NOT NULL THEN 'wechat'
                          WHEN qq_openid IS NOT NULL THEN 'qq'
                          ELSE 'local'
    END
WHERE register_source IS NULL OR register_source = '';

-- 9. Create table for storing OAuth tokens (optional, for advanced token management)
CREATE TABLE IF NOT EXISTS oauth_google_tokens (
                                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                   user_id INT NOT NULL,
                                                   google_user_id VARCHAR(255) NOT NULL,
    access_token TEXT NOT NULL,
    refresh_token TEXT,
    expires_at TIMESTAMP NULL,
    scope VARCHAR(500),
    token_type VARCHAR(50) DEFAULT 'Bearer',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_user_google (user_id, google_user_id),
    FOREIGN KEY (user_id) REFERENCES sys_user(user_id) ON DELETE CASCADE,
    INDEX idx_google_user_id (google_user_id),
    INDEX idx_expires_at (expires_at)
    ) COMMENT = 'Google OAuth tokens storage';

-- 10. Create audit log table for OAuth operations (optional)
CREATE TABLE IF NOT EXISTS oauth_audit_log (
                                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                               user_id INT,
                                               operation VARCHAR(50) NOT NULL COMMENT 'login, link, unlink, refresh',
    provider VARCHAR(20) NOT NULL COMMENT 'google, wechat, qq',
    provider_user_id VARCHAR(255),
    client_ip VARCHAR(45),
    user_agent TEXT,
    success BOOLEAN DEFAULT TRUE,
    error_message TEXT,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_user_id (user_id),
    INDEX idx_operation (operation),
    INDEX idx_provider (provider),
    INDEX idx_created_time (created_time),
    FOREIGN KEY (user_id) REFERENCES sys_user(user_id) ON DELETE SET NULL
    ) COMMENT = 'OAuth operations audit log';

-- 11. Add sample data for testing (optional - remove in production)
-- INSERT INTO sys_user (
--     user_id, username, password, email, real_name,
--     register_source, google_openid, create_time, update_time,
--     lock_flag, del_flag, dept_id
-- ) VALUES (
--     999, 'google_test_user', '$2a$10$encrypted_password_here',
--     'test@gmail.com', 'Test User', 'google', 'google_123456789',
--     NOW(), NOW(), 0, 0, 1
-- );

-- 12. Create view for user with OAuth info (optional)
CREATE OR REPLACE VIEW v_user_oauth_info AS
SELECT
    u.user_id,
    u.username,
    u.email,
    u.real_name,
    u.avatar,
    u.register_source,
    u.google_openid,
    u.wx_openid,
    u.qq_openid,
    u.create_time,
    u.update_time,
    CASE
        WHEN u.google_openid IS NOT NULL THEN 'Google'
        WHEN u.wx_openid IS NOT NULL THEN 'WeChat'
        WHEN u.qq_openid IS NOT NULL THEN 'QQ'
        ELSE 'Local'
        END as primary_auth_method,
    (CASE WHEN u.google_openid IS NOT NULL THEN 1 ELSE 0 END +
     CASE WHEN u.wx_openid IS NOT NULL THEN 1 ELSE 0 END +
     CASE WHEN u.qq_openid IS NOT NULL THEN 1 ELSE 0 END) as oauth_accounts_count
FROM sys_user u
WHERE u.del_flag = 0;

-- 13. Add comments to new columns for better documentation
ALTER TABLE sys_user
    MODIFY COLUMN google_openid VARCHAR(255) COMMENT 'Google OpenID，用于Google账号绑定';

ALTER TABLE sys_user
    MODIFY COLUMN email VARCHAR(255) COMMENT '用户邮箱地址，用于登录和通知';

ALTER TABLE sys_user
    MODIFY COLUMN real_name VARCHAR(100) COMMENT '用户真实姓名，从第三方OAuth获取';

ALTER TABLE sys_user
    MODIFY COLUMN register_source VARCHAR(20) DEFAULT 'local'
    COMMENT '用户注册来源：local-本地注册, google-Google注册, wechat-微信注册, qq-QQ注册';

-- 14. Security: Ensure sensitive columns are properly protected
-- Add triggers to log changes to OAuth columns (optional)
DELIMITER //
CREATE TRIGGER tr_sys_user_oauth_audit
    AFTER UPDATE ON sys_user
    FOR EACH ROW
BEGIN
    IF OLD.google_openid != NEW.google_openid OR
       OLD.wx_openid != NEW.wx_openid OR
       OLD.qq_openid != NEW.qq_openid THEN
        INSERT INTO oauth_audit_log (
            user_id, operation, provider, provider_user_id, success, created_time
        ) VALUES (
            NEW.user_id,
            CASE
                WHEN NEW.google_openid IS NOT NULL AND OLD.google_openid IS NULL THEN 'link'
                WHEN NEW.google_openid IS NULL AND OLD.google_openid IS NOT NULL THEN 'unlink'
                ELSE 'update'
END,
'google',
COALESCE(NEW.google_openid, OLD.google_openid),
TRUE,
NOW()
);
END IF;
END//
DELIMITER ;

-- Migration complete!
-- Remember to:
-- 1. First run oauth2-tables.sql to create OAuth2 required tables
-- 2. Update your application.yml with Google OAuth configuration
-- 3. Set environment variables for Google Client ID and Secret
-- 4. Update your frontend with the new login UI
-- 5. Test the OAuth flow thoroughly before deploying to production