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

package me.fengorz.kiwi.word.api.common.enumeration;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author Kason Zhan
 * @Date 2022/6/29 15:39
 */
public enum CrawlerStatusEnum {

    /**
     * 状态>0，代表当前爬虫正常或者已经完成； 状态<0，代表当前爬虫已经发生异常； 状态=0，代表爬虫等待开始中。
     */

    /*分割线*/
    STATUS_PARTITION(-1),
    /*查询异常*/
    STATUS_TO_QUERY_ERROR(-404),

    /*Jsoup连接失败*/
    STATUS_JSOUP_CONNECT_FAILED(-2),
    /*爬虫抓取单词失败*/
    STATUS_FETCH_FAIL(-3),
    /*删除发音文件失败*/
    STATUS_DEL_PRONUNCIATION_FAIL(-4),
    /*下载发音文件失败*/
    STATUS_TO_FETCH_PRONUNCIATION_FAIL(-5),
    /*抓取的关联词组失败*/
    STATUS_FETCH_RELATED_PHRASE_FAIL(-6),
    /*抓取词组失败*/
    STATUS_FETCH_PHRASE_FAIL(-7),
    /*删除词组失败*/
    STATUS_DEL_PHRASE_FAIL(-8),

    /*待抓取*/
    STATUS_TO_FETCH(0),
    /*正在抓取基础数据*/
    STATUS_DOING_FETCH(5),
    /*等待删除基础数据*/
    STATUS_TO_DEL_BASE(1),
    /*正在删除基础数据*/
    STATUS_DOING_DEL_BASE(10),
    /*等待删除发音文件*/
    STATUS_TO_DEL_PRONUNCIATION(2),
    /*正在删除发音文件*/
    STATUS_DOING_DEL_PRONUNCIATION(20),
    /*等待下载发音文件*/
    STATUS_TO_FETCH_PRONUNCIATION(3),
    /*正在下载发音文件*/
    STATUS_DOING_FETCH_PRONUNCIATION(30),
    /*删除基础数据失败, 这里失败之后爬虫还是会走后面的流程*/
    STATUS_DEL_BASE_FAIL(100),
    /*数据抓取完毕，待抓取其衍生的词组*/
    STATUS_ALL_SUCCESS(200),
    /*的其他衍生词组已被记录到队列表（不考虑衍生词组是否也被完全爬虫完毕）*/
    STATUS_TO_FETCH_PHRASE(201),
    /**
     * 所有爬虫逻辑都成功了
     */
    STATUS_PERFECT_SUCCESS(666),;

    @Getter
    private final int status;

    CrawlerStatusEnum(int status) {
        this.status = status;
    }

    private static final Map<Integer, CrawlerStatusEnum> STATUS_MAP = new HashMap<>();
    static {
        for (CrawlerStatusEnum crawlerStatusEnum : values()) {
            STATUS_MAP.put(crawlerStatusEnum.getStatus(), crawlerStatusEnum);
        }
    }

    public static CrawlerStatusEnum fromStatus(Integer status) {
        return STATUS_MAP.get(status);
    }

}
