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

import cn.hutool.http.Header;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.constant.EnvConstants;
import me.fengorz.kiwi.common.sdk.util.json.KiwiJsonUtils;
import me.fengorz.kiwi.word.biz.WordBizApplication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.TimeUnit;

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

    @SneakyThrows
    @Test
    // @Disabled
    void test_queryWord() {
        ResponseEntity<R> response =
                testRestTemplate.getForEntity("http://localhost:8081/word/main/query/AOB", R.class);
        Assertions.assertNotNull(response.getBody());
        log.info("Data is: {}", KiwiJsonUtils.toJsonStr(response.getBody()));

        TimeUnit.SECONDS.sleep(120);
        // HttpHeaders headers = response.getHeaders();
        // headers.forEach((k, v) -> {
        //     log.info("header name is: {}, value is: {}", k, v);
        // });
        // assertCacheControl(headers);
    }

    // @Test
    void test_removeWord() {
        ResponseEntity<R> response =
                testRestTemplate.getForEntity("http://localhost:8081/word/fetch/removeWord/1338674", R.class);
        Assertions.assertNotNull(response.getBody());
        log.info("Data is: {}", KiwiJsonUtils.toJsonStr(response.getBody()));
    }

    // @Test
    void test_setupIeltsWordList() {
        ResponseEntity<R> response =
                testRestTemplate.getForEntity("http://localhost:8081/test/setup/ielts/word-list", R.class);
        Assertions.assertEquals(response.getStatusCode(), HttpStatus.OK);
    }

    // @Test
    void test_deprecateReviewAudio() {
        ResponseEntity<R> response =
                testRestTemplate.getForEntity("http://localhost:8081/word/review/deprecate-review-audio/2350782", R.class);
        Assertions.assertEquals(response.getStatusCode(), HttpStatus.OK);
    }

    // @Test
    void test_downloadReviewAudio() {
        String url = String.format("http://localhost:8081/word/review/downloadReviewAudio/%d/%d", 2979284, 1);
        File file = testRestTemplate.execute(url, HttpMethod.GET, null, clientHttpResponse -> {
            File ret = File.createTempFile("2979284", "mp3");
            StreamUtils.copy(clientHttpResponse.getBody(), new FileOutputStream(ret));
            return ret;
        });
        Assertions.assertNotNull(file);
    }

    private void assertCacheControl(HttpHeaders headers) {
        Assertions.assertTrue(headers.containsKey(Header.CACHE_CONTROL.toString()));
        Assertions.assertEquals(headers.get(Header.CACHE_CONTROL.toString()).size(), 1);
        Assertions.assertEquals(headers.get(Header.CACHE_CONTROL.toString()).get(0),
                CacheControl.maxAge(365, TimeUnit.DAYS).getHeaderValue());
    }

}
