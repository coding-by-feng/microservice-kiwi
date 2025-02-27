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

package me.fengorz.kiwi.dict.crawler.service.impl;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.util.lang.collection.KiwiCollectionUtils;
import me.fengorz.kiwi.common.sdk.util.lang.string.KiwiStringUtils;
import me.fengorz.kiwi.common.sdk.util.validate.KiwiAssertUtils;
import me.fengorz.kiwi.dict.crawler.common.enumeration.JsoupRootTypeEnum;
import me.fengorz.kiwi.dict.crawler.constant.CrawlerSourceEnum;
import me.fengorz.kiwi.dict.crawler.service.JsoupService;
import me.fengorz.kiwi.dict.crawler.util.CrawlerAssertUtils;
import me.fengorz.kiwi.word.api.common.ApiCrawlerConstants;
import me.fengorz.kiwi.word.api.dto.queue.FetchPhraseMqDTO;
import me.fengorz.kiwi.word.api.dto.queue.FetchPhraseRunUpMqDTO;
import me.fengorz.kiwi.word.api.dto.queue.FetchWordMqDTO;
import me.fengorz.kiwi.word.api.dto.queue.result.*;
import me.fengorz.kiwi.word.api.exception.JsoupFetchConnectException;
import me.fengorz.kiwi.word.api.exception.JsoupFetchPronunciationException;
import me.fengorz.kiwi.word.api.exception.JsoupFetchResultException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * @Description 爬虫抓取单词数据服务类 @Author Kason Zhan @Date 2019/10/31 3:24 PM
 */
@Service
@Slf4j
public class JsoupServiceImpl implements JsoupService {

    private static final String JSOUP_CONNECT_EXCEPTION = "Occurred exception in the jsoup connection, the word is {}";
    private static final String JSOUP_FETCH_IN_CHINESE_EXCEPTION =
            "Occurred exception in the jsoup fetching in Chinese, the word is {}, Kiwi is trying to fetch in English.";
    private static final String KEY_WORD_HEADER = "ti fs fs12 lmb-0 hw";
    private static final String KEY_WORD_HEADER_IN_ENGLISH = "ti fs fs12 lmb-0 hw superentry";
    private static final String KEY_WORD_NAME = "tb ttn";
    private static final String FETCH_MAIN_WORD_NAME_EXCEPTION = "The word name of {} is not found!";
    private static final String KEY_ROOT = "pr entry-body__el";
    private static final String KEY_ROOT_PHRASE = "entry-body";
    private static final String KEY_ROOT_IDIOM = "di-body";
    private static final String FETCH_ROOT_EXCEPTION = "The {} is not found!";
    private static final String KEY_MAIN_PARAPHRASES = "sense-body dsense_b";
    private static final String FETCH_MAIN_PARAPHRASES_EXCEPTION = "The mainParaphrases of {} is not found!";
    private static final String KEY_CODE_HEADER = "pos-header dpos-h";
    private static final String KEY_CODE_IDIOM_HEADER = "idiom-block";
    private static final String KEY_CODE_IDIOM_HEADER_UNUSED_PREFIX = "pr";
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
    private static final String KEY_MEANING_CHINESE = "trans dtrans dtrans-se  break-cj";
    private static final String FETCH_MEANING_CHINESE_EXCEPTION = "The meaningChinese of {} is not found!";
    private static final String KEY_EXAMPLE_SENTENCES = "examp dexamp";
    private static final String FETCH_EXAMPLE_SENTENCES_EXCEPTION = "The exampleSentences of {} is not found!";
    private static final String KEY_SENTENCE = "eg deg";
    private static final String KEY_SENTENCE_TRANSLATE = "trans dtrans dtrans-se hdb break-cj";
    private static final String KEY_UK_PRONUNCIATIOIN = "uk dpron-i ";
    private static final String KEY_US_PRONUNCIATIOIN = "us dpron-i ";
    private static final String KEY_SOUNDMARK = "pron dpron";
    private static final String KEY_VOICE_FILE_URL = "i-amphtml-fill-content";
    private static final String KEY_SRC = "src";
    private static final String FETCH_PRONUNCIATION_EXCEPTION = "fetch UK's pronunciation exception, the word is {}";
    private static final String KEY_SOURCE = "source";
    private static final String KEY_DAUD = "daud";
    private static final String SOUND_MARK_DEFAULT = "音标缺失";

