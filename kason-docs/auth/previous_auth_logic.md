/*
* Original Token Checking Logic Before Google SSO Integration
* Based on your existing kason-auth and kason-bdf components
  */

// ============================================================================
// 1. AUTHORIZATION SERVER CONFIGURATION (AuthorizationServerConfig.java)
// ============================================================================

@Configuration
@RequiredArgsConstructor
@EnableAuthorizationServer
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

    private final DataSource dataSource;
    private final UserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;
    private final RedisConnectionFactory redisConnectionFactory;

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints.allowedTokenEndpointRequestMethods(HttpMethod.GET, HttpMethod.POST)
            .tokenStore(tokenStore())
            .tokenEnhancer(tokenEnhancer())
            .userDetailsService(userDetailsService)
            .authenticationManager(authenticationManager)
            .reuseRefreshTokens(false)
            .exceptionTranslator(new KasonWebResponseExceptionTranslator());

        DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
        defaultTokenServices.setTokenStore(endpoints.getTokenStore());
        defaultTokenServices.setAccessTokenValiditySeconds(60 * 60 * 24 * 30); // 30 days
        defaultTokenServices.setRefreshTokenValiditySeconds(60 * 60 * 24 * 30);
        defaultTokenServices.setSupportRefreshToken(true);
        defaultTokenServices.setClientDetailsService(endpoints.getClientDetailsService());
        defaultTokenServices.setTokenEnhancer(endpoints.getTokenEnhancer());

        endpoints.tokenServices(defaultTokenServices);
    }

    @Bean
    public TokenStore tokenStore() {
        RedisTokenStore tokenStore = new RedisTokenStore(redisConnectionFactory);
        tokenStore.setPrefix(SecurityConstants.PROJECT_PREFIX + SecurityConstants.OAUTH_PREFIX);
        return tokenStore;
    }

    @Bean
    public TokenEnhancer tokenEnhancer() {
        return (accessToken, authentication) -> {
            final Map<String, Object> additionalInfo = new HashMap<>(1);
            EnhancerUser enhancerUser = (EnhancerUser)authentication.getPrincipal();
            additionalInfo.put(SecurityConstants.DETAILS_LICENSE, SecurityConstants.PROJECT_LICENSE);
            additionalInfo.put(SecurityConstants.DETAILS_USER_ID, enhancerUser.getId());
            additionalInfo.put(SecurityConstants.DETAILS_USERNAME, enhancerUser.getUsername());
            additionalInfo.put(SecurityConstants.DETAILS_DEPT_ID, enhancerUser.getDeptId());
            ((DefaultOAuth2AccessToken)accessToken).setAdditionalInformation(additionalInfo);
            return accessToken;
        };
    }
}

// ============================================================================
// 2. TOKEN ENDPOINT FOR MANAGEMENT (KasonTokenEndpoint.java) - ORIGINAL
// ============================================================================

@RestController
@RequiredArgsConstructor
@RequestMapping("/kasonTokenEndpoint")
public class KasonTokenEndpoint {

    private static final String PROJECT_OAUTH_ACCESS =
        SecurityConstants.PROJECT_PREFIX + SecurityConstants.OAUTH_PREFIX + "access:";
    
    private final TokenStore tokenStore;
    private final RedisTemplate<String, ?> redisTemplate;

    /**
     * Original logout - only handles standard OAuth2 tokens
     */
    @DeleteMapping("/logout")
    public R<Boolean> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        if (StrUtil.isBlank(authHeader)) {
            return R.failed("Logout failed, token is empty");
        }

        String tokenValue = authHeader.replace(OAuth2AccessToken.BEARER_TYPE, StrUtil.EMPTY).trim();
        OAuth2AccessToken accessToken = tokenStore.readAccessToken(tokenValue);
        if (accessToken == null || StrUtil.isBlank(accessToken.getValue())) {
            return R.success();
        }
        
        // Remove OAuth2 tokens from Redis
        tokenStore.removeAccessToken(accessToken);
        OAuth2RefreshToken refreshToken = accessToken.getRefreshToken();
        tokenStore.removeRefreshToken(refreshToken);
        
