package me.fengorz.kiwi.word.biz.controller;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.constant.EnvConstants;
import me.fengorz.kiwi.word.biz.WordBizTestApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
@ActiveProfiles({EnvConstants.TEST})
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = WordBizTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WordMainControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testQueryGate() {
        String keyword = "test";
        String url = "http://localhost:" + port + "/word/main/query/gate/" + keyword + "?current=1&size=10";

        ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

}
