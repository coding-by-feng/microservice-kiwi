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

package me.fengorz.kiwi.common.sdk.util.bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.springframework.beans.BeanUtils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;

/**
 * @Description Bean工具类 @Author zhanshifeng @Date 2019/11/2 4:46 PM
 */
@Slf4j
public class KiwiBeanUtils extends BeanUtils {

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
            return str.substring(0, 1).toUpperCase() + str.substring(1, str.length());
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
        return WordUtils.capitalizeFully(columnName, delimiter.toCharArray()).replace(delimiter, GlobalConstants.EMPTY);
    }

    public static String defaultColumnToBeanProperty(String columnName) {
        return columnToBeanProperty(columnName, GlobalConstants.SYMBOL_DELIMITER_STR);
    }

    public static <T, E> T convertFrom(E source, Class<T> requiredType, String... ignoreProperties) {
        if (source == null) {
            return null;
        } else {
            Object target = null;

            try {
                target = requiredType.newInstance();
                copyProperties(source, target, ignoreProperties);
            } catch (IllegalAccessException | InstantiationException var5) {
                var5.printStackTrace();
            }

            return (T)target;
        }
    }

    public static <T, E> T convertFrom(E source, Class<T> requiredType, Consumer<T> consumer,
        String... ignoreProperties) {
        if (source == null) {
            return null;
        } else {
            Object target = null;

            try {
                target = requiredType.newInstance();
                copyProperties(source, target, ignoreProperties);
                consumer.accept((T)target);
            } catch (IllegalAccessException | InstantiationException var6) {
                var6.printStackTrace();
            }

            return (T)target;
        }
    }

    public static <T, E> List<T> convertFrom(List<E> sourceList, Class<T> requiredType, String... ignoreProperties) {
        if (sourceList == null) {
            return null;
        } else {
            List<T> targetList = new ArrayList();
            if (!sourceList.isEmpty()) {
                sourceList.forEach((source) -> {
                    targetList.add(convertFrom(source, requiredType, ignoreProperties));
                });
            }

            return targetList;
        }
    }

    public static <T, E> List<T> convertFrom(List<E> sourceList, Class<T> requiredType, Consumer<T> consumer,
        String... ignoreProperties) {
        if (sourceList == null) {
            return null;
        } else {
            List<T> targetList = new ArrayList();
            if (!sourceList.isEmpty()) {
                sourceList.forEach((source) -> {
                    targetList.add(convertFrom(source, requiredType, consumer, ignoreProperties));
                });
            }

            return targetList;
        }
    }

    public static <T, E> IPage<T> convertFrom(IPage<E> sourcePage, Class<T> requiredType) {
        if (sourcePage == null) {
            return null;
        } else {
            IPage<T> page = new Page(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal());
            page.setRecords(convertFrom(sourcePage.getRecords(), requiredType));
            return page;
        }
    }

    public static <T, E> IPage<T> convertFrom(IPage<E> sourcePage, Class<T> requiredType, Consumer<T> consumer) {
        if (sourcePage == null) {
            return null;
        } else {
            IPage<T> page = new Page(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal());
            page.setRecords(convertFrom(sourcePage.getRecords(), requiredType, consumer));
            return page;
        }
    }
}
