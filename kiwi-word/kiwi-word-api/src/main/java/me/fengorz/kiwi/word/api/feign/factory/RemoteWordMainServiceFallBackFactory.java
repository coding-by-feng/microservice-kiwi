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

package me.fengorz.kiwi.word.api.feign.factory;

import me.fengorz.kiwi.word.api.feign.IRemoteWordMainService;
import me.fengorz.kiwi.word.api.feign.fallback.RemoteWordMainServiceFallbackImpl;
import feign.hystrix.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * @Description TODO
 * @Author codingByFeng
 * @Date 2019/10/30 3:19 PM
 */
@Component
public class RemoteWordMainServiceFallBackFactory implements FallbackFactory<IRemoteWordMainService> {
    @Override
    public IRemoteWordMainService create(Throwable throwable) {
        RemoteWordMainServiceFallbackImpl remoteWordMainServiceFallback = new RemoteWordMainServiceFallbackImpl();
        remoteWordMainServiceFallback.setThrowable(throwable);
        return remoteWordMainServiceFallback;
    }
}