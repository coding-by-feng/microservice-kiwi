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
package me.fengorz.kason.word.biz.service.base.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.fengorz.kason.common.sdk.annotation.cache.KasonCacheKeyPrefix;
import me.fengorz.kason.common.sdk.util.bean.KasonBeanUtils;
import me.fengorz.kason.word.api.common.WordConstants;
import me.fengorz.kason.word.api.entity.CharacterDO;
import me.fengorz.kason.word.api.vo.detail.CharacterVO;
import me.fengorz.kason.word.biz.mapper.CharacterMapper;
import me.fengorz.kason.word.biz.service.base.CharacterService;
import org.springframework.stereotype.Service;

/**
 * 单词词性表
 *
 * @author zhanshifeng
 * @date 2019-10-31 20:38:37
 */
@Service()
@KasonCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_CHARACTER.CLASS)
public class CharacterServiceImpl extends ServiceImpl<CharacterMapper, CharacterDO> implements CharacterService {

    @Override
    public CharacterVO get(Integer characterId) {
        return KasonBeanUtils.convertFrom(this.getById(characterId), CharacterVO.class);
    }

    @Override
    public void evict(Integer characterId) {}
}