        return R.success();
    }

    /**
     * Original token removal - only handles standard OAuth2 tokens
     */
    @DeleteMapping("/{token}")
    public R<Boolean> removeToken(@PathVariable("token") String token, @RequestHeader(required = false) String from) {
        if (StrUtil.isBlank(from)) {
            return null;
        }
        return R.success(redisTemplate.delete(PROJECT_OAUTH_ACCESS + token));
    }

    /**
     * Original token page query - only shows standard OAuth2 token info
     */
    @PostMapping("/page")
    public R<Page<Map<String, String>>> getTokenPage(@RequestBody Map<String, Object> params,
        @RequestHeader(required = false) String from) {
        
        if (StrUtil.isBlank(from)) {
            return null;
        }

        List<Map<String, String>> list = new ArrayList<>();
        // ... pagination logic ...
        
        for (String page : pages) {
            String accessToken = StrUtil.subAfter(page, PROJECT_OAUTH_ACCESS, true);
            OAuth2AccessToken token = tokenStore.readAccessToken(accessToken);
            Map<String, String> map = new HashMap<>(8);

            // Standard OAuth2 token information only
            map.put(OAuth2AccessToken.TOKEN_TYPE, token.getTokenType());
            map.put(OAuth2AccessToken.ACCESS_TOKEN, token.getValue());
            map.put(OAuth2AccessToken.EXPIRES_IN, token.getExpiresIn() + "");

            OAuth2Authentication oAuth2Auth = tokenStore.readAuthentication(token);
            Authentication authentication = oAuth2Auth.getUserAuthentication();

            map.put(OAuth2Utils.CLIENT_ID, oAuth2Auth.getOAuth2Request().getClientId());
            map.put(OAuth2Utils.GRANT_TYPE, oAuth2Auth.getOAuth2Request().getGrantType());

            // Extract user information from authentication
            if (authentication instanceof UsernamePasswordAuthenticationToken) {
                UsernamePasswordAuthenticationToken authenticationToken =
                    (UsernamePasswordAuthenticationToken)authentication;

                if (authenticationToken.getPrincipal() instanceof EnhancerUser) {
                    EnhancerUser user = (EnhancerUser)authenticationToken.getPrincipal();
                    map.put("user_id", user.getId() + "");
                    map.put("username", user.getUsername() + "");
                }
            } else if (authentication instanceof PreAuthenticatedAuthenticationToken) {
                PreAuthenticatedAuthenticationToken authenticationToken =
                    (PreAuthenticatedAuthenticationToken)authentication;
                if (authenticationToken.getPrincipal() instanceof EnhancerUser) {
                    EnhancerUser user = (EnhancerUser)authenticationToken.getPrincipal();
                    map.put("user_id", user.getId() + "");
                    map.put("username", user.getUsername() + "");
                }
            }
            
            // No Google token information in original implementation
            
            list.add(map);
        }

        Page<Map<String, String>> result = new Page<>(MapUtil.getInt(params, CURRENT), MapUtil.getInt(params, SIZE));
        result.setRecords(list);
        result.setTotal(Objects.requireNonNull(redisTemplate.keys(PROJECT_OAUTH_ACCESS + "*")).size());
        return R.success(result);
    }
}

// ============================================================================
// 3. RESOURCE SERVER CONFIGURATION (KasonResourceServerConfigurerAdapter.java)
// ============================================================================

@Slf4j
public class KasonResourceServerConfigurerAdapter extends ResourceServerConfigurerAdapter {

    @Autowired
    protected ResourceAuthExceptionEntryPoint resourceAuthExceptionEntryPoint;
    @Autowired
    protected RemoteTokenServices remoteTokenServices;
    @Autowired
    private FilterIgnorePropertiesConfig ignorePropertiesConfig;
    @Autowired
    private AccessDeniedHandler pigAccessDeniedHandler;
    @Autowired
    private RestTemplate lbRestTemplate;
    @Autowired
    private FilterIgnorePropertiesConfig filterIgnorePropertiesConfig;

    /**
     * Original HTTP security configuration - only handles standard OAuth2
     */
    @Override
    public void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.headers().frameOptions().disable();
        ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry registry =
            httpSecurity.authorizeRequests();
        registry.antMatchers(ignorePropertiesConfig.getUrls().toArray(new String[0]))
                .permitAll()
                .anyRequest()
                .authenticated()
                .and().csrf().disable();
    }

    /**
     * Original resource server security configuration - standard OAuth2 only
     */
    @Override
    public void configure(ResourceServerSecurityConfigurer resources) {
        DefaultAccessTokenConverter accessTokenConverter = new DefaultAccessTokenConverter();
        UserAuthenticationConverter userTokenConverter =
            new KasonUserAuthenticationConverter(this.filterIgnorePropertiesConfig);
        accessTokenConverter.setUserTokenConverter(userTokenConverter);

        // Standard RemoteTokenServices configuration
        remoteTokenServices.setRestTemplate(lbRestTemplate);
        remoteTokenServices.setAccessTokenConverter(accessTokenConverter);
        
        resources.authenticationEntryPoint(resourceAuthExceptionEntryPoint)
                .accessDeniedHandler(pigAccessDeniedHandler)
                .tokenServices(remoteTokenServices);
    }
}

// ============================================================================
// 4. USER AUTHENTICATION CONVERTER (KasonUserAuthenticationConverter.java)
// ============================================================================

@RequiredArgsConstructor
public class KasonUserAuthenticationConverter implements UserAuthenticationConverter {

    private static final String N_A = "N/A";
    private final FilterIgnorePropertiesConfig filterIgnorePropertiesConfig;

