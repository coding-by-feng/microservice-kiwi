/*
 *
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 *
 */

package me.fengorz.kiwi.word.biz;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * @Description TODO
 * @Author Kason Zhan
 * @Date 2022/8/8 21:12
 */
@Slf4j
@Disabled
public class TempTest {

    @Test
    public void test() {
        // List<String> list = new ArrayList<>();
        // list.add("1");
        // list.add("2");
        // Iterator var2 = list.iterator();
        // while(var2.hasNext()) {
        // String item = (String)var2.next();
        // if ("2".equals(item)) {
        // list.remove(item);
        // }
        // }
        //
        // Assertions.assertEquals(1, list.size());
        // Assertions.assertEquals("2", list.get(0));

        // byte[] bytes1 = {10, 10};
        // byte[] bytes2 = {20, 20};
        // Assertions.assertEquals(KiwiArrayUtils.sumBytesLength(bytes1, bytes2), 4);
        // Assertions.assertEquals(KiwiArrayUtils.sumBytes(bytes1, bytes2), 60);

        log.info("test {}", ZonedDateTime.of(LocalDateTime.now().plusYears(1), ZoneId.of("GMT"))
                .format(GlobalConstants.HEADERS.HEADER_EXPIRES_TIME_FORMATTER));
    }

}
