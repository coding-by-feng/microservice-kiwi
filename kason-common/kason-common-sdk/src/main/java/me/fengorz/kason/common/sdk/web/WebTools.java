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

package me.fengorz.kason.common.sdk.web;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kason.common.sdk.constant.SecurityConstants;
import me.fengorz.kason.common.sdk.exception.AuthException;
import me.fengorz.kason.common.sdk.exception.ResourceNotFoundException;
import me.fengorz.kason.common.sdk.exception.ServiceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.util.WebUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * @Author Kason Zhan @Date 2019-09-07 21:11
 */
@Slf4j
public class WebTools extends WebUtils {

    private static final int IN_READ_BYTES_LENGTH = 1024;

    /**
     * 从request获取Authorization并解密
     *
     * @param request
     * @return
     */
    public static String decodeAuthorization(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (!StrUtil.startWith(authorization, SecurityConstants.KEY_HEADER_BASIC_)) {
            throw new AuthException("请求头中client信息不能为空");
        }

        byte[] decoded;

        try {
            byte[] authorizationBytes = authorization.substring(6).getBytes(StandardCharsets.UTF_8);
            decoded = Base64.decode(authorizationBytes);
        } catch (Exception e) {
            throw new AuthException("Failed to decode basic authentication token");
        }

        return new String(decoded, StandardCharsets.UTF_8);
    }

    public static void downloadResponseAndClose(HttpServletResponse response, InputStream inputStream) {
        downloadResponseAndClose(response, inputStream, false);
    }

    public static void downloadResponseAndClose(HttpServletResponse response, InputStream inputStream, boolean isCountLength) {
        if (inputStream == null) {
            throw new ResourceNotFoundException("inputStream cannot be null!");
        }

        ServletOutputStream temps = null;
        DataInputStream in = null;
        int readLengthTotal = 0;
        try {
            temps = response.getOutputStream();
            in = new DataInputStream(inputStream);
            // 这个方法写入音频流时有个致命问题，如果是音频流会出现尾部有杂音，因为2048如果尾部空流在音频当还是会被当做声音处理
            // 如果采用下面注释掉的这种写法的话
            byte[] b = new byte[IN_READ_BYTES_LENGTH];
            int readLength;
            do {
                readLength = in.read(b);
                readLengthTotal += readLength;
                if (readLength > 0 && readLength < IN_READ_BYTES_LENGTH) {
                    byte[] minB = new byte[readLength];
                    System.arraycopy(b, 0, minB, 0, readLength);
                    temps.write(minB);
                } else if (readLength == IN_READ_BYTES_LENGTH) {
                    temps.write(b);
                } else {
                    break;
                }
                temps.flush();
            } while (readLength == IN_READ_BYTES_LENGTH);

            if (isCountLength) {
                response.addHeader(CONTENT_TYPE, AUDIO_MPEG);
                response.addHeader(ACCEPT_RANGES, BYTES);
                response.addHeader(CONTENT_LENGTH, String.valueOf(readLengthTotal));
            }
        } catch (IOException e) {
            log.error("WebTools downloadResponse occurred error, cause of the error is {}", e.getMessage());
        } finally {
            if (temps != null) {
                try {
                    temps.close();
                } catch (IOException e) {
                    log.error("temps close exception", e);
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.error("in close exception", e);
                }
            }
            try {
                inputStream.close();
            } catch (IOException e) {
                log.error("inputStream close exception", e);
            }
        }
    }

    public static int deductCurrent(int current) {
        if (current > 0) {
            return current - 1;
        }
        return 0;
    }

    public static String decode(String originalText) {
        try {
            return URLDecoder.decode(originalText, String.valueOf(StandardCharsets.UTF_8));
        } catch (UnsupportedEncodingException e) {
            throw new ServiceException("Unable to decode original text", e);
        }
    }


    public static final String CONTENT_TYPE = "Content-Type";
    public static final String AUDIO_MPEG = "audio/mpeg";
    public static final String ACCEPT_RANGES = "Accept-Ranges";
    public static final String BYTES = "bytes";
    public static final String CONTENT_LENGTH = "Content-Length";

}
