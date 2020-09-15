/*
 *
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
 *
 *
 */

package me.fengorz.kiwi.word.crawler.service.impl;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.util.lang.string.KiwiStringUtils;
import me.fengorz.kiwi.common.sdk.util.validate.KiwiAssertUtils;
import me.fengorz.kiwi.word.api.common.WordCrawlerConstants;
import me.fengorz.kiwi.word.api.dto.queue.FetchWordMqDTO;
import me.fengorz.kiwi.word.api.dto.queue.fetch.*;
import me.fengorz.kiwi.word.api.exception.JsoupFetchConnectException;
import me.fengorz.kiwi.word.api.exception.JsoupFetchPronunciationException;
import me.fengorz.kiwi.word.api.exception.JsoupFetchResultException;
import me.fengorz.kiwi.word.crawler.service.IJsoupService;
import me.fengorz.kiwi.word.crawler.util.CrawlerAssertUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @Description 爬虫抓取单词数据服务类
 * @Author zhanshifeng
 * @Date 2019/10/31 3:24 PM
 */
@Service
@Slf4j
public class JsoupServiceImpl implements IJsoupService {

    private static final String JSOUP_CONNECT_EXCEPTION = "jsoup connect exception, the url is {}";
    private static final String KEY_WORD_HEADER = "ti fs fs12 lmb-0 hw";
    private static final String KEY_WORD_NAME = "tb ttn";
    private static final String FETCH_MAIN_WORD_NAME_EXCEPTION = "The word name of {} is not found!";
    private static final String KEY_ROOT = "pr entry-body__el";
    private static final String FETCH_ROOT_EXCEPTION = "The {} is not found!";
    private static final String KEY_MAIN_PARAPHRASES = "sense-body dsense_b";
    private static final String FETCH_MAIN_PARAPHRASES_EXCEPTION = "The mainParaphrases of {} is not found!";
    private static final String KEY_CODE_HEADER = "pos-header dpos-h";
    private static final String FETCH_CODE_HEADER_EXCEPTION = "The codeHeader of {} is not found or size great than 1!";
    private static final String KEY_HEADER_CODE = "pos dpos";
    private static final String KEY_HEADER_LABEL = "gram dgram";
    private static final String KEY_SINGLE_PARAPHRASE = "def-block ddef_block ";
    private static final String KEY_SINGLE_PHRASE = "pr phrase-block dphrase-block ";
    private static final String KEY_SINGLE_PHRASE_DETAIL = "phrase-title dphrase-title";
    private static final String FETCH_SINGLE_PARAPHRASE_EXCEPTION = "The singleParaphrase of {} is not found!";
    private static final String KEY_PARAPHRASE_ENGLISH = "def ddef_d db";
    private static final String KEY_PARAPHRASE_CODES = "gram dgram";
    private static final String FETCH_PARAPHRASE_ENGLISH_EXCEPTION = "The paraphraseEnglish of {} is not found!";
    private static final String KEY_MEANING_CHINESE = "trans dtrans dtrans-se ";
    private static final String FETCH_MEANING_CHINESE_EXCEPTION = "The meaningChinese of {} is not found!";
    private static final String KEY_EXAMPLE_SENTENCES = "examp dexamp";
    private static final String FETCH_EXAMPLE_SENTENCES_EXCEPTION = "The exampleSentences of {} is not found!";
    private static final String KEY_SENTENCE = "eg deg";
    private static final String KEY_SENTENCE_TRANSLATE = "trans dtrans dtrans-se hdb";
    private static final String KEY_UK_PRONUNCIATIOIN = "uk dpron-i ";
    private static final String KEY_US_PRONUNCIATIOIN = "us dpron-i ";
    private static final String KEY_SOUNDMARK = "pron dpron";
    private static final String KEY_VOICE_FILE_URL = "i-amphtml-fill-content";
    private static final String KEY_SRC = "src";
    private static final String FETCH_PRONUNCIATION_EXCEPTION = "fetch UK's pronunciation exception, the word is {}";
    private static final String KEY_SOURCE = "source";
    private static final String KEY_DAUD = "daud";
    private static final String SOUND_MARK_DEFAULT = "音标缺失";

