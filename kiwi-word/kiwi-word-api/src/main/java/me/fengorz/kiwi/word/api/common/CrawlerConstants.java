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

package me.fengorz.kiwi.word.api.common;

public interface CrawlerConstants {

    String VOCABULARY_ENHANCER_CRAWLER_BIZ = "kiwi-word-biz";

    /*待抓取*/
    int STATUS_TO_FETCH = 0;
    /*抓取中*/
    int STATUS_FETCHING = 1;
    /*已抓取*/
    int STATUS_FETCHED = 2;
    /*抓取异常*/
    int STATUS_ERROR = 400;
    /*wordId空异常*/
    int STATUS_ERROR_WORD_ID_NOT_NULL = 401;
    /*fastDfs操作异常*/
    int STATUS_ERROR_DFS_OPERATE_FAILED = 402;
    int STATUS_ERROR_DFS_OPERATE_DELETE_FAILED = 402;
    /*Jsoup连接失败*/
    int STATUS_ERROR_JSOUP_FETCH_CONNECT_FAILED = 403;
    /*Jsoup抓取结果失败*/
    int STATUS_ERROR_JSOUP_RESULT_FETCH_FAILED = 404;

    String CRAWLER_BASE_URL = "https://dictionary.cambridge.org/zhs/词典/英语-汉语-简体/";
    String CAMBRIDGE_BASE_URL = "https://dictionary.cambridge.org/";
    String CRAWLER_VOICE_BASE_PATH = "/Users/zhanshifeng/Documents/myDocument/temp/microSerivceVocabularyEnhancer";
    String EXT_OGG = "ogg";

    String DEFAULT_TRANSLATE_LANGUAGE = "Chinese";

    String PRONUNCIATION_TYPE_UK = "UK";
    String PRONUNCIATION_TYPE_US = "US";
}