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
import me.fengorz.kiwi.common.R;
import me.fengorz.kiwi.domain.word.mapper.WordVOMapper;
import me.fengorz.kiwi.domain.word.service.PronunciationService;
import me.fengorz.kiwi.domain.word.vo.PronunciationVO;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Pronunciation Controller
 *
 * @author codingByFeng
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/word/pronunciation")
@Tag(name = "Pronunciation", description = "Word pronunciation operations")
public class PronunciationController {

    private final PronunciationService pronunciationService;
    private final WordVOMapper wordVOMapper;

    @GetMapping("/{pronunciationId}")
    @Operation(summary = "Get pronunciation by ID")
    public R<PronunciationVO> getById(@PathVariable Integer pronunciationId) {
        return pronunciationService.findById(pronunciationId)
                .map(entity -> R.ok(wordVOMapper.toPronunciationVO(entity)))
                .orElse(R.failed("Pronunciation not found"));
    }

    @GetMapping("/word/{wordId}")
    @Operation(summary = "Get pronunciations by word ID")
    public R<List<PronunciationVO>> getByWordId(@PathVariable Integer wordId) {
        return R.ok(wordVOMapper.toPronunciationVOList(pronunciationService.findByWordId(wordId)));
    }

    @GetMapping("/character/{characterId}")
    @Operation(summary = "Get pronunciations by character ID")
    public R<List<PronunciationVO>> getByCharacterId(@PathVariable Integer characterId) {
        return R.ok(wordVOMapper.toPronunciationVOList(pronunciationService.findByCharacterId(characterId)));
    }

    @GetMapping(value = "/download/{pronunciationId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Download pronunciation audio")
    public void downloadVoice(HttpServletResponse response,
                              @PathVariable Integer pronunciationId) {
        pronunciationService.findById(pronunciationId).ifPresentOrElse(
                pronunciation -> {
                    // TODO: Implement DFS file download
                    log.info("Pronunciation found: {}", pronunciation);
                    response.setContentType("audio/mpeg");
                    response.setHeader("Content-Disposition",
                            "attachment; filename=\"pronunciation-" + pronunciationId + ".mp3\"");
                    // byte[] bytes = dfsService.downloadFile(pronunciation.getGroupName(), pronunciation.getVoiceFilePath());
                    // response.getOutputStream().write(bytes);
                },
                () -> {
                    log.error("Pronunciation not found: {}", pronunciationId);
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
        );
    }
}
