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

package me.fengorz.kiwi.bdf.security.component;

import cn.hutool.http.HttpStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.ResultCode;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 客户端异常处理 1. 可以根据 AuthenticationException 不同细化异常处理
 *
 * @Author zhanshifeng
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceAuthExceptionEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
        AuthenticationException authException) throws IOException {
        response.setCharacterEncoding(CommonConstants.UTF8);
        response.setContentType(CommonConstants.CONTENT_TYPE);
        R<String> result = R.failed();
        result.setCode(ResultCode.build(() -> HttpStatus.HTTP_UNAUTHORIZED, null).getCode());
        if (authException != null) {
            result.setMsg("error");
            result.setData(authException.getMessage());
        }
        response.setStatus(HttpStatus.HTTP_UNAUTHORIZED);
        PrintWriter printWriter = response.getWriter();
        printWriter.append(objectMapper.writeValueAsString(result));
    }
}
