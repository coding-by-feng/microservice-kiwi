/*
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.fengorz.kiwi.word.biz.controller;

import lombok.AllArgsConstructor;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.web.security.SecurityUtils;
import me.fengorz.kiwi.word.api.vo.WordReviewDailyCounterVO;
import me.fengorz.kiwi.word.biz.service.base.IWordReviewService;
import me.fengorz.kiwi.word.biz.service.operate.IOperateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zhanShiFeng
 * @date 2021-08-19 20:42:11
 */
@RestController
@AllArgsConstructor
@RequestMapping("/word/review/")
public class WordReviewController {

    private final IWordReviewService reviewService;
    private final IOperateService operateService;

    @GetMapping("/getReviewBreakpointPageNumber/{listId}")
    public R<Integer> getReviewBreakpointPageNumber(@PathVariable Integer listId) {
        return R.success(operateService.getReviewBreakpointPageNumber(listId));
    }

    @GetMapping("/createTheDays")
    public R<Void> createTheDays() {
        reviewService.createTheDays();
        return R.success();
    }

    @GetMapping("/getVO/{type}")
    public R<WordReviewDailyCounterVO> getVO(@PathVariable("type") Integer type) {
        return R.success(reviewService.getVO(SecurityUtils.getCurrentUserId(), type));
    }

}

