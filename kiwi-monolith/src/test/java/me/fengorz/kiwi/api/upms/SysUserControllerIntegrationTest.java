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
package me.fengorz.kiwi.api.upms;

import me.fengorz.kiwi.BaseIntegrationTest;
import me.fengorz.kiwi.domain.upms.entity.SysUser;
import me.fengorz.kiwi.domain.upms.service.SysUserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests for SysUserController
 *
 * Tests the user management API endpoints with real database connections.
 *
 * @author codingByFeng
 */
@DisplayName("Sys User Controller Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SysUserControllerIntegrationTest extends BaseIntegrationTest {

    private static final String API_BASE = "/api/upms/user";

    @Autowired
    private SysUserService userService;

    @Autowired(required = false)
    private PasswordEncoder passwordEncoder;

    private SysUser testUser;

    @BeforeEach
    void setUpTestData() {
        // Create test user if it doesn't exist
        testUser = userService.findByUsername("integration-test-user")
            .orElseGet(() -> {
                SysUser user = new SysUser();
                user.setUsername("integration-test-user");
                user.setPassword(passwordEncoder != null ?
                    passwordEncoder.encode("test123") : "test123");
                user.setEmail("integration-test@example.com");
                user.setPhone("1234567890");
                user.setRealName("Integration Test User");
                user.setDeptId(1);
                user.setLockFlag(0);
                user.setDelFlag(0);
                user.setCreateTime(LocalDateTime.now());
                user.setUpdateTime(LocalDateTime.now());
                userService.save(user);
                return user;
            });
    }

    @Test
    @Order(1)
    @DisplayName("GET /api/upms/user/{userId} - Should return user by ID")
    void getUserById_ShouldReturnUser() throws Exception {
        mockMvc.perform(get(API_BASE + "/" + testUser.getUserId())
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.username").value("integration-test-user"))
            .andExpect(jsonPath("$.data.email").value("integration-test@example.com"));
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/upms/user/{userId} - Should return error for non-existent user")
    void getUserById_NotFound_ShouldReturnError() throws Exception {
        mockMvc.perform(get(API_BASE + "/999999")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1))
            .andExpect(jsonPath("$.msg").value("User not found"));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/upms/user/username/{username} - Should return user by username")
    void getUserByUsername_ShouldReturnUser() throws Exception {
        mockMvc.perform(get(API_BASE + "/username/integration-test-user")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.userId").value(testUser.getUserId()));
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/upms/user - Should return paginated user list")
    void listUsers_ShouldReturnPaginatedList() throws Exception {
        mockMvc.perform(get(API_BASE)
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.content").isArray())
            .andExpect(jsonPath("$.data.totalElements").isNumber());
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/upms/user - Should create new user")
    void createUser_ShouldCreateAndReturnUser() throws Exception {
        String uniqueUsername = "test-create-user-" + System.currentTimeMillis();

        SysUser newUser = new SysUser();
        newUser.setUsername(uniqueUsername);
        newUser.setPassword("newpassword123");
        newUser.setEmail(uniqueUsername + "@example.com");
        newUser.setPhone("9876543210");
        newUser.setRealName("New Test User");
        newUser.setDeptId(1);
        newUser.setLockFlag(0);
        newUser.setDelFlag(0);

        mockMvc.perform(post(API_BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(newUser)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.username").value(uniqueUsername))
            .andExpect(jsonPath("$.data.email").value(uniqueUsername + "@example.com"))
            .andExpect(jsonPath("$.data.userId").isNumber());
    }

    @Test
    @Order(6)
    @DisplayName("POST /api/upms/user - Should fail for duplicate username")
    void createUser_DuplicateUsername_ShouldFail() throws Exception {
        SysUser duplicateUser = new SysUser();
        duplicateUser.setUsername("integration-test-user");  // Already exists
        duplicateUser.setPassword("password123");
        duplicateUser.setEmail("another@example.com");
        duplicateUser.setDeptId(1);

        mockMvc.perform(post(API_BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(duplicateUser)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1))
            .andExpect(jsonPath("$.msg").value("Username already exists"));
    }

    @Test
    @Order(7)
    @DisplayName("POST /api/upms/user - Should fail for duplicate email")
    void createUser_DuplicateEmail_ShouldFail() throws Exception {
        SysUser duplicateUser = new SysUser();
        duplicateUser.setUsername("unique-username-" + System.currentTimeMillis());
        duplicateUser.setPassword("password123");
        duplicateUser.setEmail("integration-test@example.com");  // Already exists
        duplicateUser.setDeptId(1);

        mockMvc.perform(post(API_BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(duplicateUser)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1))
            .andExpect(jsonPath("$.msg").value("Email already exists"));
    }

    @Test
    @Order(8)
    @DisplayName("PUT /api/upms/user/{userId} - Should update existing user")
    void updateUser_ShouldUpdateAndReturnUser() throws Exception {
        testUser.setRealName("Updated Test User");

        mockMvc.perform(put(API_BASE + "/" + testUser.getUserId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(testUser)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.realName").value("Updated Test User"));
    }

    @Test
    @Order(9)
    @DisplayName("PUT /api/upms/user/{userId} - Should fail for non-existent user")
    void updateUser_NotFound_ShouldFail() throws Exception {
        SysUser nonExistentUser = new SysUser();
        nonExistentUser.setUsername("non-existent");
        nonExistentUser.setPassword("password");

        mockMvc.perform(put(API_BASE + "/999999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(nonExistentUser)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1))
            .andExpect(jsonPath("$.msg").value("User not found"));
    }

    @Test
    @Order(10)
    @DisplayName("POST /api/upms/user/{userId}/lock - Should lock user account")
    void lockUser_ShouldSucceed() throws Exception {
        mockMvc.perform(post(API_BASE + "/" + testUser.getUserId() + "/lock")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));

        // Verify user is locked
        mockMvc.perform(get(API_BASE + "/" + testUser.getUserId())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.lockFlag").value(1));
    }

    @Test
    @Order(11)
    @DisplayName("POST /api/upms/user/{userId}/unlock - Should unlock user account")
    void unlockUser_ShouldSucceed() throws Exception {
        mockMvc.perform(post(API_BASE + "/" + testUser.getUserId() + "/unlock")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));

        // Verify user is unlocked
        mockMvc.perform(get(API_BASE + "/" + testUser.getUserId())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.lockFlag").value(0));
    }

    @Test
    @Order(12)
    @DisplayName("POST /api/upms/user/{userId}/roles - Should assign roles to user")
    void assignRoles_ShouldSucceed() throws Exception {
        mockMvc.perform(post(API_BASE + "/" + testUser.getUserId() + "/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[1, 2]"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @Order(100)  // Run last
    @DisplayName("DELETE /api/upms/user/{userId} - Should delete user")
    void deleteUser_ShouldSucceed() throws Exception {
        // Create a user specifically for deletion
        SysUser userToDelete = new SysUser();
        userToDelete.setUsername("user-to-delete-" + System.currentTimeMillis());
        userToDelete.setPassword(passwordEncoder != null ?
            passwordEncoder.encode("test123") : "test123");
        userToDelete.setEmail("delete-" + System.currentTimeMillis() + "@example.com");
        userToDelete.setPhone("1111111111");
        userToDelete.setDeptId(1);
        userToDelete.setLockFlag(0);
        userToDelete.setDelFlag(0);
        userToDelete.setCreateTime(LocalDateTime.now());
        userService.save(userToDelete);

        mockMvc.perform(delete(API_BASE + "/" + userToDelete.getUserId())
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));
    }
}
