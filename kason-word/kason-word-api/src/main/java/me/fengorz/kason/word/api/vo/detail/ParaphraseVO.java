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

package me.fengorz.kason.word.api.vo.detail;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import me.fengorz.kason.word.api.vo.ParaphraseExampleVO;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.util.List;

/**
 * @Author Kason Zhan @Date 2019/11/26 9:45 AM
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ParaphraseVO implements Serializable {

    private static final long serialVersionUID = 1358094160893456358L;

    @Field(type = FieldType.Keyword)
    private String codes;

    private Integer wordId;

    private Integer characterId;

    @Field(type = FieldType.Keyword)
    private String wordName;

    // TODO ZSF 这里暂时没有和DO字段名同步
    @Field(type = FieldType.Keyword)
    private String wordCharacter;

    // TODO ZSF 这里暂时没有和DO字段名同步
    @Field(type = FieldType.Keyword)
    private String wordLabel;

    private List<String> phraseList;

    @Field(type = FieldType.Keyword)
    private Integer paraphraseId;
    /**
     * 英文释义
     */
    @Field(type = FieldType.Text)
    private String paraphraseEnglish;
    /**
     * 英文释义翻译
     */
    @Field(type = FieldType.Text)
    private String paraphraseEnglishTranslate;
    /**
     * 中文词义
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String meaningChinese;

    private String isCollect;

    private Boolean isOverlength;

    @Field(type = FieldType.Nested)
    private List<ParaphraseExampleVO> exampleVOList;

    @Field(index = false)
    private List<PronunciationVO> pronunciationVOList;
}
