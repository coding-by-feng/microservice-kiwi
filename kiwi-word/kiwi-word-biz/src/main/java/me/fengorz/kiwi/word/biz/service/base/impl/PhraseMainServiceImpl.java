/*
 * Copyright [2019~2025] [codingByFeng]
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
package me.fengorz.kiwi.word.biz.service.base.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import me.fengorz.kiwi.word.api.entity.PhraseMainDO;
import me.fengorz.kiwi.word.biz.mapper.PhraseMainMapper;
import me.fengorz.kiwi.word.biz.service.base.IPhraseMainService;

/**
 * @author zhanShiFeng
 * @date 2020-10-10 20:09:06
 */
@Service
public class PhraseMainServiceImpl extends ServiceImpl<PhraseMainMapper, PhraseMainDO> implements IPhraseMainService {}
