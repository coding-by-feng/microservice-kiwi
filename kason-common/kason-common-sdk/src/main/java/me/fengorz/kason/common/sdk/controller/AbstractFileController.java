/*
 *
 * Copyright [2019~2025] [zhanshifeng]
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

package me.fengorz.kason.common.sdk.controller;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static me.fengorz.kason.common.sdk.web.WebTools.*;

/**
 * @Description 抽象控制层基类 @Author Kason Zhan @Date 2020/4/21 7:28 PM
 */
public abstract class AbstractFileController extends BaseController {

    protected InputStream buildInputStream(HttpServletResponse response, byte[] bytes) {
        InputStream inputStream;
        inputStream = new ByteArrayInputStream(bytes);
        response.addHeader(CONTENT_TYPE, AUDIO_MPEG);
        response.addHeader(ACCEPT_RANGES, BYTES);
        response.addHeader(CONTENT_LENGTH, String.valueOf(bytes.length));
        return inputStream;
    }

}
