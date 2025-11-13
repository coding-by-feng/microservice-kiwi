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
package me.fengorz.kason.word.biz.service.base;

import com.baomidou.mybatisplus.extension.service.IService;
import me.fengorz.kason.word.api.entity.ParaphraseDO;
import me.fengorz.kason.word.api.request.ParaphraseRequest;
import me.fengorz.kason.word.api.vo.detail.ParaphraseVO;

import java.util.List;

/**
 * 单词释义表
 *
 * @author zhanshifeng
 * @date 2019-10-31 20:39:48
 */
public interface ParaphraseService extends IService<ParaphraseDO> {

    Integer countById(Integer id);

    @Deprecated
    List<ParaphraseVO> selectParaphraseAndIsCollect(Integer characterId, Integer currentUserId);

    List<ParaphraseVO> listPhrase(Integer wordId);

    List<ParaphraseDO> listByWordName(String wordName);

    void delByWordId(Integer wordId);

    boolean modifyMeaningChinese(ParaphraseRequest request);

    /**
     * 默认查询还没有生产音频文件而且也没被收藏过的10个paraphrase
     * @return
     */
    List<Integer> listNotGeneratedAndNotCollectVoice();

}