    private final Map<String, Integer> paraphraseSerialNumMap;
    private final Map<String, Integer> exampleSerialNumMap;
    private final Map<String, CrawlerSourceEnum> crawlerSourceEnumMap;

    public JsoupServiceImpl() {
        this.crawlerSourceEnumMap = Collections.synchronizedMap(new HashMap<>());
        this.paraphraseSerialNumMap = Collections.synchronizedMap(new HashMap<>());
        this.exampleSerialNumMap = Collections.synchronizedMap(new HashMap<>());
    }

    @Override
    public FetchWordResultDTO fetchWordInfo(FetchWordMqDTO dto)
            throws JsoupFetchResultException, JsoupFetchConnectException, JsoupFetchPronunciationException {
        final String finalWord = dto.getWord();
        FetchWordResultDTO result;
        try {
            this.paraphraseSerialNumMap.put(finalWord, 1);
            this.exampleSerialNumMap.put(finalWord, 1);
            this.crawlerSourceEnumMap.put(finalWord, CrawlerSourceEnum.CAMBRIDGE_CHINESE);
            result = fetch(ApiCrawlerConstants.URL_CAMBRIDGE_FETCH_CHINESE, finalWord);
        } catch (JsoupFetchConnectException | JsoupFetchResultException | JsoupFetchPronunciationException e) {
            log.error(JSOUP_FETCH_IN_CHINESE_EXCEPTION, finalWord);
            this.crawlerSourceEnumMap.put(finalWord, CrawlerSourceEnum.CAMBRIDGE_ENGLISH);
            return fetch(ApiCrawlerConstants.URL_CAMBRIDGE_FETCH_ENGLISH, finalWord);
        } finally {
            this.paraphraseSerialNumMap.remove(finalWord);
            this.exampleSerialNumMap.remove(finalWord);
        }
        return result;
    }

    @Override
    public FetchPhraseRunUpResultDTO fetchPhraseRunUp(FetchPhraseRunUpMqDTO dto) throws JsoupFetchConnectException {
        String wordTmp = dto.getWord();
        if (wordTmp.contains(GlobalConstants.SYMBOL_FORWARD_SLASH)) {
            wordTmp = wordTmp.replaceAll(GlobalConstants.SYMBOL_FORWARD_SLASH, GlobalConstants.SYMBOL_RAIL)
                    .replaceAll(GlobalConstants.SPACING, GlobalConstants.SYMBOL_RAIL);
        }
        final String word = wordTmp;

        Document doc;
        try {
            doc = Jsoup.connect(ApiCrawlerConstants.URL_CAMBRIDGE_FETCH_CHINESE
                    + word.replaceAll(GlobalConstants.SPACING, GlobalConstants.SYMBOL_RAIL)).get();
        } catch (IOException e) {
            log.error(JSOUP_CONNECT_EXCEPTION, ApiCrawlerConstants.URL_CAMBRIDGE_FETCH_CHINESE + word);
            throw new JsoupFetchConnectException();
        }

        FetchPhraseRunUpResultDTO result = new FetchPhraseRunUpResultDTO().setWord(word).setWordId(dto.getWordId());
        Set<String> phrases = new HashSet<>();
        Set<String> relatedWords = new HashSet<>();

        /*词组抓取 begin*/
        Optional.ofNullable(doc.getElementsByClass("hax lp-10 lb lb-cm lbt0 dbrowse")).ifPresent(idiomElements -> {
            for (Element element : idiomElements) {
                Optional.ofNullable(element.getElementsByClass("lmb-12")).ifPresent(elements -> {
                    putPhrase(word, phrases, elements, ApiCrawlerConstants.PHRASE_FETCH_VERBOSE_IDIOM,
                            ApiCrawlerConstants.PHRASE_FETCH_VERBOSE_IDIOM_LENGTH);
                });
            }
        });
        /*词组抓取 end*/
        /*习语抓取 begin*/
        Optional.ofNullable(doc.getElementsByClass("xref idioms hax dxref-w lmt-25 lmb-25"))
                .ifPresent(idiomElements -> {
                    for (Element element : idiomElements) {
                        Optional.ofNullable(element.getElementsByClass("item lc lc1 lpb-10 lpr-10")).ifPresent(elements -> {
                            putPhrase(word, phrases, elements, ApiCrawlerConstants.PHRASE_FETCH_VERBOSE_MEAN,
                                    ApiCrawlerConstants.PHRASE_FETCH_VERBOSE_MEAN_LENGTH);
                        });
                    }
                });
        /*习语抓取 end*/
        /*短语动词抓取 begin*/
        Optional.ofNullable(doc.getElementsByClass("xref phrasal_verbs hax dxref-w lmt-25 lmb-25"))
                .ifPresent(idiomElements -> {
                    for (Element element : idiomElements) {
                        Optional.ofNullable(element.getElementsByClass("item lc lc1 lc-xs6-12 lpb-10 lpr-10"))
                                .ifPresent(elements -> {
                                    putPhrase(word, relatedWords, elements, ApiCrawlerConstants.PHRASE_FETCH_VERBOSE_MEAN,
                                            ApiCrawlerConstants.PHRASE_FETCH_VERBOSE_MEAN_LENGTH);
                                });
                    }
                });
        /*短语动词抓取 end*/

        // phrases里面要过滤掉relatedWords里面拥有的
        phrases.removeIf(relatedWords::contains);

        return result.setPhrases(phrases).setRelatedWords(relatedWords);
    }

