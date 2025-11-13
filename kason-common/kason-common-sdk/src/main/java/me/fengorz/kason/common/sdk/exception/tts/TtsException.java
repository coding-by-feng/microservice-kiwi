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

package me.fengorz.kason.common.sdk.exception.tts;

import me.fengorz.kason.common.api.ResultCode;
import me.fengorz.kason.common.sdk.exception.BaseException;

/**
 * @Description TODO
 * @Author Kason Zhan
 * @Date 2022/7/5 22:53
 */
public class TtsException extends BaseException {

    private static final long serialVersionUID = -4427505576163765302L;

    public TtsException() {
    }

    public TtsException(Throwable cause) {
        super(cause);
    }

    public TtsException(String msg, Object... args) {
        super(msg, args);
    }

    public TtsException(String msg, Throwable throwable, Object... args) {
        super(msg, throwable, args);
    }

    public TtsException(ResultCode resultCode, Object... args) {
        super(resultCode, args);
    }

    public TtsException(String msg, ResultCode resultCode, Object... args) {
        super(msg, resultCode, args);
    }

    public TtsException(String msg, ResultCode resultCode, Throwable throwable, Object... args) {
        super(msg, resultCode, throwable, args);
    }
}
