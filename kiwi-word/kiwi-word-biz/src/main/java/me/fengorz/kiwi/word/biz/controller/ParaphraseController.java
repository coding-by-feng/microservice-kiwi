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
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.annotation.log.SysLog;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.word.api.request.ParaphraseRequest;
import me.fengorz.kiwi.word.biz.service.base.IParaphraseService;
import me.fengorz.kiwi.word.biz.service.operate.IOperateService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 单词释义表
 *
 * @author zhanshifeng
 * @date 2019-10-31 20:39:48
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/word/paraphrase")
public class ParaphraseController extends BaseController {

    private final IParaphraseService service;
    private final IOperateService operateService;

    @SysLog("修改单词释义")
    @PostMapping("/modifyMeaningChinese")
    public R<Boolean> modifyMeaningChinese(@Valid ParaphraseRequest request) {
        return R.success(operateService.modifyMeaningChinese(request));
    }

}
