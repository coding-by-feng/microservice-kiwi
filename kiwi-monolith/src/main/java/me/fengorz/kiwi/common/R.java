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

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Unified API response wrapper.
 *
 * @param <T> the type of the response data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int SUCCESS_CODE = 0;
    public static final int FAIL_CODE = 1;
    public static final String SUCCESS_MSG = "Success";

    /**
     * Response code: 0 for success, non-zero for failure
     */
    private int code;

    /**
     * Response message
     */
    private String msg;

    /**
     * Response data
     */
    private T data;

    /**
     * Create a successful response with data
     */
    public static <T> R<T> ok(T data) {
        return R.<T>builder()
                .code(SUCCESS_CODE)
                .msg(SUCCESS_MSG)
                .data(data)
                .build();
    }

    /**
     * Create a successful response without data
     */
    public static <T> R<T> ok() {
        return ok(null);
    }

    /**
     * Create a successful response with custom message
     */
    public static <T> R<T> ok(T data, String msg) {
        return R.<T>builder()
                .code(SUCCESS_CODE)
                .msg(msg)
                .data(data)
                .build();
    }

    /**
     * Create a failed response with message
     */
    public static <T> R<T> failed(String msg) {
        return R.<T>builder()
                .code(FAIL_CODE)
                .msg(msg)
                .build();
    }

    /**
     * Create a failed response with code and message
     */
    public static <T> R<T> failed(int code, String msg) {
        return R.<T>builder()
                .code(code)
                .msg(msg)
                .build();
    }

    /**
     * Create a failed response with default message
     */
    public static <T> R<T> failed() {
        return failed("Operation failed");
    }

    /**
     * Check if response is successful
     */
    public boolean isSuccess() {
        return SUCCESS_CODE == this.code;
    }

    /**
     * Check if response is failed
     */
    public boolean isFailed() {
        return !isSuccess();
    }
}
