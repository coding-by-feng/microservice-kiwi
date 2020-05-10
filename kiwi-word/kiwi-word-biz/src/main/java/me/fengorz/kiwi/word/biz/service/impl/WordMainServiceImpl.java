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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.word.api.entity.WordMainDO;
import me.fengorz.kiwi.word.biz.mapper.WordMainMapper;
import me.fengorz.kiwi.word.biz.service.IWordMainService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 单词主表
 *
 * @author codingByFeng
 * @date 2019-10-31 20:32:07
 */
@Service("wordMainService")
@AllArgsConstructor
public class WordMainServiceImpl extends ServiceImpl<WordMainMapper, WordMainDO> implements IWordMainService {

    @Override
    public WordMainDO getOneByWordName(String wordName) {
        return this.getOne(
                new QueryWrapper<>(
                        new WordMainDO()
                                .setWordName(wordName)
                                .setIsDel(CommonConstants.FALSE)
                )
        );
    }

    @Override
    public List<WordMainDO> fuzzyQueryList(Page page, String wordName) {
        // 这个查询不去全部字典，只查询word_name
        QueryWrapper<WordMainDO> wordMainQueryWrapper = new QueryWrapper<>();
        // wordMainQueryWrapper.likeRight("word_name", wordName).select("word_name");
        wordMainQueryWrapper.likeRight("word_name", wordName).eq("is_del", CommonConstants.FALSE)
                .select(WordMainDO.class, tableFieldInfo -> "word_name".equals(tableFieldInfo.getColumn()));
        return this.page(page, wordMainQueryWrapper).getRecords();
        // return this.listObjs(wordMainQueryWrapper, Object::toString);
    }
}
