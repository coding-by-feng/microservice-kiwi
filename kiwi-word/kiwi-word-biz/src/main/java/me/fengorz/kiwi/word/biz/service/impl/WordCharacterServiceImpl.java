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
import me.fengorz.kiwi.word.api.entity.WordCharacterDO;
import me.fengorz.kiwi.word.biz.mapper.WordCharacterMapper;
import me.fengorz.kiwi.word.biz.service.IWordCharacterService;
import org.springframework.stereotype.Service;

/**
 * 单词词性表
 *
 * @author codingByFeng
 * @date 2019-10-31 20:38:37
 */
@Service("wordCharacterService")
public class WordCharacterServiceImpl extends ServiceImpl<WordCharacterMapper, WordCharacterDO> implements IWordCharacterService {

}