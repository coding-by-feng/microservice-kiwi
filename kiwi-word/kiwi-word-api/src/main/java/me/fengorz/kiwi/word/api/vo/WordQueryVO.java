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

package me.fengorz.kiwi.word.api.vo;

import me.fengorz.kiwi.common.api.constant.CommonConstants;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @Description A view of the word query results
 * @Author zhanshifeng
 * @Date 2019/11/25 10:57 PM
 */
@Data
@Accessors(chain = true)
@ToString
public class WordQueryVO {

    private Integer wordId;
    private String wordName;
    private String isCollect;
    private String isLogin;
    private List<WordCharacterVO> wordCharacterVOList;

}