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

package me.fengorz.kiwi.generator.common;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author Kason Zhan @Date 2020/2/24 11:42 AM
 */
public class ToolConstants {

    public static final String UTF_8 = "UTF-8";
    public static final String DEFAULT_BACK_END_PROJECT = "auto-generate";

    private static Map<String, String> initDataTypeMap() {
        if (DATA_TYPE_MAP == null) {
            Map<String, String> map = new HashMap<>();
            map.put("tinyint", "Integer");
            map.put("smallint", "Integer");
            map.put("mediumint", "Integer");
            map.put("int", "Integer");
            map.put("integer", "Integer");
            map.put("bigint", "Long");
            map.put("float", "Float");
            map.put("double", "Double");
            map.put("decimal", "BigDecimal");
            map.put("bit", "Boolean");
            map.put("char", "String");
            map.put("varchar", "String");
            map.put("tinytext", "String");
            map.put("text", "String");
            map.put("mediumtext", "String");
            map.put("longtext", "String");
            map.put("date", "LocalDateTime");
            map.put("datetime", "LocalDateTime");
            map.put("timestamp", "LocalDateTime");
            return map;
        }
        return DATA_TYPE_MAP;
    }

    public static Map<String, String> DATA_TYPE_MAP = initDataTypeMap();

}
