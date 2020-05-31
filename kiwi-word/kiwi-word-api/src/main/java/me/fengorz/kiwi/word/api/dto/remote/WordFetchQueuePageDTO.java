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

package me.fengorz.kiwi.word.api.dto.remote;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import me.fengorz.kiwi.word.api.entity.WordFetchQueueDO;

import javax.validation.constraints.NotNull;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2019/11/9 5:47 PM
 */
@Data
@ToString
@NoArgsConstructor
@Accessors(chain = true)
public class WordFetchQueuePageDTO {

    @NotNull
    private Page page;
    private WordFetchQueueDO wordFetchQueue;

    public WordFetchQueuePageDTO(@JsonProperty("page") Page page, @JsonProperty("wordFetchQueue") WordFetchQueueDO wordFetchQueue) {
        this.page = page;
        this.wordFetchQueue = wordFetchQueue;
    }
}
