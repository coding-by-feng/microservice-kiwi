/*
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.fengorz.kiwi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Kiwi Monolith Application
 *
 * Consolidated from microservices:
 * - kiwi-upms: User Permission Management System
 * - kiwi-word: Dictionary and Word Processing
 * - kiwi-ai: AI/YouTube Subtitle Translation
 * - kiwi-tools: General-purpose Tools
 * - kiwi-flow: Flowable Workflow Engine
 * - kiwi-auth: OAuth2 Authentication
 *
 * @author codingByFeng
 * @since 3.0.0
 */
@Slf4j
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class KiwiApplication {

    public static void main(String[] args) {
        SpringApplication.run(KiwiApplication.class, args);
        log.info("""

                ╔═══════════════════════════════════════════════════════════╗
                ║                                                           ║
                ║   Kiwi Monolith Application Started Successfully!         ║
                ║                                                           ║
                ║   Version: 3.0.0                                          ║
                ║   Java: 17                                                ║
                ║   Spring Boot: 3.2.x                                      ║
                ║   Profile: {}                                             ║
                ║                                                           ║
                ╚═══════════════════════════════════════════════════════════╝
                """, System.getProperty("spring.profiles.active", "default"));
    }
}
