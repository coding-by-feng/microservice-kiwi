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

package me.fengorz.kiwi.word.api.exception;

import me.fengorz.kiwi.common.api.exception.BaseException;

/**
 * @Author codingByFeng
 * @Date 2019/10/31 4:01 PM
 */
public class JsoupFetchPronunciationException extends BaseException {
    public JsoupFetchPronunciationException() {
    }

    public JsoupFetchPronunciationException(String message) {
        super(message);
    }

    public JsoupFetchPronunciationException(String message, Throwable cause) {
        super(message, cause);
    }

    public JsoupFetchPronunciationException(Throwable cause) {
        super(cause);
    }

    public JsoupFetchPronunciationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}