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

package me.fengorz.finance;

import lombok.extern.slf4j.Slf4j;

/**
* @Author zhanshifeng
 * @Date 2020/8/3 10:08 AM
 */
@Slf4j
public class MonetaryETFFundTool {

    private MonetaryETFFundTool() {
    }

    public static final MonetaryETFFundTool me = new MonetaryETFFundTool();

    public void calAnotherFallPoint(String positionPrice, String anotherPrice) {
        double result = (Double.parseDouble(anotherPrice) - Double.parseDouble(positionPrice)) / 100D;
        log.info("calAnotherFallPoint = {}%", result);
    }

}