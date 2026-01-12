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
package me.fengorz.kiwi.api.word;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;

/**
 * Grammar Controller
 * Serves grammar audio and subtitle files
 *
 * @author codingByFeng
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/grammar")
@Tag(name = "Grammar", description = "Grammar resources operations")
public class GrammarController {

    private static final String FOLDER = "grammar";
    private static final String MP3 = ".mp3";
    private static final String SRT = ".srt";

    @GetMapping(value = "/mp3/{type}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Download grammar MP3 audio")
    public void downloadMp3(HttpServletResponse response, @PathVariable String type) {
        try {
            ClassPathResource resource = new ClassPathResource(FOLDER + "/" + type + MP3);
            InputStream inputStream = resource.getInputStream();

            response.setContentType("audio/mpeg");
            response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
            response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(inputStream.available()));

            StreamUtils.copy(inputStream, response.getOutputStream());
            inputStream.close();
        } catch (IOException e) {
            log.error("Error downloading grammar MP3: {}", type, e);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @GetMapping(value = "/srt/{type}", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Download grammar SRT subtitle")
    public void downloadSrt(HttpServletResponse response, @PathVariable String type) {
        try {
            ClassPathResource resource = new ClassPathResource(FOLDER + "/" + type + SRT);
            InputStream inputStream = resource.getInputStream();

            response.setContentType("text/plain; charset=UTF-8");
            response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
            response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(inputStream.available()));

            StreamUtils.copy(inputStream, response.getOutputStream());
            inputStream.close();
        } catch (IOException e) {
            log.error("Error downloading grammar SRT: {}", type, e);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