    @Override
    public FetchPhraseResultDTO fetchPhraseInfo(FetchPhraseMqDTO dto)
            throws JsoupFetchConnectException, JsoupFetchResultException {
        final String finalPhrase = dto.getPhrase();
        String phrase = dto.getPhrase();
        if (phrase.contains(GlobalConstants.SYMBOL_FORWARD_SLASH)) {
            phrase = phrase.replaceAll(GlobalConstants.SYMBOL_FORWARD_SLASH, GlobalConstants.SYMBOL_RAIL)
                    .replaceAll(GlobalConstants.SPACING, GlobalConstants.SYMBOL_RAIL);
        }

        Document doc;
        try {
            doc = Jsoup.connect(ApiCrawlerConstants.URL_CAMBRIDGE_FETCH_CHINESE
                    + phrase.replaceAll(GlobalConstants.SPACING, GlobalConstants.SYMBOL_RAIL)).get();
        } catch (IOException e) {
            log.error(JSOUP_CONNECT_EXCEPTION, phrase);
            throw new JsoupFetchConnectException();
        }

        FetchPhraseResultDTO result = new FetchPhraseResultDTO();
        List<FetchParaphraseDTO> paraphraseDTOs = new LinkedList<>();
        Set<String> relatedWords = new HashSet<>();
        /*词组 begin*/
        Elements paraphraseElements = requireJsoupElements(doc, () -> "idiom-body didiom-body");
        paraphraseElements.addAll(doc.getElementsByClass("pv-body dpv-body"));
        if (KiwiCollectionUtils.isNotEmpty(paraphraseElements)) {
            for (Element idiomElement : paraphraseElements) {
                Elements mainParaphrases = idiomElement.getElementsByClass(KEY_MAIN_PARAPHRASES);
                CrawlerAssertUtils.notEmpty(mainParaphrases, FETCH_PARAPHRASE_ENGLISH_EXCEPTION, finalPhrase);

                // Each label can have many paraphrases
                for (Element paraphrase : mainParaphrases) {
                    FetchParaphraseDTO paraphraseDTO = new FetchParaphraseDTO();
                    // fetch English paraphrase
                    Elements paraphraseEnglish = paraphrase.getElementsByClass(KEY_PARAPHRASE_ENGLISH);
                    CrawlerAssertUtils.notEmpty(paraphraseEnglish, FETCH_PARAPHRASE_ENGLISH_EXCEPTION, finalPhrase);
                    String paraphraseEnglishText = paraphraseEnglish.text();
                    paraphraseDTO.setParaphraseEnglish(paraphraseEnglishText);
                    Elements meaningChinese = paraphrase.getElementsByClass(KEY_MEANING_CHINESE);
                    paraphraseDTO.setMeaningChinese(meaningChinese.text());

                    Elements exampleSentences = paraphrase.getElementsByClass(KEY_EXAMPLE_SENTENCES);
                    if (CollUtil.isNotEmpty(exampleSentences)) {
                        List<FetchParaphraseExampleDTO> exampleDTOList = new LinkedList<>();
                        for (Element sentence : exampleSentences) {
                            subFetchExamples(exampleDTOList, sentence);
                            paraphraseDTO.setExampleDTOList(exampleDTOList);
                        }
                    }
                    paraphraseDTOs.add(paraphraseDTO);
                }
            }
        }
        /*词组 end*/

        /*动词词组、形容词词组能要当做单词去抓取 begin*/
        Optional.ofNullable(doc.getElementsByClass("pr entry-body__el")).ifPresent(elements -> {
            for (Element element : elements) {
                Optional.ofNullable(element.getElementsByClass("headword hdb tw-bw dhw dpos-h_hw "))
                        .ifPresent(singlePhraseElement -> {
                            String word = singlePhraseElement.text().trim();
                            if (KiwiStringUtils.equals(word, finalPhrase)) {
                                return;
                            }
                            relatedWords.add(word);
                        });
            }
        });
        /*动词词组、形容词词组能要当做单词去抓取 end*/

        return result.setFetchParaphraseDTOList(paraphraseDTOs).setPhrase(phrase).setQueueId(dto.getQueueId())
                .setRelatedWords(relatedWords).setDerivation(dto.getDerivation());
    }

