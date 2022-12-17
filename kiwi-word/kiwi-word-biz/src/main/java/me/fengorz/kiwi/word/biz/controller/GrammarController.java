/*
 *
 * Copyright [2019~2025] [zhanshifeng]
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.common.sdk.exception.ResourceNotFoundException;
import me.fengorz.kiwi.common.sdk.util.CommonUtils;
import me.fengorz.kiwi.common.sdk.web.WebTools;
import org.apache.commons.io.IOUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/grammar/")
public class GrammarController extends BaseController {

    @GetMapping("/mp3/{type}")
    public void downloadMp3(HttpServletResponse response, @PathVariable("type") String type) {
        InputStream inputStream = null;
        try {
            inputStream = CommonUtils.getResourceFileInputStream(FOLDER + File.separator + type + MP3);
            WebTools.downloadResponseAndClose(response, inputStream, true);
        } catch (Exception e) {
            log.error("Method downloadMp3 invoked failed.", e);
            throw new ResourceNotFoundException();
        } finally {
            try {
                IOUtils.close(inputStream);
            } catch (IOException e) {
                log.error("Close input stream error!", e);
            }
        }
    }

    @GetMapping("/srt/{type}")
    public void downloadSrt(HttpServletResponse response, @PathVariable("type") String type) {
        InputStream inputStream = null;
        try {
            inputStream = CommonUtils.getResourceFileInputStream(FOLDER + File.separator + type + SRT);
            WebTools.downloadResponseAndClose(response, inputStream, true);
        } catch (Exception e) {
            log.error("Method downloadSrt invoked failed.", e);
            throw new ResourceNotFoundException();
        } finally {
            try {
                IOUtils.close(inputStream);
            } catch (IOException e) {
                log.error("Close input stream error!", e);
            }
        }
    }

    private static final String FOLDER = "grammar";
    private static final String MP3 = ".mp3";
    private static final String SRT = ".srt";

}
