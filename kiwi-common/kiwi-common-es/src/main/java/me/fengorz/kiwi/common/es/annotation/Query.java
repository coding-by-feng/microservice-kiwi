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
package me.fengorz.kiwi.common.es.annotation;

import java.lang.annotation.*;

/**
 * 用于查询条件的注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Inherited
public @interface Query {
    /**
     * 查询类型
     */
    QueryType value() default QueryType.TERM;

    /**
     * ES字段名， 默认与实体的名字相同
     */
    String name() default "";

    /**
     * 嵌套路径
     */
    String nestedPath() default "";

    /**
     * 用于{@link QueryType.BETWEEN}指定TO字段名
     */
    String toField() default "";
}
