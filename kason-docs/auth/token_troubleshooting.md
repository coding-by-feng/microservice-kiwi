# Token Troubleshooting Guide

## Issue: Bearer Token Not Detected

Your Bearer token `7267e51d-89a7-4c52-af42-0e99cdefb353` is not being detected by Spring Security. This guide will help you troubleshoot and fix the issue.

## Step 1: Enable Debug Logging

Add these configurations to your `application.yml`:

```yaml
logging:
  level:
    me.fengorz.kason.auth: DEBUG
    me.fengorz.kason.bdf.security: DEBUG
    org.springframework.security: DEBUG
    org.springframework.security.oauth2: DEBUG
```

## Step 2: Check Token Store

### 2.1 Verify Token Exists in Redis

Use Redis CLI to check if your token exists:

```bash
# Connect to Redis
redis-cli

# Check if token exists in OAuth2 token store
EXISTS kason_oauth:access:7267e51d-89a7-4c52-af42-0e99cdefb353

# Check Google token cache
EXISTS kason:google:token:7267e51d-89a7-4c52-af42-0e99cdefb353

# List all OAuth2 tokens
KEYS kason_oauth:access:*

# List all Google tokens
KEYS kason:google:token:*
```

### 2.2 Use Token Management Endpoint

Check token via the management endpoint:

```bash
curl -X POST http://localhost:3001/kasonTokenEndpoint/page \
  -H "Content-Type: application/json" \
  -H "from: internal" \
  -d '{"current": 1, "size": 10}'
```

## Step 3: Test Token Validation

### 3.1 Direct Token Validation

```bash
# Test OAuth2 check_token endpoint
curl -X POST http://localhost:3001/oauth/check_token \
  -d "token=7267e51d-89a7-4c52-af42-0e99cdefb353"

# Test enhanced validation endpoint
curl -X POST http://localhost:3001/oauth/validate_token \
  -d "token=7267e51d-89a7-4c52-af42-0e99cdefb353"

# Get token info
curl -X GET "http://localhost:3001/oauth/token_info?token=7267e51d-89a7-4c52-af42-0e99cdefb353"
```

### 3.2 Test Resource Server Access

```bash
# Test protected endpoint with Bearer token
curl -X GET http://localhost:8080/sys/user/current/info \
  -H "Authorization: Bearer 7267e51d-89a7-4c52-af42-0e99cdefb353"
```

## Step 4: Common Issues and Solutions

### Issue 1: Token Not in Redis

**Symptoms:**
- Token not found in Redis
- Authentication fails immediately

**Solutions:**
1. Check if token was properly stored during authentication
2. Verify Redis connection and configuration
3. Check token expiration settings

```java
// Add this to your application for debugging
@RestController
public class TokenDebugController {
    
    @Autowired
    private TokenStore tokenStore;
    
    @GetMapping("/debug/token/{token}")
    public ResponseEntity<?> debugToken(@PathVariable String token) {
        OAuth2AccessToken accessToken = tokenStore.readAccessToken(token);
        if (accessToken == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> info = new HashMap<>();
        info.put("exists", true);
        info.put("expired", accessToken.isExpired());
        info.put("tokenType", accessToken.getTokenType());
        info.put("expiresIn", accessToken.getExpiresIn());
        
        return ResponseEntity.ok(info);
    }
}
```

### Issue 2: Resource Server Configuration

**Symptoms:**
- Token exists but resource server doesn't recognize it
- 401 Unauthorized on protected endpoints

**Solutions:**

1. **Check RemoteTokenServices Configuration:**

```java
@Configuration
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {
    
    @Override
    public void configure(ResourceServerSecurityConfigurer resources) {
        RemoteTokenServices tokenServices = new RemoteTokenServices();
        tokenServices.setCheckTokenEndpointUrl("http://kason-auth:3001/oauth/check_token");
        tokenServices.setClientId("your-resource-server-client-id");
        tokenServices.setClientSecret("your-resource-server-client-secret");
        
        resources.tokenServices(tokenServices);
    }
}
```

2. **Update WebSecurityConfigurer:**

