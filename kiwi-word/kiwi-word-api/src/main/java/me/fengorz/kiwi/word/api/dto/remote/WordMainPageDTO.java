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

package me.fengorz.kiwi.word.api.dto.remote;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import me.fengorz.kiwi.word.api.entity.WordMainDO;

import java.io.Serializable;

/**
 * @Author Kason Zhan @Date 2019/11/2 4:13 PM
 */
@Data
@ToString
@NoArgsConstructor
@Accessors(chain = true)
public class WordMainPageDTO implements Serializable {

    private static final long serialVersionUID = 482725146179897504L;
    private Page page;
    private WordMainDO wordMainDO;

    public WordMainPageDTO(@JsonProperty("page") Page page, @JsonProperty("wordMainDO") WordMainDO wordMainDO) {
        this.page = page;
        this.wordMainDO = wordMainDO;
    }
}
