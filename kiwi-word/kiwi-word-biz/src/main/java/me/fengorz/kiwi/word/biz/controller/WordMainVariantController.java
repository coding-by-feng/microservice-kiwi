/*
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
 */
package me.fengorz.kiwi.word.biz.controller;

import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.word.biz.service.operate.IOperateService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 单词时态、单复数等的变化
 *
 * @Author zhanshifeng
 * @date 2020-05-24 01:20:49
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/word/main/variant")
public class WordMainVariantController {

    private final IOperateService wordOperateService;

    @GetMapping("/insertVariant/{inputWordName}/{fetchWordName}")
    public R<Void> insertVariant(@PathVariable String inputWordName, @PathVariable String fetchWordName) {
        return R.auto(wordOperateService.insertVariant(inputWordName, fetchWordName));
    }

}
