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

package me.fengorz.kiwi.word.api.common;

public interface WordCrawlerConstants {

    String RECORDS = "records";

    /*待抓取*/
    int STATUS_TO_FETCH = 0;
    /*Jsoup连接失败*/
    int STATUS_JSOUP_CONNECT_FAILED = 1;
    /*爬虫抓取单词失败*/
    int STATUS_FETCH_FAIL = 10;
    /*等待删除单词基础数据*/
    int STATUS_TO_DEL_BASE = 20;
    /*删除单词基础数据失败*/
    int STATUS_DEL_BASE_FAIL = 30;
    /*等待删除单词发音文件*/
    int STATUS_TO_DEL_PRONUNCIATION = 40;
    /*删除单词发音文件失败*/
    int STATUS_DEL_PRONUNCIATION_FAIL = 50;
    /*抓取单词基础数据失败*/
    int STATUS_FETCH_WORD_FAIL = 70;
    /*等待下载单词发音文件*/
    int STATUS_TO_FETCH_PRONUNCIATION = 80;
    /*单词数据抓取完毕*/
    int STATUS_ALL_SUCCESS = 100;

    // TODO ZSF 删掉
    /*抓取中*/
    int STATUS_FETCHING = 1;
    /*抓取成功*/
    int STATUS_SUCCESS = 2;
    /*抓取异常*/
    int STATUS_ERROR = 400;
    /*wordId空异常*/
    int STATUS_ERROR_WORD_ID_NOT_NULL = 401;
    /*fastDfs操作异常*/
    int STATUS_ERROR_DFS_OPERATE_FAILED = 402;
    int STATUS_ERROR_DFS_OPERATE_DELETE_FAILED = 404;
    /*Jsoup连接失败*/
    int STATUS_ERROR_JSOUP_FETCH_CONNECT_FAILED = 405;
    /*Jsoup抓取结果失败*/
    int STATUS_ERROR_JSOUP_RESULT_FETCH_FAILED = 406;

    String CAMBRIDGE_FETCH_CHINESE_URL = "https://dictionary.cambridge.org/zhs/词典/英语-汉语-简体/";
    String CAMBRIDGE_FETCH_ENGLISH_URL = "https://dictionary.cambridge.org/dictionary/english/";
    String CAMBRIDGE_BASE_URL = "https://dictionary.cambridge.org/";
    String EXT_OGG = "ogg";

    String DEFAULT_TRANSLATE_LANGUAGE = "Chinese";

    String PRONUNCIATION_TYPE_UK = "UK";
    String PRONUNCIATION_TYPE_US = "US";
}
