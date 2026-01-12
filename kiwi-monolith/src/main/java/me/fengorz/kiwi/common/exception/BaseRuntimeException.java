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
package me.fengorz.kiwi.common.exception;

import lombok.Getter;
import me.fengorz.kiwi.common.ResultCode;
import org.apache.commons.lang3.StringUtils;

/**
 * Base Runtime Exception
 *
 * @author codingByFeng
 */
@Getter
public class BaseRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 6424688358439346362L;

    protected Object[] args;
    protected ResultCode resultCode;

    public BaseRuntimeException() {
        super();
        this.resultCode = ResultCode.FAIL;
    }

    public BaseRuntimeException(Throwable cause) {
        super(cause);
        this.resultCode = ResultCode.FAIL;
    }

    public BaseRuntimeException(String msg, Object... args) {
        super(formatMessage(msg, args));
        this.args = args;
        this.resultCode = ResultCode.FAIL;
    }

    public BaseRuntimeException(String msg, Throwable throwable, Object... args) {
        super(formatMessage(msg, args), throwable);
        this.args = args;
        this.resultCode = ResultCode.FAIL;
    }

    public BaseRuntimeException(ResultCode resultCode, Object... args) {
        super();
        this.args = args;
        this.resultCode = resultCode;
    }

    public BaseRuntimeException(String msg, ResultCode resultCode, Object... args) {
        super(formatMessage(msg, args));
        this.args = args;
        this.resultCode = resultCode;
    }

    public BaseRuntimeException(String msg, ResultCode resultCode, Throwable throwable, Object... args) {
        super(formatMessage(msg, args), throwable);
        this.args = args;
        this.resultCode = resultCode;
    }

    @Override
    public String getMessage() {
        if (args == null) {
            return super.getMessage();
        } else {
            String message = StringUtils.defaultString(super.getMessage(), "");
            return String.format(message, args);
        }
    }

    private static String formatMessage(String template, Object... args) {
        if (template == null || args == null || args.length == 0) {
            return template;
        }
        // Simple placeholder replacement for {} patterns
        String result = template;
        for (Object arg : args) {
            result = result.replaceFirst("\\{}", arg != null ? arg.toString() : "null");
        }
        return result;
    }
}
