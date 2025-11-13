/*
 *
 * Copyright [2019~2025] [zhanshifeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 *
 */

package me.fengorz.kason.bdf.security.exception;

import lombok.NoArgsConstructor;

/**
 * 403 授权拒绝 @Author Kason Zhan
 */
@NoArgsConstructor
public class KasonDeniedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public KasonDeniedException(String message) {
        super(message);
    }

    public KasonDeniedException(Throwable cause) {
        super(cause);
    }

    public KasonDeniedException(String message, Throwable cause) {
        super(message, cause);
    }

    public KasonDeniedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
