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

package me.fengorz.kiwi.test;

import lombok.Data;
import lombok.experimental.Accessors;
import me.fengorz.kiwi.common.sdk.util.json.KiwiJsonUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.HashMap;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2022/11/12 21:27
 */
public class DeepCopyTest {

    @Test
    public void mapTest() {
        Model model = new Model();
        model.setName("test1");
        model.setChildModel(new ChildModel().setName("child1"));
        HashMap<String, Model> originMap = new HashMap<>();
        originMap.put("test", model);
        HashMap<String, Model> updatedFromConstructorMap = new HashMap<>(originMap);
        HashMap<String, Model> updatedFromSerializationMap = SerializationUtils.clone(originMap);
        updatedFromSerializationMap.get("test").getChildModel().setName("child2");
        System.out.println(KiwiJsonUtils.toJsonStr(originMap));

        model.setName("test2");
        System.out.println(KiwiJsonUtils.toJsonStr(originMap));
        System.out.println(KiwiJsonUtils.toJsonStr(updatedFromConstructorMap));
        System.out.println(KiwiJsonUtils.toJsonStr(updatedFromSerializationMap));
    }

    @Data
    private static class Model implements Serializable {
        private static final long serialVersionUID = -6407178064601811647L;
        private String name;
        private ChildModel childModel;
    }

    @Data
    @Accessors(chain = true)
    private static class ChildModel implements Serializable {
        private static final long serialVersionUID = -8622029563295806497L;
        private String name;
    }

}
