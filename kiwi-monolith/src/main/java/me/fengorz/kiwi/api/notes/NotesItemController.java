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
package me.fengorz.kiwi.api.notes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.R;
import me.fengorz.kiwi.domain.notes.dto.NotesAudioRequest;
import me.fengorz.kiwi.domain.notes.dto.NotesImageRequest;
import me.fengorz.kiwi.domain.notes.dto.NotesItemRequest;
import me.fengorz.kiwi.domain.notes.service.NotesAudioService;
import me.fengorz.kiwi.domain.notes.service.NotesImageService;
import me.fengorz.kiwi.domain.notes.service.NotesItemService;
import me.fengorz.kiwi.domain.notes.service.NotesLockService;
import me.fengorz.kiwi.domain.notes.vo.ImageStyleVO;
import me.fengorz.kiwi.domain.notes.vo.NotesItemVO;
import me.fengorz.kiwi.security.KiwiUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Notes Item Controller
 *
 * @author codingByFeng
 */
@Slf4j
@RestController
@RequestMapping("/api/notes/item")
@RequiredArgsConstructor
@Tag(name = "Notes Item", description = "Private notes item management with audio/image")
public class NotesItemController {

    private final NotesItemService itemService;
    private final NotesAudioService audioService;
    private final NotesImageService imageService;
    private final NotesLockService lockService;

    // ==================== CRUD Operations ====================

    @GetMapping("/list/{categoryId}")
    @Operation(summary = "List items by category")
    public R<List<NotesItemVO>> listItems(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable Long categoryId) {
        lockService.verifyUnlocked(user.getUserId());
        return R.ok(itemService.listByCategoryId(categoryId, user.getUserId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get item by ID")
    public R<NotesItemVO> getItem(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable Long id) {
        lockService.verifyUnlocked(user.getUserId());
        NotesItemVO item = itemService.getByIdAndUser(id, user.getUserId());
        if (item == null) {
            return R.failed("Note item not found");
        }
        return R.ok(item);
    }

    @PostMapping
    @Operation(summary = "Create new note item")
    public R<NotesItemVO> createItem(
            @AuthenticationPrincipal KiwiUser user,
            @RequestBody @Validated NotesItemRequest request) {
        lockService.verifyUnlocked(user.getUserId());
        return R.ok(itemService.create(request, user.getUserId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update note item")
    public R<NotesItemVO> updateItem(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable Long id,
            @RequestBody @Validated NotesItemRequest request) {
        lockService.verifyUnlocked(user.getUserId());
        return R.ok(itemService.update(id, request, user.getUserId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete note item (physical delete)")
    public R<Void> deleteItem(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable Long id) {
        lockService.verifyUnlocked(user.getUserId());
        itemService.delete(id, user.getUserId());
        return R.ok();
    }

    // ==================== Navigation ====================

    @GetMapping("/{id}/next")
    @Operation(summary = "Get next item in category (supports loop)")
    public R<NotesItemVO> getNextItem(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable Long id) {
        lockService.verifyUnlocked(user.getUserId());
        NotesItemVO next = itemService.getNextItem(id, user.getUserId());
        if (next == null) {
            return R.failed("No next item found");
        }
        return R.ok(next);
    }

    @GetMapping("/{id}/previous")
    @Operation(summary = "Get previous item in category (supports loop)")
    public R<NotesItemVO> getPreviousItem(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable Long id) {
        lockService.verifyUnlocked(user.getUserId());
        NotesItemVO prev = itemService.getPreviousItem(id, user.getUserId());
        if (prev == null) {
            return R.failed("No previous item found");
        }
        return R.ok(prev);
    }

    @PutMapping("/reorder/{categoryId}")
    @Operation(summary = "Reorder items in category")
    public R<Void> reorderItems(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable Long categoryId,
            @RequestBody List<Long> itemIds) {
        lockService.verifyUnlocked(user.getUserId());
        itemService.reorder(categoryId, itemIds, user.getUserId());
        return R.ok();
    }

    // ==================== Audio Operations ====================

    @PostMapping("/audio/generate")
    @Operation(summary = "Generate audio for note item using TTS")
    public R<NotesItemVO> generateAudio(
            @AuthenticationPrincipal KiwiUser user,
            @RequestBody @Validated NotesAudioRequest request) {
        lockService.verifyUnlocked(user.getUserId());
        return R.ok(audioService.generateAudio(request, user.getUserId()));
    }

    @GetMapping("/{id}/audio/stream")
    @Operation(summary = "Stream audio file for note item")
    public ResponseEntity<byte[]> streamAudio(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable Long id) {
        lockService.verifyUnlocked(user.getUserId());
        try {
            byte[] audioBytes = audioService.getAudioBytes(id, user.getUserId());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("audio/mpeg"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"note_" + id + ".mp3\"")
                    .body(audioBytes);
        } catch (Exception e) {
            log.error("Failed to stream audio for note {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/audio")
    @Operation(summary = "Delete audio for note item")
    public R<Void> deleteAudio(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable Long id) {
        lockService.verifyUnlocked(user.getUserId());
        audioService.deleteAudio(id, user.getUserId());
        return R.ok();
    }

    // ==================== Image Operations ====================

    @GetMapping("/image/styles")
    @Operation(summary = "List available image generation styles")
    public R<List<ImageStyleVO>> listImageStyles() {
        return R.ok(imageService.listAvailableStyles());
    }

    @PostMapping("/image/generate")
    @Operation(summary = "Generate image for note item via Gemini Imagen")
    public R<NotesItemVO> generateImage(
            @AuthenticationPrincipal KiwiUser user,
            @RequestBody @Validated NotesImageRequest request) {
        lockService.verifyUnlocked(user.getUserId());
        return R.ok(imageService.generateImage(request, user.getUserId()));
    }

    @GetMapping("/{id}/image/stream")
    @Operation(summary = "Stream image file for note item")
    public ResponseEntity<byte[]> streamImage(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable Long id) {
        lockService.verifyUnlocked(user.getUserId());
        try {
            byte[] imageBytes = imageService.getImageBytes(id, user.getUserId());
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"note_" + id + ".png\"")
                    .body(imageBytes);
        } catch (Exception e) {
            log.error("Failed to stream image for note {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/image")
    @Operation(summary = "Delete image for note item")
    public R<Void> deleteImage(
            @AuthenticationPrincipal KiwiUser user,
            @PathVariable Long id) {
        lockService.verifyUnlocked(user.getUserId());
        imageService.deleteImage(id, user.getUserId());
        return R.ok();
    }
}
