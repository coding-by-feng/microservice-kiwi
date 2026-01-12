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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.common.R;
import me.fengorz.kiwi.domain.word.entity.FetchQueue;
import me.fengorz.kiwi.domain.word.service.FetchQueueService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * FetchQueue REST Controller
 *
 * @author codingByFeng
 */
@RestController
@RequestMapping("/api/word/fetch")
@RequiredArgsConstructor
@Tag(name = "Fetch Queue", description = "Word fetching queue operations")
public class FetchQueueController {

    private final FetchQueueService fetchQueueService;

    @GetMapping("/{queueId}")
    @Operation(summary = "Get queue item by ID")
    public R<FetchQueue> getById(@PathVariable Integer queueId) {
        return fetchQueueService.findById(queueId)
                .map(R::ok)
                .orElse(R.failed("Queue item not found"));
    }

    @GetMapping("/word/{wordName}")
    @Operation(summary = "Get queue item by word name")
    public R<FetchQueue> getByWordName(@PathVariable String wordName) {
        return fetchQueueService.findByWordName(wordName)
                .map(R::ok)
                .orElse(R.failed("Queue item not found"));
    }

    @GetMapping("/waiting")
    @Operation(summary = "Get waiting items with pagination")
    public R<Page<FetchQueue>> getWaitingItems(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        Page<FetchQueue> page = new Page<>(current, size);
        return R.ok(fetchQueueService.findWaitingItems(page));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get queue statistics")
    public R<Map<String, Long>> getStats() {
        return R.ok(Map.of(
                "waiting", fetchQueueService.countByStatus(FetchQueue.STATUS_WAITING),
                "fetching", fetchQueueService.countByStatus(FetchQueue.STATUS_FETCHING),
                "success", fetchQueueService.countByStatus(FetchQueue.STATUS_SUCCESS),
                "fail", fetchQueueService.countByStatus(FetchQueue.STATUS_FAIL)
        ));
    }

    @PostMapping
    @Operation(summary = "Add word to fetch queue")
    public R<FetchQueue> addToQueue(@RequestParam String wordName,
                                     @RequestParam(required = false, defaultValue = "100") Integer priority) {
        FetchQueue queue = fetchQueueService.addToQueue(wordName, priority);
        return R.ok(queue);
    }

    @PostMapping("/{queueId}/success")
    @Operation(summary = "Mark fetch as success")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> markSuccess(@PathVariable Integer queueId, @RequestParam Integer wordId) {
        fetchQueueService.markSuccess(queueId, wordId);
        return R.ok();
    }

    @PostMapping("/{queueId}/fail")
    @Operation(summary = "Mark fetch as failed")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> markFailed(@PathVariable Integer queueId, @RequestParam String errorMessage) {
        fetchQueueService.markFailed(queueId, errorMessage);
        return R.ok();
    }

    @PostMapping("/reset-failed")
    @Operation(summary = "Reset failed items for retry")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> resetFailed(@RequestParam(defaultValue = "3") int maxRetries) {
        fetchQueueService.resetFailedItems(maxRetries);
        return R.ok();
    }

    @DeleteMapping("/{queueId}")
    @Operation(summary = "Delete queue item")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Integer queueId) {
        fetchQueueService.deleteById(queueId);
        return R.ok();
    }
}
