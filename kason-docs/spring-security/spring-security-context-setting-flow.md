# Spring Security Context Setting Flow in OAuth2 Resource Server

## 1. Filter Chain Order (Spring Boot 2.x)

```
HTTP Request
    ↓
1. SecurityContextPersistenceFilter
    ↓
2. OAuth2AuthenticationProcessingFilter  ← Key filter for OAuth2
    ↓
3. ExceptionTranslationFilter
    ↓
4. FilterSecurityInterceptor
    ↓
Your Controller Method
```

## 2. Where SecurityContext is Set

### A. OAuth2AuthenticationProcessingFilter
**Location**: This is the main filter that sets your SecurityContext for OAuth2 tokens.

**What it does**:
1. Extracts Bearer token from Authorization header
2. Calls your `ResourceServerTokenServices` (your `EnhancedRemoteTokenServices`)
3. Gets `OAuth2Authentication` from token validation
4. Sets the authentication in `SecurityContextHolder`

### B. Your Token Validation Chain

```java
OAuth2AuthenticationProcessingFilter
    ↓
EnhancedRemoteTokenServices.loadAuthentication()
    ↓
TokenStore.readAuthentication() OR RemoteTokenServices
    ↓
Returns OAuth2Authentication with EnhancerUser
    ↓
SecurityContextHolder.getContext().setAuthentication(auth)
```

## 3. Key Components in Your Setup

### Your EnhancedRemoteTokenServices
```java
@Override
public OAuth2Authentication loadAuthentication(String accessToken) {
    // This method's return value gets set in SecurityContext
    OAuth2Authentication localAuth = tokenStore.readAuthentication(accessToken);
    if (localAuth != null) {
        return localAuth; // ← This becomes your SecurityContext
    }
    return super.loadAuthentication(accessToken);
}
```

### Your KasonResourceServerConfigurerAdapter
```java
@Override
public void configure(ResourceServerSecurityConfigurer resources) {
    // This configures which TokenServices to use
    resources.tokenServices(remoteTokenServices); // ← Your EnhancedRemoteTokenServices
}
```

## 4. Debugging Points

Add logging at these key points to see where the flow breaks:

### A. Filter Level Debugging
```java
// Add this filter to see when OAuth2AuthenticationProcessingFilter runs
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class SecurityContextDebugFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String token = extractToken(httpRequest);
        
        System.out.println("BEFORE FILTER CHAIN - Token: " + (token != null ? "present" : "absent"));
        System.out.println("BEFORE FILTER CHAIN - Auth: " + SecurityContextHolder.getContext().getAuthentication());
        
        chain.doFilter(request, response);
        
        System.out.println("AFTER FILTER CHAIN - Auth: " + SecurityContextHolder.getContext().getAuthentication());
    }
    
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
```

### B. Token Services Debugging
```java
// In your EnhancedRemoteTokenServices
@Override
public OAuth2Authentication loadAuthentication(String accessToken) throws InvalidTokenException {
    log.info("loadAuthentication called with token: {}", accessToken);
    
    try {
        OAuth2Authentication localAuth = tokenStore.readAuthentication(accessToken);
        if (localAuth != null) {
            log.info("Found local authentication: {}", localAuth.getName());
            log.info("Principal type: {}", localAuth.getPrincipal().getClass().getName());
            return localAuth;
        }
        
        log.info("No local auth found, trying remote validation");
        OAuth2Authentication remoteAuth = super.loadAuthentication(accessToken);
        log.info("Remote authentication result: {}", remoteAuth != null ? remoteAuth.getName() : "null");
        return remoteAuth;
        
    } catch (Exception e) {
        log.error("Error in loadAuthentication: ", e);
        throw new InvalidTokenException("Invalid token: " + accessToken);
    }
}
```

## 5. Common Issues

### A. Filter Not Running
- Check if `@EnableResourceServer` is present
- Verify your resource server configuration

### B. Token Not Found
- Ensure Redis is connected
- Check if token exists in your TokenStore

### C. Wrong Authentication Type
- Verify your token contains the right user information
- Check if `KasonUserAuthenticationConverter` is working correctly

## 6. Manual SecurityContext Setting (for testing)

```java
// You can manually set context for testing
public void setTestSecurityContext() {
    EnhancerUser testUser = new EnhancerUser(1, 1, "testuser", "password", 
                                           true, true, true, true, 
                                           Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    
    UsernamePasswordAuthenticationToken auth = 
        new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
    
    SecurityContextHolder.getContext().setAuthentication(auth);
}
```

## 7. Verification Steps

1. **Check if OAuth2AuthenticationProcessingFilter is in your filter chain**
2. **Verify your EnhancedRemoteTokenServices is being called**
3. **Confirm your TokenStore contains the expected authentication**
4. **Ensure no exceptions are thrown during token validation**