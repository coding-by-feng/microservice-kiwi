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
package me.fengorz.kiwi.word.api.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 单词例句表
 *
 * @author zhanshifeng
 * @date 2019-11-27 10:40:46
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("word_pronunciation")
@Accessors(chain = true)
public class PronunciationDO extends Model<PronunciationDO> {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    @TableId
    private Integer pronunciationId;
    /**
     * 单词ID
     */
    private Integer wordId;
    /**
     * 音标
     */
    private String soundmark;
    /**
     * 音标类别
     */
    private String soundmarkType;
    /**
     * 发音文件存放路径
     */
    private String voiceFilePath;
    /**
     * 逻辑删除标记
     */
    private Integer isDel;
    /**
     * 分布式文件系统存放的组名
     */
    private String groupName;
    /**
     *
     */
    private Integer characterId;

    private String sourceUrl;
}
