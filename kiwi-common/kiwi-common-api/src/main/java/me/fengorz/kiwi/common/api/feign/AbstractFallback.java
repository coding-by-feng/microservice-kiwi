/*
 *
 * Copyright [2019~2025] [codingByFeng]
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

package me.fengorz.kiwi.common.api.feign;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;

/**
 * @Description TODO
 * @Author Kason Zhan
 * @Date 2022/4/24 15:11
 */
@Slf4j
public class AbstractFallback {

    @Setter
    protected Throwable throwable;

    protected <T> R<T> handleError() {
        // log.error(throwable.getCause().getMessage());
        log.error(throwable.getMessage());
        return R.feignCallFailed(throwable.getMessage());
    }

    protected void handleErrorNotReturn() {
        // log.error(throwable.getCause().getMessage());
        log.error(throwable.getMessage());
        R.feignCallFailed(throwable.getMessage());
    }

}
