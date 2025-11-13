package me.fengorz.kason.test.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;

@Configuration
public class TestSecurityConfig {

    @Bean
    public RemoteTokenServices remoteTokenServices() {
        RemoteTokenServices tokenServices = new RemoteTokenServices();
        tokenServices.setCheckTokenEndpointUrl("http://localhost:8080/dummy-uri");
        tokenServices.setClientId("dummy-client");
        tokenServices.setClientSecret("dummy-secret");
        return tokenServices;
    }
}