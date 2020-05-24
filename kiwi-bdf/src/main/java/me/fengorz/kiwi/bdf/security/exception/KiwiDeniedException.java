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

package me.fengorz.kiwi.bdf.security.exception;

import lombok.NoArgsConstructor;

/**
 * 403 授权拒绝
 *
 * @Author ZhanShiFeng
 */
@NoArgsConstructor
public class KiwiDeniedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public KiwiDeniedException(String message) {
        super(message);
    }

    public KiwiDeniedException(Throwable cause) {
        super(cause);
    }

    public KiwiDeniedException(String message, Throwable cause) {
        super(message, cause);
    }

    public KiwiDeniedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
