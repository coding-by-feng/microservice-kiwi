/*
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.fengorz.kiwi.word.api.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import me.fengorz.kiwi.common.api.valid.ValidTypeInsert;
import me.fengorz.kiwi.common.api.valid.ValidTypeUpdate;

import javax.validation.constraints.NotNull;

/**
 * 词组主表
 *
 * @author zhanShiFeng
 * @date 2020-10-10 20:09:06
 */
@Data
@ApiModel
@EqualsAndHashCode(callSuper = true)
@TableName("phrase_main")
@Accessors(chain = true)
public class PhraseMainDO extends Model<PhraseMainDO> {

    private static final long serialVersionUID = 1602331746772L;

    /**
     *
     */
    @TableId
    @NotNull(groups = {ValidTypeUpdate.class})
    private Integer phraseId;

    /**
     *
     */
    @NotNull(groups = {ValidTypeInsert.class})
    private Integer wordId;

    /**
     * 英文释义
     */
    private String paraphraseEnglish;

    /**
     * 英文释义翻译
     */
    private String paraphraseEnglishTranslate;

    /**
     * 中文词义
     */
    private String meaningChinese;

    /**
     * 翻译语种
     */
    private String translateLanguage;

    /**
     * 逻辑删除标记
     */
    private Integer isDel;
}
