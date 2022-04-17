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

package me.fengorz.kiwi.admin.api.feign.factory;

import org.springframework.stereotype.Component;

import feign.hystrix.FallbackFactory;
import me.fengorz.kiwi.admin.api.feign.IUserAPI;
import me.fengorz.kiwi.admin.api.feign.fallback.IUserAPIFallBackImpl;

/**
 * @Author zhanshifeng @Date 2019-09-26 17:03
 */
@Component
public class UserAPIFallBackFactory implements FallbackFactory<IUserAPI> {
    @Override
    public IUserAPI create(Throwable throwable) {
        IUserAPIFallBackImpl remoteUserServiceFallback = new IUserAPIFallBackImpl();
        remoteUserServiceFallback.setThrowable(throwable);
        return remoteUserServiceFallback;
    }
}
