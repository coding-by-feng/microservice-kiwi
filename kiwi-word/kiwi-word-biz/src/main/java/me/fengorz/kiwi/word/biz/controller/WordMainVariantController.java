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

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.word.api.dto.WordMainVariantDTO;
import me.fengorz.kiwi.word.api.vo.WordMainVariantVO;
import me.fengorz.kiwi.word.biz.service.IWordMainVariantService;
import me.fengorz.kiwi.word.biz.service.operate.IWordOperateService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;


/**
 * 单词时态、单复数等的变化
 *
 * @Author ZhanShiFeng
 * @date 2020-05-24 01:20:49
 */
@Validated
@RestController
@AllArgsConstructor
@RequestMapping("/word/main/variant")
public class WordMainVariantController {

    private final IWordMainVariantService wordMainVariantService;
    private final IWordOperateService wordOperateService;

    /**
     * 分页查询
     */
    @PostMapping("/page/{current}/{size}")
    public R<IPage<WordMainVariantVO>> page(
            @PathVariable Integer current,
            @PathVariable Integer size,
            WordMainVariantDTO dto) {
        return R.success(wordMainVariantService.page(current, size, dto));
    }

    /**
     * 通过id查询
     */
    @GetMapping("/get/{id}")
    public R<WordMainVariantVO> get(@PathVariable Integer id) {
        return R.success(wordMainVariantService.getVO(id));
    }

    /**
     * 新增 or 修改
     */
    @PostMapping("/saveOne")
    public R<Void> saveOne(WordMainVariantDTO dto) {
        return R.auto(wordMainVariantService.saveOne(dto));
    }

    /**
     * 删除
     */
    @PostMapping("/del")
    public R<Void> del(@NotNull Integer id) {
        return R.auto(wordMainVariantService.removeById(id));
    }

    @PostMapping("/insertVariant")
    public R<Void> insertVariant(@NotBlank String inputWordName, @NotBlank String fetchWordName) {
        return R.auto(wordOperateService.insertVariant(inputWordName, fetchWordName));
    }

}

