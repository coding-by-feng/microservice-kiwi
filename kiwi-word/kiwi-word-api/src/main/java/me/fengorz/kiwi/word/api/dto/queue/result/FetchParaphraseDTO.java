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

package me.fengorz.kiwi.word.api.dto.queue.result;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * @Author zhanshifeng @Date 2019/10/25 9:05 AM
 */
@Data
@ToString
public class FetchParaphraseDTO implements Serializable {

    private static final long serialVersionUID = -9079654607482425376L;
    private Integer serialNumber;

    private String codes;

    private String paraphraseEnglish;

    private String paraphraseEnglishTranslate;

    private String meaningChinese;

    private String translateLanguage;

    private List<FetchParaphraseExampleDTO> exampleDTOList;

    private List<FetchPhraseDTO> phraseDTOList;
}
