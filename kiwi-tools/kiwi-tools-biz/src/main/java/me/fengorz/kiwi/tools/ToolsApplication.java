package me.fengorz.kiwi.tools;

import me.fengorz.kiwi.bdf.feign.annotation.EnableEnhancerFeignClients;
import me.fengorz.kiwi.bdf.security.annotation.KiwiResourceSecurityServer;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;

@EnableEnhancerFeignClients
@SpringCloudApplication
@KiwiResourceSecurityServer
public class ToolsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ToolsApplication.class, args);
    }
}

