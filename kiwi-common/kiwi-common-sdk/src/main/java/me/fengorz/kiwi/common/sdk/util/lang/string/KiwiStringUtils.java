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

package me.fengorz.kiwi.common.sdk.util.lang.string;

import static java.lang.Character.UnicodeBlock.*;

import org.apache.commons.lang3.StringUtils;

import cn.hutool.core.util.StrUtil;

/**
 * @Author zhanshifeng @Date 2020/5/17 12:39 PM
 */
public class KiwiStringUtils extends StrUtil {

    public static boolean isNotEquals(final CharSequence cs1, final CharSequence cs2) {
        return !equals(cs1, cs2);
    }

    public static boolean isContainChinese(String checkStr) {
        if (StringUtils.isNotBlank(checkStr)) {
            char[] checkChars = checkStr.toCharArray();
            for (char checkChar : checkChars) {
                if (checkCharContainChinese(checkChar)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean checkCharContainChinese(char checkChar) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(checkChar);
        return CJK_UNIFIED_IDEOGRAPHS == ub || CJK_COMPATIBILITY_IDEOGRAPHS == ub || CJK_COMPATIBILITY_FORMS == ub
            || CJK_RADICALS_SUPPLEMENT == ub || CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A == ub
            || CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B == ub;
    }

}
