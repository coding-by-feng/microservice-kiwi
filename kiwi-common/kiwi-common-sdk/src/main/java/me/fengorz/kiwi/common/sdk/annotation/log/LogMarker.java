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

package me.fengorz.kiwi.common.sdk.annotation.log;

import java.lang.annotation.*;

/**
 * 操作日志注解
 *
 * @Author zhanshifeng
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogMarker {

    String value() default "no default value";

    /**
     * 是否打印方法参数
     *
     * @return
     */
    boolean isPrintParameter() default false;

    /**
     * 是否打印返回结果
     *
     * @return
     */
    boolean isPrintReturnValue() default false;

    /**
     * 是否打印方法执行时间
     *
     * @return
     */
    boolean isPrintExecutionTime() default false;

}
