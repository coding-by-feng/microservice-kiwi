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

package me.fengorz.kiwi.word.biz.util;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.word.api.common.WordCrawlerConstants;
import me.fengorz.kiwi.word.api.dto.mapper.in.SelectStarListItemDTO;
import me.fengorz.kiwi.word.api.entity.*;

/**
 * @Author zhanshifeng
 * @Date 2019/11/3 5:36 PM
 */
public class WordBizUtils {

    public static WordMainDO initWordMain(String wordName) {
        WordMainDO word = new WordMainDO();
        word.setWordName(wordName);
        word.setIsDel(CommonConstants.FLAG_DEL_NO);
        return word;
    }

    @Deprecated
    public static WordCharacterDO initCharacter(String wordCode, String wordLabel, Integer wordId) {
        WordCharacterDO character = new WordCharacterDO();
        character.setWordId(wordId);
        character.setCharacterCode(wordCode);
        character.setTag(wordLabel);
        character.setIsDel(CommonConstants.FLAG_N);
        return character;
    }

    @Deprecated
    public static WordParaphraseDO initParaphrase(Integer characterId, Integer wordId, String meaningChinese,
                                                  String paraphraseEnglish, String translateLanguage, String codes) {
        WordParaphraseDO paraphrase = new WordParaphraseDO();
        paraphrase.setWordId(wordId);
        paraphrase.setCharacterId(characterId);
        paraphrase.setCodes(codes);
        paraphrase.setMeaningChinese(meaningChinese);
        paraphrase.setIsDel(CommonConstants.FLAG_DEL_NO);
        paraphrase.setParaphraseEnglish(paraphraseEnglish);
        paraphrase.setTranslateLanguage(translateLanguage);
        return paraphrase;
    }

    @Deprecated
    public static WordParaphraseExampleDO initExample(Integer paraphraseId, Integer wordId,
                                                      String exampleSentence, String exampleTranslate, String translateLanguage) {
        WordParaphraseExampleDO example = new WordParaphraseExampleDO();
        example.setWordId(wordId);
        example.setParaphraseId(paraphraseId);
        example.setExampleSentence(exampleSentence);
        example.setExampleTranslate(exampleTranslate);
        example.setTranslateLanguage(translateLanguage);
        return example;
    }

    public static WordPronunciationDO initPronunciation(Integer wordId, Integer characterId, String voiceUrl,
                                                        String soundmark, String soundmarkType) {
        WordPronunciationDO pronunciation = new WordPronunciationDO();
        pronunciation.setWordId(wordId);
        pronunciation.setCharacterId(characterId);
        pronunciation.setIsDel(CommonConstants.FLAG_DEL_NO);
        pronunciation.setSoundmark(soundmark);
        pronunciation.setSoundmarkType(soundmarkType);
        pronunciation.setVoiceFilePath(voiceUrl);
        pronunciation.setSourceUrl(WordCrawlerConstants.CAMBRIDGE_BASE_URL + voiceUrl);
        return pronunciation;
    }

    public static SelectStarListItemDTO assembleSelectStarListItemDTO(Page page, Integer listId) {
        return (SelectStarListItemDTO) new SelectStarListItemDTO().setListId(listId).setSize(page.getSize())
                .setCurrent((page.getCurrent() - 1) * page.getSize());
    }

    public static boolean fetchQueueIsRunning(Integer status) {
        if (status == WordCrawlerConstants.STATUS_ALL_SUCCESS) {
            return false;
        }
        return status >= WordCrawlerConstants.STATUS_PARTITION ;
    }

}
