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

import org.junit.Test;

import me.fengorz.kiwi.vocabulary.crawler.service.baidu.sdk.TransApi;

/**
 * TransApi Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since
 * 
 *        <pre>
 * Nov 20, 2020
 *        </pre>
 */
public class TransApiTest {

    // 在平台申请的APP_ID 详见 http://api.fanyi.baidu.com/api/trans/product/desktop?req=developer
    private static final String APP_ID = "20201110000613546";
    private static final String SECURITY_KEY = "X8KDg_X0ji6XUiSx_gLu";

    @Test
    public void test() {
        TransApi api = new TransApi(APP_ID, SECURITY_KEY);

        String query = "apple";
        System.out.println("----------------------->>>>>>>>>>>>>>>>>>>>>");
        System.out.println(api.getTransResult(query, "en", "zh"));
    }
}
