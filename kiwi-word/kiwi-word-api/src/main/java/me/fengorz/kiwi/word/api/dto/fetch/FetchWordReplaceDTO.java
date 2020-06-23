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

package me.fengorz.kiwi.word.api.dto.fetch;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @Author zhanshifeng
 * @Date 2020/6/19 4:45 PM
 */
@Data
@Accessors(chain = true)
public class FetchWordReplaceDTO implements Serializable {
    private static final long serialVersionUID = -7940740618350547699L;

    public FetchWordReplaceDTO() {
        this.setNewParaphraseIdMap(new HashMap<>());
        this.setOldParaphraseIdMap(new HashMap<>());
        this.setNewExampleIdMap(new HashMap<>());
        this.setOldExampleIdMap(new HashMap<>());
    }

    private Integer newRelWordId;
    private Integer oldRelWordId;
    private Map<String, Integer> oldParaphraseIdMap;
    private Map<String, Integer> newParaphraseIdMap;
    private Map<String, Integer> oldExampleIdMap;
    private Map<String, Integer> newExampleIdMap;

}
