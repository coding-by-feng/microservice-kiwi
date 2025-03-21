package me.fengorz.kiwi.ai;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.entity.YtbChannelDO;
import me.fengorz.kiwi.ai.api.vo.ytb.YtbChannelVideoVO;
import me.fengorz.kiwi.common.api.ApiContants;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.constant.EnvConstants;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for YtbChannelController using a real application instance
 */
@Slf4j
@ActiveProfiles(EnvConstants.TEST)
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = AiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class YtbChannelControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * Helper method to get the base URL for API requests
     */
    private String getBaseUrl() {
        return "http://localhost:" + port + "/ai";
    }

    @Test
    public void testSubmitChannel_Success() {
        // Arrange
        String channelLinkOrName = "https%3A%2F%2Fwww.youtube.com%2Fc%2FScientificAmerican";

        // Prepare request headers and body
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("channelLinkOrName", channelLinkOrName);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        // Act
        ResponseEntity<R<Long>> response = restTemplate.exchange(
                getBaseUrl() + "/ytb/channel",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<R<Long>>() {
                }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ApiContants.RESULT_CODE_SUCCESS);
        assertThat(response.getBody().getData()).isNotNull();
        log.info("Channel ID: {}", response.getBody().getData());
    }

    @Test
    @Disabled
    public void testSubmitChannel_EmptyInput() {
        // Arrange
        String channelLinkOrName = "";

        // Prepare request headers and body
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("channelLinkOrName", channelLinkOrName);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        // Act
        ResponseEntity<R<String>> response = restTemplate.exchange(
                getBaseUrl() + "/ytb/channel",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<R<String>>() {
                }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isNotEqualTo(0); // Error code
        assertThat(response.getBody().getMsg()).contains("cannot be empty");
    }

    @Test
    @Disabled
    public void testGetUserChannelPage() {
        // First submit a channel to ensure we have data
        testSubmitChannel_Success();

        // Act
        ResponseEntity<R<IPage<YtbChannelDO>>> response = restTemplate.exchange(
                getBaseUrl() + "/ai/ytb/channel/page?current=1&size=10",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<R<IPage<YtbChannelDO>>>() {
                }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(0);
        assertThat(response.getBody().getData()).isNotNull();
        log.info("Channel count: {}", response.getBody().getData().getTotal());
    }

    @Test
    @Disabled
    public void testGetVideosByChannelId() {
        // First submit a channel to ensure we have a channel ID
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("channelLinkOrName", "https%3A%2F%2Fwww.youtube.com%2Fc%2FScientificAmerican");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        ResponseEntity<R<Long>> submitResponse = restTemplate.exchange(
                getBaseUrl() + "/ytb/channel/submit",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<R<Long>>() {
                }
        );

        assertThat(submitResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(submitResponse.getBody()).isNotNull();
        assertThat(submitResponse.getBody().getCode()).isEqualTo(0);

        Long channelId = submitResponse.getBody().getData();
        assertThat(channelId).isNotNull();

        // Act - Get videos for this channel
        ResponseEntity<R<IPage<YtbChannelVideoVO>>> response = restTemplate.exchange(
                getBaseUrl() + "/ai/ytb/channel/" + channelId + "/videos?current=1&size=10",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<R<IPage<YtbChannelVideoVO>>>() {
                }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(0);
        assertThat(response.getBody().getData()).isNotNull();
        log.info("Video count: {}", response.getBody().getData().getTotal());
    }

}