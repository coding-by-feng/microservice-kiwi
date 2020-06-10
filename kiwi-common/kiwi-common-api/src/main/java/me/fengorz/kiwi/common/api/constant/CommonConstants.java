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

package me.fengorz.kiwi.common.api.constant;

/**
 * @Author zhanshifeng
 * @Date 2019-09-19 10:42
 */
public interface CommonConstants {

    String EMPTY = "";
    String SYMBOL_DOT = ".";
    String SYMBOL_COMMA = ",";
    String SYMBOL_SQUARE_BRACKET_LEFT = "[";
    String SYMBOL_SQUARE_BRACKET_RIGHT = "]";
    String SYMBOL_FORWARD_SLASH = "/";
    String SYMBOL_BACK_SLASH = "\\";
    String SYMBOL_DELIMITER_STR = "_";

    String FLAG_Y = "Y";
    String FLAG_N = "N";

    int FLAG_DEL_YES = 1;
    int FLAG_DEL_NO = 0;

    int FLAG_YES = 1;
    int FLAG_NO = 0;

    /**
     * 成功标记
     */
    Integer RESULT_CODE_SUCCESS = 1;

    Integer RESULT_CODE_FAIL = 0;

    /**
     * 错误标记
     */
    Integer RESULT_CODE_SERVICE_ERROR = -1;

    /**
     * 服务逻辑错误
     */
    Integer RESULT_CODE_SERVICE_LOGIC_ERROR = -2;

    /**
     * 服务之间调用错误
     */
    Integer RESULT_CODE_INVOCATION_ERROR = -3;

    /**
     * 没有权限
     */
    Integer RESULT_CODE_NOT_PRIVILEGE = -4;

    /**
     * 未登录
     */
    Integer RESULT_CODE_NOT_LOGIN = -5;

    /**
     * 登录超时
     */
    Integer RESULT_CODE_LOGIN_TIMEOUT = -6;

    /**
     * url无效
     */
    Integer RESULT_CODE_INVALID_URL = -7;

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
