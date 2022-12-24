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

package me.fengorz.kiwi.common.tts.service.impl;

import cn.hutool.http.HttpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.exception.tts.TtsException;
import me.fengorz.kiwi.common.tts.TtsConstants;
import me.fengorz.kiwi.common.tts.enumeration.TtsSourceEnum;
import me.fengorz.kiwi.common.tts.model.BaiduTtsProperties;
import me.fengorz.kiwi.common.tts.service.BaiduTtsService;
import okhttp3.*;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

@Slf4j
@Service(TtsConstants.BEAN_NAMES.BAIDU_TTS_WEB_API_SERVICE_IMPL)
@RequiredArgsConstructor
public class BaiduTtsWebApiServiceImpl implements BaiduTtsService {

    private final BaiduTtsProperties config;
    // TODO ZSF Setup for the mutilple threads call.
    private final OkHttpClient client = new OkHttpClient();

    private static final String BAIDU_TTS_URL = "https://tsn.baidu.com";
    private static final String BAIDU_TTS_TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";

    @Override
    public byte[] speech(String text) throws TtsException {

        RequestBody formBody = new FormBody.Builder()
                .add(TtsConstants.BAIDU_TTS_OPTIONS.ACCESS_TOKEN, config.getAccessToken())
                .add(TtsConstants.BAIDU_TTS_OPTIONS.SPEAK_SPEED, SPEAK_SPEED_DEFAULT_VALUE)
                .add(TtsConstants.BAIDU_TTS_OPTIONS.SPEAK_TONE, SPEAK_TONE_DEFAULT_VALUE)
                .add(TtsConstants.BAIDU_TTS_OPTIONS.SPEAK_PER, SPEAK_PER_DEFAULT_VALUE_DUXIAOMEI)
                .add(TtsConstants.BAIDU_TTS_OPTIONS.SPEAK_AUE, SPEAK_AUE_DEFAULT_VALUE)
                .build();

        Request request = new Request.Builder()
                .url(BAIDU_TTS_URL)
                .post(formBody)
                .build();

        Call call = client.newCall(request);
        try {
            Response response = call.execute();
            return Objects.requireNonNull(response.body()).bytes();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new TtsException("%s TTS speech failed, text=%s", TtsSourceEnum.BAIDU.getSource(), text);
        }
    }

    /**
     * 从用户的AK，SK生成鉴权签名（Access Token）
     *
     * @return 鉴权签名（Access Token）
     * @throws IOException IO异常
     */
    @Deprecated
    private String getAccessToken() {
        HashMap<String, Object> options = new HashMap<>(3);
        options.put("grant_type", "client_credentials");
        options.put("client_id", config.getApiKey());
        options.put("client_secret", config.getSecretKey());

        return new JSONObject(HttpUtil.post(BAIDU_TTS_TOKEN_URL, options)).getString("access_token");
    }

}