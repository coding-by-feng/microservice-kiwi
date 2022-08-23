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

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.common.sdk.controller.BaseController;
import me.fengorz.kiwi.word.biz.service.base.ICharacterService;
import me.fengorz.kiwi.word.biz.service.operate.OperateService;

/**
 * 单词词性表
 *
 * @author zhanshifeng
 * @date 2019-10-31 20:38:37
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/word/character")
public class CharacterController extends BaseController {

    private final ICharacterService wordCharacterService;
    private final OperateService wordOperateService;
}
