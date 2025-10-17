package me.fengorz.kiwi.tools.config;

import me.fengorz.kiwi.tools.service.RateLimitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.UUID;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

    private final ToolsProperties props;
    private final RateLimitService rateLimitService;

    public WebConfig(ToolsProperties props, RateLimitService rateLimitService) {
        this.props = props;
        this.rateLimitService = rateLimitService;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = "file:" + props.getUploadDir() + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(props.getCorsAllowedOrigins().toArray(new String[0]))
                // include PATCH/DELETE to avoid preflight failing
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Idempotency-Key")
                .allowCredentials(false);
    }

    // +++ Boot 2.3-compatible security config: permit /api/** and OPTIONS + ignore static/docs +++
    @Configuration
    @EnableWebSecurity
    @Order(1)
    @ConditionalOnClass(WebSecurityConfigurerAdapter.class)
    static class ApiSecurityConfig extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                .antMatcher("/api/**")
                .cors().and()
                .csrf().disable()
                .authorizeRequests()
                    .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .anyRequest().permitAll();
        }

        @Override
        public void configure(WebSecurity web) {
            web.ignoring().antMatchers(
                "/uploads/**",
                "/static/**", "/webjars/**",
                "/favicon.ico",
                "/actuator/health",
                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
            );
        }
    }

    @Bean
    public HandlerInterceptor loggingAndRateLimitInterceptor() {
        AntPathMatcher matcher = new AntPathMatcher();
        return new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                request.setAttribute("_start", Instant.now().toEpochMilli());
                String ip = request.getRemoteAddr();
                String path = request.getRequestURI();
                boolean isWrite = "POST".equalsIgnoreCase(request.getMethod()) || "PUT".equalsIgnoreCase(request.getMethod()) || "PATCH".equalsIgnoreCase(request.getMethod()) || "DELETE".equalsIgnoreCase(request.getMethod());
                boolean isUpload = matcher.match("/api/projects/*/photo", path) && "POST".equalsIgnoreCase(request.getMethod());
                if (isUpload) {
                    if (!rateLimitService.allowUpload(ip)) {
                        response.setStatus(429);
                        response.setHeader("X-RateLimit-Limit", String.valueOf(props.getUploadRateLimitPerMin()));
                        response.setHeader("X-RateLimit-Remaining", "0");
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"error\":{\"code\":\"rate_limited\",\"message\":\"Too Many Requests\"}}\n");
                        return false;
                    }
                } else if (isWrite) {
                    if (!rateLimitService.allowWrite(ip)) {
                        response.setStatus(429);
                        response.setHeader("X-RateLimit-Limit", String.valueOf(props.getWriteRateLimitPerMin()));
                        response.setHeader("X-RateLimit-Remaining", "0");
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"error\":{\"code\":\"rate_limited\",\"message\":\"Too Many Requests\"}}\n");
                        return false;
                    }
                }
                return true;
            }

            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
                Long start = (Long) request.getAttribute("_start");
                long cost = start != null ? (Instant.now().toEpochMilli() - start) : -1;
                String reqId = request.getHeader("X-Request-Id");
                if (reqId == null || reqId.isEmpty()) reqId = UUID.randomUUID().toString();
                int status = response.getStatus();
                String method = request.getMethod();
                String path = request.getRequestURI();
                log.info("rid={} {} {} {}ms status={}", reqId, method, path, cost, status);
            }
        };
    }

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(loggingAndRateLimitInterceptor()).addPathPatterns("/**");
    }
}
