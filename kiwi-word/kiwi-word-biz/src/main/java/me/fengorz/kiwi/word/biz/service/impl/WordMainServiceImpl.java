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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.common.sdk.util.lang.collection.EnhancedCollectionUtils;
import me.fengorz.kiwi.word.api.entity.WordMainDO;
import me.fengorz.kiwi.word.biz.mapper.WordMainMapper;
import me.fengorz.kiwi.word.biz.service.IWordMainService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 单词主表
 *
 * @author codingByFeng
 * @date 2019-10-31 20:32:07
 */
@Service("wordMainService")
@AllArgsConstructor
public class WordMainServiceImpl extends ServiceImpl<WordMainMapper, WordMainDO> implements IWordMainService {

    public static final String VALUE = "value";

    @Override
    public boolean save(WordMainDO entity) {
        return super.save(entity);
    }

    @Override
    @Cacheable
    public WordMainDO getOneByWordName(String wordName) {
        return this.getOne(
                new LambdaQueryWrapper<WordMainDO>().eq(WordMainDO::getWordName, wordName)
                        .eq(WordMainDO::getIsDel, CommonConstants.FALSE)
        );
    }

    @Override
    public List<Map> fuzzyQueryList(Page page, String wordName) {
        LambdaQueryWrapper queryWrapper = new LambdaQueryWrapper<WordMainDO>()
                .likeRight(WordMainDO::getWordName, wordName)
                .eq(WordMainDO::getIsDel, CommonConstants.FALSE)
                .orderByAsc(WordMainDO::getWordName)
                .select(WordMainDO::getWordName);

        List<WordMainDO> records = this.page(page, queryWrapper).getRecords();
        if (EnhancedCollectionUtils.isEmpty(records)) {
            return Collections.emptyList();
        }

        return records.parallelStream()
                .map(wordMainDO ->
                        EnhancedCollectionUtils.putAndReturn(new HashMap<>(), VALUE, wordMainDO.getWordName())
                ).collect(Collectors.toList());
    }
}
