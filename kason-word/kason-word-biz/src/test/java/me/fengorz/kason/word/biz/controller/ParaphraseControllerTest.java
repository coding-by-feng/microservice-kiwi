/*
 *
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 *
 */

package me.fengorz.kason.word.biz.controller;

import me.fengorz.kason.common.sdk.constant.EnvConstants;
import me.fengorz.kason.word.api.request.ParaphraseRequest;
import me.fengorz.kason.word.biz.service.base.ParaphraseService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ActiveProfiles({EnvConstants.TEST})
@ExtendWith(SpringExtension.class)
@WebMvcTest(ParaphraseController.class)
@Disabled
class ParaphraseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ParaphraseService mockService;

    @Test
    void testModifyMeaningChinese() throws Exception {
        // Setup
        when(mockService.modifyMeaningChinese(new ParaphraseRequest())).thenReturn(false);

        // Run the test
        final MockHttpServletResponse response =
            mockMvc
                .perform(post("/word/paraphrase/modifyMeaningChinese").content("content")
                    .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andReturn().getResponse();

        // Verify the results
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertEquals("expectedResponse", response.getContentAsString());
    }

}
