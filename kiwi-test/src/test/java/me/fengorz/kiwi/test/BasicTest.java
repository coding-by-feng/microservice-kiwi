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

package me.fengorz.kiwi.test;

import org.junit.jupiter.api.Test;

/**
 * @Description TODO
 * @Author Kason Zhan
 * @Date 2022/11/16 20:48
 */
public class BasicTest {

    @Test
    public void test_ternaryOperator() {
        final Integer a = 1;
        final Integer b = 2;
        final Integer c = null;
        Integer result = false ? a * b : c;
    }

    // @Test
    public void test_switch() {
        final String test = null;
        switch (test) {
            case "1":
                System.out.println("1");
                break;
            default:
                System.out.println("2");
        }
    }

}
