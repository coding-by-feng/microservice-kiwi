/*
 *
 *   Copyright [2019~2025] [zhanshifeng]
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
import me.fengorz.kiwi.common.api.annotation.cache.KiwiCacheKey;
import me.fengorz.kiwi.common.api.annotation.cache.KiwiCacheKeyPrefix;
import me.fengorz.kiwi.common.api.constant.CacheConstants;
import me.fengorz.kiwi.common.sdk.util.bean.KiwiBeanUtils;
import me.fengorz.kiwi.word.api.common.WordConstants;
import me.fengorz.kiwi.word.api.entity.WordCharacterDO;
import me.fengorz.kiwi.word.api.vo.detail.WordCharacterVO;
import me.fengorz.kiwi.word.biz.mapper.WordCharacterMapper;
import me.fengorz.kiwi.word.biz.service.IWordCharacterService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 单词词性表
 *
 * @author zhanshifeng
 * @date 2019-10-31 20:38:37
 */
@Service()
@KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_CHARACTER.CLASS)
public class WordCharacterServiceImpl extends ServiceImpl<WordCharacterMapper, WordCharacterDO> implements IWordCharacterService {

    @Override
    @KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_CHARACTER.METHOD_ID)
    @Cacheable(cacheNames = WordConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN, unless = "#result == null")
    public WordCharacterVO getFromCache(@KiwiCacheKey Integer characterId) {
        return KiwiBeanUtils.convertFrom(this.getById(characterId), WordCharacterVO.class);
    }

    @Override
    @KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_CHARACTER.METHOD_ID)
    @CacheEvict(cacheNames = WordConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN)
    public void evict(@KiwiCacheKey Integer characterId) {
    }

}
