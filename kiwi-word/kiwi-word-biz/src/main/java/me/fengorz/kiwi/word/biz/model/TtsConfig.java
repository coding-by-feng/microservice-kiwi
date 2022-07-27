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

package me.fengorz.kiwi.word.biz.model;

import java.util.Set;

import org.apache.commons.collections4.SetUtils;

import lombok.Getter;
import lombok.Setter;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2022/7/11 23:32
 */
@Getter
@Setter
public class TtsConfig {

    private String url;
    private String apiKey1;
    private String apiKey2;
    private String apiKey3;
    private String apiKey4;
    private String apiKey5;
    private String apiKey6;
    private String apiKey7;

    public Set<String> listApiKey() {
        return SetUtils.unmodifiableSet(apiKey1, apiKey2, apiKey3, apiKey4, apiKey5, apiKey6, apiKey7);
    }

}
