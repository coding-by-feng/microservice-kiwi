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

import java.io.Serializable;
import java.util.List;

/**
* @Author zhanshifeng
 * @Date 2019/11/26 9:33 AM
 */
@Data
@Accessors(chain = true)
@ToString
public class CharacterVO implements Serializable {

    private static final long serialVersionUID = -3201692024160094034L;
    private Integer characterId;
    private String characterCode;
    private String tag;
    private List<ParaphraseVO> paraphraseVOList;
    private List<PronunciationVO> pronunciationVOList;

}
