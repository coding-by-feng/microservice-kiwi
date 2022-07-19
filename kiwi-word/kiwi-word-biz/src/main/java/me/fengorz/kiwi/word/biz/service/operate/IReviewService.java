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
package me.fengorz.kiwi.word.biz.service.operate;

import java.util.List;

import me.fengorz.kiwi.common.sdk.exception.dfs.DfsOperateException;
import me.fengorz.kiwi.common.sdk.exception.tts.TtsException;
import me.fengorz.kiwi.word.api.entity.WordBreakpointReviewDO;
import me.fengorz.kiwi.word.api.entity.WordReviewAudioDO;
import me.fengorz.kiwi.word.api.vo.WordReviewDailyCounterVO;

/**
 * @author zhanShiFeng
 * @date 2021-08-19 20:42:11
 */
public interface IReviewService {

    List<WordBreakpointReviewDO> listBreakpointReview(Integer listId);

    void addOne(Integer listId, Integer pageNum);

    void createTheDays(Integer userId);

    void increase(int type, Integer userId);

    WordReviewDailyCounterVO getVO(int userId, int type);

    /**
     * Record the page number currently reviewed.
     *
     * @param listId
     * @param pageNumber
     * @param type
     */
    void recordReviewPageNumber(int listId, Long pageNumber, int type, Integer userId);

    WordReviewAudioDO findWordReviewAudio(Integer paraphraseId, Integer type);

    void initPermanent(boolean isReplace, boolean isOnlyTest) throws DfsOperateException, TtsException;

    void generateTtsVoice(boolean isReplace) throws DfsOperateException, TtsException;

    void generateTtsVoiceFromParaphraseId(Integer paraphraseId);
}
