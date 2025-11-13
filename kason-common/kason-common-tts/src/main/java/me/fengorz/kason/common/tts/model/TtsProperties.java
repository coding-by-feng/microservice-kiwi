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

package me.fengorz.kason.common.tts.model;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.SetUtils;

import java.util.Set;

/**
 * @Description TODO
 * @Author Kason Zhan
 * @Date 2022/7/11 23:32
 */
@Getter
@Setter
public class TtsProperties {

    private String url;
    private String apiKey1;
    private String apiKey2;
    private String apiKey3;
    private String apiKey4;
    private String apiKey5;
    private String apiKey6;
    private String apiKey7;
    private String apiKey8;
    private String apiKey9;
    private String apiKey10;
    private String apiKey11;
    private String apiKey12;
    private String apiKey13;
    private String apiKey14;
    private String apiKey15;
    private String apiKey16;
    private String apiKey17;
    private String apiKey18;
    private String apiKey19;
    private String apiKey20;
    private String apiKey21;
    private String apiKey22;

    public Set<String> listApiKey() {
        return SetUtils.unmodifiableSet(apiKey1, apiKey2, apiKey3, apiKey4, apiKey5, apiKey6,
                apiKey7, apiKey8, apiKey9, apiKey10, apiKey11, apiKey12, apiKey13, apiKey14,
                apiKey15, apiKey16, apiKey17, apiKey19, apiKey20, apiKey21, apiKey22);
    }

}
