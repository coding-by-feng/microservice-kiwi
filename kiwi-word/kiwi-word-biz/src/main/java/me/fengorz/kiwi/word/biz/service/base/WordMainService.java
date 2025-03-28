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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import me.fengorz.kiwi.word.api.dto.mapper.out.FuzzyQueryResultDTO;
import me.fengorz.kiwi.word.api.entity.WordMainDO;
import me.fengorz.kiwi.word.api.vo.WordMainVO;

import java.util.List;

/**
 * 单词主表
 *
 * @author zhanshifeng
 * @date 2019-10-31 20:32:07
 */
public interface WordMainService extends IService<WordMainDO> {

    WordMainVO getOneAndCatch(String wordName, Integer... infoType);

    String getWordName(Integer id);

    List<FuzzyQueryResultDTO> fuzzyQueryList(Page<WordMainDO> page, String wordName);

    boolean isExist(String wordName);

    void evictById(Integer id);

    List<WordMainDO> list(String wordName, Integer infoType);

    List<WordMainDO> listDirtyData(Integer wordId);

    List<String> listOverlapAnyway();

}