    @Override
    public FetchWordResultDTO fetchWordInfo(FetchWordMqDTO wordMessage)
            throws JsoupFetchResultException, JsoupFetchConnectException, JsoupFetchPronunciationException {
        final String word = wordMessage.getWord();
        String jsoupWord = null;

        FetchWordResultDTO fetchWordResultDTO = null;
        try {
            fetchWordResultDTO = subFetch(WordCrawlerConstants.CAMBRIDGE_FETCH_CHINESE_URL, word);
        } catch (JsoupFetchConnectException | JsoupFetchResultException | JsoupFetchPronunciationException e) {
            log.error(e.getMessage());
            fetchWordResultDTO = subFetch(WordCrawlerConstants.CAMBRIDGE_FETCH_ENGLISH_URL, word);
        }
        return fetchWordResultDTO;
    }

    private FetchWordResultDTO subFetch(String url, String word)
            throws JsoupFetchConnectException, JsoupFetchResultException, JsoupFetchPronunciationException {
        String jsoupWord;
        Document doc;
        try {
            doc = Jsoup.connect(url + word).get();
        } catch (IOException e) {
            log.error(JSOUP_CONNECT_EXCEPTION, word);
            throw new JsoupFetchConnectException();
        }

        /**
         * Fetch back the important data can not be empty, empty to throw an exception, let the upper logic to do
         * callback processing
         */
        FetchWordResultDTO fetchWordResultDTO = new FetchWordResultDTO();

        // fetchWordResultDTO 放入 爬虫回来的wordName，不是传进来的wordName
        Elements wordNameHeader = doc.getElementsByClass(KEY_WORD_HEADER);
        CrawlerAssertUtils.notEmpty(wordNameHeader, FETCH_MAIN_WORD_NAME_EXCEPTION, word);
        Elements wordName = wordNameHeader.get(0).getElementsByClass(KEY_WORD_NAME);
        CrawlerAssertUtils.notEmpty(wordName, FETCH_MAIN_WORD_NAME_EXCEPTION, word);
        jsoupWord = wordName.get(0).text();
        KiwiAssertUtils.serviceEmpty(jsoupWord, FETCH_MAIN_WORD_NAME_EXCEPTION, word);
        fetchWordResultDTO.setWordName(jsoupWord);

        Elements root = doc.getElementsByClass(KEY_ROOT);
        CrawlerAssertUtils.notEmpty(root, FETCH_ROOT_EXCEPTION, word);
        List<FetchWordCodeDTO> fetchWordCodeDTOList = new ArrayList<>();
        boolean isAlreadyFetchPronunciation = false;

        for (Element block : root) {
            Elements mainParaphrases = block.getElementsByClass(KEY_MAIN_PARAPHRASES);
            CrawlerAssertUtils.notEmpty(root, FETCH_MAIN_PARAPHRASES_EXCEPTION, word);

            FetchWordCodeDTO fetchWordCodeDTO = new FetchWordCodeDTO();
            List<FetchWordPronunciationDTO> fetchWordPronunciationDTOList = new ArrayList<>();
            List<FetchParaphraseDTO> fetchParaphraseDTOList = new ArrayList<>();
            Elements codeHeader = block.getElementsByClass(KEY_CODE_HEADER);
            // The number of parts of code and label per main paraphrase block can normally only be 1
            // TODO ZSF 大于0的时候这里要特殊处理，比如：flirt，目前只是抓取主要词性，关联单词没有抓到
            CrawlerAssertUtils.mustBeTrue(codeHeader != null && !codeHeader.isEmpty(), FETCH_CODE_HEADER_EXCEPTION,
                    word);
            // TODO ZSF fetchWordResultDTO 放入 爬虫回来的wordName，不是传进来的wordName
            final Element header = codeHeader.get(0);
            Optional.ofNullable(header.getElementsByClass(KEY_HEADER_CODE))
                    .flatMap(element -> Optional.ofNullable(element.text())).ifPresent(fetchWordCodeDTO::setCode);
            Optional.ofNullable(header.getElementsByClass(KEY_HEADER_LABEL))
                    .flatMap(element -> Optional.ofNullable(element.text())).ifPresent(fetchWordCodeDTO::setLabel);

            try {
                // fetch UK pronunciation
                Optional.ofNullable(subFetchPronunciation(word, block, KEY_UK_PRONUNCIATIOIN,
                        WordCrawlerConstants.PRONUNCIATION_TYPE_UK, isAlreadyFetchPronunciation)).ifPresent(fetchWordPronunciationDTOList::add);
                // fetch US pronunciation
                Optional.ofNullable(subFetchPronunciation(word, block, KEY_US_PRONUNCIATIOIN,
                        WordCrawlerConstants.PRONUNCIATION_TYPE_US, isAlreadyFetchPronunciation)).ifPresent(fetchWordPronunciationDTOList::add);
            } catch (JsoupFetchPronunciationException e) {
                if (!isAlreadyFetchPronunciation) {
                    throw e;
                }
            }
            fetchWordCodeDTO.setFetchWordPronunciationDTOList(fetchWordPronunciationDTOList);
            isAlreadyFetchPronunciation = true;

            // Each label can have many paraphrases
            for (Element paraphrases : mainParaphrases) {
                Elements singleParaphrase = paraphrases.getElementsByClass(KEY_SINGLE_PARAPHRASE);
                Elements phrases = paraphrases.getElementsByClass(KEY_SINGLE_PHRASE);
                boolean singleParaphraseIsNotEmpty = singleParaphrase != null && !singleParaphrase.isEmpty();
                boolean phrasesIsNotEmpty = phrases != null && !phrases.isEmpty();
                CrawlerAssertUtils.mustBeTrue(singleParaphraseIsNotEmpty || phrasesIsNotEmpty,
                        FETCH_SINGLE_PARAPHRASE_EXCEPTION, word);

                subFetchParaphrase(word, fetchParaphraseDTOList, singleParaphrase, phrases, phrasesIsNotEmpty);
            }

            fetchWordCodeDTO.setFetchParaphraseDTOList(fetchParaphraseDTOList);
            fetchWordCodeDTOList.add(fetchWordCodeDTO);
        }

        fetchWordResultDTO.setFetchWordCodeDTOList(fetchWordCodeDTOList);
        return fetchWordResultDTO;
    }

