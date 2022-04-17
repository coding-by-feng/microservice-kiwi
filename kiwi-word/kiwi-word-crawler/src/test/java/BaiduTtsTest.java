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

import java.io.IOException;
import java.util.HashMap;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;

import com.baidu.aip.speech.AipSpeech;
import com.baidu.aip.speech.TtsResponse;
import com.baidu.aip.util.Util;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2022/1/10 10:02 PM
 */
public class BaiduTtsTest {

    public static final String APP_ID = "20116041";
    public static final String API_KEY = "lTZsmSq37OtRKj6WDpjeemip";
    public static final String SECRET_KEY = "iQkIhGf6O93FH8Dhe446w8rB3jd2A137";

    @BeforeEach
    private void beforeEach() {}

    @Test
    public void test() {
        // 初始化一个AipSpeech
        AipSpeech client = new AipSpeech(APP_ID, API_KEY, SECRET_KEY);

        // 可选：设置网络连接参数
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);

        HashMap<String, Object> options = new HashMap<>();
        options.put("spd", "5");
        options.put("pit", "5");
        options.put("per", "5");

        TtsResponse res = client.synthesis(
            " A。R。E。F。E。R。L。a way of discovering, by questions or practical activities, what someone knows, or what someone or something can do or is like",
            "zh", 1, options);
        byte[] data = res.getData();
        JSONObject res1 = res.getResult();
        if (data != null) {
            try {
                Util.writeBytesToFileSystem(data, "output.mp3");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (res1 != null) {
            System.out.println(res1.toString(2));
        }
    }
}
