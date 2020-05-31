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
package me.fengorz.kiwi.word.api.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 单词待抓取列表
 *
 * @author zhanshifeng
 * @date 2019-10-31 14:21:21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("word_fetch_queue")
@ToString
@Accessors(chain = true)
public class WordFetchQueueDO extends Model<WordFetchQueueDO> {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    @TableId
    private Integer queueId;
    /**
     * 单词
     */
    private String wordName;
    /**
     * 入库时间
     */
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime inTime;
    /**
     * 优先级，越小越高
     */
    private Integer fetchPriority;
    /**
     * 抓取状态 （0：待抓取，1：抓取中，2：抓取完毕，4：抓取异常）
     */
    private Integer fetchStatus;
    /**
     * 发音文件存放路径
     */
    private String voiceFilePath;
    /**
     * 是否有效标记(Y--正常 N--删除)
     */
    private String isValid;
    /**
     *
     */
    private String fetchResult;
    private Integer isLock;

}