    private void putPhrase(String word, Set<String> phrases, Elements phraseElements, String verbose,
                           int verboseLength) {
        if (phraseElements != null && phraseElements.size() > 0) {
            for (Element phraseElement : phraseElements) {
                Elements href = phraseElement.getElementsByTag("a");
                if (href == null || href.size() == 0) {
                    continue;
                }
                Optional.ofNullable(href.get(0).attr("title")).ifPresent(phrase -> {
                    // 如果是单词本身跳过
                    if (KiwiStringUtils.equals(word, phrase)) {
                        return;
                    }
                    // 后面附带"XXX"多余的结尾要去掉
                    if (phrase.endsWith(verbose)) {
                        phrase = phrase.substring(0, phrase.length() - verboseLength);
                    }
                    phrase = phrase.trim();
                    if (KiwiStringUtils.equals(phrase, word)) {
                        return;
                    }
                    phrases.add(phrase);
                });
            }
        }
    }

    private FetchWordResultDTO fetch(String url, String word)
            throws JsoupFetchConnectException, JsoupFetchResultException, JsoupFetchPronunciationException {
        String queryWord = word;
        String jsoupWord;
        if (queryWord.contains(GlobalConstants.SYMBOL_FORWARD_SLASH)) {
            queryWord = queryWord.replaceAll(GlobalConstants.SYMBOL_FORWARD_SLASH, GlobalConstants.SYMBOL_RAIL)
                    .replaceAll(GlobalConstants.SPACING, GlobalConstants.SYMBOL_RAIL);
        }

        Document doc;
        try {
            doc = Jsoup.connect(url + queryWord).get();
        } catch (IOException e) {
            log.error(JSOUP_CONNECT_EXCEPTION, word);
            throw new JsoupFetchConnectException();
        }

        /**
         * Fetch back the important data can not be empty, empty to throw an exception, let the upper logic to do
         * callback processing
         */
        FetchWordResultDTO resultDTO = new FetchWordResultDTO();

        // fetchWordResultDTO 放入 爬虫回来的wordName，不是传进来的wordName
        Elements wordNameHeader = requireJsoupElements(doc, () -> {
            if (CrawlerSourceEnum.CAMBRIDGE_CHINESE.equals(crawlerSourceEnumMap.get(word))) {
                return KEY_WORD_HEADER;
            } else {
                return KEY_WORD_HEADER_IN_ENGLISH;
            }
        });
        CrawlerAssertUtils.notEmpty(wordNameHeader, FETCH_MAIN_WORD_NAME_EXCEPTION, word);
        Elements wordName = requireJsoupElements(wordNameHeader.get(0), () -> KEY_WORD_NAME);
        CrawlerAssertUtils.notEmpty(wordName, FETCH_MAIN_WORD_NAME_EXCEPTION, word);
        jsoupWord = wordName.get(0).text();
        KiwiAssertUtils.assertNotEmpty(jsoupWord, FETCH_MAIN_WORD_NAME_EXCEPTION, word);
        resultDTO.setWordName(jsoupWord);

        AtomicReference<JsoupRootTypeEnum> currentRootType = new AtomicReference<>();
        Elements root = requireJsoupElements(doc, () -> {
            currentRootType.set(JsoupRootTypeEnum.WORD);
            return KEY_ROOT;
        });
        if (root == null || root.size() == 0) {
            root = requireJsoupElements(doc, () -> {
                currentRootType.set(JsoupRootTypeEnum.PHRASE);
                return KEY_ROOT_PHRASE;
            });
        }
        if (root == null || root.size() == 0) {
            root = requireJsoupElements(doc, () -> {
                currentRootType.set(JsoupRootTypeEnum.IDIOM);
                return KEY_ROOT_IDIOM;
            });
        }
        CrawlerAssertUtils.notEmpty(root, FETCH_ROOT_EXCEPTION, word);
        List<FetchWordCodeDTO> codeDTOList = new LinkedList<>();
        boolean isAlreadyFetchPronunciation = false;

        for (Element block : root) {
            Elements mainParaphrases = block.getElementsByClass(KEY_MAIN_PARAPHRASES);
            CrawlerAssertUtils.notEmpty(root, FETCH_MAIN_PARAPHRASES_EXCEPTION, word);

            FetchWordCodeDTO codeDTO = new FetchWordCodeDTO();
            List<FetchWordPronunciationDTO> pronunciationDTOList = new LinkedList<>();
            List<FetchParaphraseDTO> paraphraseDTOList = new LinkedList<>();
            Elements codeHeader = requireJsoupElements(doc, () -> {
                if (currentRootType.get() == JsoupRootTypeEnum.IDIOM) {
                    return KEY_CODE_IDIOM_HEADER;
                }
                return KEY_CODE_HEADER;
            });
            if (currentRootType.get() == JsoupRootTypeEnum.IDIOM
                    && codeHeader.hasClass(KEY_CODE_IDIOM_HEADER_UNUSED_PREFIX)) {
                codeHeader.remove(0);
            }
            // The number of parts of code and label per main paraphrase block can normally only be 1
            // TODO ZSF 大于0的时候这里要特殊处理，比如：flirt，目前只是抓取主要词性，关联单词没有抓到
            CrawlerAssertUtils.mustBeTrue(codeHeader != null && !codeHeader.isEmpty(), FETCH_CODE_HEADER_EXCEPTION,
                    word);
            // TODO ZSF fetchWordResultDTO 放入爬虫回来的wordName，不是传进来的wordName
            final Element header = codeHeader.get(0);
            Optional.ofNullable(header.getElementsByClass(KEY_HEADER_CODE))
                    .flatMap(element -> Optional.ofNullable(element.text())).ifPresent(codeDTO::setCharacterCode);
            Optional.ofNullable(header.getElementsByClass(KEY_HEADER_LABEL))
                    .flatMap(element -> Optional.ofNullable(element.text())).ifPresent(codeDTO::setTag);

            try {
                // fetch UK pronunciation
                Optional
                        .ofNullable(subFetchPronunciation(word, block, KEY_UK_PRONUNCIATIOIN,
                                ApiCrawlerConstants.PRONUNCIATION_TYPE_UK, isAlreadyFetchPronunciation))
                        .ifPresent(pronunciationDTOList::add);
                // fetch US pronunciation
                Optional
                        .ofNullable(subFetchPronunciation(word, block, KEY_US_PRONUNCIATIOIN,
                                ApiCrawlerConstants.PRONUNCIATION_TYPE_US, isAlreadyFetchPronunciation))
                        .ifPresent(pronunciationDTOList::add);
            } catch (JsoupFetchPronunciationException e) {
                if (!isAlreadyFetchPronunciation) {
                    throw e;
                }
            }
            codeDTO.setFetchWordPronunciationDTOList(pronunciationDTOList);
            isAlreadyFetchPronunciation = true;

            // Each label can have many paraphrases
            for (Element paraphrases : mainParaphrases) {
                Elements singleParaphrase = paraphrases.getElementsByClass(KEY_SINGLE_PARAPHRASE);
                Elements phrases = paraphrases.getElementsByClass(KEY_SINGLE_PHRASE);
                boolean singleParaphraseIsNotEmpty = singleParaphrase != null && !singleParaphrase.isEmpty();
                boolean phrasesIsNotEmpty = phrases != null && !phrases.isEmpty();
                CrawlerAssertUtils.mustBeTrue(singleParaphraseIsNotEmpty || phrasesIsNotEmpty,
                        FETCH_SINGLE_PARAPHRASE_EXCEPTION, word);

                subFetchParaphrase(word, paraphraseDTOList, singleParaphrase, phrases, phrasesIsNotEmpty);
            }

            codeDTO.setFetchParaphraseDTOList(paraphraseDTOList);
            codeDTOList.add(codeDTO);
        }

        resultDTO.setFetchWordCodeDTOList(codeDTOList);
        return resultDTO;
    }

