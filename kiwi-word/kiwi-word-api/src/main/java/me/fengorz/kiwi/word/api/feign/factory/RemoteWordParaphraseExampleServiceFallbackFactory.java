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

import me.fengorz.kiwi.word.api.feign.IRemoteWordParaphraseExampleService;
import me.fengorz.kiwi.word.api.feign.fallback.RemoteWordParaphraseExampleServiceFallbackImpl;
import feign.hystrix.FallbackFactory;
import org.springframework.stereotype.Component;


/**
 * 单词例句表
 *
 * @author codingByFeng
 * @date 2019-11-01 14:43:28
 */
@Component
public class RemoteWordParaphraseExampleServiceFallbackFactory implements FallbackFactory<IRemoteWordParaphraseExampleService> {
    @Override
    public IRemoteWordParaphraseExampleService create(Throwable throwable) {
        RemoteWordParaphraseExampleServiceFallbackImpl remoteWordParaphraseExampleServiceFallback = new RemoteWordParaphraseExampleServiceFallbackImpl();
        remoteWordParaphraseExampleServiceFallback.setThrowable(throwable);
        return remoteWordParaphraseExampleServiceFallback;
    }
}
