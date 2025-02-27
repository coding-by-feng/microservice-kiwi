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

package me.fengorz.kiwi.word.api.common.enumeration;

import lombok.AllArgsConstructor;
import me.fengorz.kiwi.common.api.ResultCode;

/**
 * @Description 定义所有异常的Error Code
 * @Author Kason Zhan
 * @Date 2022/4/17 14:37
 */
@AllArgsConstructor
public enum ErrorCodeEnum implements ResultCode {

    QUERY_WORD_GET_ONE_FAILED(101);

    private final Integer code;

    @Override
    public Integer getCode() {
        return this.code;
    }

    @Override
    public String getI18nCode() {
        return null;
    }
}
