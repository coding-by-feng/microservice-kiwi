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

package me.fengorz.kiwi.word.api.vo.star;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2020/1/6 2:09 PM
 */
@Data
@Accessors(chain = true)
@ToString
public class ExampleStarItemVO {

    private Integer wordId;
    private String wordName;
    private Integer exampleId;
    private String exampleSentence;
    private String exampleTranslate;

}