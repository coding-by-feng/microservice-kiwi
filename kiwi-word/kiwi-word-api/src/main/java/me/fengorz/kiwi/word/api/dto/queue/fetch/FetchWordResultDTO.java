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

package me.fengorz.kiwi.word.api.dto.queue.fetch;

import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2019/10/24 10:26 PM
 */
@Data
@ToString
@Accessors(chain = true)
public class FetchWordResultDTO {

    @NotBlank(message = "抓取不到剑桥词典对应的单词数据")
    private String wordName;

    @NotNull(message = "队列id不能为空")
    private Integer queueId;

    @NotEmpty(message = "抓取不到单词对应的词性数据")
    private List<FetchWordCodeDTO> fetchWordCodeDTOList;

}
