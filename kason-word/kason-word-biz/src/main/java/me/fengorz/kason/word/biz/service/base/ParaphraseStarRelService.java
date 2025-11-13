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
import me.fengorz.kason.word.api.entity.ParaphraseStarRelDO;

import java.util.List;

/**
 * @author zhanshifeng
 * @date 2020-01-03 14:44:37
 */
public interface ParaphraseStarRelService extends IService<ParaphraseStarRelDO> {

    void replaceFetchResult(Integer oldRelId, Integer newRelId);

    /**
     * 默认查询还没有生产音频文件的10个paraphrase
     * 
     * @return
     */
    List<Integer> listNotGeneratedVoice();

    /**
     * 查询已经收藏的释义中，有哪些关联音频还没有完全生成的
     * 
     * @param type
     * @return
     */
    List<Integer> listNotAllGeneratedVoice();

    /**
     * 查询已经收藏的释义中，有哪些词组的读音音频还没有完全生成的
     *
     * @param type
     * @return
     */
    List<Integer> listNotGeneratedPronunciationVoiceForPhrase();

}
