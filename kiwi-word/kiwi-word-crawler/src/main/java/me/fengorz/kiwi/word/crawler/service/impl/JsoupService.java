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

package me.fengorz.kiwi.word.crawler.service.impl;

import cn.hutool.core.collection.CollUtil;
import me.fengorz.kiwi.word.api.common.CrawlerConstants;
import me.fengorz.kiwi.word.api.dto.fetch.*;
import me.fengorz.kiwi.word.api.exception.JsoupFetchConnectException;
import me.fengorz.kiwi.word.api.exception.JsoupFetchResultException;
import me.fengorz.kiwi.word.api.util.CrawlerAssertUtils;
import me.fengorz.kiwi.word.crawler.service.IJsoupService;
import lombok.extern.slf4j.Slf4j;
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
 * @Description TODO
 * @Author codingByFeng
 * @Date 2019/10/31 3:24 PM
 */
@Service
@Slf4j
public class JsoupService implements IJsoupService {

    public static final String JSOUP_CONNECT_EXCEPTION = "jsoup connect exception, the url is {}";
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
    public FetchWordResultDTO fetchWordInfo(WordMessageDTO wordMessage) throws JsoupFetchConnectException, JsoupFetchResultException {
        final String word = wordMessage.getWord();

        Document doc;
        try {
            doc = Jsoup.connect(CrawlerConstants.CRAWLER_BASE_URL + word).get();
        } catch (IOException e) {
            log.error(JSOUP_CONNECT_EXCEPTION, word);
            throw new JsoupFetchConnectException();
        }

        /**
         * Fetch back the important data can not be empty,
         * empty to throw an exception,
         * let the upper logic to do callback processing
         */
        Elements root = doc.getElementsByClass(KEY_ROOT);
        CrawlerAssertUtils.notEmpty(root, FETCH_ROOT_EXCEPTION, word);

        FetchWordResultDTO fetchWordResultDTO = new FetchWordResultDTO();
        fetchWordResultDTO.setWordName(word);
        List<FetchWordCodeDTO> fetchWordCodeDTOList = new ArrayList<>();

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
            final Element header = codeHeader.get(0);
            Optional.ofNullable(header.getElementsByClass(KEY_HEADER_CODE)).flatMap(element -> Optional.ofNullable(element.text())).ifPresent(code -> {
                fetchWordCodeDTO.setCode(code);
            });
            Optional.ofNullable(header.getElementsByClass(KEY_HEADER_LABEL)).flatMap(element -> Optional.ofNullable(element.text())).ifPresent(label -> {
                fetchWordCodeDTO.setLabel(label);
            });

            // fetch UK pronunciation
            fetchWordPronunciationDTOList.add(subFetchPronunciation(word, block, KEY_UK_PRONUNCIATIOIN, CrawlerConstants.PRONUNCIATION_TYPE_UK));
            // fetch US pronunciation
            fetchWordPronunciationDTOList.add(subFetchPronunciation(word, block, KEY_US_PRONUNCIATIOIN, CrawlerConstants.PRONUNCIATION_TYPE_US));
            fetchWordCodeDTO.setFetchWordPronunciationDTOList(fetchWordPronunciationDTOList);

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
                    CrawlerAssertUtils.notEmpty(meaningChinese, FETCH_MEANING_CHINESE_EXCEPTION, word);
                    CrawlerAssertUtils.fetchValueNotEmpty(meaningChinese.text(), FETCH_MEANING_CHINESE_EXCEPTION, word);
                    fetchParaphraseDTO.setMeaningChinese(meaningChinese.text());

                    // fetch example sentences, it can be empty.
                    Elements exampleSentences = paraphrase.getElementsByClass(KEY_EXAMPLE_SENTENCES);
                    if (CollUtil.isNotEmpty(exampleSentences)) {
                        List<FetchParaphraseExampleDTO> fetchParaphraseExampleDTOList = new ArrayList<>();
                        for (Element sentence : exampleSentences) {
                            FetchParaphraseExampleDTO fetchParaphraseExampleDTO = new FetchParaphraseExampleDTO();
                            Optional.ofNullable(sentence.getElementsByClass(KEY_SENTENCE)).ifPresent(
                                    elements -> {
                                        fetchParaphraseExampleDTO.setExampleSentence(elements.text());
                                    }
                            );
                            Optional.ofNullable(sentence.getElementsByClass(KEY_SENTENCE_TRANSLATE)).ifPresent(
                                    elements -> {
                                        fetchParaphraseExampleDTO.setExampleTranslate(elements.text());
                                    }
                            );

                            // TODO codingByFeng The default is English, but consider how flexible it will be in the future if there are other languages
                            fetchParaphraseExampleDTO.setTranslateLanguage(CrawlerConstants.DEFAULT_TRANSLATE_LANGUAGE);
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

    private FetchWordPronunciationDTO subFetchPronunciation(String word, Element block, String pronunciationKey, String pronunciationType) throws JsoupFetchResultException {
        Elements ukPronunciations = block.getElementsByClass(pronunciationKey);
        CrawlerAssertUtils.mustBeTrue(ukPronunciations != null && ukPronunciations.size() == 1, FETCH_PRONUNCIATION_EXCEPTION, word);
        Element ukPronunciation = ukPronunciations.get(0);

        FetchWordPronunciationDTO ukPronunciationDTO = new FetchWordPronunciationDTO();
        // index[0] is type="audio/mpeg", index[1] is type="audio/ogg"
        return ukPronunciationDTO.setSoundmarkType(pronunciationType)
                .setSoundmark(Optional.ofNullable(ukPronunciation.getElementsByClass(KEY_SOUNDMARK))
                        .orElseThrow(JsoupFetchResultException::new).get(0).text())
                .setVoiceFileUrl(Optional.ofNullable(ukPronunciation.getElementsByTag(KEY_SOURCE)).orElseThrow(JsoupFetchResultException::new).get(1).attr(KEY_SRC));
    }
}
