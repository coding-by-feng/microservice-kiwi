/*
 *
 * Copyright [2019~2025] [codingByFeng]
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

package me.fengorz.kiwi.common.api;

/**
 * @Author zhanshifeng @Date 2021/12/27 10:35 PM
 */
public interface ApiContants {

    String ADMIN_USERNAME = "admin";
    String EMPTY = "";

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
}
