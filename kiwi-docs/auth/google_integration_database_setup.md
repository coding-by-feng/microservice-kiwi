# Database Setup Guide for Google OAuth2 Integration

## Overview

You need to run two SQL scripts in the correct order to set up Google OAuth2 integration with your Kiwi system.

## Step-by-Step Setup

### Step 1: Create OAuth2 Tables

**Run this script first**: `oauth2-tables.sql`

This script creates the required Spring Security OAuth2 tables:
- `oauth_client_details` - OAuth2 client configurations
- `oauth_access_token` - Access tokens storage
- `oauth_refresh_token` - Refresh tokens storage
- `oauth_code` - Authorization codes storage
- `oauth_approvals` - User approvals storage

```sql
-- Execute oauth2-tables.sql
mysql -u your_username -p kiwi_db < oauth2-tables.sql
```

### Step 2: Update User Table for Google Integration

**Run this script second**: `database-migration.sql`

This script adds Google OAuth support to your existing user table:
- Adds `google_openid`, `email`, `real_name`, `register_source` columns
- Creates indexes for performance
- Adds audit logging tables
- Creates helpful views

```sql
-- Execute database-migration.sql
mysql -u your_username -p kiwi_db < database-migration.sql
```

## Verification

After running both scripts, verify the setup:

```sql
-- Check if OAuth2 tables exist
SHOW TABLES LIKE 'oauth_%';

-- Should show:
-- oauth_access_token
-- oauth_approvals  
-- oauth_client_details
-- oauth_code
-- oauth_refresh_token

-- Check if user table has new columns
DESCRIBE sys_user;

-- Should include new columns:
-- google_openid
-- email
-- real_name
-- register_source

-- Check OAuth2 clients
SELECT client_id, scope, authorized_grant_types FROM oauth_client_details;

-- Should show:
-- kiwi-client (traditional auth)
-- google-sso-client (Google OAuth)
-- kiwi-mobile-client (mobile app)
```

## Default OAuth2 Clients Created

The scripts create these default clients:

### 1. Traditional Kiwi Client
```
Client ID: kiwi-client
Secret: secret (bcrypt encoded)
Grant Types: password, refresh_token, authorization_code, client_credentials
Scope: read, write
```

### 2. Google SSO Client
```
Client ID: google-sso-client
Secret: secret (bcrypt encoded)
Grant Types: authorization_code, refresh_token, google_sso
Scope: read, write, profile, email
```

### 3. Mobile App Client
```
Client ID: kiwi-mobile-client
Secret: (none - public client)
Grant Types: authorization_code, refresh_token
Scope: read, write
```

## Security Notes

### Production Deployment

**⚠️ IMPORTANT**: Before deploying to production:

1. **Change default client secrets**:
```sql
-- Generate a secure secret
UPDATE oauth_client_details 
SET client_secret = '{bcrypt}$2a$10$your_secure_bcrypt_encoded_secret'
WHERE client_id = 'kiwi-client';

UPDATE oauth_client_details 
SET client_secret = '{bcrypt}$2a$10$your_secure_bcrypt_encoded_secret'
WHERE client_id = 'google-sso-client';
```

2. **Update redirect URIs for your domain**:
```sql
UPDATE oauth_client_details 
SET web_server_redirect_uri = 'https://yourdomain.com/oauth/google/callback,https://yourdomain.com/auth/callback'
WHERE client_id = 'google-sso-client';
```

3. **Set appropriate token validity periods**:
```sql
-- Example: 1 hour access tokens, 30 day refresh tokens
UPDATE oauth_client_details 
SET access_token_validity = 3600,
    refresh_token_validity = 2592000
WHERE client_id = 'google-sso-client';
```

## Troubleshooting

### Common Issues

1. **"Table doesn't exist" errors**
    - Make sure you ran `oauth2-tables.sql` first
    - Check if you're connected to the correct database

2. **"Column already exists" errors**
    - Some columns might already exist in your user table
    - Modify the ALTER TABLE statements to use `ADD COLUMN IF NOT EXISTS` if your MySQL version supports it

3. **Foreign key constraint failures**
    - Make sure your `sys_user` table exists before running the migration
    - Check that the user_id column in sys_user is the correct type (INT)

### Manual Column Addition

If you get errors about existing columns, add them manually:

```sql
-- Check which columns already exist
SELECT COLUMN_NAME 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'kiwi_db' 
AND TABLE_NAME = 'sys_user'
AND COLUMN_NAME IN ('google_openid', 'email', 'real_name', 'register_source');

-- Add only missing columns
ALTER TABLE sys_user ADD COLUMN google_openid VARCHAR(255) COMMENT 'Google OpenID';
-- (repeat for other missing columns)
```

### Index Creation Issues

If you get duplicate index errors:

```sql
-- Check existing indexes
SHOW INDEX FROM sys_user;

-- Drop existing indexes if needed
DROP INDEX idx_sys_user_email ON sys_user;
DROP INDEX idx_sys_user_google_openid ON sys_user;

-- Then recreate them
CREATE UNIQUE INDEX idx_sys_user_google_openid ON sys_user(google_openid);
CREATE INDEX idx_sys_user_email ON sys_user(email);
```

## Configuration After Database Setup

### 1. Update Application Configuration

