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

import me.fengorz.kiwi.domain.ai.service.AiChatService;
import me.fengorz.kiwi.domain.upms.service.SysUserService;
import me.fengorz.kiwi.domain.word.service.WordMainService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Application Startup Test
 *
 * Verifies that the Spring Boot application context loads correctly
 * and all essential beans are available.
 *
 * @author codingByFeng
 */
@DisplayName("Application Startup Tests")
class ApplicationStartupTest extends BaseIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private WordMainService wordMainService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired(required = false)
    private AiChatService aiChatService;

    @Test
    @DisplayName("Application context should load successfully")
    void contextLoads() {
        assertNotNull(applicationContext);
        System.out.println("Application context loaded successfully with " +
            applicationContext.getBeanDefinitionCount() + " beans");
    }

    @Test
    @DisplayName("WordMainService should be available")
    void wordMainService_ShouldBeAvailable() {
        assertNotNull(wordMainService);
    }

    @Test
    @DisplayName("SysUserService should be available")
    void sysUserService_ShouldBeAvailable() {
        assertNotNull(sysUserService);
    }

    @Test
    @DisplayName("AiChatService should be available")
    void aiChatService_ShouldBeAvailable() {
        assertNotNull(aiChatService, "AiChatService should be available");
    }

    @Test
    @DisplayName("Application should have required beans")
    void requiredBeans_ShouldBePresent() {
        // Check for essential beans
        assertTrue(applicationContext.containsBean("wordMainService") ||
            applicationContext.containsBean("wordMainServiceImpl"));

        assertTrue(applicationContext.containsBean("sysUserService") ||
            applicationContext.containsBean("sysUserServiceImpl"));

        // Check for mappers (MyBatis Plus)
        String[] expectedBeanTypes = {
            "wordMainMapper",
            "sysUserMapper",
            "sysRoleMapper"
        };

        for (String beanName : expectedBeanTypes) {
            if (applicationContext.containsBean(beanName)) {
                System.out.println("Found bean: " + beanName);
            }
        }
    }

    @Test
    @DisplayName("Database connection should be established")
    void databaseConnection_ShouldBeEstablished() {
        // Try to count words using service - this will verify DB connection
        try {
            long count = wordMainService.count();
            System.out.println("Database connection verified. Word count: " + count);
            assertTrue(count >= 0);
        } catch (Exception e) {
            fail("Database connection failed: " + e.getMessage());
        }
    }
}
