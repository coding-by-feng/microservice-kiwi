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

package me.fengorz.kiwi.common.api.dto;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * @Author Kason Zhan @Date 2020/1/4 9:52 PM
 */
@Data
@ToString
@Accessors(chain = true)
public class PageDTO {

    private long current;
    private long size;
}
