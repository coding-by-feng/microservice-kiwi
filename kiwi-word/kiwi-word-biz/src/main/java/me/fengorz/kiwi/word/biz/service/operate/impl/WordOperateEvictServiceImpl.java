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

package me.fengorz.kiwi.word.biz.service.operate.impl;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.common.api.annotation.cache.KiwiCacheKey;
import me.fengorz.kiwi.common.api.annotation.cache.KiwiCacheKeyPrefix;
import me.fengorz.kiwi.common.api.constant.CacheConstants;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.common.sdk.util.lang.collection.KiwiCollectionUtils;
import me.fengorz.kiwi.word.api.common.WordConstants;
import me.fengorz.kiwi.word.api.entity.WordParaphraseDO;
import me.fengorz.kiwi.word.api.vo.WordMainVO;
import me.fengorz.kiwi.word.biz.service.IWordMainService;
import me.fengorz.kiwi.word.biz.service.IWordParaphraseService;
import me.fengorz.kiwi.word.biz.service.operate.IWordOperateEvictService;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2020/5/31 9:03 PM
 */
@Service
@RequiredArgsConstructor
@KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_OPERATE.CLASS)
public class WordOperateEvictServiceImpl implements IWordOperateEvictService {

    private final IWordMainService wordMainService;
    private final IWordParaphraseService wordParaphraseService;

    @Override
    @KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_OPERATE.METHOD_WORD_NAME)
    @CacheEvict(cacheNames = WordConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN)
    public void evict(String wordName) {
        WordMainVO one = wordMainService.getOne(wordName);
        if (one == null) {
            return;
        }
        List<WordParaphraseDO> list = wordParaphraseService
            .list(Wrappers.<WordParaphraseDO>lambdaQuery().eq(WordParaphraseDO::getWordId, one.getWordId())
                .eq(WordParaphraseDO::getIsDel, CommonConstants.FLAG_DEL_NO));
        if (KiwiCollectionUtils.isNotEmpty(list)) {
            for (WordParaphraseDO paraphraseDO : list) {
                this.evictParaphrase(paraphraseDO.getParaphraseId());
            }
        }
    }

    @KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_OPERATE.METHOD_PARAPHRASE_ID)
    @CacheEvict(cacheNames = WordConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN)
    private void evictParaphrase(@KiwiCacheKey Integer paraphraseId) {}
}