    private Elements requireJsoupElements(Element element, Supplier<String> key) {
        return element.getElementsByClass(key.get());
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
            Optional.ofNullable(this.paraphraseSerialNumMap.get(word)).ifPresent(serialNumber -> {
                paraphraseDTO.setSerialNumber(serialNumber);
                this.paraphraseSerialNumMap.put(word, ++serialNumber);
            });

            if (phrasesIsEmpty) {
                List<FetchPhraseDTO> phraseDTOList = new LinkedList<>();
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
            // CrawlerAssertUtils.fetchValueNotEmpty(meaningChinese.text(),
            // FETCH_MEANING_CHINESE_EXCEPTION, word);
            paraphraseDTO.setMeaningChinese(meaningChinese.text());

            // fetch example sentences, it can be empty.
            Elements exampleSentences = paraphrase.getElementsByClass(KEY_EXAMPLE_SENTENCES);
            if (CollUtil.isNotEmpty(exampleSentences)) {
                List<FetchParaphraseExampleDTO> exampleDTOList = new LinkedList<>();
                for (Element sentence : exampleSentences) {
                    FetchParaphraseExampleDTO exampleDTO = subFetchExamples(exampleDTOList, sentence);

                    Optional.ofNullable(this.exampleSerialNumMap.get(word)).ifPresent(serialNumber -> {
                        exampleDTO.setSerialNumber(serialNumber);
                        this.exampleSerialNumMap.put(word, ++serialNumber);
                    });

                    paraphraseDTO.setExampleDTOList(exampleDTOList);
                }
            }

            paraphraseDTOList.add(paraphraseDTO);
        }
    }

