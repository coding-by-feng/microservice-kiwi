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

package me.fengorz.kiwi.common.api.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import me.fengorz.kiwi.common.api.ReturnCode;

/**
 * @Description 异常基类
 * @Author zhanshifeng
 * @Date 2020/4/8 4:32 PM
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class BaseException extends RuntimeException {
    private static final long serialVersionUID = 6424688358439346362L;
    private Integer code;
    private String i18nCode;
    private Object[] args;

    public BaseException(Throwable cause) {
        super(cause);
    }


    public BaseException(String exceptionMsg, Object... args) {
        super(exceptionMsg);
        this.args = args;
        this.code = ReturnCode.SERVICE_ERROR.getCode();
    }

    public BaseException(String exceptionMsg, String i18nCode, Object... args) {
        super(exceptionMsg);
        this.args = args;
        this.i18nCode = i18nCode;
        this.code = ReturnCode.SERVICE_ERROR.getCode();
    }

    public BaseException(Integer code, String i18nCode, String exceptionMsg, Object... args) {
        super(exceptionMsg);
        this.args = args;
        this.code = code;
        this.i18nCode = i18nCode;
    }

    public BaseException(String exceptionMsg, Throwable throwable, Object... args) {
        super(exceptionMsg, throwable);
        this.args = args;
        this.code = ReturnCode.SERVICE_ERROR.getCode();
    }

    public BaseException(String exceptionMsg, String i18nCode, Throwable throwable, Object... args) {
        super(exceptionMsg, throwable);
        this.args = args;
        this.i18nCode = i18nCode;
        this.code = ReturnCode.SERVICE_ERROR.getCode();
    }

    public BaseException(Integer code, String exceptionMsg, Throwable throwable, Object... args) {
        super(exceptionMsg, throwable);
        this.args = args;
        this.code = code;
    }

    public BaseException(Integer code, String i18nCode, String exceptionMsg, Throwable throwable, Object... args) {
        super(exceptionMsg, throwable);
        this.args = args;
        this.code = code;
        this.i18nCode = i18nCode;
    }

}
