# Google SSO Integration Guide for Kiwi Auth & UPMS

## Overview

This guide provides a complete integration of Google Single Sign-On (SSO) with your existing Kiwi authentication system and UPMS module. The integration includes:

- Google OAuth2 authentication flow
- User account creation and linking
- Enhanced login UI with social login options
- Account management in user center
- Database schema updates

## Architecture Overview

```
Frontend (Vue.js) ↔ Auth Module (Spring Security OAuth2) ↔ UPMS Module ↔ Database
                                     ↕
                              Google OAuth2 API
```

## Implementation Steps

### 1. Backend Setup

#### A. Update Dependencies (pom.xml)
Add the Google OAuth2 dependencies to your `kiwi-auth/pom.xml`:

```xml
<!-- Google OAuth2 dependencies -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
<dependency>
    <groupId>com.google.api-client</groupId>
    <artifactId>google-api-client</artifactId>
    <version>2.2.0</version>
</dependency>
```

#### B. Configuration Files

Update `bootstrap.yml` in kiwi-auth:
```yaml
google:
  oauth2:
    client-id: ${GOOGLE_OAUTH2_CLIENT_ID}
    client-secret: ${GOOGLE_OAUTH2_CLIENT_SECRET}
    redirect-uri: ${GOOGLE_OAUTH2_REDIRECT_URI:http://localhost:3001/oauth/google/callback}
    scopes:
      - openid
      - profile
      - email
```

#### C. Database Schema Updates

Run the database migration script to add Google OAuth support:

1. Add new columns to `sys_user` table
2. Create indexes for performance
3. Add audit logging tables (optional)

#### D. Backend Components

The integration includes these new backend components:

1. **GoogleOAuth2Config** - Configuration for Google OAuth2 settings
2. **GoogleUserInfo** - DTO for Google user data
3. **GoogleOAuth2Service** - Service for Google OAuth operations
4. **GoogleOAuth2Controller** - REST endpoints for Google authentication
5. **GoogleUserService** - Integration service with UPMS
6. **GoogleUserController** - UPMS controller for Google user management

### 2. Google Cloud Console Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing one
3. Enable the Google+ API and Google OAuth2 API
4. Create OAuth 2.0 credentials:
    - Application type: Web application
    - Authorized JavaScript origins: `http://localhost:3000` (your frontend)
    - Authorized redirect URIs: `http://localhost:3001/oauth/google/callback` (your backend)
5. Copy Client ID and Client Secret

### 3. Environment Variables

Set these environment variables:

```bash
export GOOGLE_OAUTH2_CLIENT_ID="your_google_client_id"
export GOOGLE_OAUTH2_CLIENT_SECRET="your_google_client_secret"
export GOOGLE_OAUTH2_REDIRECT_URI="http://localhost:3001/oauth/google/callback"
```

For frontend (Vue.js), add to your `.env` file:
```
VUE_APP_GOOGLE_CLIENT_ID=your_google_client_id
```

### 4. Frontend Integration

#### A. Install Google API

Add to your HTML template:
```html
<script src="https://apis.google.com/js/api.js"></script>
```

#### B. API Integration

Use the provided `google-api.js` for frontend API calls.

#### C. Enhanced Login UI

Replace your existing login page with the enhanced version that includes:

- Traditional username/password login
- Google SSO button
- Account linking functionality
- Modern responsive design

#### D. User Center Updates

Update your user center with the enhanced version that includes:
- Account binding management
- Google account status display
- Profile management with OAuth data

## API Endpoints

### Authentication Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/oauth/google/authorize` | Get Google authorization URL |
| GET | `/oauth/google/callback` | Handle Google OAuth callback |
| POST | `/oauth/google/login` | Login with Google access token |
| POST | `/oauth/google/refresh` | Refresh Google access token |