    private FetchParaphraseExampleDTO subFetchExamples(List<FetchParaphraseExampleDTO> exampleDTOList,
                                                       Element sentence) {
        FetchParaphraseExampleDTO exampleDTO = new FetchParaphraseExampleDTO();
        Optional.ofNullable(sentence.getElementsByClass(KEY_SENTENCE))
                .ifPresent((Elements elements) -> exampleDTO.setExampleSentence(elements.text()));
        Optional.ofNullable(sentence.getElementsByClass(KEY_SENTENCE_TRANSLATE))
                .ifPresent(elements -> exampleDTO.setExampleTranslate(elements.text()));
        exampleDTO.setTranslateLanguage(ApiCrawlerConstants.DEFAULT_TRANSLATE_LANGUAGE);
        exampleDTOList.add(exampleDTO);
        return exampleDTO;
    }

    private FetchWordPronunciationDTO subFetchPronunciation(String word, Element block, String pronunciationKey,
                                                            String pronunciationType, boolean isAlreadyFetchPronunciation) throws JsoupFetchPronunciationException {
        Elements pronunciations = block.getElementsByClass(pronunciationKey);
        if (pronunciations == null || pronunciations.isEmpty()) {
            return null;
        }

        // try {
        // CrawlerAssertUtils.mustBeTrue(pronunciations != null && pronunciations.size() == 1,
        // FETCH_PRONUNCIATION_EXCEPTION, word);
        // } catch (JsoupFetchResultException e) {
        // throw new JsoupFetchPronunciationException(e);
        // }

        Element ukPronunciation = pronunciations.get(0);

        FetchWordPronunciationDTO pronunciationDTO = new FetchWordPronunciationDTO();
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
            soundMarkSrc = soundMarkSrcElement.get(0).attr(KEY_SRC); // mp3
            // soundMarkSrc = soundMarkSrcElement.get(1).attr(KEY_SRC);// ogg
        }

        return pronunciationDTO.setSoundmarkType(pronunciationType).setSoundmark(soundMark)
                .setVoiceFileUrl(soundMarkSrc);
    }
}
