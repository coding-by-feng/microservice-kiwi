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
package me.fengorz.kiwi.word.biz.service.base.impl;

import java.io.Serializable;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.annotation.cache.KiwiCacheKey;
import me.fengorz.kiwi.common.sdk.annotation.cache.KiwiCacheKeyPrefix;
import me.fengorz.kiwi.common.sdk.annotation.log.LogMarker;
import me.fengorz.kiwi.common.sdk.constant.CacheConstants;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.exception.ServiceException;
import me.fengorz.kiwi.common.sdk.util.bean.KiwiBeanUtils;
import me.fengorz.kiwi.word.api.common.WordConstants;
import me.fengorz.kiwi.word.api.common.WordCrawlerConstants;
import me.fengorz.kiwi.word.api.common.enumeration.ErrorCodeEnum;
import me.fengorz.kiwi.word.api.dto.mapper.out.FuzzyQueryResultDTO;
import me.fengorz.kiwi.word.api.entity.WordMainDO;
import me.fengorz.kiwi.word.api.vo.WordMainVO;
import me.fengorz.kiwi.word.biz.mapper.WordMainMapper;
import me.fengorz.kiwi.word.biz.service.base.IWordFetchQueueService;
import me.fengorz.kiwi.word.biz.service.base.WordMainService;

/**
 * 单词主表
 *
 * @author zhanshifeng
 * @date 2019-10-31 20:32:07
 */
@Slf4j
@Service
@RequiredArgsConstructor
@KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_WORD_MAIN.CLASS)
public class WordMainServiceImpl extends ServiceImpl<WordMainMapper, WordMainDO> implements WordMainService {

    private static final String VALUE = "value";

    private final IWordFetchQueueService queueService;
    private final WordMainMapper mapper;

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
    public WordMainVO getOneAndCatch(String wordName, Integer... infoType) {
        try {
            final LambdaQueryWrapper<WordMainDO> query = Wrappers.<WordMainDO>lambdaQuery()
                .eq(WordMainDO::getWordName, wordName).eq(WordMainDO::getIsDel, GlobalConstants.FLAG_DEL_NO);
            // 如果指定infoType直接指定查询，如果不指定默认查询单词
            boolean isNotSpecialize = infoType == null || infoType.length == 0;
            WordMainDO one = this.getOne(query.clone().eq(WordMainDO::getInfoType,
                isNotSpecialize ? WordCrawlerConstants.QUEUE_INFO_TYPE_WORD : infoType[0]));
            if (one == null && isNotSpecialize) {
                one = this.getOne(query.eq(WordMainDO::getInfoType, WordCrawlerConstants.QUEUE_INFO_TYPE_PHRASE));
            }
            return KiwiBeanUtils.convertFrom(one, WordMainVO.class);
        } catch (Exception e) {
            log.error("Error in getOneAndCatch.", e);
            queueService.flagWordQueryException(wordName);
            throw new ServiceException("wordMainService.getOne error, wordName={}",
                    ErrorCodeEnum.QUERY_WORD_GET_ONE_FAILED, wordName);
        }
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
    public List<FuzzyQueryResultDTO> fuzzyQueryList(Page<WordMainDO> page, String wordName) {
        return mapper.fuzzyQuery(page, wordName + GlobalConstants.SYMBOL_PERCENT).getRecords();
    }

    @Override
    public boolean isExist(String wordName) {
        return this.getOne(Wrappers.<WordMainDO>lambdaQuery().eq(WordMainDO::getWordName, wordName)) != null;
    }

    @Override
    @KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_WORD_MAIN.METHOD_ID)
    @CacheEvict(cacheNames = WordConstants.CACHE_NAMES, keyGenerator = CacheConstants.CACHE_KEY_GENERATOR_BEAN)
    public void evictById(@KiwiCacheKey Integer id) {}

    @Override
    public List<WordMainDO> list(String wordName, Integer infoType) {
        return this.list(Wrappers.<WordMainDO>lambdaQuery().eq(WordMainDO::getWordName, wordName)
            .eq(WordMainDO::getInfoType, infoType));
    }

    @Override
    public List<WordMainDO> listDirtyData(Integer wordId) {
        WordMainDO one = this.getById(wordId);
        if (one == null) {
            return null;
        }
        return this.list(Wrappers.<WordMainDO>lambdaQuery().eq(WordMainDO::getWordName, one.getWordName())
            .eq(WordMainDO::getInfoType, one.getInfoType()));
    }

    @Override
    @LogMarker
    public List<String> listOverlapAnyway() {
        List<String> result = mapper.selectOverlapAnyway();
        log.info("listOverlapAnyway result size is {}", result.size());
        return result;
    }
}
