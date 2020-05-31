/*
 *
 *   Copyright [2019~2025] [zhanshifeng]
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

import feign.hystrix.FallbackFactory;
import me.fengorz.kiwi.word.api.feign.IWordParaphraseStarListAPI;
import me.fengorz.kiwi.word.api.feign.fallback.WordParaphraseStarListAPIFallback;
import org.springframework.stereotype.Component;


/**
 * 单词本
 *
 * @author zhanshifeng
 * @date 2019-12-08 23:27:41
 */
@Component
public class RemoteWordParaphraseStarListServiceFallbackFactory implements FallbackFactory<IWordParaphraseStarListAPI> {
    @Override
    public IWordParaphraseStarListAPI create(Throwable throwable) {
        WordParaphraseStarListAPIFallback remoteWordParaphraseStarListServiceFallback = new WordParaphraseStarListAPIFallback();
        remoteWordParaphraseStarListServiceFallback.setThrowable(throwable);
        return remoteWordParaphraseStarListServiceFallback;
    }
}

