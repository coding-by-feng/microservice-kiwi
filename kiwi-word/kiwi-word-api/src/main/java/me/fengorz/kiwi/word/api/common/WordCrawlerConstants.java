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

    /**
     * 状态>0，代表当前爬虫正常或者已经完成；
     * 状态<0，代表当前爬虫已经发生异常；
     * 状态=0，达标爬虫等待开始中。
     */
    /*分割线*/
    int STATUS_PARTITION = -1;

    /*单词查询异常*/
    int STATUS_TO_QUERY_ERROR = -404;
    /*Jsoup连接失败*/
    int STATUS_JSOUP_CONNECT_FAILED = -2;
    /*爬虫抓取单词失败*/
    int STATUS_FETCH_FAIL = -3;
    /*删除单词发音文件失败*/
    int STATUS_DEL_PRONUNCIATION_FAIL = -4;
    /*抓取单词基础数据失败*/
    int STATUS_FETCH_WORD_FAIL = -5;
    /*下载单词发音文件失败*/
    int STATUS_TO_FETCH_PRONUNCIATION_FAIL = -6;

    /*待抓取*/
    int STATUS_TO_FETCH = 0;
    /*正在抓取基础数据中*/
    int STATUS_DOING_FETCH = 5;
    /*等待删除单词基础数据*/
    int STATUS_TO_DEL_BASE = 1;
    /*正在删除单词基础数据*/
    int STATUS_DOING_DEL_BASE = 10;
    /*等待删除单词发音文件*/
    int STATUS_TO_DEL_PRONUNCIATION = 2;
    /*正在删除单词发音文件*/
    int STATUS_DOING_DEL_PRONUNCIATION = 20;
    /*等待下载单词发音文件*/
    int STATUS_TO_FETCH_PRONUNCIATION = 3;
    /*正在下载单词发音文件*/
    int STATUS_DOING_FETCH_PRONUNCIATION = 30;
    /*删除单词基础数据失败, 这里失败之后爬虫还是会走后面的流程*/
    int STATUS_DEL_BASE_FAIL = 100;
    /*单词数据抓取完毕*/
    int STATUS_ALL_SUCCESS = 200;

    int WORD_MAX_FETCH_LIMITED_TIME = 10;

    String CAMBRIDGE_FETCH_CHINESE_URL = "https://dictionary.cambridge.org/zhs/词典/英语-汉语-简体/";
    String CAMBRIDGE_FETCH_ENGLISH_URL = "https://dictionary.cambridge.org/dictionary/english/";
    String CAMBRIDGE_BASE_URL = "https://dictionary.cambridge.org/";
    String EXT_OGG = "ogg";

    String DEFAULT_TRANSLATE_LANGUAGE = "Chinese";

    String PRONUNCIATION_TYPE_UK = "UK";
    String PRONUNCIATION_TYPE_US = "US";
}
