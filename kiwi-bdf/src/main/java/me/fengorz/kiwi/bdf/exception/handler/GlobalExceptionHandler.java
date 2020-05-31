/*
 *
 *   Copyright [2019~2025] [zhanshifeng]
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
import me.fengorz.kiwi.common.api.exception.BaseRuntimeException;
import me.fengorz.kiwi.common.api.exception.ResourceNotFoundException;
import me.fengorz.kiwi.common.api.exception.ServiceException;
import me.fengorz.kiwi.common.api.exception.dfs.DfsOperateDeleteException;
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
    public R handleException(BaseRuntimeException e) {
        log.error("global exception:{}", e.getMessage(), e);
        return R.error(e.getMessage());
    }

    @ExceptionHandler({BaseRuntimeException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R handleBaseRuntimeException(BaseRuntimeException e) {
        log.error("global BaseRuntimeException:{}", e.getMessage(), e);
        return R.error(e.getMessage());
    }

    @ExceptionHandler({ServiceException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R handleServiceRuntimeException(ServiceException e) {
        log.error("global ServiceException:{}", e.getMessage(), e);
        return R.error(e.getMessage());
    }

    @ExceptionHandler({DfsOperateException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R handleDfsOperateException(DfsOperateException e) {
        log.error("global DfsOperateException:{}", e.getMessage(), e);
        return R.error(e.getMessage());
    }

    @ExceptionHandler({DfsOperateDeleteException.class})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public R handleDfsOperateDeleteException(DfsOperateDeleteException e) {
        log.error("global DfsOperateDeleteException:{}", e.getMessage(), e);
        return R.failed(e.getMessage());
    }

    @ExceptionHandler({ResourceNotFoundException.class})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public R handleResourceNotFoundException(ResourceNotFoundException e) {
        log.error("global ResourceNotFoundException:{}", e.getMessage(), e);
        return R.failed(e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class})
    public R handleValidException(MethodArgumentNotValidException e) {
        //日志记录错误信息
        final String errorMessage = getLogMessage(e);
        log.error(errorMessage);
        //将错误信息返回给前台
        return R.failed(errorMessage);
    }

    @ExceptionHandler({BindException.class})
    public R handleValidException(BindException e) {
        //日志记录错误信息
        final String errorMessage = getLogMessage(e);
        log.error(errorMessage);
        //将错误信息返回给前台
        return R.failed(errorMessage);
    }

    private static String getLogMessage(MethodArgumentNotValidException e) {
        FieldError fieldError = Objects.requireNonNull(e.getBindingResult().getFieldError());
        return new StringBuilder().append(fieldError.getObjectName()).append(CommonConstants.SYMBOL_DOT).append(fieldError.getField())
                .append(CommonConstants.SYMBOL_SQUARE_BRACKET_LEFT).append(fieldError.getDefaultMessage()).append(CommonConstants.SYMBOL_SQUARE_BRACKET_RIGHT)
                .toString();
    }

    private static String getLogMessage(BindException e) {
        FieldError fieldError = Objects.requireNonNull(e.getBindingResult().getFieldError());
        return new StringBuilder().append(fieldError.getObjectName()).append(CommonConstants.SYMBOL_DOT).append(fieldError.getField())
                .append(CommonConstants.SYMBOL_SQUARE_BRACKET_LEFT).append(fieldError.getDefaultMessage()).append(CommonConstants.SYMBOL_SQUARE_BRACKET_RIGHT)
                .toString();
    }

}
