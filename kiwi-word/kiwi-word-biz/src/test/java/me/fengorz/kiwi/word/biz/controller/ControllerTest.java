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

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.Header;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.constant.EnvConstants;
import me.fengorz.kiwi.common.sdk.util.json.KiwiJsonUtils;
import me.fengorz.kiwi.word.biz.WordBizApplication;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
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
@SpringBootTest(classes = WordBizApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ControllerTest {

    private final TestRestTemplate testRestTemplate = new TestRestTemplate();
    @LocalServerPort
    private Integer port;

    @SneakyThrows
    @Test
    @Disabled
    void test_queryWord() {
        ResponseEntity<R> response =
                testRestTemplate.getForEntity(String.format("http://localhost:%d/word/main/query/AOB", port), R.class);
        Assertions.assertNotNull(response.getBody());
        log.info("Data is: {}", KiwiJsonUtils.toJsonStr(response.getBody()));

        // TimeUnit.SECONDS.sleep(120);
        // HttpHeaders headers = response.getHeaders();
        // headers.forEach((k, v) -> {
        //     log.info("header name is: {}, value is: {}", k, v);
        // });
        // assertCacheControl(headers);
    }

    @Disabled
    @Test
    void test_removeWord() {
        ResponseEntity<R> response =
                testRestTemplate.getForEntity(String.format("http://localhost:%d/word/fetch/removeWord/1338674", port), R.class);
        Assertions.assertNotNull(response.getBody());
        log.info("Data is: {}", KiwiJsonUtils.toJsonStr(response.getBody()));
    }

    @Disabled
    @Test
    void test_setupIeltsWordList() {
        ResponseEntity<R> response =
                testRestTemplate.getForEntity(String.format("http://localhost:%d/test/setup/ielts/word-list", port), R.class);
        Assertions.assertEquals(response.getStatusCode(), HttpStatus.OK);
    }

    @Disabled
    @Test
    void test_deprecateReviewAudio() {
        ResponseEntity<R> response =
                testRestTemplate.getForEntity(String.format("http://localhost:%d/word/review/deprecate-review-audio/2350782", port), R.class);
        Assertions.assertEquals(response.getStatusCode(), HttpStatus.OK);
    }

    @Disabled
    @Test
    void test_downloadReviewAudio() {
        String url = String.format("http://localhost:%d/word/review/downloadReviewAudio/%d/%d", port, 2979284, 1);
        File file = testRestTemplate.execute(url, HttpMethod.GET, null, clientHttpResponse -> {
            Assertions.assertFalse(clientHttpResponse.getStatusCode().isError());
            File ret = File.createTempFile("2979284", ".mp3", new File("/Users/zhanshifeng/Documents/temp"));
            FileUtils.copyInputStreamToFile(clientHttpResponse.getBody(), ret);
            HttpHeaders headers = clientHttpResponse.getHeaders();
            headers.forEach((k, v) -> {
                log.info("header name is: {}, value is: {}", k, v);
            });
            return ret;
        });
        Assertions.assertNotNull(file);
    }

    @Test
    void test_downloadCharacterReviewAudio() {
        String url = String.format("http://localhost:%d/word/review/character/downloadReviewAudio/%s", port, "adjective");
        File file = testRestTemplate.execute(url, HttpMethod.GET, null, clientHttpResponse -> {
            Assertions.assertFalse(clientHttpResponse.getStatusCode().isError());
            File ret = File.createTempFile("2979284", ".mp3", new File("/Users/zhanshifeng/Documents/temp"));
            FileUtil.writeFromStream(clientHttpResponse.getBody(), "/Users/zhanshifeng/Documents/temp/test_adjective.mp3");
            HttpHeaders headers = clientHttpResponse.getHeaders();
            headers.forEach((k, v) -> {
                log.info("header name is: {}, value is: {}", k, v);
            });
            return ret;
        });
        Assertions.assertNotNull(file);
    }

    @Disabled
    @Test
    void test_grammar_downloadMp3() {
        String url = String.format("http://localhost:%d/grammar/mp3/%s", port, "article");
        File file = testRestTemplate.execute(url, HttpMethod.GET, null, clientHttpResponse -> {
            Assertions.assertFalse(clientHttpResponse.getStatusCode().isError());
            File ret = File.createTempFile("grammar", ".mp3", new File("/Users/zhanshifeng/Documents/temp"));
            FileUtils.copyInputStreamToFile(clientHttpResponse.getBody(), ret);
            HttpHeaders headers = clientHttpResponse.getHeaders();
            headers.forEach((k, v) -> {
                log.info("header name is: {}, value is: {}", k, v);
            });
            return ret;
        });
        Assertions.assertNotNull(file);
    }

    @Disabled
    @Test
    void test_grammar_downloadSrt() {
        String url = String.format("http://localhost:%d/grammar/srt/%s", port, "article");
        File file = testRestTemplate.execute(url, HttpMethod.GET, null, clientHttpResponse -> {
            Assertions.assertFalse(clientHttpResponse.getStatusCode().isError());
            File ret = File.createTempFile("grammar", ".srt", new File("/Users/zhanshifeng/Documents/temp"));
            FileUtils.copyInputStreamToFile(clientHttpResponse.getBody(), ret);
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
