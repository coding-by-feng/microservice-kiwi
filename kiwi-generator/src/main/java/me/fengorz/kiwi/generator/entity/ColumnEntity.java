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

package me.fengorz.kiwi.generator.entity;

import lombok.Data;

/**
 * @Author zhanshifeng
 * @Date 2019-09-10 19:15
 */
@Data
public class ColumnEntity {
    /**
     * 列表
     */
    private String columnName;
    /**
     * 大写的字段名
     */
    private String columnNameUpper;
    /**
     * 数据类型
     */
    private String dataType;
    /**
     * 备注
     */
    private String comments;

    /**
     * 驼峰属性
     */
    private String caseAttrName;
    /**
     * 普通属性
     */
    private String lowerAttrName;
    /**
     * 属性类型
     */
    private String attrType;
    /**
     * 其他信息。
     */
    private String extra;

    /**
     * 是否能为空
     */
    private String isNullable;
    private String columnDefault;
}
