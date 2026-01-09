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
package me.fengorz.kiwi.api.ai;

import me.fengorz.kiwi.BaseIntegrationTest;
import me.fengorz.kiwi.domain.ai.ytb.YouTubeClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests for YouTube Controller
 *
 * Tests the YouTube-related API endpoints.
 * Some tests require yt-dlp to be installed.
 *
 * @author codingByFeng
 */
@DisplayName("YouTube Controller Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class YouTubeControllerIntegrationTest extends BaseIntegrationTest {

    private static final String API_BASE = "/api/ai/youtube";

    @Autowired(required = false)
    private YouTubeClient youTubeClient;

    // Test video URL - use a short, public video
    private static final String TEST_VIDEO_URL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
    private static final String TEST_VIDEO_ID = "dQw4w9WgXcQ";

    @Test
    @Order(1)
    @DisplayName("YouTube Client may be available")
    void youTubeClient_MayBeAvailable() {
        // YouTubeClient is optional - just log whether it's available
        if (youTubeClient != null) {
            System.out.println("YouTubeClient is available");
        } else {
            System.out.println("YouTubeClient is not configured");
        }
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/ai/youtube/title - Should return video title")
    @EnabledIfEnvironmentVariable(named = "TEST_YT_DLP_ENABLED", matches = "true")
    void getVideoTitle_ShouldReturnTitle() throws Exception {
        mockMvc.perform(get(API_BASE + "/title")
                .param("url", TEST_VIDEO_URL)
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data").isNotEmpty());
    }

    @Test
    @Order(3)
    @DisplayName("Should get video title using YouTubeClient directly")
    @EnabledIfEnvironmentVariable(named = "TEST_YT_DLP_ENABLED", matches = "true")
    void youTubeClient_GetVideoTitle_ShouldReturnTitle() {
        if (youTubeClient != null) {
            String title = youTubeClient.getVideoTitle(TEST_VIDEO_URL);
            assertNotNull(title);
            assertFalse(title.isEmpty());
            System.out.println("Video title: " + title);
        }
    }
}