    /**
     * Original authentication extraction - only handles standard OAuth2 tokens
     */
    @Override
    public Authentication extractAuthentication(Map<String, ?> map) {
        if (map.containsKey(USERNAME)) {
            Collection<? extends GrantedAuthority> authorities = getAuthorities(map);

            // Extract standard OAuth2 user information
            String username = (String)map.get(SecurityConstants.DETAILS_USERNAME);
            Integer id = (Integer)map.get(SecurityConstants.DETAILS_USER_ID);
            Integer deptId = (Integer)map.get(SecurityConstants.DETAILS_DEPT_ID);
            
            // Create EnhancerUser with standard OAuth2 information only
            EnhancerUser user = new EnhancerUser(id, deptId, username, N_A, true, true, true, true, authorities);
            return new UsernamePasswordAuthenticationToken(user, N_A, authorities);
        }
        return null;
    }

    private Collection<? extends GrantedAuthority> getAuthorities(Map<String, ?> map) {
        Object authorities = map.get(AUTHORITIES);
        
        if (authorities instanceof String) {
            return AuthorityUtils.commaSeparatedStringToAuthorityList((String)authorities);
        }
        if (authorities instanceof Collection) {
            return AuthorityUtils.commaSeparatedStringToAuthorityList(
                StringUtils.collectionToCommaDelimitedString((Collection<?>)authorities));
        }
        return AuthorityUtils.commaSeparatedStringToAuthorityList(GlobalConstants.EMPTY);
    }
}

// ============================================================================
// 5. USER DETAILS SERVICE (KasonUserDetailServiceImpl.java)
// ============================================================================

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ms.config.exclude-cache", havingValue = "false")
public class KasonUserDetailServiceImpl implements UserDetailsService {

    private final CacheManager cacheManager;
    private final UserApi userApi;

    /**
     * Original user loading - only handles standard username/password authentication
     */
    @Override
    public UserDetails loadUserByUsername(String username) {
        // Check cache first
        Cache cache = cacheManager.getCache("user_details");
        if (cache != null && cache.get(username) != null) {
            return (EnhancerUser) cache.get(username).get();
        }
        
        // Load user from UPMS service
        R<UserFullInfoDTO> info = userApi.info(username, SecurityConstants.FROM_IN);
        UserDetails userDetails = getUserDetails(info);
        cache.put(username, userDetails);
        return userDetails;
    }

    /**
     * Original user details creation - standard OAuth2 user only
     */
    private UserDetails getUserDetails(R<UserFullInfoDTO> userFullInfoDTO) {
        if (userFullInfoDTO == null || userFullInfoDTO.getData() == null) {
            throw new UsernameNotFoundException("User does not exist");
        }

        UserFullInfoDTO info = userFullInfoDTO.getData();

        // Build authorities from roles and permissions
        Set<String> dbAuthsSet = new HashSet<>();
        if (ArrayUtil.isNotEmpty(info.getRoles())) {
            Arrays.stream(info.getRoles()).forEach(role -> {
                dbAuthsSet.add(SecurityConstants.ROLE + role);
            });
            dbAuthsSet.addAll(Arrays.asList(info.getPermissions()));
        }

        Collection<? extends GrantedAuthority> authorities =
                AuthorityUtils.createAuthorityList(dbAuthsSet.toArray(new String[0]));
        SysUser user = info.getSysUser();

        // Create standard EnhancerUser - no Google SSO information
        return new EnhancerUser(
            user.getUserId(), 
            user.getDeptId(), 
            user.getUsername(),
            SecurityConstants.BCRYPT + user.getPassword(), 
            GlobalConstants.FLAG_DEL_NO == user.getDelFlag(), 
            true, 
            true,
            true, 
            authorities
        );
    }
}

// ============================================================================
// 6. WEB SECURITY CONFIGURATION (WebSecurityConfigurer.java)
// ============================================================================

@Primary
@Order(90)
@Configuration
public class WebSecurityConfigurer extends WebSecurityConfigurerAdapter {

    /**
     * Original HTTP security - only OAuth2 token endpoints
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/oauth/token/**").permitAll()
                .antMatchers("/kasonTokenEndpoint/**").permitAll()
                // No Google OAuth2 endpoints in original configuration
                .anyRequest().authenticated()
                .and().csrf().disable();
    }

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}

// ============================================================================
// 7. REDIS TOKEN STORAGE STRUCTURE (Original)
// ============================================================================

/*
Original Redis Key Structure (Before Google SSO):

1. OAuth2 Access Tokens:
   kason_oauth:access:{token_value} -> OAuth2AccessToken object

2. OAuth2 Refresh Tokens:
   kason_oauth:refresh:{refresh_token_value} -> OAuth2RefreshToken object

3. OAuth2 Authentication:
   kason_oauth:auth:{token_value} -> OAuth2Authentication object

4. OAuth2 Client Details:
   (Stored in database, cached in Redis with client_details prefix)

NO Google token caching in original implementation.
*/

// ============================================================================
// 8. TOKEN VALIDATION FLOW (Original)
// ============================================================================

/*
Original Token Validation Flow:

1. Client sends request with Bearer token
2. Resource server extracts token from Authorization header
3. RemoteTokenServices calls /oauth/check_token on auth server
4. Auth server validates token using TokenStore (Redis)
5. If valid, returns user information and authorities
6. Resource server creates Authentication object
7. Spring Security allows/denies access based on authorities

No Google token validation or Google user information handling.
*/