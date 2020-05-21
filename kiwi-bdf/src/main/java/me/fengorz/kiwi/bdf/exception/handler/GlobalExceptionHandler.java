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

package me.fengorz.kiwi.bdf.exception.handler;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.api.constant.CommonConstants;
import me.fengorz.kiwi.common.api.exception.BaseException;
import me.fengorz.kiwi.common.api.exception.dfs.DfsOperateException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

/**
 * @Description 全局的异常处理器
 * @Author zhanshifeng
 * @Date 2019/11/26 3:28 PM
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({Exception.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R exception(BaseException e) {
        log.error("global exception:{}", e.getMessage(), e);
        return R.failed(e.getMessage());
    }

    @ExceptionHandler({DfsOperateException.class})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public R dfsException(BaseException e) {
        log.error("dfsException:{}", e.getMessage(), e);
        return R.ok(e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class})
    public R handleValidException(MethodArgumentNotValidException e) {
        //日志记录错误信息
        final String errorMessage = getLogMessage(e);
        log.error(errorMessage);
        //将错误信息返回给前台
        return R.failed(CommonConstants.FAIL, errorMessage);
    }

    @ExceptionHandler({BindException.class})
    public R handleValidException(BindException e) {
        //日志记录错误信息
        final String errorMessage = getLogMessage(e);
        log.error(errorMessage);
        //将错误信息返回给前台
        return R.failed(CommonConstants.FAIL, errorMessage);
    }

    private static String getLogMessage(MethodArgumentNotValidException e) {
        FieldError fieldError = Objects.requireNonNull(e.getBindingResult().getFieldError());
        return new StringBuilder().append(fieldError.getObjectName()).append(CommonConstants.DOT).append(fieldError.getField())
                .append(CommonConstants.SQUARE_BRACKET_LEFT).append(fieldError.getDefaultMessage()).append(CommonConstants.SQUARE_BRACKET_RIGHT)
                .toString();
    }

    private static String getLogMessage(BindException e) {
        FieldError fieldError = Objects.requireNonNull(e.getBindingResult().getFieldError());
        return new StringBuilder().append(fieldError.getObjectName()).append(CommonConstants.DOT).append(fieldError.getField())
                .append(CommonConstants.SQUARE_BRACKET_LEFT).append(fieldError.getDefaultMessage()).append(CommonConstants.SQUARE_BRACKET_RIGHT)
                .toString();
    }

}
