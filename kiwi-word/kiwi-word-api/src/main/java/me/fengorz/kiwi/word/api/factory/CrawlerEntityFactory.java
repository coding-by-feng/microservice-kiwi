/*
 *
 *   Copyright [2019~2025] [zhanshifeng]
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

package me.fengorz.kiwi.word.api.factory;

import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.word.api.entity.*;
import me.fengorz.kiwi.word.api.util.CrawlerUtils;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2019/11/3 5:36 PM
 */
public class CrawlerEntityFactory {

    public static WordMainDO initWordMain(String wordName) {
        WordMainDO wordMainDO = new WordMainDO();
        wordMainDO.setWordName(wordName);
        wordMainDO.setIsDel(CommonConstants.FLAG_N);
        return wordMainDO;
    }

    public static WordCharacterDO initWordCharacter(String wordCode, String wordLabel, Integer wordId) {
        WordCharacterDO wordCharacter = new WordCharacterDO();
        wordCharacter.setWordId(wordId);
        wordCharacter.setWordCharacter(wordCode);
        wordCharacter.setWordLabel(wordLabel);
        wordCharacter.setIsDel(CommonConstants.FLAG_N);
        return wordCharacter;
    }

    public static WordParaphraseDO initWordParaphrase(Integer characterId, Integer wordId, String meaningChinese, String paraphraseEnglish, String translateLanguage) {
        WordParaphraseDO wordParaphraseDO = new WordParaphraseDO();
        wordParaphraseDO.setWordId(wordId);
        wordParaphraseDO.setCharacterId(characterId);
        wordParaphraseDO.setMeaningChinese(meaningChinese);
        wordParaphraseDO.setIsDel(CommonConstants.FLAG_N);
        wordParaphraseDO.setParaphraseEnglish(paraphraseEnglish);
        wordParaphraseDO.setTranslateLanguage(translateLanguage);
        return wordParaphraseDO;
    }

    public static WordParaphraseExampleDO initWordParaphraseExample(Integer paraphraseId, Integer wordId, String exampleSentence, String exampleTranslate, String translateLanguage) {
        WordParaphraseExampleDO wordParaphraseExampleDO = new WordParaphraseExampleDO();
        wordParaphraseExampleDO.setWordId(wordId);
        wordParaphraseExampleDO.setParaphraseId(paraphraseId);
        wordParaphraseExampleDO.setExampleSentence(exampleSentence);
        wordParaphraseExampleDO.setExampleTranslate(exampleTranslate);
        wordParaphraseExampleDO.setTranslateLanguage(translateLanguage);
        return wordParaphraseExampleDO;
    }

    public static WordPronunciationDO initWordPronunciation(Integer wordId, Integer characterId, String uploadResult, String soundmark, String soundmarkType) {
        WordPronunciationDO wordPronunciation = new WordPronunciationDO();
        wordPronunciation.setWordId(wordId);
        wordPronunciation.setCharacterId(characterId);
        wordPronunciation.setGroupName(CrawlerUtils.getGroupName(uploadResult));
        wordPronunciation.setIsDel(CommonConstants.FLAG_N);
        wordPronunciation.setSoundmark(soundmark);
        wordPronunciation.setSoundmarkType(soundmarkType);
        wordPronunciation.setVoiceFilePath(CrawlerUtils.getUploadVoiceFilePath(uploadResult));
        return wordPronunciation;
    }

}
