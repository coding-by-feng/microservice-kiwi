/*
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.fengorz.kiwi.common.constant;

/**
 * API Constants
 *
 * @author codingByFeng
 */
public interface ApiConstants {

    String ADMIN_USERNAME = "admin";
    Integer ADMIN_ID = 1;
    String EMPTY = "";

    /** Success code */
    Integer RESULT_CODE_SUCCESS = 1;

    /** Fail code */
    Integer RESULT_CODE_FAIL = 0;

    /** Service error code */
    Integer RESULT_CODE_SERVICE_ERROR = -1;

    /** Service logic error */
    Integer RESULT_CODE_SERVICE_LOGIC_ERROR = -2;

    /** Microservice invocation error */
    Integer RESULT_CODE_INVOCATION_ERROR = -3;

    /** No privilege */
    Integer RESULT_CODE_NOT_PRIVILEGE = -4;

    /** Not logged in */
    Integer RESULT_CODE_NOT_LOGIN = -5;

    /** Login timeout */
    Integer RESULT_CODE_LOGIN_TIMEOUT = -6;

    /** Invalid URL */
    Integer RESULT_CODE_INVALID_URL = -7;
}
