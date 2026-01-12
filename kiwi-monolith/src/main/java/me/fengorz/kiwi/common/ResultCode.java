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
package me.fengorz.kiwi.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import me.fengorz.kiwi.common.constant.ApiConstants;

import java.util.function.Supplier;

/**
 * Result Code interface for API responses
 *
 * @author codingByFeng
 */
public interface ResultCode {

    String I18N_CODE_SUCCESS = "common.operate.success";
    String I18N_CODE_FAIL = "common.operate.fail";
    String I18N_CODE_ERROR = "common.operate.error";

    /** Success */
    ResultCode SUCCESS = build(() -> ApiConstants.RESULT_CODE_SUCCESS, () -> I18N_CODE_SUCCESS);

    /** Failure */
    ResultCode FAIL = build(() -> ApiConstants.RESULT_CODE_FAIL, () -> I18N_CODE_FAIL);

    /** Error */
    ResultCode ERROR = () -> I18N_CODE_ERROR;

    /** Microservice invocation error */
    ResultCode MICROSERVICE_INVOCATION_ERROR =
            build(() -> ApiConstants.RESULT_CODE_INVOCATION_ERROR, () -> "common.operate.microservice.error");

    static ResultCode build(Supplier<Integer> code, Supplier<String> i18nCode) {
        return new ResultCode() {
            @Override
            public String getI18nCode() {
                return i18nCode.get();
            }

            @Override
            public Integer getCode() {
                return code.get();
            }
        };
    }

    /**
     * Response code
     */
    default Integer getCode() {
        return ApiConstants.RESULT_CODE_SERVICE_ERROR;
    }

    /**
     * Message internationalization code
     */
    @JsonIgnore
    String getI18nCode();
}
