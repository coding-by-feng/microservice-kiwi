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

package me.fengorz.kiwi.common.sdk.bean;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.fengorz.kiwi.common.api.constant.WordConstant;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.util.Map;

/**
 * @Description TODO
 * @Author codingByFeng
 * @Date 2019/11/2 4:46 PM
 */
public class EnhancedBeanUtils {

    public static Object mapConvertPOJO(Map map, Class pojoClass) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(map, pojoClass);
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
            return str.substring(0, 1).toUpperCase()
                    + str.substring(1, str.length());
        }
        return str;
    }


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

    /**
     * 列名转换成Java属性名
     */
    public static String columnToBeanProperty(String columnName, String delimiter) {
        return WordUtils.capitalizeFully(columnName, delimiter.toCharArray()).replace(delimiter, WordConstant.EMPTY);
    }

    public static String defaultColumnToBeanProperty(String columnName) {
        return columnToBeanProperty(columnName, WordConstant.DELIMITER_STR_);
    }
}
