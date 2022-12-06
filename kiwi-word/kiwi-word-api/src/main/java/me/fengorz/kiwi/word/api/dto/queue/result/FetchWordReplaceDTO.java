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

package me.fengorz.kiwi.word.api.dto.queue.result;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @Author zhanshifeng @Date 2020/6/19 4:45 PM
 */
@Data
@Accessors(chain = true)
public class FetchWordReplaceDTO implements Serializable {
    private static final long serialVersionUID = -7940740618350547699L;
    private Integer newRelWordId;
    private Integer oldRelWordId;
    private Map<Integer, Binder> paraphraseBinderMap;
    private Map<Integer, Binder> exampleBinderMap;

    public FetchWordReplaceDTO() {
        this.setParaphraseBinderMap(new HashMap<>());
        this.setExampleBinderMap(new HashMap<>());
    }

    public static class Binder implements Serializable {

        private static final long serialVersionUID = 3541504398792790251L;

        private Integer oldId;
        private Integer newId;

        public Integer getOldId() {
            return oldId;
        }

        public Binder setOldId(Integer oldId) {
            this.oldId = oldId;
            return this;
        }

        public Integer getNewId() {
            return newId;
        }

        public Binder setNewId(Integer newId) {
            this.newId = newId;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Binder that = (Binder)o;
            return Objects.equals(oldId, that.oldId) && Objects.equals(newId, that.newId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(oldId, newId);
        }
    }
}
