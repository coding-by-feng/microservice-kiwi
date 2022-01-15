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

package me.fengorz.kiwi.bdf.exception.handler;

import com.github.tobato.fastdfs.exception.FdfsServerException;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.api.R;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;
import me.fengorz.kiwi.common.sdk.exception.BaseRuntimeException;
import me.fengorz.kiwi.common.sdk.exception.ResourceNotFoundException;
import me.fengorz.kiwi.common.sdk.exception.ServiceException;
import me.fengorz.kiwi.common.sdk.exception.dfs.DfsOperateDeleteException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

/** @Description 全局的异常处理器 @Author zhanshifeng @Date 2019/11/26 3:28 PM */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static String getLogMessage(MethodArgumentNotValidException e) {
    FieldError fieldError = Objects.requireNonNull(e.getBindingResult().getFieldError());
    return fieldError.getObjectName()
        + GlobalConstants.SYMBOL_DOT
        + fieldError.getField()
        + GlobalConstants.SYMBOL_SQUARE_BRACKET_LEFT
        + fieldError.getDefaultMessage()
        + GlobalConstants.SYMBOL_SQUARE_BRACKET_RIGHT;
  }

  private static String getLogMessage(BindException e) {
    FieldError fieldError = Objects.requireNonNull(e.getBindingResult().getFieldError());
    return fieldError.getObjectName()
        + GlobalConstants.SYMBOL_DOT
        + fieldError.getField()
        + GlobalConstants.SYMBOL_SQUARE_BRACKET_LEFT
        + fieldError.getDefaultMessage()
        + GlobalConstants.SYMBOL_SQUARE_BRACKET_RIGHT;
  }

  @ExceptionHandler({Exception.class})
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public R<String> handleException(BaseRuntimeException e) {
    log.error("global exception:{}", e.getMessage(), e);
    return R.error(e.getMessage());
  }

  @ExceptionHandler({BaseRuntimeException.class})
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public R<String> handleBaseRuntimeException(BaseRuntimeException e) {
    log.error("global BaseRuntimeException:{}", e.getMessage(), e);
    return R.error(e.getMessage());
  }

  @ExceptionHandler({ServiceException.class})
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public R<String> handleServiceRuntimeException(ServiceException e) {
    log.error("global ServiceException:{}", e.getMessage());
    return R.error(e.getMessage());
  }

  @ExceptionHandler({DfsOperateDeleteException.class})
  @ResponseStatus(HttpStatus.CREATED)
  public R<String> handleDfsOperateDeleteException(DfsOperateDeleteException e) {
    log.error("global DfsOperateDeleteException:{}", e.getMessage());
    return R.failed(e.getMessage());
  }

  @ExceptionHandler({ResourceNotFoundException.class})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public R<String> handleResourceNotFoundException(ResourceNotFoundException e) {
    log.error("global ResourceNotFoundException:{}", e.getMessage());
    return R.failed(e.getMessage());
  }

  @ExceptionHandler({FdfsServerException.class})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public R<String> handleFdfsServerException(FdfsServerException e) {
    log.error("global FdfsServerException:{}", e.getMessage());
    return R.failed(e.getMessage());
  }

  @ExceptionHandler({MethodArgumentNotValidException.class})
  public R<String> handleValidException(MethodArgumentNotValidException e) {
    // 日志记录错误信息
    final String errorMessage = getLogMessage(e);
    log.error(errorMessage);
    // 将错误信息返回给前台
    return R.failed(errorMessage);
  }

  @ExceptionHandler({BindException.class})
  public R<String> handleValidException(BindException e) {
    // 日志记录错误信息
    final String errorMessage = getLogMessage(e);
    log.error(errorMessage);
    // 将错误信息返回给前台
    return R.failed(errorMessage);
  }
}
