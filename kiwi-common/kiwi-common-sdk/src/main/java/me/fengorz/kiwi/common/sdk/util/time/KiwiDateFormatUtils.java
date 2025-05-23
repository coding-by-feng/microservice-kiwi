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

package me.fengorz.kiwi.common.sdk.util.time;

import org.apache.commons.lang3.time.DateFormatUtils;

import java.time.format.DateTimeFormatter;

/**
 * @Author Kason Zhan
 * @Date 2020/4/21 8:09 PM
 */
public class KiwiDateFormatUtils extends DateFormatUtils {
    public static final String DATE_FORMATTER_YYYY_MM_DD = "yyyy-MM-dd";

    public static final DateTimeFormatter DATE_TIME_FORMATTER_YYYY_MM_DD =
        DateTimeFormatter.ofPattern(DATE_FORMATTER_YYYY_MM_DD);
}
