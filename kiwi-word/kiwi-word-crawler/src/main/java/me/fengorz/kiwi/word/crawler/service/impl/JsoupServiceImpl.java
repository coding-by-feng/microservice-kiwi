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

package me.fengorz.kiwi.word.crawler.service.impl;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.util.validate.KiwiAssertUtils;
import me.fengorz.kiwi.word.api.common.WordCrawlerConstants;
import me.fengorz.kiwi.word.api.dto.fetch.*;
import me.fengorz.kiwi.word.api.exception.JsoupFetchConnectException;
import me.fengorz.kiwi.word.api.exception.JsoupFetchPronunciationException;
import me.fengorz.kiwi.word.api.exception.JsoupFetchResultException;
import me.fengorz.kiwi.word.api.util.CrawlerAssertUtils;
import me.fengorz.kiwi.word.crawler.service.IJsoupService;
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

    public static final String JSOUP_CONNECT_EXCEPTION = "jsoup connect exception, the url is {}";
    public static final String KEY_WORD_HEADER = "ti fs fs12 lmb-0 hw";
    public static final String KEY_WORD_NAME = "tb ttn";
    public static final String FETCH_MAIN_WORD_NAME_EXCEPTION = "The word name of {} is not found!";
    public static final String KEY_ROOT = "pr entry-body__el";
    public static final String FETCH_ROOT_EXCEPTION = "The {} is not found!";
    public static final String KEY_MAIN_PARAPHRASES = "sense-body dsense_b";
    public static final String FETCH_MAIN_PARAPHRASES_EXCEPTION = "The mainParaphrases of {} is not found!";
    public static final String KEY_CODE_HEADER = "pos-header dpos-h";
    public static final String FETCH_CODE_HEADER_EXCEPTION = "The codeHeader of {} is not found or size great than 1!";
    public static final String KEY_HEADER_CODE = "pos dpos";
    public static final String KEY_HEADER_LABEL = "gram dgram";
    public static final String KEY_SINGLE_PARAPHRASE = "def-block ddef_block ";
    public static final String FETCH_SINGLE_PARAPHRASE_EXCEPTION = "The singleParaphrase of {} is not found!";
    public static final String KEY_PARAPHRASE_ENGLISH = "def ddef_d db";
    public static final String FETCH_PARAPHRASE_ENGLISH_EXCEPTION = "The paraphraseEnglish of {} is not found!";
    public static final String KEY_MEANING_CHINESE = "trans dtrans dtrans-se ";
    public static final String FETCH_MEANING_CHINESE_EXCEPTION = "The meaningChinese of {} is not found!";
    public static final String KEY_EXAMPLE_SENTENCES = "examp dexamp";
    public static final String FETCH_EXAMPLE_SENTENCES_EXCEPTION = "The exampleSentences of {} is not found!";
    public static final String KEY_SENTENCE = "eg deg";
    public static final String KEY_SENTENCE_TRANSLATE = "trans dtrans dtrans-se hdb";
    public static final String KEY_UK_PRONUNCIATIOIN = "uk dpron-i ";
    public static final String KEY_US_PRONUNCIATIOIN = "us dpron-i ";
    public static final String KEY_SOUNDMARK = "pron dpron";
    public static final String KEY_VOICE_FILE_URL = "i-amphtml-fill-content";
    public static final String KEY_SRC = "src";
    public static final String FETCH_PRONUNCIATION_EXCEPTION = "fetch UK's pronunciation exception, the word is {}";
    public static final String KEY_SOURCE = "source";
    public static final String KEY_DAUD = "daud";

    @Override
    public FetchWordResultDTO fetchWordInfo(WordMessageDTO wordMessage) throws JsoupFetchResultException, JsoupFetchConnectException, JsoupFetchPronunciationException {
        final String word = wordMessage.getWord();
        String jsoupWord = null;

        FetchWordResultDTO fetchWordResultDTO = null;
        try {
            fetchWordResultDTO = subFetch(WordCrawlerConstants.CAMBRIDGE_FETCH_CHINESE_URL, word);
        } catch (JsoupFetchConnectException | JsoupFetchResultException | JsoupFetchPronunciationException e) {
            fetchWordResultDTO = subFetch(WordCrawlerConstants.CAMBRIDGE_FETCH_ENGLISH_URL, word);
        }
        return fetchWordResultDTO;
    }

    private FetchWordResultDTO subFetch(String url, String word) throws JsoupFetchConnectException, JsoupFetchResultException, JsoupFetchPronunciationException {
        String jsoupWord;
        Document doc;
        try {
            doc = Jsoup.connect(url + word).get();
        } catch (IOException e) {
            log.error(JSOUP_CONNECT_EXCEPTION, word);
            throw new JsoupFetchConnectException();
        }

        /**
         * Fetch back the important data can not be empty,
         * empty to throw an exception,
         * let the upper logic to do callback processing
         */
        FetchWordResultDTO fetchWordResultDTO = new FetchWordResultDTO();

        //fetchWordResultDTO 放入 爬虫回来的wordName，不是传进来的wordName
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
            //  The number of parts of code and label per main paraphrase block can normally only be 1
            CrawlerAssertUtils.mustBeTrue(codeHeader != null && codeHeader.size() == 1,
                    FETCH_CODE_HEADER_EXCEPTION, word);
            // TODO ZSF fetchWordResultDTO 放入 爬虫回来的wordName，不是传进来的wordName
            final Element header = codeHeader.get(0);
            Optional.ofNullable(header.getElementsByClass(KEY_HEADER_CODE)).flatMap(element -> Optional.ofNullable(element.text())).ifPresent(fetchWordCodeDTO::setCode);
            Optional.ofNullable(header.getElementsByClass(KEY_HEADER_LABEL)).flatMap(element -> Optional.ofNullable(element.text())).ifPresent(fetchWordCodeDTO::setLabel);

            // fetch UK pronunciation
            try {
                fetchWordPronunciationDTOList.add(subFetchPronunciation(word, block, KEY_UK_PRONUNCIATIOIN, WordCrawlerConstants.PRONUNCIATION_TYPE_UK, isAlreadyFetchPronunciation));
                // fetch US pronunciation
                fetchWordPronunciationDTOList.add(subFetchPronunciation(word, block, KEY_US_PRONUNCIATIOIN, WordCrawlerConstants.PRONUNCIATION_TYPE_US, isAlreadyFetchPronunciation));
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
                CrawlerAssertUtils.notEmpty(singleParaphrase, FETCH_SINGLE_PARAPHRASE_EXCEPTION, word);

                for (Element paraphrase : singleParaphrase) {
                    FetchParaphraseDTO fetchParaphraseDTO = new FetchParaphraseDTO();

                    // fetch English paraphrase
                    Elements paraphraseEnglish = paraphrase.getElementsByClass(KEY_PARAPHRASE_ENGLISH);
                    CrawlerAssertUtils.notEmpty(paraphraseEnglish, FETCH_PARAPHRASE_ENGLISH_EXCEPTION, word);
                    CrawlerAssertUtils.fetchValueNotEmpty(paraphraseEnglish.text(), FETCH_PARAPHRASE_ENGLISH_EXCEPTION, word);
                    fetchParaphraseDTO.setParaphraseEnglish(paraphraseEnglish.text());

                    // fetch Chinese meaning
                    Elements meaningChinese = paraphrase.getElementsByClass(KEY_MEANING_CHINESE);
                    // CrawlerAssertUtils.notEmpty(meaningChinese, FETCH_MEANING_CHINESE_EXCEPTION, word);
                    // CrawlerAssertUtils.fetchValueNotEmpty(meaningChinese.text(), FETCH_MEANING_CHINESE_EXCEPTION, word);
                    fetchParaphraseDTO.setMeaningChinese(meaningChinese.text());

                    // fetch example sentences, it can be empty.
                    Elements exampleSentences = paraphrase.getElementsByClass(KEY_EXAMPLE_SENTENCES);
                    if (CollUtil.isNotEmpty(exampleSentences)) {
                        List<FetchParaphraseExampleDTO> fetchParaphraseExampleDTOList = new ArrayList<>();
                        for (Element sentence : exampleSentences) {
                            FetchParaphraseExampleDTO fetchParaphraseExampleDTO = new FetchParaphraseExampleDTO();
                            Optional.ofNullable(sentence.getElementsByClass(KEY_SENTENCE)).ifPresent(
                                    (Elements elements) -> fetchParaphraseExampleDTO.setExampleSentence(elements.text())
                            );
                            Optional.ofNullable(sentence.getElementsByClass(KEY_SENTENCE_TRANSLATE)).ifPresent(
                                    elements -> fetchParaphraseExampleDTO.setExampleTranslate(elements.text())
                            );

                            // TODO zhanshifeng The default is English, but consider how flexible it will be in the future if there are other languages
                            fetchParaphraseExampleDTO.setTranslateLanguage(WordCrawlerConstants.DEFAULT_TRANSLATE_LANGUAGE);
                            fetchParaphraseExampleDTOList.add(fetchParaphraseExampleDTO);
                            fetchParaphraseDTO.setFetchParaphraseExampleDTOList(fetchParaphraseExampleDTOList);
                        }
                    }

                    fetchParaphraseDTOList.add(fetchParaphraseDTO);
                }
            }

            fetchWordCodeDTO.setFetchParaphraseDTOList(fetchParaphraseDTOList);
            fetchWordCodeDTOList.add(fetchWordCodeDTO);
        }

        fetchWordResultDTO.setFetchWordCodeDTOList(fetchWordCodeDTOList);
        return fetchWordResultDTO;
    }

    private FetchWordPronunciationDTO subFetchPronunciation(String word, Element block, String pronunciationKey, String pronunciationType, boolean isAlreadyFetchPronunciation) throws JsoupFetchPronunciationException {
        Elements ukPronunciations = block.getElementsByClass(pronunciationKey);
        try {
            CrawlerAssertUtils.mustBeTrue(ukPronunciations != null && ukPronunciations.size() == 1, FETCH_PRONUNCIATION_EXCEPTION, word);
        } catch (JsoupFetchResultException e) {
            throw new JsoupFetchPronunciationException(e);
        }
        Element ukPronunciation = ukPronunciations.get(0);

        FetchWordPronunciationDTO ukPronunciationDTO = new FetchWordPronunciationDTO();
        // index[0] is type="audio/mpeg", index[1] is type="audio/ogg"
        return ukPronunciationDTO.setSoundmarkType(pronunciationType)
                .setSoundmark(Optional.ofNullable(ukPronunciation.getElementsByClass(KEY_SOUNDMARK))
                        .orElseThrow(JsoupFetchPronunciationException::new).get(0).text())
                .setVoiceFileUrl(Optional.ofNullable(ukPronunciation.getElementsByTag(KEY_SOURCE)).orElseThrow(JsoupFetchPronunciationException::new).get(0).attr(KEY_SRC));
    }
}
