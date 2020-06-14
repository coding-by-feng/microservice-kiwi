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
package me.fengorz.kiwi.word.biz.service.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.ibatis.exceptions.TooManyResultsException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.common.api.annotation.cache.KiwiCacheKey;
import me.fengorz.kiwi.common.api.annotation.cache.KiwiCacheKeyPrefix;
import me.fengorz.kiwi.common.api.constant.CacheConstants;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.common.sdk.util.bean.KiwiBeanUtils;
import me.fengorz.kiwi.common.sdk.util.lang.collection.KiwiCollectionUtils;
import me.fengorz.kiwi.word.api.common.WordConstants;
import me.fengorz.kiwi.word.api.entity.WordMainDO;
import me.fengorz.kiwi.word.api.vo.WordMainVO;
import me.fengorz.kiwi.word.biz.mapper.WordMainMapper;
import me.fengorz.kiwi.word.biz.service.IWordMainService;

/**
 * 单词主表
 *
 * @author zhanshifeng
 * @date 2019-10-31 20:32:07
 */
@Service()
@RequiredArgsConstructor
@KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_WORD_MAIN.CLASS)
public class WordMainServiceImpl extends ServiceImpl<WordMainMapper, WordMainDO> implements IWordMainService {

    public static final String VALUE = "value";

    @Override
    public boolean save(WordMainDO entity) {
        return super.save(entity);
    }

    @Override
    @KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_WORD_MAIN.METHOD_ID)
    @Cacheable(cacheNames = WordConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN,
        unless = "#result == null")
    public WordMainDO getById(@KiwiCacheKey Serializable id) {
        return super.getById(id);
    }

    @Override
    @KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_WORD_MAIN.METHOD_NAME)
    @Cacheable(cacheNames = WordConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN,
        unless = "#result == null")
    public WordMainVO getOne(@KiwiCacheKey String wordName) {
        // TODO ZSF isDel要改成tinyint类型
        try {
            return KiwiBeanUtils.convertFrom(this.getOne(new LambdaQueryWrapper<WordMainDO>()
                .eq(WordMainDO::getWordName, wordName).eq(WordMainDO::getIsDel, CommonConstants.FLAG_DEL_NO)),
                WordMainVO.class);
        } catch (TooManyResultsException e) {
            log.error(e.getMessage());
            this.remove(Wrappers.<WordMainDO>lambdaQuery().eq(WordMainDO::getWordName, wordName));
        }
        return null;
    }

    @Override
    public String getWordName(Integer id) {
        WordMainDO word = this.getById(id);
        if (word == null) {
            return null;
        }
        return word.getWordName();
    }

    @Override
    public List<Map> fuzzyQueryList(Page page, String wordName) {
        LambdaQueryWrapper queryWrapper = new LambdaQueryWrapper<WordMainDO>()
            .likeRight(WordMainDO::getWordName, wordName).eq(WordMainDO::getIsDel, CommonConstants.FLAG_DEL_NO)
            .orderByAsc(WordMainDO::getWordName).select(WordMainDO::getWordName);

        List<WordMainDO> records = this.page(page, queryWrapper).getRecords();
        if (KiwiCollectionUtils.isEmpty(records)) {
            return Collections.emptyList();
        }

        return records.parallelStream()
            .map(wordMainDO -> KiwiCollectionUtils.putAndReturn(new HashMap<>(), VALUE, wordMainDO.getWordName()))
            .collect(Collectors.toList());
    }

    @Override
    public boolean isExist(String wordName) {
        return this.getOne(wordName) != null;
    }

    @Override
    @KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_WORD_MAIN.METHOD_NAME)
    @CacheEvict(cacheNames = WordConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN)
    public void evictByName(@KiwiCacheKey String wordName) {}

    @Override
    @KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_WORD_MAIN.METHOD_ID)
    @CacheEvict(cacheNames = WordConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN)
    public void evictById(@KiwiCacheKey Integer id) {}
}