### User Management Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/sys/user/google/createOrUpdate` | Create/update user from Google |
| POST | `/sys/user/google/link` | Link Google account to existing user |
| POST | `/sys/user/google/unlink/{userId}` | Unlink Google account |

## Usage Flows

### Flow 1: New User Registration via Google

1. User clicks "Sign in with Google"
2. Redirected to Google OAuth consent screen
3. User authorizes application
4. Google redirects back with authorization code
5. Backend exchanges code for access token
6. Backend fetches user info from Google
7. Backend creates new user in UPMS
8. Backend generates system access token
9. User is logged in

### Flow 2: Existing User Login via Google

1. User clicks "Sign in with Google"
2. Same OAuth flow as above
3. Backend finds existing user by email
4. Backend updates user info with Google data
5. Backend generates system access token
6. User is logged in

### Flow 3: Account Linking

1. Existing user logs in with username/password
2. User goes to account settings
3. User clicks "Link Google Account"
4. Google OAuth flow for authorization
5. Backend links Google ID to existing user
6. User can now login with either method

## Security Considerations

1. **Token Storage**: System tokens are stored in Redis with expiration
2. **CSRF Protection**: State parameter used in OAuth flow
3. **Account Verification**: Email verification for security
4. **Audit Logging**: All OAuth operations are logged
5. **Rate Limiting**: Implement rate limiting on OAuth endpoints

## Testing

### Unit Tests

Create tests for:
- GoogleOAuth2Service methods
- GoogleUserService integration
- OAuth flow controllers

### Integration Tests

Test complete flows:
- New user registration via Google
- Existing user login via Google
- Account linking/unlinking
- Token refresh

### Manual Testing Checklist

- [ ] Google OAuth flow works end-to-end
- [ ] User creation from Google profile
- [ ] Account linking with existing users
- [ ] Token refresh functionality
- [ ] Error handling (invalid tokens, etc.)
- [ ] Security measures (CSRF, rate limiting)

## Monitoring and Analytics

### Key Metrics to Track

1. **Authentication Metrics**:
    - Google SSO success/failure rates
    - Token refresh rates
    - Account linking success rates

2. **User Metrics**:
    - New registrations via Google
    - Login method preferences
    - Account binding adoption

3. **Performance Metrics**:
    - OAuth flow response times
    - Google API response times
    - Database query performance

### Logging

Important events to log:
- OAuth authentication attempts
- Account creation/linking
- Token refresh operations
- Security-related events
- API errors and failures

## Troubleshooting

### Common Issues

1. **"Invalid Client" Error**
    - Check Google Client ID/Secret
    - Verify redirect URI configuration

2. **"Unauthorized" Errors**
    - Check Google API enablement
    - Verify OAuth consent screen setup

3. **Token Expiration Issues**
    - Implement proper token refresh logic
    - Handle expired token scenarios

4. **Account Linking Failures**
    - Verify user authentication
    - Check for existing Google account links

### Debug Tips

1. Enable debug logging for OAuth components
2. Use browser developer tools to inspect OAuth flow
3. Check Google Cloud Console logs
4. Verify database constraints and indexes

## Deployment

### Development Environment

1. Set up local Google OAuth credentials
2. Use localhost redirect URIs
3. Test with development database

### Production Environment

1. Create production Google OAuth credentials
2. Use HTTPS redirect URIs
3. Set production environment variables
4. Monitor OAuth flows and performance

### Security Checklist

- [ ] HTTPS enabled for all OAuth redirects
- [ ] Secure token storage implementation
- [ ] Rate limiting configured
- [ ] Audit logging enabled
- [ ] Error handling doesn't expose sensitive data
- [ ] CSRF protection active

## Future Enhancements

1. **Additional OAuth Providers**:
    - WeChat OAuth integration
    - QQ OAuth integration
    - GitHub/GitLab for developer users

2. **Advanced Features**:
    - Multi-factor authentication
    - Account merging capabilities
    - Advanced user profile syncing