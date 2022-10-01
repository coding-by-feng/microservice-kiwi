/*
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
 */
package me.fengorz.kiwi.word.biz.service.base.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import me.fengorz.kiwi.bdf.core.service.SeqService;
import me.fengorz.kiwi.common.sdk.annotation.cache.KiwiCacheKeyPrefix;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.util.bean.KiwiBeanUtils;
import me.fengorz.kiwi.common.sdk.util.lang.collection.KiwiCollectionUtils;
import me.fengorz.kiwi.word.api.common.WordConstants;
import me.fengorz.kiwi.word.api.dto.WordMainVariantDTO;
import me.fengorz.kiwi.word.api.entity.WordMainDO;
import me.fengorz.kiwi.word.api.entity.WordMainVariantDO;
import me.fengorz.kiwi.word.api.vo.WordMainVariantVO;
import me.fengorz.kiwi.word.biz.mapper.WordMainVariantMapper;
import me.fengorz.kiwi.word.biz.service.base.WordFetchQueueService;
import me.fengorz.kiwi.word.biz.service.base.WordMainService;
import me.fengorz.kiwi.word.biz.service.base.WordMainVariantService;

/**
 * 单词时态、单复数等的变化 @Author zhanshifeng
 *
 * @date 2020-05-24 01:20:49
 */
@Service()
@RequiredArgsConstructor
@KiwiCacheKeyPrefix(WordConstants.CACHE_KEY_PREFIX_WORD_VARIANT.CLASS)
public class WordMainVariantServiceImpl extends ServiceImpl<WordMainVariantMapper, WordMainVariantDO>
    implements WordMainVariantService {

    private final WordMainVariantMapper wordMainVariantMapper;
    private final WordFetchQueueService wordFetchQueueService;
    private final WordMainService wordMainService;
    private final SeqService seqService;

    @Override
    public IPage<WordMainVariantVO> page(int current, int size, WordMainVariantDTO dto) {
        IPage<WordMainVariantDO> page =
            wordMainVariantMapper.selectPage(new Page<>(current, size), Wrappers.query(dto));
        return KiwiBeanUtils.convertFrom(page, WordMainVariantVO.class, vo -> {
        });
    }

    @Override
    public WordMainVariantVO getVO(Integer id) {
        return KiwiBeanUtils.convertFrom(wordMainVariantMapper.selectById(id), WordMainVariantVO.class);
    }

    @Override
    public Integer getWordId(String variantName) {
        List<Integer> result = new ArrayList<>();
        WordMainVariantDO one = wordMainVariantMapper
            .selectOne(Wrappers.<WordMainVariantDO>lambdaQuery().eq(WordMainVariantDO::getVariantName, variantName)
                .eq(WordMainVariantDO::getIsValid, GlobalConstants.FLAG_YES));
        return one == null ? null : one.getWordId();
    }

    @Override
    public List<WordMainDO> listWordMain(String variantName, Integer queueId) {
        WordMainVariantDO one = wordMainVariantMapper
            .selectOne(Wrappers.<WordMainVariantDO>lambdaQuery().eq(WordMainVariantDO::getVariantName, variantName)
                .eq(WordMainVariantDO::getIsValid, GlobalConstants.FLAG_YES));
        AtomicReference<List<WordMainDO>> result = new AtomicReference<>();
        if (one == null) {
            if (queueId == null) {
                return null;
            }
            Optional.ofNullable(wordFetchQueueService.getById(queueId))
                .ifPresent(queue -> result.set(wordMainService.listDirtyData(queue.getWordId())));
            return result.get();
        }
        return wordMainService.listDirtyData(one.getWordId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOne(WordMainVariantDTO dto) {
        final Integer id = dto.getId();
        boolean isInsert = id == null || !isExist(id);

        // TODO ZSF 校验新增和修改看下怎么通过注解来自动识别分组校验
        if (isInsert) {
            return this.save(dto);
        } else {
            return this.updateById(dto);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delByWordId(Integer wordId) {
        List<WordMainVariantDO> list = wordMainVariantMapper.selectList(new LambdaQueryWrapper<WordMainVariantDO>()
            .eq(WordMainVariantDO::getWordId, wordId).eq(WordMainVariantDO::getIsValid, GlobalConstants.FLAG_DEL_YES));
        if (KiwiCollectionUtils.isEmpty(list)) {
            return;
        }

        for (WordMainVariantDO variantDO : list) {
            wordMainVariantMapper.deleteById(variantDO.getId());
        }
    }

    @Override
    public boolean isExist(Integer id) {
        return this.getById(id) != null;
    }

    @Override
    public boolean isExist(Integer wordId, String variantName) {
        Integer count = wordMainVariantMapper.selectCount(Wrappers.<WordMainVariantDO>lambdaQuery()
            .eq(WordMainVariantDO::getWordId, wordId).eq(WordMainVariantDO::getVariantName, variantName)
            .eq(WordMainVariantDO::getIsValid, GlobalConstants.FLAG_DEL_YES));
        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertOne(Integer wordId, String variantName) {
        return this.insertOne(wordId, variantName, WordConstants.VARIANT_TYPE_UNKNOWN);
    }

    @Transactional(rollbackFor = Exception.class)
    private boolean insertOne(Integer wordId, String variantName, Integer type) {
        WordMainVariantDO entity =
            new WordMainVariantDO().setId(seqService.genCommonIntSequence()).setWordId(wordId)
                .setVariantName(variantName).setType(WordConstants.VARIANT_TYPE_UNKNOWN)
                .setIsValid(GlobalConstants.FLAG_DEL_YES);
        return this.save(entity);
    }
}
