/*
 *
 *   Copyright [2019~2025] [codingByFeng]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *
 */
package me.fengorz.kiwi.word.biz.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.fengorz.kiwi.word.api.entity.WordStarRelDO;
import me.fengorz.kiwi.word.biz.mapper.WordStarRelMapper;
import me.fengorz.kiwi.word.biz.service.IWordStarRelService;
import org.springframework.stereotype.Service;

/**
 * 单词本与单词的关联表
 *
 * @author codingByFeng
 * @date 2020-01-03 14:39:28
 */
@Service("WordStarRelService")
public class WordStarRelServiceImpl extends ServiceImpl<WordStarRelMapper, WordStarRelDO> implements IWordStarRelService {

}
