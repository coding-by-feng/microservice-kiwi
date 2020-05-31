/*
 *
 *   Copyright [2019~2025] [zhanshifeng]
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

package me.fengorz.kiwi.generator.util;

import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

/**
 * @Author zhanshifeng
 */
public class ToolBeanUtils {

    public static String columnToBeanProperty(String columnName, String delimiter) {
        return WordUtils.capitalizeFully(columnName, delimiter.toCharArray()).replace(delimiter, Constants.EMPTY);
    }

    public static String defaultColumnToBeanProperty(String columnName) {
        return columnToBeanProperty(columnName, Constants.UNDERSCORE);
    }

    public static String firstUpperCamelCase(String str, String preName) {
        if (StringUtils.isNotBlank(str)) {
            str = str.replaceFirst(preName, "");
            str = str.toLowerCase();
            String[] strs = str.split("_");
            if (strs.length == 1) {
                return firstLetterUpper(str);
            } else {
                String convertedStr = "";
                for (int i = 0; i < strs.length; i++) {
                    convertedStr += firstLetterUpper(strs[i]);
                }
                return convertedStr;
            }
        }
        return str;
    }

    @Deprecated
    public static String firstLowerCamelCase(String str) {
        if (StringUtils.isNotBlank(str)) {
            str = str.replace("T_", "");
            str = str.toLowerCase();
            String[] strs = str.split("_");
            if (strs.length == 1) {
                return allLower(str);
            } else {
                String convertedStr = "";
                for (int i = 1; i < strs.length; i++) {
                    convertedStr += firstLetterUpper(strs[i]);
                }
                return strs[0] + convertedStr;
            }
        }
        return str;
    }

    public static String firstLetterUpper(String str) {
        if (StringUtils.isNotBlank(str)) {
            str = str.replace("T_", "");
            str = str.toLowerCase();
            return str.substring(0, 1).toUpperCase() + str.substring(1, str.length());
        }
        return str;
    }

    @Deprecated
    public static String allUpper(String str) {
        if (StringUtils.isNotBlank(str)) {
            str = str.replace("T_", "");
            str = str.toLowerCase();
            String[] strs = str.split("_");
            if (strs.length == 1) {
                return str.toUpperCase();
            } else {
                String convertedStr = "";
                for (int i = 0; i < strs.length; i++) {
                    convertedStr += strs[i].toUpperCase();
                }
                return convertedStr;
            }
        }
        return str;
    }

    public static String allLower(String str) {
        if (StringUtils.isNotBlank(str)) {
            str = str.replace("T_", "");
            str = str.toLowerCase();
            String[] strs = str.split("_");
            if (strs.length == 1) {
                return str.toLowerCase();
            } else {
                String convertedStr = "";
                for (int i = 0; i < strs.length; i++) {
                    convertedStr += strs[i].toLowerCase();
                }
                return convertedStr;
            }
        }
        return str;
    }
}
