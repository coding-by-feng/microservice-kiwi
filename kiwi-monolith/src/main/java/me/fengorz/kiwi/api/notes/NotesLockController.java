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
import me.fengorz.kiwi.domain.notes.dto.NotesPasscodeRequest;
import me.fengorz.kiwi.domain.notes.dto.NotesUnlockRequest;
import me.fengorz.kiwi.domain.notes.service.NotesLockService;
import me.fengorz.kiwi.domain.notes.vo.NotesLockStatusVO;
import me.fengorz.kiwi.security.KiwiUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Notes Lock Controller - Passcode and lock management for notes feature
 *
 * @author codingByFeng
 */
@Slf4j
@RestController
@RequestMapping("/api/notes/lock")
@RequiredArgsConstructor
@Tag(name = "Notes Lock", description = "Passcode and lock management for private notes")
public class NotesLockController {

    private final NotesLockService lockService;

    @GetMapping("/status")
    @Operation(summary = "Get lock status")
    public R<NotesLockStatusVO> getLockStatus(@AuthenticationPrincipal KiwiUser user) {
        return R.ok(lockService.getLockStatus(user.getUserId()));
    }

    @PostMapping("/passcode")
    @Operation(summary = "Set or change passcode")
    public R<Void> setPasscode(
            @AuthenticationPrincipal KiwiUser user,
            @RequestBody @Validated NotesPasscodeRequest request) {
        lockService.setPasscode(request, user.getUserId());
        return R.ok();
    }

    @DeleteMapping("/passcode")
    @Operation(summary = "Remove passcode (requires current passcode)")
    public R<Void> removePasscode(
            @AuthenticationPrincipal KiwiUser user,
            @RequestParam String currentPasscode) {
        lockService.removePasscode(currentPasscode, user.getUserId());
        return R.ok();
    }

    @PostMapping
    @Operation(summary = "Lock notes")
    public R<Void> lock(@AuthenticationPrincipal KiwiUser user) {
        lockService.lock(user.getUserId());
        return R.ok();
    }

    @PostMapping("/unlock")
    @Operation(summary = "Unlock notes with passcode")
    public R<Void> unlock(
            @AuthenticationPrincipal KiwiUser user,
            @RequestBody @Validated NotesUnlockRequest request) {
        lockService.unlock(request, user.getUserId());
        return R.ok();
    }
}
