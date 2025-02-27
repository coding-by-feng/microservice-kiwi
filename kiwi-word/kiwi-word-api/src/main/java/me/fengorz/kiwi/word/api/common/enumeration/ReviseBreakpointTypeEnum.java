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

/**
 * @Description Enumeration classes for different types of breakpoint records in review mode.
 * @Author Kason Zhan
 * @Date 2021/8/22 7:27 PM
 */
public enum ReviseBreakpointTypeEnum {
    REMEMBER(1), KEEP_IN_MIND(2);

    /**
     * 1：remember 2：keep in mind
     */
    private final int type;

    ReviseBreakpointTypeEnum(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