    /**
     * 抓取单词的某个释义
     *
     * @param word
     * @param paraphraseDTOList
     * @param paraphraseElements
     * @param phrases            释义附带的词组
     * @param phrasesIsEmpty     释义是否附带词组
     * @throws JsoupFetchResultException
     */
    private void subFetchParaphrase(String word, List<FetchParaphraseDTO> paraphraseDTOList,
                                    Elements paraphraseElements, Elements phrases, boolean phrasesIsEmpty) throws JsoupFetchResultException {
        for (Element paraphrase : paraphraseElements) {
            FetchParaphraseDTO paraphraseDTO = new FetchParaphraseDTO();

            // fetch English paraphrase
            Elements paraphraseEnglish = paraphrase.getElementsByClass(KEY_PARAPHRASE_ENGLISH);
            Elements codes = paraphrase.getElementsByClass(KEY_PARAPHRASE_CODES);
            CrawlerAssertUtils.notEmpty(paraphraseEnglish, FETCH_PARAPHRASE_ENGLISH_EXCEPTION, word);
            String paraphraseEnglishText = paraphraseEnglish.text();
            String codesText = codes != null && !codes.isEmpty() ? codes.text() : "";
            CrawlerAssertUtils.fetchValueNotEmpty(paraphraseEnglishText, FETCH_PARAPHRASE_ENGLISH_EXCEPTION, word);
            paraphraseDTO.setParaphraseEnglish(paraphraseEnglishText);
            paraphraseDTO.setCodes(codesText);

            if (phrasesIsEmpty) {
                List<FetchPhraseDTO> phraseDTOList = new ArrayList<>();
                for (Element phraseBlock : phrases) {
                    Elements subPhrases = phraseBlock.getElementsByClass(KEY_SINGLE_PHRASE_DETAIL);
                    Elements subPhrasesParaphrases = phraseBlock.getElementsByClass(KEY_SINGLE_PARAPHRASE);
                    if (subPhrases == null || subPhrases.isEmpty()) {
                        continue;
                    }
                    for (int i = 0; i < subPhrases.size(); i++) {
                        String phraseDetail = subPhrases.get(i).text();
                        String subPhrasesParaphrase =
                                subPhrasesParaphrases.get(i).getElementsByClass(KEY_PARAPHRASE_ENGLISH).text();
                        if (KiwiStringUtils.equals(subPhrasesParaphrase, paraphraseEnglishText)) {
                            phraseDTOList.add(new FetchPhraseDTO().setPhrase(phraseDetail));
                        }
                    }
                }
                paraphraseDTO.setPhraseDTOList(phraseDTOList);
            }

            // fetch Chinese meaning
            Elements meaningChinese = paraphrase.getElementsByClass(KEY_MEANING_CHINESE);
            // CrawlerAssertUtils.notEmpty(meaningChinese, FETCH_MEANING_CHINESE_EXCEPTION, word);
            // CrawlerAssertUtils.fetchValueNotEmpty(meaningChinese.text(), FETCH_MEANING_CHINESE_EXCEPTION, word);
            paraphraseDTO.setMeaningChinese(meaningChinese.text());

            // fetch example sentences, it can be empty.
            Elements exampleSentences = paraphrase.getElementsByClass(KEY_EXAMPLE_SENTENCES);
            if (CollUtil.isNotEmpty(exampleSentences)) {
                List<FetchParaphraseExampleDTO> paraphraseExampleDTOList = new ArrayList<>();
                for (Element sentence : exampleSentences) {
                    FetchParaphraseExampleDTO paraphraseExampleDTO = new FetchParaphraseExampleDTO();
                    Optional.ofNullable(sentence.getElementsByClass(KEY_SENTENCE)).ifPresent(
                            (Elements elements) -> paraphraseExampleDTO.setExampleSentence(elements.text()));
                    Optional.ofNullable(sentence.getElementsByClass(KEY_SENTENCE_TRANSLATE))
                            .ifPresent(elements -> paraphraseExampleDTO.setExampleTranslate(elements.text()));

                    // TODO zhanshifeng The default is English, but consider how flexible it will be in the future if
                    // there are other languages
                    paraphraseExampleDTO.setTranslateLanguage(WordCrawlerConstants.DEFAULT_TRANSLATE_LANGUAGE);
                    paraphraseExampleDTOList.add(paraphraseExampleDTO);
                    paraphraseDTO.setExampleDTOList(paraphraseExampleDTOList);
                }
            }

            paraphraseDTOList.add(paraphraseDTO);
        }
    }

