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

package me.fengorz.kiwi.admin.api.feign.factory;

import feign.hystrix.FallbackFactory;
import me.fengorz.kiwi.admin.api.feign.RemoteUserService;
import me.fengorz.kiwi.admin.api.feign.fallback.RemoteUserServiceFallBackImpl;
import org.springframework.stereotype.Component;

/**
 * @Author codingByFeng
 * @Date 2019-09-26 17:03
 */
@Component
public class RemoteUserServiceFallBackFactory implements FallbackFactory<RemoteUserService> {
    @Override
    public RemoteUserService create(Throwable throwable) {
        RemoteUserServiceFallBackImpl remoteUserServiceFallback = new RemoteUserServiceFallBackImpl();
        remoteUserServiceFallback.setThrowable(throwable);
        return remoteUserServiceFallback;
    }
}
