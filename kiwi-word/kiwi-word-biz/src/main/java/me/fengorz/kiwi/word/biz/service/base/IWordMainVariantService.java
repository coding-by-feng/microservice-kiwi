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
package me.fengorz.kiwi.word.biz.service.base;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import me.fengorz.kiwi.word.api.dto.WordMainVariantDTO;
import me.fengorz.kiwi.word.api.entity.WordMainDO;
import me.fengorz.kiwi.word.api.entity.WordMainVariantDO;
import me.fengorz.kiwi.word.api.vo.WordMainVariantVO;

import java.util.List;

/**
 * 服务类 @Author zhanshifeng
 *
 * @date 2020-05-24 01:40:36
 */
public interface IWordMainVariantService extends IService<WordMainVariantDO> {

    IPage<WordMainVariantVO> page(int current, int size, WordMainVariantDTO dto);

    WordMainVariantVO getVO(Integer id);

    Integer getWordId(String variantName);

    List<WordMainDO> listWordMain(String variantName, Integer queueId);

    boolean saveOne(WordMainVariantDTO dto);

    void delByWordId(Integer wordId);

    boolean isExist(Integer id);

    boolean isExist(Integer wordId, String variantName);

    boolean insertOne(Integer wordId, String variantName);
}
