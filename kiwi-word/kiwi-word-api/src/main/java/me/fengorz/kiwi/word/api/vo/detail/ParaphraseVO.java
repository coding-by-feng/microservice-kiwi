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

package me.fengorz.kiwi.word.api.vo.detail;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import me.fengorz.kiwi.word.api.vo.ParaphraseExampleVO;

import java.io.Serializable;
import java.util.List;

/**
* @Author zhanshifeng
 * @Date 2019/11/26 9:45 AM
 */
@Data
@Accessors(chain = true)
@ToString
public class ParaphraseVO implements Serializable {

    private static final long serialVersionUID = 1358094160893456358L;

    private String codes;

    private String wordName;

    // TODO ZSF 这里暂时没有和DO字段名同步
    private String wordCharacter;

    // TODO ZSF 这里暂时没有和DO字段名同步
    private String wordLabel;

    private List<String> phraseList;

    private Integer paraphraseId;
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

    private String isCollect;

    private Boolean isOverlength;

    private List<ParaphraseExampleVO> exampleVOList;

    private List<PronunciationVO> pronunciationVOList;

}