    private FetchWordPronunciationDTO subFetchPronunciation(String word, Element block, String pronunciationKey,
                                                            String pronunciationType, boolean isAlreadyFetchPronunciation) throws JsoupFetchPronunciationException {
        Elements pronunciations = block.getElementsByClass(pronunciationKey);
        if (pronunciations == null || pronunciations.isEmpty()) {
            return null;
        }

        // try {
        //     CrawlerAssertUtils.mustBeTrue(pronunciations != null && pronunciations.size() == 1,
        //             FETCH_PRONUNCIATION_EXCEPTION, word);
        // } catch (JsoupFetchResultException e) {
        //     throw new JsoupFetchPronunciationException(e);
        // }

        Element ukPronunciation = pronunciations.get(0);

        FetchWordPronunciationDTO ukPronunciationDTO = new FetchWordPronunciationDTO();
        // index[0] is type="audio/mpeg", index[1] is type="audio/ogg"
        // 如果音标缺失使用默认是"音标缺失"
        String soundMark = SOUND_MARK_DEFAULT;
        // TODO ZSF 这里应该增加一个音标缺失语音常量
        String soundMarkSrc = "";
        Elements soundMarkElement = Optional.ofNullable(ukPronunciation.getElementsByClass(KEY_SOUNDMARK))
                .orElseThrow(JsoupFetchPronunciationException::new);
        Elements soundMarkSrcElement = Optional.ofNullable(ukPronunciation.getElementsByTag(KEY_SOURCE))
                .orElseThrow(JsoupFetchPronunciationException::new);
        if (!soundMarkElement.isEmpty()) {
            soundMark = soundMarkElement.get(0).text();
        }
        if (!soundMarkSrcElement.isEmpty()) {
            soundMarkSrc = soundMarkSrcElement.get(0).attr(KEY_SRC);// mp3
            // soundMarkSrc = soundMarkSrcElement.get(1).attr(KEY_SRC);// ogg
        }

        return ukPronunciationDTO.setSoundmarkType(pronunciationType)
                .setSoundmark(soundMark)
                .setVoiceFileUrl(soundMarkSrc);
    }
}
