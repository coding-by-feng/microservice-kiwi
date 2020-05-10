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

package me.fengorz.kiwi.common.api;

/**
 * @Description 响应体编码
 * @Author zhanshifeng
 * @Date 2020/4/8 4:48 PM
 */
public enum ReturnCode {
    SUCCESS(1),
    SERVICE_ERROR(-1),
    SERVICE_LOGIC_ERROR(-2),
    MICROSERVICE_INVOCATION_ERROR(-3),
    LOGIN_TIMEOUT(-4),
    NOT_PRIVILEGE(-5),
    NOT_LOGIN(-6),
    INVALID_URL(-7);

    private final Integer code;

    ReturnCode(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return this.code;
    }
}
