package me.fengorz.kason.tools;

import me.fengorz.kason.bdf.feign.annotation.EnableEnhancerFeignClients;
import me.fengorz.kason.bdf.security.annotation.KasonResourceSecurityServer;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;

@EnableEnhancerFeignClients
@SpringCloudApplication
@KasonResourceSecurityServer
public class ToolsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ToolsApplication.class, args);
    }
}
