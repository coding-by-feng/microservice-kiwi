/*
 *
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
 *
 *
 */
package me.fengorz.kiwi.word.biz.service.base;

import com.baomidou.mybatisplus.extension.service.IService;
import me.fengorz.kiwi.word.api.entity.CharacterDO;
import me.fengorz.kiwi.word.api.vo.detail.CharacterVO;

/**
 * 单词词性表
 *
 * @author zhanshifeng
 * @date 2019-10-31 20:38:37
 */
public interface CharacterService extends IService<CharacterDO> {

    CharacterVO get(Integer characterId);

    void evict(Integer characterId);
}
