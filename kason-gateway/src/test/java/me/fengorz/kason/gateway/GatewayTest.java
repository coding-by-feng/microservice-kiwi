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

package me.fengorz.kason.gateway;

import cn.hutool.http.Header;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kason.common.api.R;
import me.fengorz.kason.common.sdk.constant.GlobalConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.concurrent.TimeUnit;

@Slf4j
@ExtendWith(SpringExtension.class)
public class GatewayTest {

    private final TestRestTemplate testRestTemplate = new TestRestTemplate();

    @Test
    void test_gateway() {
        ResponseEntity<R> response =
            testRestTemplate.getForEntity("http://localhost:9991/wordBiz/word/main/query/test", R.class);
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(response.getStatusCode(), HttpStatus.OK);
        log.info("Data is: {}", response.getBody());
        HttpHeaders headers = response.getHeaders();
        headers.forEach((k, v) -> {
            log.info("header name is: {}, value is: {}", k, v);
        });
        Assertions.assertFalse(headers.containsKey(Header.PRAGMA.toString()));
        Assertions.assertFalse(headers.containsKey(GlobalConstants.HEADERS.HEADER_EXPIRES_UPPER_CASE));
        Assertions.assertFalse(headers.containsKey(GlobalConstants.HEADERS.HEADER_EXPIRES_LOWER_CASE));
        Assertions.assertTrue(headers.containsKey(Header.CACHE_CONTROL.toString()));
        Assertions.assertEquals(headers.get(Header.CACHE_CONTROL.toString()).size(), 1);
        Assertions.assertEquals(headers.get(Header.CACHE_CONTROL.toString()).get(0),
                CacheControl.maxAge(365, TimeUnit.DAYS).getHeaderValue());

    }

    @Test
    @Disabled
    void test_fuzzyQueryList() {
        ResponseEntity<R> response =
            testRestTemplate.getForEntity("http://localhost:9991/wordBiz/word/main/fuzzyQueryList/te", R.class);
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(response.getStatusCode(), HttpStatus.OK);
        log.info("Data is: {}", response.getBody());
        HttpHeaders headers = response.getHeaders();
        headers.forEach((k, v) -> {
            log.info("header name is: {}, value is: {}", k, v);
        });
        Assertions.assertFalse(headers.containsKey(Header.PRAGMA.toString()));
        Assertions.assertFalse(headers.containsKey(GlobalConstants.HEADERS.HEADER_EXPIRES_UPPER_CASE));
        Assertions.assertFalse(headers.containsKey(GlobalConstants.HEADERS.HEADER_EXPIRES_LOWER_CASE));
        Assertions.assertTrue(headers.containsKey(Header.CACHE_CONTROL.toString()));
    }

}