```java
@Override
protected void configure(HttpSecurity http) throws Exception {
    http.authorizeRequests()
        .antMatchers("/oauth/token/**").permitAll()
        .antMatchers("/oauth/check_token/**").permitAll()
        .antMatchers("/oauth/google/**").permitAll()
        .antMatchers("/debug/**").permitAll() // Add for debugging
        .anyRequest().authenticated()
        .and().csrf().disable();
}
```

### Issue 3: Google Token Integration

**Symptoms:**
- Standard OAuth2 tokens work but Google tokens don't
- Google token not found in cache

**Solutions:**

1. **Verify Google Token Storage:**

```bash
# Check if Google token cache exists
redis-cli KEYS "kason:google:token:*"

# Check specific token
redis-cli GET "kason:google:token:7267e51d-89a7-4c52-af42-0e99cdefb353"
```

2. **Test Google Token Cache Service:**

```java
@RestController
public class GoogleTokenDebugController {
    
    @Autowired
    private GoogleTokenCacheService googleTokenCacheService;
    
    @GetMapping("/debug/google-token/{token}")
    public ResponseEntity<?> debugGoogleToken(@PathVariable String token) {
        GoogleTokenCacheInfo info = googleTokenCacheService.getGoogleTokenInfo(token);
        return ResponseEntity.ok(info);
    }
}
```

## Step 5: Fix Your Specific Token

For your specific token `7267e51d-89a7-4c52-af42-0e99cdefb353`:

### 5.1 Check Token Status

```bash
# Check if token exists in OAuth2 store
curl -X POST http://localhost:3001/oauth/check_token \
  -d "token=7267e51d-89a7-4c52-af42-0e99cdefb353" \
  -v

# Check Redis directly
redis-cli GET "kason_oauth:access:7267e51d-89a7-4c52-af42-0e99cdefb353"
```

### 5.2 Re-authenticate if Needed

If the token is missing or invalid:

1. **For Google SSO:**
```bash
# Get new authorization URL
curl -X GET "http://localhost:3001/oauth/google/authorize"

# Use the URL to re-authenticate
```

2. **For Standard OAuth2:**
```bash
# Get new token
curl -X POST http://localhost:3001/oauth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&username=your_username&password=your_password&client_id=your_client_id&client_secret=your_client_secret"
```

## Step 6: Application Configuration Updates

### 6.1 Update AuthorizationServerConfig

Make sure your `AuthorizationServerConfig` properly configures the token store:

```java
@Bean
public TokenStore tokenStore() {
    RedisTokenStore tokenStore = new RedisTokenStore(redisConnectionFactory);
    tokenStore.setPrefix(SecurityConstants.PROJECT_PREFIX + SecurityConstants.OAUTH_PREFIX);
    return tokenStore;
}
```

### 6.2 Update Application Properties

```yaml
# OAuth2 Configuration
security:
  oauth2:
    resource:
      token-info-uri: http://kason-auth:3001/oauth/check_token
      prefer-token-info: true

# Redis Configuration
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
    timeout: 2000ms
```

## Step 7: Debugging Commands

Use these curl commands to test each component:

```bash
# 1. Test auth server health
curl -X GET http://localhost:3001/actuator/health

# 2. Test token endpoint
curl -X POST http://localhost:3001/oauth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&client_id=test&client_secret=test"

# 3. Test check_token endpoint
curl -X POST http://localhost:3001/oauth/check_token \
  -d "token=YOUR_TOKEN_HERE"

# 4. Test resource server
curl -X GET http://localhost:8080/sys/user/current/info \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"

# 5. Test Google endpoints
curl -X GET http://localhost:3001/oauth/google/authorize
```

## Step 8: Enable the Debug Filter

The `DebugTokenFilter` I provided will log detailed information about every token. Enable it by:

1. Adding the filter to your configuration
2. Setting appropriate log levels
3. Making requests to see detailed token analysis

The filter will output logs like:
```
=== TOKEN ANALYSIS START ===
Request URI: /sys/user/current/info
Token: 7267e51d-89a7-4c52-af42-0e99cdefb353
✓ Token found in TokenStore
  - Token Type: Bearer
  - Expires In: 3600
  - Is Expired: false
✓ Google SSO token detected
  - Google User ID: 123456789
  - Google Email: user@example.com
=== TOKEN ANALYSIS END ===
```

This will help you identify exactly where the token validation is failing.