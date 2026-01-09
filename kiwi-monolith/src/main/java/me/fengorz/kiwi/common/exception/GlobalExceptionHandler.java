/*
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.fengorz.kiwi.common.exception;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.R;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

/**
 * Global Exception Handler
 *
 * @author codingByFeng
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<String> handleAuthException(AuthException e) {
        log.warn("AuthException: {}", e.getMessage());
        return R.failed(HttpStatus.FORBIDDEN.value(), e.getMessage());
    }

    @ExceptionHandler(ServiceException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<String> handleServiceException(ServiceException e) {
        log.error("ServiceException: {}", e.getMessage(), e);
        return R.failed(e.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<String> handleResourceNotFoundException(ResourceNotFoundException e) {
        log.warn("ResourceNotFoundException: {}", e.getMessage());
        return R.failed(HttpStatus.NOT_FOUND.value(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<String> handleValidException(MethodArgumentNotValidException e) {
        FieldError fieldError = Objects.requireNonNull(e.getBindingResult().getFieldError());
        String errorMessage = fieldError.getField() + ": " + fieldError.getDefaultMessage();
        log.warn("Validation error: {}", errorMessage);
        return R.failed(HttpStatus.BAD_REQUEST.value(), errorMessage);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<String> handleBindException(BindException e) {
        FieldError fieldError = Objects.requireNonNull(e.getBindingResult().getFieldError());
        String errorMessage = fieldError.getField() + ": " + fieldError.getDefaultMessage();
        log.warn("Bind error: {}", errorMessage);
        return R.failed(HttpStatus.BAD_REQUEST.value(), errorMessage);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<String> handleException(Exception e) {
        log.error("Unexpected exception: {}", e.getMessage(), e);
        return R.failed(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error");
    }
}