Add to your `application.yml` or `bootstrap.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/kiwi_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_OAUTH2_CLIENT_ID}
            client-secret: ${GOOGLE_OAUTH2_CLIENT_SECRET}
            scope: openid,profile,email
            redirect-uri: "{baseUrl}/oauth/google/callback"
```

### 2. Environment Variables

Set these environment variables:

```bash
# Database
export DB_USERNAME="your_db_user"
export DB_PASSWORD="your_db_password"

# Google OAuth2
export GOOGLE_OAUTH2_CLIENT_ID="your_google_client_id"
export GOOGLE_OAUTH2_CLIENT_SECRET="your_google_client_secret"
export GOOGLE_OAUTH2_REDIRECT_URI="http://localhost:3001/oauth/google/callback"

# Encryption (if using jasypt)
export KIWI_ENC_PASSWORD="your_encryption_password"
```

### 3. Restart Services

After running the database scripts:

1. **Restart your Spring Boot application**
    - The OAuth2 configuration will automatically detect the new tables
    - New endpoints will be available

2. **Clear any cached configurations**
    - If using Redis, clear relevant caches
    - Browser cache for frontend changes

## Testing the Setup

### 1. Verify OAuth2 Endpoints

Test that OAuth2 endpoints are working:

```bash
# Check OAuth2 token endpoint
curl -X POST http://localhost:3001/oauth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&username=test&password=123456&client_id=kiwi-client&client_secret=secret"

# Check Google authorization URL
curl -X GET http://localhost:3001/oauth/google/authorize
```

### 2. Database Connectivity Test

```sql
-- Test basic OAuth2 table access
SELECT COUNT(*) FROM oauth_client_details;
SELECT COUNT(*) FROM oauth_access_token;

-- Test user table with new columns
SELECT user_id, username, email, register_source FROM sys_user LIMIT 5;

-- Test view
SELECT * FROM v_user_oauth_info LIMIT 5;
```

### 3. Application Logs

Check your application logs for:

```
INFO - OAuth2 Authorization Server configured
INFO - TokenStore bean of type RedisTokenStore created
INFO - OAuth2 endpoints registered
```

## Data Migration (For Existing Users)

If you have existing users, you may want to migrate some data:

### 1. Set Default Register Source

```sql
-- Update existing users to have 'local' as register source
UPDATE sys_user 
SET register_source = 'local' 
WHERE register_source IS NULL AND del_flag = 0;
```

### 2. Extract Emails from Usernames

If some usernames are email addresses:

```sql
-- Copy email-format usernames to email column
UPDATE sys_user 
SET email = username 
WHERE email IS NULL 
  AND username LIKE '%@%' 
  AND username LIKE '%.%'
  AND del_flag = 0;
```

### 3. Migrate Profile Data

If you have profile data in other tables:

```sql
-- Example: Copy from a profile table
UPDATE sys_user u
JOIN user_profile p ON u.user_id = p.user_id
SET u.real_name = p.full_name,
    u.email = COALESCE(u.email, p.email)
WHERE u.del_flag = 0;
```

## Monitoring and Maintenance

### 1. Regular Cleanup

Set up scheduled jobs to clean old tokens:

```sql
-- Clean expired access tokens (run daily)
DELETE FROM oauth_access_token 
WHERE authentication_id IN (
  SELECT authentication_id FROM (
    SELECT oa.authentication_id 
    FROM oauth_access_token oa
    JOIN oauth_client_details ocd ON oa.client_id = ocd.client_id
    WHERE ocd.access_token_validity > 0 
    -- Add proper expiration logic based on your token format
  ) AS expired_tokens
);

-- Clean old audit logs (run weekly)
DELETE FROM oauth_audit_log 
WHERE created_time < DATE_SUB(NOW(), INTERVAL 90 DAY);
```

### 2. Performance Monitoring

```sql
-- Monitor OAuth usage
SELECT 
  client_id,
  COUNT(*) as active_tokens,
  COUNT(DISTINCT user_name) as unique_users
FROM oauth_access_token 
GROUP BY client_id;

-- Monitor registration sources
SELECT 
  register_source,
  COUNT(*) as user_count,
  COUNT(*) * 100.0 / (SELECT COUNT(*) FROM sys_user WHERE del_flag = 0) as percentage
FROM sys_user 
WHERE del_flag = 0 
GROUP BY register_source;
```

## Backup Recommendations

Before running the migration in production:

```bash
# Create full database backup
mysqldump -u username -p kiwi_db > kiwi_db_backup_$(date +%Y%m%d_%H%M%S).sql

# Create table-specific backups
mysqldump -u username -p kiwi_db sys_user > sys_user_backup_$(date +%Y%m%d_%H%M%S).sql
```

## Next Steps

After successful database setup:

1. ✅ **Deploy backend code** with Google OAuth2 integration
2. ✅ **Update frontend** with new login UI
3. ✅ **Configure Google Cloud Console** OAuth2 credentials
4. ✅ **Test complete OAuth flow** in development
5. ✅ **Deploy to production** with proper security configurations

## Support

If you encounter issues:

1. **Check application logs** for detailed error messages
2. **Verify database connectivity** and user permissions
3. **Confirm OAuth2 table structure** matches Spring Security requirements
4. **Test with simple OAuth2 flow** before adding Google integration
5. **Review Google Cloud Console** configuration

The database setup is now complete and ready for Google OAuth2 integration!