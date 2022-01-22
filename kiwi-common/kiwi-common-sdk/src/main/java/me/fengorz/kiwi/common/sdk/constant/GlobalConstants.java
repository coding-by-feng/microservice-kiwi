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

package me.fengorz.kiwi.common.sdk.constant;

import me.fengorz.kiwi.common.api.ApiContants;

/**
 * @Author zhanshifeng @Date 2019-09-19 10:42
 */
public interface GlobalConstants extends ApiContants {

    String SPACING = " ";
    String SYMBOL_DOT = ".";
    String SYMBOL_COMMA = ",";
    String SYMBOL_SQUARE_BRACKET_LEFT = "[";
    String SYMBOL_SQUARE_BRACKET_RIGHT = "]";
    String SYMBOL_FORWARD_SLASH = "/";
    String SYMBOL_BACK_SLASH = "\\";
    String SYMBOL_DELIMITER_STR = "_";
    String SYMBOL_RAIL = "-";
    String SYMBOL_LF = "\n";
    String SYMBOL_PERCENT = "%";

    String FLAG_Y = "Y";
    String FLAG_N = "N";

    int FLAG_DEL_YES = 1;
    int FLAG_DEL_NO = 0;

    int FLAG_YES = 1;
    int FLAG_NO = 0;

    /**
     * 菜单
     */
    String MENU = "0";

    /**
     * 编码
     */
    String UTF8 = "UTF-8";

    /**
     * JSON 资源
     */
    String CONTENT_TYPE = "application/json; charset=utf-8";

    String BASE_PACKAGES = "me.fengorz.kiwi";

    /**
     * 前端工程名
     */
    String FRONT_END_PROJECT = "kiwi-ui";

    /**
     * 后端工程名
     */
    String BACK_END_PROJECT = "microservice-kiwi";

    /**
     * 验证码前缀
     */
    String DEFAULT_CODE_KEY = "DEFAULT_CODE_KEY_";
}
