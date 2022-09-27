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

package me.fengorz.kiwi.word.biz.controller;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import cn.hutool.http.Header;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.constant.EnvConstants;
import me.fengorz.kiwi.word.biz.WordBizApplication;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2022/9/23 09:36
 */
@Slf4j
@ActiveProfiles({EnvConstants.DEV, EnvConstants.BASE})
@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:env.properties")
@SpringBootTest(classes = WordBizApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class ControllerTest {

    private final TestRestTemplate testRestTemplate = new TestRestTemplate();

    @Test
    @Disabled
    void test_queryWord() {
        ResponseEntity<R> response =
            testRestTemplate.getForEntity("http://localhost:8081/word/main/query/test", R.class);
        Assertions.assertNotNull(response.getBody());
        log.info("Data is: {}", response.getBody());
        HttpHeaders headers = response.getHeaders();
        headers.forEach((k, v) -> {
            log.info("header name is: {}, value is: {}", k, v);
        });
        assertCacheControl(headers);
    }

    @Test
    @Disabled
    void test_setupIeltsWordList() {
        ResponseEntity<R> response =
            testRestTemplate.getForEntity("http://localhost:8081/test/setup/ielts/word-list", R.class);
        Assertions.assertEquals(response.getStatusCode(), HttpStatus.OK);
    }

    @Test
    void test_deprecateReviewAudio() {
        ResponseEntity<R> response =
            testRestTemplate.getForEntity("http://localhost:8081/word/review/deprecate-review-audio/2350782", R.class);
        Assertions.assertEquals(response.getStatusCode(), HttpStatus.OK);
    }

    private void assertCacheControl(HttpHeaders headers) {
        Assertions.assertTrue(headers.containsKey(Header.CACHE_CONTROL.toString()));
        Assertions.assertEquals(headers.get(Header.CACHE_CONTROL.toString()).size(), 1);
        Assertions.assertEquals(headers.get(Header.CACHE_CONTROL.toString()).get(0),
            CacheControl.maxAge(365, TimeUnit.DAYS).getHeaderValue());
    }

}
