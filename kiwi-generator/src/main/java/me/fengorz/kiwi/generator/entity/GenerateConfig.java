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
import lombok.experimental.Accessors;

/**
 * @Author zhanshifeng
 * @Date 2020/2/24 1:32 PM
 */
@Data
@Accessors(chain = true)
public class GenerateConfig {

    private String tablePreName;
    private String moduleName;
    private String packageName;
    private String serviceId;
    private String zipPath;

}
