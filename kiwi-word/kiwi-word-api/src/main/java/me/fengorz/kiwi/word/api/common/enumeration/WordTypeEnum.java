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

package me.fengorz.kiwi.word.api.common.enumeration;

import lombok.Getter;

/**
 * @Author Kason Zhan
 * @Date 2022/12/6 21:14
 */
public enum WordTypeEnum {

    WORD(1), PHRASE(2);

    @Getter
    private final int type;

    WordTypeEnum(int type) {
        this.type = type;
    }
}
